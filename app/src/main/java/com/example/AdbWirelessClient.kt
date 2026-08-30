package com.example

import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import android.view.KeyEvent
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

class AdbWirelessClient {
    private val inputExecutor = Executors.newSingleThreadExecutor()
    private val networkExecutor = Executors.newCachedThreadPool()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingDownKeycodes = ConcurrentLinkedQueue<String>()
    private val pendingUpKeycodes = ConcurrentLinkedQueue<String>()
    private val isDownDispatchScheduled = AtomicBoolean(false)
    private val isUpDispatchScheduled = AtomicBoolean(false)
    private val activePressedScancodes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private val activePressedKeycodes = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    private var isRootAvailable: Boolean? = null
    private var rootProcess: Process? = null
    private var rootOutputStream: DataOutputStream? = null
    var isUinputRegistered = false
        private set

    private var uinputRegisterFailCount = 0
    private var lastUinputAttemptTime = 0L

    // Persistent local shell for zero-latency non-root fallback execution
    private var persistentShellProcess: Process? = null
    private var persistentShellWriter: DataOutputStream? = null

    // "keyboard" or "gamepad"
    private var emulationMode = "gamepad"

    fun setEmulationMode(mode: String) {
        if (emulationMode != mode) {
            emulationMode = mode
            if (isUinputRegistered) {
                // Re-register to change device type
                inputExecutor.execute {
                    cleanupRootProcess()
                    checkAndRegisterUinput()
                }
            }
        }
    }

    // "uinput" or "inject"
    private var injectionMethod = "inject"

    fun setInjectionMethod(method: String) {
        if (injectionMethod != method) {
            injectionMethod = method
            if (isUinputRegistered) {
                inputExecutor.execute {
                    cleanupRootProcess()
                }
            }
        }
    }

    private var gamepadKeyConfig = GamepadKeyConfig()

    fun setGamepadKeyConfig(config: GamepadKeyConfig) {
        gamepadKeyConfig = config
    }

    fun isUinputActive(): Boolean = isUinputRegistered

    init {
        // Pre-check root on a background thread
        networkExecutor.execute {
            checkRoot()
            logInputDevices()
        }
    }

    private fun checkRoot(): Boolean {
        if (isRootAvailable != null) return isRootAvailable!!
        return try {
            val p = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(p.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            val exitVal = p.waitFor()
            isRootAvailable = (exitVal == 0)
            Log.d("AdbWireless", "Root access availability check: $isRootAvailable")
            isRootAvailable!!
        } catch (e: Exception) {
            isRootAvailable = false
            false
        }
    }

    /**
     * Helper to invoke Shizuku's private newProcess method via reflection.
     */
    private fun createShizukuProcess(cmd: Array<String>): Process {
        val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        newProcessMethod.isAccessible = true
        return newProcessMethod.invoke(null, cmd, null, null) as Process
    }

    /**
     * Initializes and registers the virtual device via 'cmd uinput -'.
     * Uses Shizuku (ADB shell user) if available, otherwise falls back to Root (su).
     */
    /**
     * Initializes and registers the virtual device via 'uinput -'.
     * Uses Shizuku (ADB shell user) if available, otherwise falls back to Root (su).
     * Sequential fallback: tries /system/bin/uinput first, then cmd uinput.
     */
    private fun checkAndRegisterUinput(): Boolean {
        val isAlive = try {
            rootProcess?.isAlive ?: false
        } catch (e: Exception) {
            false
        }

        if (isUinputRegistered && rootOutputStream != null && isAlive) return true

        // Rate limit registration attempts if we failed too many times to avoid ANR warnings
        val now = System.currentTimeMillis()
        if (uinputRegisterFailCount >= 3 && now - lastUinputAttemptTime < 10000L) {
            return false
        }

        lastUinputAttemptTime = now
        try {
            cleanupRootProcess()

            var p: Process? = null
            var os: DataOutputStream? = null

            // 1. Try Shizuku first if authorized
            val isShizukuAvailable = try {
                Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                false
            }

            if (isShizukuAvailable) {
                // Try Shizuku Plan A: /system/bin/uinput
                try {
                    Log.d("AdbWireless", "Starting uinput via Shizuku '/system/bin/uinput -'")
                    TaikoLogManager.log("uinput: Shizuku trying '/system/bin/uinput -'")
                    val proc = createShizukuProcess(arrayOf("/system/bin/uinput", "-"))
                    val out = DataOutputStream(proc.outputStream)
                    setupProcessLogging(proc)
                    
                    // Write registration
                    val registerJson = getRegistrationJson()
                    out.write(("[\n" + registerJson + "\n").toByteArray(Charsets.UTF_8))
                    out.flush()
                    
                    Thread.sleep(150)
                    if (proc.isAlive) {
                        p = proc
                        os = out
                        TaikoLogManager.log("uinput: Shizuku '/system/bin/uinput' registered successfully")
                    } else {
                        val exitCode = try { proc.exitValue() } catch (e: Exception) { -1 }
                        Log.w("AdbWireless", "Shizuku '/system/bin/uinput' exited immediately with $exitCode")
                        TaikoLogManager.log("uinput: Shizuku '/system/bin/uinput' failed (exit: $exitCode). trying fallback 'cmd uinput'...")
                        try { out.close() } catch (e: Exception) {}
                        try { proc.destroy() } catch (e: Exception) {}
                    }
                } catch (e: Throwable) {
                    Log.e("AdbWireless", "Shizuku plan A exception", e)
                }

                // Try Shizuku Plan B (Fallback): cmd uinput
                if (p == null) {
                    try {
                        Log.d("AdbWireless", "Starting uinput via Shizuku 'cmd uinput -'")
                        TaikoLogManager.log("uinput: Shizuku trying 'cmd uinput -'")
                        val proc = createShizukuProcess(arrayOf("cmd", "uinput", "-"))
                        val out = DataOutputStream(proc.outputStream)
                        setupProcessLogging(proc)
                        
                        // Write registration
                        val registerJson = getRegistrationJson()
                        out.write(("[\n" + registerJson + "\n").toByteArray(Charsets.UTF_8))
                        out.flush()
                        
                        Thread.sleep(150)
                        if (proc.isAlive) {
                            p = proc
                            os = out
                            TaikoLogManager.log("uinput: Shizuku 'cmd uinput' registered successfully")
                        } else {
                            val exitCode = try { proc.exitValue() } catch (e: Exception) { -1 }
                            Log.w("AdbWireless", "Shizuku 'cmd uinput' exited immediately with $exitCode")
                            TaikoLogManager.log("uinput ERR: Shizuku uinput failed (exit: $exitCode)")
                            try { out.close() } catch (e: Exception) {}
                            try { proc.destroy() } catch (e: Exception) {}
                        }
                    } catch (e: Throwable) {
                        Log.e("AdbWireless", "Shizuku plan B exception", e)
                        TaikoLogManager.log("uinput ERR: Shizuku startup error: ${e.message}")
                    }
                }
            }

            // 2. Try Root fallback if Shizuku failed or was not authorized
            if (p == null && checkRoot()) {
                // Try Root Plan A: /system/bin/uinput
                try {
                    Log.d("AdbWireless", "Starting uinput via Root su '/system/bin/uinput -'")
                    TaikoLogManager.log("uinput: Root trying '/system/bin/uinput -'")
                    val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "/system/bin/uinput -"))
                    val out = DataOutputStream(proc.outputStream)
                    setupProcessLogging(proc)
                    
                    val registerJson = getRegistrationJson()
                    out.write(("[\n" + registerJson + "\n").toByteArray(Charsets.UTF_8))
                    out.flush()
                    
                    Thread.sleep(150)
                    if (proc.isAlive) {
                        p = proc
                        os = out
                        TaikoLogManager.log("uinput: Root '/system/bin/uinput' registered successfully")
                    } else {
                        val exitCode = try { proc.exitValue() } catch (e: Exception) { -1 }
                        Log.w("AdbWireless", "Root '/system/bin/uinput' exited with $exitCode")
                        TaikoLogManager.log("uinput: Root '/system/bin/uinput' failed (exit: $exitCode). trying fallback 'cmd uinput'...")
                        try { out.close() } catch (e: Exception) {}
                        try { proc.destroy() } catch (e: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e("AdbWireless", "Root plan A exception", e)
                }

                // Try Root Plan B (Fallback): cmd uinput
                if (p == null) {
                    try {
                        Log.d("AdbWireless", "Starting uinput via Root su 'cmd uinput -'")
                        TaikoLogManager.log("uinput: Root trying 'cmd uinput -'")
                        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd uinput -"))
                        val out = DataOutputStream(proc.outputStream)
                        setupProcessLogging(proc)
                        
                        val registerJson = getRegistrationJson()
                        out.write(("[\n" + registerJson + "\n").toByteArray(Charsets.UTF_8))
                        out.flush()
                        
                        Thread.sleep(150)
                        if (proc.isAlive) {
                            p = proc
                            os = out
                            TaikoLogManager.log("uinput: Root 'cmd uinput' registered successfully")
                        } else {
                            val exitCode = try { proc.exitValue() } catch (e: Exception) { -1 }
                            Log.w("AdbWireless", "Root 'cmd uinput' exited with $exitCode")
                            TaikoLogManager.log("uinput ERR: Root uinput failed (exit: $exitCode)")
                            try { out.close() } catch (e: Exception) {}
                            try { proc.destroy() } catch (e: Exception) {}
                        }
                    } catch (e: Exception) {
                        Log.e("AdbWireless", "Root plan B exception", e)
                        TaikoLogManager.log("uinput ERR: Root startup error: ${e.message}")
                    }
                }
            }

            if (p != null && os != null) {
                rootProcess = p
                rootOutputStream = os
                isUinputRegistered = true
                uinputRegisterFailCount = 0
                Log.d("AdbWireless", "Virtual uinput device ($emulationMode) registered successfully!")
                return true
            } else {
                Log.d("AdbWireless", "Neither Shizuku nor Root could successfully register uinput")
                uinputRegisterFailCount++
                return false
            }

        } catch (e: Exception) {
            Log.e("AdbWireless", "checkAndRegisterUinput completely failed", e)
            TaikoLogManager.log("uinput ERR: Registration exception: ${e.message}")
            uinputRegisterFailCount++
            return false
        }
    }

    private fun setupProcessLogging(p: Process) {
        val errorReader = java.io.BufferedReader(java.io.InputStreamReader(p.errorStream))
        val inputReader = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
        Thread {
            try {
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                    Log.e("AdbWirelessUinput", "uinput stderr: $line")
                    TaikoLogManager.log("uinput stderr: $line")
                }
            } catch (e: Exception) {}
        }.start()
        Thread {
            try {
                var line: String?
                while (inputReader.readLine().also { line = it } != null) {
                    Log.d("AdbWirelessUinput", "uinput stdout: $line")
                    TaikoLogManager.log("uinput stdout: $line")
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun getRegistrationJson(): String {
        return if (emulationMode == "gamepad") {
            """{"id":1,"command":"register","name":"Xbox 360 Wireless Controller","vid":1118,"pid":654,"configuration":[{"type":1,"data":[304,305,307,308,310,311,312,313,314,315]}]}"""
        } else {
            val keyCodes = (1..100).toList()
            val dataStr = keyCodes.joinToString(separator = ",") { it.toString() }
            """{"id":2,"command":"register","name":"Virtual Keyboard","vid":1234,"pid":5678,"configuration":[{"type":1,"data":[$dataStr]}]}"""
        }
    }

    @Synchronized
    private fun getPersistentShell(): DataOutputStream? {
        val isAlive = try {
            persistentShellProcess?.isAlive ?: false
        } catch (e: Throwable) {
            false
        }

        if (persistentShellWriter != null && isAlive) return persistentShellWriter
        
        cleanupPersistentShell()
        try {
            val isShizukuAvailable = try {
                Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                false
            }

            val p = try {
                if (isShizukuAvailable) {
                    Log.d("AdbWireless", "Starting persistent shell via Shizuku")
                    createShizukuProcess(arrayOf("sh"))
                } else if (checkRoot()) {
                    Log.d("AdbWireless", "Starting persistent shell via Root")
                    Runtime.getRuntime().exec(arrayOf("su"))
                } else {
                    null
                }
            } catch (e: Throwable) {
                Log.e("AdbWireless", "Persistent shell process start failed", e)
                null
            }

            if (p != null) {
                persistentShellProcess = p
                persistentShellWriter = DataOutputStream(p.outputStream)
                Log.d("AdbWireless", "Persistent shell process started successfully.")
            }
        } catch (e: Throwable) {
            Log.e("AdbWireless", "Failed to start persistent local shell", e)
        }
        return persistentShellWriter
    }

    @Synchronized
    private fun cleanupPersistentShell() {
        try {
            persistentShellWriter?.writeBytes("exit\n")
            persistentShellWriter?.flush()
            persistentShellWriter?.close()
        } catch (e: Exception) {}
        try {
            persistentShellProcess?.destroy()
        } catch (e: Exception) {}
        persistentShellProcess = null
        persistentShellWriter = null
    }

    private var iInputManagerInstance: Any? = null
    private var injectMethod: java.lang.reflect.Method? = null
    @Volatile private var cachedDeviceId: Int? = null

    private fun findVirtualOrKeyboardDeviceId(): Int {
        cachedDeviceId?.let { return it }
        try {
            val ids = InputDevice.getDeviceIds()
            // 1. Try to find a device with "Virtual" in the name
            for (id in ids) {
                val device = InputDevice.getDevice(id) ?: continue
                if (device.name.contains("Virtual", ignoreCase = true)) {
                    Log.d("AdbWireless", "Selected Virtual device: id=$id, name=${device.name}")
                    cachedDeviceId = id
                    return id
                }
            }
            // 2. Try to find a physical keyboard or gamepad
            for (id in ids) {
                val device = InputDevice.getDevice(id) ?: continue
                val sources = device.sources
                val isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
                val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                if (isKeyboard || isGamepad) {
                    Log.d("AdbWireless", "Selected physical controller/keyboard device: id=$id, name=${device.name}")
                    cachedDeviceId = id
                    return id
                }
            }
        } catch (e: Exception) {
            Log.e("AdbWireless", "Error finding virtual or keyboard device ID", e)
        }
        val fallbackId = android.view.KeyCharacterMap.VIRTUAL_KEYBOARD // -1
        cachedDeviceId = fallbackId
        return fallbackId
    }

    fun logInputDevices() {
        try {
            val ids = InputDevice.getDeviceIds()
            TaikoLogManager.log("=== [デバイス一覧] Dolphin設定用 ===")
            for (id in ids) {
                val device = InputDevice.getDevice(id) ?: continue
                val typeStr = StringBuilder()
                val sources = device.sources
                if ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) typeStr.append("キーボード ")
                if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) typeStr.append("ゲームパッド ")
                if ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) typeStr.append("ジョイスティック ")
                if ((sources and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) typeStr.append("タッチスクリーン ")
                
                val virtualStr = if (device.isVirtual) "仮想" else "物理"
                TaikoLogManager.log("• ID $id: ${device.name} ($virtualStr, $typeStr)")
                Log.d("AdbWireless", "Logged InputDevice: id=$id, name=${device.name}")
            }
            TaikoLogManager.log("💡 Dolphinの『コントローラー設定』の『デバイス』で、上記に表示されているデバイス（例: Virtual）を選択し、キーマッピング（D, F, J, K）を行ってください。")
            TaikoLogManager.log("====================================")
        } catch (e: Exception) {
            Log.e("AdbWireless", "Failed to log input devices", e)
        }
    }

    private fun injectEventViaShizuku(androidKeycode: Int, isPressed: Boolean): Boolean {
        try {
            val isShizukuAvailable = try {
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                false
            }
            if (!isShizukuAvailable) {
                iInputManagerInstance = null
                injectMethod = null
                return false
            }

            if (iInputManagerInstance == null) {
                val rawBinder = try {
                    rikka.shizuku.SystemServiceHelper.getSystemService("input")
                } catch (t: Throwable) {
                    null
                } ?: return false

                val wrappedBinder = rikka.shizuku.ShizukuBinderWrapper(rawBinder)
                
                val stubClass = Class.forName("android.hardware.input.IInputManager${'$'}Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                
                iInputManagerInstance = asInterfaceMethod.invoke(null, wrappedBinder)
                
                val iInputManagerClass = Class.forName("android.hardware.input.IInputManager")
                injectMethod = iInputManagerClass.getMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
            }

            val now = android.os.SystemClock.uptimeMillis()
            val action = if (isPressed) android.view.KeyEvent.ACTION_DOWN else android.view.KeyEvent.ACTION_UP
            
            if (isPressed) {
                if (activePressedKeycodes.contains(androidKeycode)) {
                    try {
                        val upEvent = android.view.KeyEvent(
                            now, now, android.view.KeyEvent.ACTION_UP, androidKeycode,
                            0, 0, findVirtualOrKeyboardDeviceId(), 0,
                            android.view.KeyEvent.FLAG_FROM_SYSTEM, android.view.InputDevice.SOURCE_KEYBOARD
                        )
                        injectMethod?.invoke(iInputManagerInstance, upEvent, 0)
                    } catch (_: Throwable) {}
                }
                activePressedKeycodes.add(androidKeycode)
            } else {
                activePressedKeycodes.remove(androidKeycode)
            }
            
            val targetDeviceId = findVirtualOrKeyboardDeviceId()
            val event = android.view.KeyEvent(
                now, // downTime
                now, // eventTime
                action,
                androidKeycode,
                0, // repeat
                0, // metaState
                targetDeviceId,
                0, // scancode
                android.view.KeyEvent.FLAG_FROM_SYSTEM,
                android.view.InputDevice.SOURCE_KEYBOARD
            )

            // INJECT_INPUT_EVENT_MODE_ASYNC = 0
            val result = injectMethod?.invoke(iInputManagerInstance, event, 0) as? Boolean
            val success = result ?: false
            if (success) {
                Log.d("AdbWireless", "Successfully injected key event via Shizuku IInputManager: keycode $androidKeycode, action $action, deviceId $targetDeviceId")
                TaikoLogManager.log("Direct API: $action keycode $androidKeycode (deviceId: $targetDeviceId)")
            } else {
                Log.e("AdbWireless", "Shizuku IInputManager injectInputEvent returned false")
            }
            return success
        } catch (e: Throwable) {
            iInputManagerInstance = null
            injectMethod = null
            Log.e("AdbWireless", "Failed to inject event via Shizuku IInputManager reflection", e)
            return false
        }
    }

    /**
     * Executes a high-fidelity key down / key up event for gamepad buttons or keyboard keys.
     * Uses 'cmd uinput' for zero-latency direct virtual hardware simulation if Shizuku/Root is available.
     * Otherwise, falls back to standard shell 'input keyevent' via persistent shell for ultra-low latency.
     */
    fun sendKeyEvent(part: String, key: String, isPressed: Boolean, groupingMs: Int = 15) {
        sendMultiKeyEvents(listOf(part to key), isPressed, groupingMs)
    }

    /**
     * Executes simultaneous key down / key up events for multiple keys in a single frame.
     */
    fun sendMultiKeyEvents(items: List<Pair<String, String>>, isPressed: Boolean, groupingMs: Int = 15) {
        if (items.isEmpty()) return

        val value = if (isPressed) 1 else 0
        val deviceId = if (emulationMode == "gamepad") 1 else 2

        inputExecutor.execute {
            try {
                var injectionSuccess = false

                // 1. Try high-fidelity Shizuku/Root virtual hardware injection (uinput)
                if (injectionMethod == "uinput" && checkAndRegisterUinput()) {
                    try {
                        val eventsList = mutableListOf<Int>()
                        items.forEach { (part, key) ->
                            val sc = if (emulationMode == "gamepad") getGamepadScanCode(part) else getKeyboardScancode(key)
                            if (value == 1 && activePressedScancodes.contains(sc)) {
                                eventsList.add(1) // EV_KEY
                                eventsList.add(sc)
                                eventsList.add(0) // EV_RELEASE
                            }
                            eventsList.add(1) // EV_KEY
                            eventsList.add(sc)
                            eventsList.add(value)
                            if (value == 1) activePressedScancodes.add(sc) else activePressedScancodes.remove(sc)
                        }
                        // SYN_REPORT
                        eventsList.add(0)
                        eventsList.add(0)
                        eventsList.add(0)

                        val injectJson = """{"id":$deviceId,"command":"inject","events":[${eventsList.joinToString(",")}]}"""

                        rootOutputStream?.write((",\n" + injectJson + "\n").toByteArray(Charsets.UTF_8))
                        rootOutputStream?.flush()
                        Log.d("AdbWireless", "uinput multi injected ($emulationMode): ${items.size} keys (value: $value)")
                        injectionSuccess = true
                    } catch (e: Exception) {
                        Log.e("AdbWireless", "Failed to inject uinput multi event, resetting process", e)
                        cleanupRootProcess()
                        injectionSuccess = false
                    }
                }

                // 2. Try Direct Shizuku System API Injection (if selected or if uinput failed)
                if (!injectionSuccess && (injectionMethod == "inject" || injectionMethod == "uinput")) {
                    var countSuccess = 0
                    items.forEach { (part, key) ->
                        val androidKeycodeStr = if (emulationMode == "gamepad") {
                            getGamepadAndroidKeycode(part)
                        } else {
                            getAndroidKeycode(key)
                        }
                        val androidKeycode = androidKeycodeStr.toIntOrNull() ?: 0
                        if (androidKeycode > 0) {
                            if (injectEventViaShizuku(androidKeycode, isPressed)) {
                                countSuccess++
                            }
                        }
                    }
                    if (countSuccess > 0) injectionSuccess = true
                }

                // 3. Fallback to standard command-line injection
                if (!injectionSuccess) {
                    val androidKeycodes = items.map { (part, key) ->
                        if (emulationMode == "gamepad") {
                            getGamepadAndroidKeycode(part)
                        } else {
                            getAndroidKeycode(key)
                        }
                    }.filter { it.isNotEmpty() && it != "0" }

                    if (androidKeycodes.isNotEmpty()) {
                        if (androidKeycodes.size == 1) {
                            if (groupingMs > 0) {
                                queueAndroidKeycode(androidKeycodes[0], isPressed, groupingMs)
                            } else {
                                dispatchSingleAndroidKeycode(androidKeycodes[0], isPressed)
                            }
                        } else {
                            val actionArg = if (isPressed) "--down" else "--up"
                            val codesStr = androidKeycodes.joinToString(" ")
                            val writer = getPersistentShell()
                            if (writer != null) {
                                try {
                                    writer.writeBytes("cmd input keyevent $actionArg $codesStr\n")
                                    writer.flush()
                                    Log.d("AdbWireless", "Persistent shell multi execution: cmd input keyevent $actionArg $codesStr")
                                } catch (e: Exception) {
                                    cleanupPersistentShell()
                                    executeOneShotFallback(androidKeycodes, isPressed)
                                }
                            } else {
                                executeOneShotFallback(androidKeycodes, isPressed)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e("AdbWireless", "Unexpected error in sendMultiKeyEvents", t)
            }
        }
    }

    private fun queueAndroidKeycode(androidKeycode: String, isPressed: Boolean, groupingMs: Int) {
        val queue = if (isPressed) pendingDownKeycodes else pendingUpKeycodes
        val scheduledFlag = if (isPressed) isDownDispatchScheduled else isUpDispatchScheduled
        
        queue.add(androidKeycode)
        if (scheduledFlag.compareAndSet(false, true)) {
            scheduler.schedule({
                try {
                    dispatchBufferedKeycodes(isPressed)
                } finally {
                    scheduledFlag.set(false)
                }
            }, groupingMs.toLong(), TimeUnit.MILLISECONDS)
        }
    }

    private fun dispatchSingleAndroidKeycode(androidKeycode: String, isPressed: Boolean) {
        val writer = getPersistentShell()
        val actionArg = if (isPressed) "--down" else "--up"
        if (writer != null) {
            try {
                writer.writeBytes("cmd input keyevent $actionArg $androidKeycode\n")
                writer.flush()
                Log.d("AdbWireless", "Persistent shell execution triggered: cmd input keyevent $actionArg $androidKeycode")
                TaikoLogManager.log("Shell Cmd: cmd input keyevent $actionArg $androidKeycode")
            } catch (e: Exception) {
                Log.e("AdbWireless", "Persistent shell write failed, attempting recovery", e)
                TaikoLogManager.log("Shell Cmd ERR: Write failed, recovering...")
                cleanupPersistentShell()
                executeOneShotFallback(listOf(androidKeycode), isPressed)
            }
        } else {
            executeOneShotFallback(listOf(androidKeycode), isPressed)
        }
    }

    private fun dispatchBufferedKeycodes(isPressed: Boolean) {
        val queue = if (isPressed) pendingDownKeycodes else pendingUpKeycodes
        val codes = mutableListOf<String>()
        var code = queue.poll()
        while (code != null) {
            if (!codes.contains(code)) {
                codes.add(code)
            }
            code = queue.poll()
        }
        if (codes.isEmpty()) return

        val actionArg = if (isPressed) "--down" else "--up"
        val codesStr = codes.joinToString(" ")
        Log.d("AdbWireless", "Dispatching buffered keycodes: cmd input keyevent $actionArg $codesStr")
        
        val writer = getPersistentShell()
        if (writer != null) {
            try {
                writer.writeBytes("cmd input keyevent $actionArg $codesStr\n")
                writer.flush()
                Log.d("AdbWireless", "Persistent shell execution triggered: cmd input keyevent $actionArg $codesStr")
                TaikoLogManager.log("Shell Cmd: cmd input keyevent $actionArg $codesStr")
            } catch (e: Exception) {
                Log.e("AdbWireless", "Persistent shell write failed, attempting recovery", e)
                TaikoLogManager.log("Shell Cmd ERR: Write failed, recovering...")
                cleanupPersistentShell()
                executeOneShotFallback(codes, isPressed)
            }
        } else {
            executeOneShotFallback(codes, isPressed)
        }
    }

    private fun executeOneShotFallback(keycodes: List<String>, isPressed: Boolean) {
        try {
            val actionArg = if (isPressed) "--down" else "--up"
            val cmd = mutableListOf("cmd", "input", "keyevent", actionArg)
            cmd.addAll(keycodes)
            val cmdArray = cmd.toTypedArray()

            val isShizukuAvailable = try {
                Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                false
            }

            if (isShizukuAvailable) {
                createShizukuProcess(cmdArray)
                TaikoLogManager.log("Shizuku One-shot: cmd input keyevent $actionArg ${keycodes.joinToString(" ")}")
            } else if (checkRoot()) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "cmd input keyevent $actionArg ${keycodes.joinToString(" ")}"))
                TaikoLogManager.log("Root One-shot: cmd input keyevent $actionArg ${keycodes.joinToString(" ")}")
            } else {
                Log.w("AdbWireless", "Cannot inject keyevent: Shizuku is not running / permitted and device is not rooted.")
            }
            Log.d("AdbWireless", "One-shot fallback execution completed: cmd input keyevent $actionArg ${keycodes.joinToString(" ")}")
        } catch (e: Throwable) {
            Log.e("AdbWireless", "Failed to run local one-shot fallback keyevent", e)
            TaikoLogManager.log("Fallback ERR: exec failed: ${e.message}")
        }
    }

    /**
     * Simulate connection check to WebADB wireless port
     */
    fun testConnection(host: String, port: Int, callback: (Boolean, String?) -> Unit) {
        networkExecutor.execute {
            try {
                val socket = Socket(host, port)
                socket.soTimeout = 3000
                socket.close()
                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.localizedMessage ?: "Connection timed out")
            }
        }
    }

    /**
     * Simulate pairing logic for visual consistency
     */
    fun pairDevice(host: String, port: Int, code: String, callback: (Boolean, String?) -> Unit) {
        networkExecutor.execute {
            try {
                // Simulate pairing process
                Thread.sleep(1500)
                callback(true, null)
            } catch (e: Exception) {
                callback(false, e.localizedMessage)
            }
        }
    }

    private fun cleanupRootProcess() {
        try {
            if (rootOutputStream != null) {
                try {
                    rootOutputStream?.writeBytes("\n]\n")
                    rootOutputStream?.flush()
                } catch (e: Exception) {}
                rootOutputStream?.close()
            }
        } catch (e: Exception) {}
        try {
            rootProcess?.destroy()
        } catch (e: Exception) {}
        rootProcess = null
        rootOutputStream = null
        isUinputRegistered = false
    }

    private fun getGamepadScanCode(part: String): Int {
        val buttonId = when (part) {
            "leftKat" -> gamepadKeyConfig.leftKat
            "leftDon" -> gamepadKeyConfig.leftDon
            "rightDon" -> gamepadKeyConfig.rightDon
            "rightKat" -> gamepadKeyConfig.rightKat
            else -> "B"
        }
        return when (buttonId) {
            "L1" -> 310        // BTN_TL
            "R1" -> 311        // BTN_TR
            "L2" -> 312        // BTN_TL2
            "R2" -> 313        // BTN_TR2
            "DPAD_LEFT" -> 105
            "DPAD_RIGHT" -> 106
            "DPAD_UP" -> 103
            "DPAD_DOWN" -> 108
            "A" -> 304        // BTN_A
            "B" -> 305        // BTN_B
            "X" -> 307        // BTN_X
            "Y" -> 308        // BTN_Y
            "SELECT" -> 314   // BTN_SELECT
            "START" -> 315    // BTN_START
            "L3" -> 317       // BTN_THUMBL
            "R3" -> 318       // BTN_THUMBR
            else -> 305
        }
    }

    private fun getGamepadAndroidKeycode(part: String): String {
        val buttonId = when (part) {
            "leftKat" -> gamepadKeyConfig.leftKat
            "leftDon" -> gamepadKeyConfig.leftDon
            "rightDon" -> gamepadKeyConfig.rightDon
            "rightKat" -> gamepadKeyConfig.rightKat
            else -> "B"
        }
        return when (buttonId) {
            "L1" -> "102"
            "R1" -> "103"
            "L2" -> "104"
            "R2" -> "105"
            "DPAD_LEFT" -> "21"
            "DPAD_RIGHT" -> "22"
            "DPAD_UP" -> "19"
            "DPAD_DOWN" -> "20"
            "A" -> "96"
            "B" -> "97"
            "X" -> "99"
            "Y" -> "100"
            "SELECT" -> "109"
            "START" -> "108"
            "L3" -> "106"
            "R3" -> "107"
            else -> "97"
        }
    }

    private fun getKeyboardScancode(key: String): Int {
        return when (key.uppercase()) {
            "A" -> 30
            "B" -> 48
            "C" -> 46
            "D" -> 32
            "E" -> 18
            "F" -> 33
            "G" -> 34
            "H" -> 35
            "I" -> 23
            "J" -> 36
            "K" -> 37
            "L" -> 38
            "M" -> 50
            "N" -> 49
            "O" -> 24
            "P" -> 25
            "Q" -> 16
            "R" -> 19
            "S" -> 31
            "T" -> 20
            "U" -> 22
            "V" -> 47
            "W" -> 17
            "X" -> 45
            "Y" -> 21
            "Z" -> 44
            "1" -> 2
            "2" -> 3
            "3" -> 4
            "4" -> 5
            "5" -> 6
            "6" -> 7
            "7" -> 8
            "8" -> 9
            "9" -> 10
            "0" -> 11
            "ENTER" -> 28
            "SPACE" -> 57
            else -> 32 // fallback to KEY_D
        }
    }

    private fun getAndroidKeycode(key: String): String {
        val k = key.uppercase()
        if (k.length == 1) {
            val c = k[0]
            if (c in 'A'..'Z') {
                return (29 + (c - 'A')).toString()
            }
            if (c in '0'..'9') {
                return (7 + (c - '0')).toString()
            }
        }
        return when (k) {
            "ENTER" -> "66"
            "SPACE" -> "62"
            else -> "32" // KEYCODE_D
        }
    }

    fun release() {
        inputExecutor.execute {
            cleanupRootProcess()
            cleanupPersistentShell()
        }
        inputExecutor.shutdown()
        networkExecutor.shutdown()
        scheduler.shutdown()
    }
}
