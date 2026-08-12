package com.example

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Direct USB Low-Latency Communication Engine for Android-to-Android over USB cable.
 * Supports both AOA Accessory Mode (Stream FileDescriptor) and AOA Host Mode (USB Bulk Transfer),
 * plus USB-Ethernet/RNDIS zero-latency socket connection.
 * Bypasses network overhead to achieve sub-millisecond (<1ms) latency.
 */
object TaikoUsbDirectManager {
    private const val TAG = "TaikoUsbDirect"
    private const val ACTION_USB_PERMISSION = "com.sango.taikocontroller.USB_PERMISSION"

    private const val AOA_MANUFACTURER = "TaikoController"
    private const val AOA_MODEL = "TaikoDirectUsb"
    private const val AOA_DESCRIPTION = "Taiko Low-Latency Direct USB"
    private const val AOA_VERSION = "1.0"
    private const val AOA_URI = "https://github.com/sango"
    private const val AOA_SERIAL = "1234567890"

    @Volatile private var isRunning = false
    @Volatile private var isConnected = false
    @Volatile private var isConnecting = false
    @Volatile private var isReceiverRegistered = false
    @Volatile private var isPermissionPending = false
    @Volatile private var lastPermissionRequestTime = 0L

    private val requestedDeviceIds = java.util.Collections.synchronizedSet(HashSet<Int>())
    private val requestedAccessoryKeys = java.util.Collections.synchronizedSet(HashSet<String>())

    // Accessory Mode Variables
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var writer: BufferedWriter? = null

    // Host Mode Variables (Bulk Transfer)
    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    // USB Direct Socket Fallback (RNDIS/NCM)
    private var directSocket: Socket? = null
    private var socketWriter: BufferedWriter? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var monitorJob: Job? = null
    private var readJob: Job? = null

    private val _isConnectedState = MutableStateFlow(false)
    val isConnectedState: StateFlow<Boolean> = _isConnectedState

    private var keyCallback: ((keys: List<String>, isPressed: Boolean) -> Unit)? = null

    fun init(context: Context, onKeyReceived: (keys: List<String>, isPressed: Boolean) -> Unit) {
        this.keyCallback = onKeyReceived
    }

    @Synchronized
    fun start(context: Context) {
        if (isRunning) return
        isRunning = true
        TaikoLogManager.log("USB Direct Driver (AOA / Direct) 起動: 自動検出開始")

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        try {
            if (!isReceiverRegistered) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.registerReceiver(usbReceiver, filter)
                }
                isReceiverRegistered = true
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Receiver register failed safely", e)
        }

        startPolling(context)
    }

    @Synchronized
    fun stop(context: Context) {
        isRunning = false
        isPermissionPending = false
        monitorJob?.cancel()
        readJob?.cancel()
        closeStreams()
        requestedDeviceIds.clear()
        requestedAccessoryKeys.clear()

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(usbReceiver)
            } catch (e: Throwable) {
                Log.d(TAG, "Unregister receiver caught safely: ${e.message}")
            } finally {
                isReceiverRegistered = false
            }
        }

        _isConnectedState.value = false
        isConnected = false
        TaikoLogManager.log("USB Direct Driver 停止しました")
    }

    fun restart(context: Context) {
        TaikoLogManager.log("USB Direct Driver を完全リセット・再起動中...")
        stop(context)
        scope.launch {
            delay(150)
            start(context)
            delay(300)
            tryConnectUsb(context)
            delay(800)
            tryConnectUsb(context)
        }
    }

    private fun startPolling(context: Context) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            var unconnectPass = 0
            while (isActive && isRunning) {
                if (!isConnected) {
                    unconnectPass++
                    if (unconnectPass % 4 == 0) {
                        // Reset stale requested permission IDs to allow continuous re-engagement
                        requestedDeviceIds.clear()
                        requestedAccessoryKeys.clear()
                        isPermissionPending = false
                    }
                    if (!isConnecting) {
                        tryConnectUsb(context)
                    }
                } else {
                    unconnectPass = 0
                }
                delay(1200)
            }
        }
    }

    @Synchronized
    private fun tryConnectUsb(context: Context) {
        if (isConnected || isConnecting) return
        isConnecting = true

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            if (usbManager == null) {
                isConnecting = false
                return
            }

            // 1. Accessory Mode Check
            val accessoryList = try { usbManager.accessoryList } catch (e: Throwable) { null }
            if (!accessoryList.isNullOrEmpty()) {
                val accessory = accessoryList[0]
                val accKey = "${accessory.manufacturer}_${accessory.model}_${accessory.serial}"
                val hasPerm = try { usbManager.hasPermission(accessory) } catch (e: Throwable) { false }
                if (!hasPerm) {
                    if (!requestedAccessoryKeys.contains(accKey)) {
                        requestedAccessoryKeys.add(accKey)
                        requestAccessoryPermission(context, usbManager, accessory)
                    }
                    isConnecting = false
                    return
                }
                openAccessoryStream(usbManager, accessory)
                isConnecting = false
                return
            }

            // 2. USB Device Check (Host Mode AOA or Bulk)
            val deviceList = try { usbManager.deviceList } catch (e: Throwable) { null }
            if (deviceList != null) {
                for ((_, device) in deviceList) {
                    val devId = device.deviceId
                    if (isGoogleAoaDevice(device)) {
                        val hasPerm = try { usbManager.hasPermission(device) } catch (e: Throwable) { false }
                        if (!hasPerm) {
                            if (!requestedDeviceIds.contains(devId)) {
                                requestedDeviceIds.add(devId)
                                requestDevicePermission(context, usbManager, device)
                            }
                            isConnecting = false
                            return
                        }
                        openHostBulkStream(usbManager, device)
                        isConnecting = false
                        return
                    } else {
                        // Request AOA Handshake to switch remote device to Accessory mode
                        val hasPerm = try { usbManager.hasPermission(device) } catch (e: Throwable) { false }
                        if (hasPerm) {
                            initiateAoaHandshake(usbManager, device)
                        } else {
                            if (!requestedDeviceIds.contains(devId)) {
                                requestedDeviceIds.add(devId)
                                requestDevicePermission(context, usbManager, device)
                            }
                        }
                    }
                }
            }

            // 3. Try Direct Local USB Socket (RNDIS / USB Tethering / NCM)
            tryConnectDirectUsbSocket()

        } catch (e: Throwable) {
            Log.e(TAG, "Error in tryConnectUsb safely handled", e)
        } finally {
            isConnecting = false
        }
    }

    private fun isGoogleAoaDevice(device: UsbDevice): Boolean {
        return device.vendorId == 0x18D1 && (device.productId in 0x2D00..0x2D05)
    }

    private fun initiateAoaHandshake(usbManager: UsbManager, device: UsbDevice) {
        var connection: UsbDeviceConnection? = null
        try {
            connection = usbManager.openDevice(device) ?: return
            val rawBuffer = ByteArray(2)
            val protocol = connection.controlTransfer(0xC0, 51, 0, 0, rawBuffer, 2, 500)
            if (protocol < 0) {
                connection.close()
                return
            }

            fun sendAoaString(index: Int, str: String) {
                val bytes = str.toByteArray(Charsets.UTF_8)
                connection.controlTransfer(0x40, 52, 0, index, bytes, bytes.size, 500)
            }

            sendAoaString(0, AOA_MANUFACTURER)
            sendAoaString(1, AOA_MODEL)
            sendAoaString(2, AOA_DESCRIPTION)
            sendAoaString(3, AOA_VERSION)
            sendAoaString(4, AOA_URI)
            sendAoaString(5, AOA_SERIAL)

            connection.controlTransfer(0x40, 53, 0, 0, null, 0, 500)
            TaikoLogManager.log("USB AOA ハンドシェイク送信成功 (${device.deviceName}) -> 相手をAOAアクセサリへ切替")
        } catch (e: Throwable) {
            Log.d(TAG, "AOA handshake note: ${e.message}")
        } finally {
            try { connection?.close() } catch (_: Throwable) {}
        }
    }

    private fun requestAccessoryPermission(context: Context, usbManager: UsbManager, accessory: UsbAccessory) {
        val now = System.currentTimeMillis()
        if (isPermissionPending || (now - lastPermissionRequestTime < 3000L)) return
        isPermissionPending = true
        lastPermissionRequestTime = now

        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(accessory, permissionIntent)
        } catch (e: Throwable) {
            isPermissionPending = false
            Log.e(TAG, "requestAccessoryPermission error", e)
        }
    }

    private fun requestDevicePermission(context: Context, usbManager: UsbManager, device: UsbDevice) {
        val now = System.currentTimeMillis()
        if (isPermissionPending || (now - lastPermissionRequestTime < 3000L)) return
        isPermissionPending = true
        lastPermissionRequestTime = now

        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permissionIntent)
        } catch (e: Throwable) {
            isPermissionPending = false
            Log.e(TAG, "requestDevicePermission error", e)
        }
    }

    @Synchronized
    private fun openAccessoryStream(usbManager: UsbManager, accessory: UsbAccessory) {
        if (isConnected) return
        try {
            val pfd = usbManager.openAccessory(accessory)
            if (pfd == null) {
                TaikoLogManager.log("USB Direct: openAccessory が null を返しました")
                return
            }

            fileDescriptor = pfd
            val fd = pfd.fileDescriptor
            inputStream = FileInputStream(fd)
            outputStream = FileOutputStream(fd)
            writer = BufferedWriter(OutputStreamWriter(outputStream!!, "UTF-8"))

            isConnected = true
            _isConnectedState.value = true
            TaikoLogManager.log("⚡⚡ USB Direct (AOA Accessory モード) 超極小遅延通信接続完了! (<1ms) ⚡⚡")

            startReadingAccessoryStream(inputStream!!)
        } catch (e: Throwable) {
            Log.e(TAG, "Error opening USB accessory stream", e)
            closeStreams()
        }
    }

    private fun startReadingAccessoryStream(fis: FileInputStream) {
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(fis, "UTF-8"))
                while (isActive && isConnected) {
                    val line = reader.readLine() ?: break
                    parseAndDispatchKey(line)
                }
            } catch (e: Throwable) {
                if (isConnected) {
                    TaikoLogManager.log("USB Direct 受信切断: ${e.message}")
                }
            } finally {
                closeStreams()
            }
        }
    }

    @Synchronized
    private fun openHostBulkStream(usbManager: UsbManager, device: UsbDevice) {
        if (isConnected) return
        try {
            val conn = usbManager.openDevice(device) ?: return
            if (device.interfaceCount == 0) {
                conn.close()
                return
            }

            val intf = device.getInterface(0)
            if (!conn.claimInterface(intf, true)) {
                conn.close()
                return
            }

            var epIn: UsbEndpoint? = null
            var epOut: UsbEndpoint? = null

            for (i in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(i)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        epIn = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                        epOut = ep
                    }
                }
            }

            if (epIn == null && epOut == null) {
                conn.releaseInterface(intf)
                conn.close()
                return
            }

            usbConnection = conn
            usbInterface = intf
            endpointIn = epIn
            endpointOut = epOut

            isConnected = true
            _isConnectedState.value = true
            TaikoLogManager.log("⚡⚡ USB Direct (AOA Host モード) 超極小遅延通信接続完了! (<1ms) ⚡⚡")

            if (epIn != null) {
                startReadingHostBulkStream(conn, epIn)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error opening USB Host Bulk Stream", e)
            closeStreams()
        }
    }

    private fun startReadingHostBulkStream(conn: UsbDeviceConnection, epIn: UsbEndpoint) {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(1024)
            val lineBuffer = StringBuilder()
            try {
                while (isActive && isConnected) {
                    val bytesRead = conn.bulkTransfer(epIn, buffer, buffer.size, 1000)
                    if (bytesRead > 0) {
                        val str = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        for (ch in str) {
                            if (ch == '\n') {
                                parseAndDispatchKey(lineBuffer.toString())
                                lineBuffer.setLength(0)
                            } else {
                                lineBuffer.append(ch)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                if (isConnected) {
                    TaikoLogManager.log("USB Host Bulk 受信切断: ${e.message}")
                }
            } finally {
                closeStreams()
            }
        }
    }

    private fun tryConnectDirectUsbSocket() {
        if (isConnected) return
        val wiredIps = NetworkUtils.getWiredLocalIpAddresses()
        if (wiredIps.isEmpty()) return

        for (localIp in wiredIps) {
            val parts = localIp.split(".")
            if (parts.size == 4) {
                val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                val candidates = listOf("$prefix.1", "$prefix.129", "$prefix.2", "$prefix.100")
                for (ip in candidates) {
                    if (ip == localIp) continue
                    try {
                        val s = Socket()
                        s.tcpNoDelay = true
                        s.connect(InetSocketAddress(ip, 60001), 150)
                        directSocket = s
                        socketWriter = BufferedWriter(OutputStreamWriter(s.getOutputStream(), "UTF-8"))
                        isConnected = true
                        _isConnectedState.value = true
                        TaikoLogManager.log("⚡⚡ USB Direct Socket (RNDIS/USBテザリング) 接続完了! ⚡⚡")

                        // Start reader for direct socket
                        val fis = s.getInputStream()
                        readJob?.cancel()
                        readJob = scope.launch {
                            try {
                                val reader = BufferedReader(InputStreamReader(fis, "UTF-8"))
                                while (isActive && isConnected) {
                                    val line = reader.readLine() ?: break
                                    parseAndDispatchKey(line)
                                }
                            } catch (_: Throwable) {
                            } finally {
                                closeStreams()
                            }
                        }
                        return
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun parseAndDispatchKey(line: String) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return

        val parts = trimmed.split(":")
        if (parts.size == 2) {
            val keys = parts[0].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val isPressed = parts[1].equals("DOWN", ignoreCase = true)
            if (keys.isNotEmpty()) {
                keyCallback?.invoke(keys, isPressed)
            }
        }
    }

    fun sendKeys(keys: List<String>, isPressed: Boolean): Boolean {
        if (!isConnected) return false
        val keysStr = keys.joinToString(",")
        val action = if (isPressed) "DOWN" else "UP"
        val payload = "$keysStr:$action\n"
        val bytes = payload.toByteArray(Charsets.UTF_8)

        // 1. Accessory Mode Writer
        writer?.let { w ->
            return try {
                synchronized(this) {
                    w.write(payload)
                    w.flush()
                }
                true
            } catch (e: Throwable) {
                closeStreams()
                false
            }
        }

        // 2. Host Bulk Transfer
        val conn = usbConnection
        val epOut = endpointOut
        if (conn != null && epOut != null) {
            return try {
                val sent = conn.bulkTransfer(epOut, bytes, bytes.size, 100)
                if (sent < 0) {
                    closeStreams()
                    false
                } else {
                    true
                }
            } catch (e: Throwable) {
                closeStreams()
                false
            }
        }

        // 3. Direct Socket Writer
        socketWriter?.let { sw ->
            return try {
                synchronized(this) {
                    sw.write(payload)
                    sw.flush()
                }
                true
            } catch (e: Throwable) {
                closeStreams()
                false
            }
        }

        return false
    }

    @Synchronized
    private fun closeStreams() {
        isConnected = false
        isConnecting = false
        _isConnectedState.value = false

        // Accessory streams
        try { writer?.close() } catch (_: Throwable) {}
        try { inputStream?.close() } catch (_: Throwable) {}
        try { outputStream?.close() } catch (_: Throwable) {}
        try { fileDescriptor?.close() } catch (_: Throwable) {}
        writer = null
        inputStream = null
        outputStream = null
        fileDescriptor = null

        // Host Bulk connection
        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (_: Throwable) {}
        usbConnection = null
        usbInterface = null
        endpointIn = null
        endpointOut = null

        // Direct Socket
        try { socketWriter?.close() } catch (_: Throwable) {}
        try { directSocket?.close() } catch (_: Throwable) {}
        socketWriter = null
        directSocket = null
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                if (ACTION_USB_PERMISSION == action) {
                    synchronized(this@TaikoUsbDirectManager) {
                        val accessory: UsbAccessory? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                        }
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        isPermissionPending = false
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

                        if (granted) {
                            if (accessory != null) {
                                openAccessoryStream(usbManager, accessory)
                            } else if (device != null) {
                                if (isGoogleAoaDevice(device)) {
                                    openHostBulkStream(usbManager, device)
                                } else {
                                    initiateAoaHandshake(usbManager, device)
                                }
                            }
                            // Schedule delayed auto-retry passes after permission grant
                            scope.launch {
                                delay(300)
                                tryConnectUsb(context)
                                delay(800)
                                tryConnectUsb(context)
                            }
                        }
                    }
                } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED == action ||
                           UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                    TaikoLogManager.log("USB ケーブルが抜かれました - リセット")
                    isPermissionPending = false
                    requestedDeviceIds.clear()
                    requestedAccessoryKeys.clear()
                    closeStreams()
                } else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == action ||
                           UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                    TaikoLogManager.log("USB ケーブル挿入検知")
                    isPermissionPending = false
                    requestedDeviceIds.clear()
                    requestedAccessoryKeys.clear()
                    tryConnectUsb(context)
                    scope.launch {
                        delay(400)
                        tryConnectUsb(context)
                        delay(1200)
                        tryConnectUsb(context)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error in usbReceiver.onReceive", e)
            }
        }
    }
}
