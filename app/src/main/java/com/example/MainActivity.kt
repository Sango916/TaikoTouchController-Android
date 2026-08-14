package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.widget.Toast
import rikka.shizuku.Shizuku
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    // Native audio synthesizer and player
    private var audioPlayer: TaikoAudioPlayer? = null
    
    // WebSocket client manager
    private var webSocketClient: TaikoWebSocketClient? = null
    
    // Local direct shell key injector
    private var adbClient: AdbWirelessClient? = null

    // Local TCP Server for USB-Wired PC Connection
    private var tcpServer: TaikoTcpServer? = null
    private val tcpClientsCountState = mutableStateOf(0)

    // Another Android Remote Connection
    private var remoteSender: TaikoAndroidRemoteSender? = null
    private var remoteReceiver: TaikoAndroidRemoteReceiver? = null
    private val remoteSenderStatusState = mutableStateOf("disconnected")
    private val remoteReceiverClientsCountState = mutableStateOf(0)

    // Vibration hardware service
    private var vibrator: Vibrator? = null

    // Last pressed timestamps to guarantee a minimum key-press duration of 40ms for high reliability
    private val lastPressTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Active software-repeat jobs for non-root / WebSocket connections
    private val activeRepeatJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    // Active release jobs to prevent early releases on fast multi-tap overlaps
    private val pendingReleaseJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    // UI state holder
    private var settingsState = mutableStateOf(ControllerSettings())
    private var wsConnectedState = mutableStateOf(false)
    private var peerCountState = mutableStateOf(0)
    private var wsServerUrlState = mutableStateOf("ws://10.0.2.2:3000") // 10.0.2.2 points to host localhost in standard android emulator
    private var lastReceivedInputsState = mutableStateOf<List<InputMessageEvent>>(emptyList())
    private var adbStatusState = mutableStateOf("disconnected")
    private var adbErrorState = mutableStateOf<String?>(null)

    // Active inputs for real-time visual highlight syncing
    private val activeInputsState = mutableStateOf(RecordActiveInputs())

    // Shizuku states
    private val shizukuPermissionGranted = mutableStateOf(false)
    private val shizukuInstalledAndRunning = mutableStateOf(false)

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        shizukuPermissionGranted.value = (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            shizukuInstalledAndRunning.value = Shizuku.pingBinder()
            if (Shizuku.pingBinder()) {
                shizukuPermissionGranted.value = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            shizukuInstalledAndRunning.value = false
            shizukuPermissionGranted.value = false
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    shizukuPermissionGranted.value = true
                } else {
                    Shizuku.requestPermission(1001)
                }
            } else {
                shizukuInstalledAndRunning.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to request Shizuku permission", e)
        }
    }

    private fun openShizukuApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                startActivity(intent)
            } else {
                val uri = android.net.Uri.parse("market://details?id=moe.shizuku.privileged.api")
                val playIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                startActivity(playIntent)
            }
        } catch (e: Exception) {
            try {
                val uri = android.net.Uri.parse("https://shizuku.rikka.app/")
                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            } catch (ex: Exception) {
                android.util.Log.e("MainActivity", "Failed to open Shizuku app/website", ex)
            }
        }
    }

    private fun refreshShizukuStatus() {
        try {
            val ping = Shizuku.pingBinder()
            shizukuInstalledAndRunning.value = ping
            if (ping) {
                shizukuPermissionGranted.value = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                shizukuPermissionGranted.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to manual check Shizuku status", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        TaikoLogManager.log("=== App Started ===")
        TaikoLogManager.log("Android OS SDK Level: ${Build.VERSION.SDK_INT}")
        TaikoLogManager.log("Device model: ${Build.MODEL}")

        // Load saved settings
        loadPersistedSettings()

        // Clean application exit on Back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                TaikoLogManager.log("Back button pressed: Closing app cleanly and releasing port 60001...")
                stopTcpServer()
                finishAndRemoveTask()
            }
        })

        // Init low-latency Vibration service and ADB client
        adbClient = AdbWirelessClient()
        initVibrator()

        // Init USB Direct (AOA) Manager for ultra low-latency Android-to-Android cable connection (<1ms)
        TaikoUsbDirectManager.init(this) { keys, isPressed ->
            handleIncomingRemoteKeys(keys, isPressed)
        }

        // Init local TCP Server for PC Connection (Only active when PC USB mode is selected)
        tcpServer = TaikoTcpServer { count ->
            runOnUiThread {
                tcpClientsCountState.value = count
            }
        }
        if (settingsState.value.connectionMode == "usb-wired") {
            startTcpServer()
        }

        // Shizuku init
        try {
            Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
            Shizuku.addBinderDeadListener(shizukuBinderDeadListener)
            Shizuku.addRequestPermissionResultListener(shizukuListener)
            shizukuInstalledAndRunning.value = Shizuku.pingBinder()
            if (Shizuku.pingBinder()) {
                shizukuPermissionGranted.value = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                TaikoLogManager.log("Shizuku status: Installed & Running (Granted=${shizukuPermissionGranted.value})")
            } else {
                TaikoLogManager.log("Shizuku status: Not running or not installed")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Shizuku initialization failed", e)
            TaikoLogManager.log("Shizuku init failed: ${e.message}")
        }

        // Active background polling routine to check Shizuku status every second
        lifecycleScope.launch {
            while (isActive) {
                try {
                    val ping = Shizuku.pingBinder()
                    if (shizukuInstalledAndRunning.value != ping) {
                        shizukuInstalledAndRunning.value = ping
                    }
                    if (ping) {
                        val granted = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (shizukuPermissionGranted.value != granted) {
                            shizukuPermissionGranted.value = granted
                        }
                    } else {
                        if (shizukuPermissionGranted.value) {
                            shizukuPermissionGranted.value = false
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error in Shizuku status polling", e)
                }
                delay(1000)
            }
        }

        setContent {
            val settings by settingsState
            val wsConnected by wsConnectedState
            val peerCount by peerCountState
            val wsServerUrl by wsServerUrlState
            val lastReceivedInputs by lastReceivedInputsState
            val adbStatus by adbStatusState
            val adbError by adbErrorState
            val activeInputs by activeInputsState
            val pcClientsCount by tcpClientsCountState
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            val screenWidthDp = configuration.screenWidthDp
            val useTabMode = isPortrait || screenWidthDp < 640
            var isFullScreen by remember { mutableStateOf(false) }
            var activeTab by remember { mutableStateOf(1) } // 0 = Controller, 1 = Settings (default to Settings/Shizuku to help user find it easily on first open!)
            val isDarkTheme = resolveIsDarkTheme(settings.themeMode)

            // 画面分割(Split Screen)時のフォーカス問題対策:
            // 全画面コントローラーのときは FLAG_NOT_FOCUSABLE を設定し、タップしても上画面のエミュレーターからフォーカスを奪わないようにします。
            // これにより、下画面をタップしながらでも上画面のエミュレーターに確実にキー入力が送信されます。
            LaunchedEffect(isFullScreen) {
                if (isFullScreen) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(if (isFullScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing),
                color = Color(0xFFFDF6E2).invertIfDark(isDarkTheme) // Antique Japanese Beige Background (inverted in dark theme)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isFullScreen) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            TaikoPad(
                                settings = settings,
                                activeInputs = activeInputs,
                                onInputTriggered = { part, isPressed ->
                                    triggerInput(part, isPressed)
                                },
                                onMultiInputTriggered = { inputsList ->
                                    triggerMultiInputs(inputsList)
                                },
                                audioPlayer = audioPlayer,
                                vibrateAction = { isBig -> triggerVibration(isBig) },
                                isFullScreen = true,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Overlaid exit button in a semi-transparent container
                            IconButton(
                                onClick = { isFullScreen = false },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.5f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // --- App Header ---
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🥁", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "太鼓タッチコントローラー",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF78350F).invertIfDark(isDarkTheme)
                                    )
                                }

                                if (pcClientsCount > 0) {
                                    Surface(
                                        color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDarkTheme) Color(0xFF34D399) else Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "💻 PC接続中 (${pcClientsCount}台)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                            )
                                        }
                                    }
                                }
                            }

                            // --- Main Split Content Body ---
                            if (useTabMode) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TabRow(
                                        selectedTabIndex = activeTab,
                                        containerColor = Color(0xFFF9F5EB).invertIfDark(isDarkTheme),
                                        contentColor = Color(0xFF78350F).invertIfDark(isDarkTheme)
                                    ) {
                                        Tab(
                                            selected = activeTab == 0,
                                            onClick = { activeTab = 0 },
                                            text = { Text("🥁 太鼓コントローラー", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                                        )
                                        Tab(
                                            selected = activeTab == 1,
                                            onClick = { activeTab = 1 },
                                            text = { Text("⚙️ 設定", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                                        )
                                    }

                                    if (activeTab == 0) {
                                        TaikoPad(
                                            settings = settings,
                                            activeInputs = activeInputs,
                                            onInputTriggered = { part, isPressed ->
                                                triggerInput(part, isPressed)
                                            },
                                            onMultiInputTriggered = { inputsList ->
                                                triggerMultiInputs(inputsList)
                                            },
                                            audioPlayer = audioPlayer,
                                            vibrateAction = { isBig -> triggerVibration(isBig) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        SettingsPanel(
                                            settings = settings,
                                            onSettingsChanged = { updateAndPersistSettings(it) },
                                            wsConnected = false,
                                            peerCount = 0,
                                            wsServerUrl = "",
                                            onWsServerUrlChanged = {},
                                            adbStatus = "disconnected",
                                            adbError = null,
                                            onAdbConnect = {},
                                            onAdbPair = {},
                                            onEnterFullScreen = { isFullScreen = true },
                                            shizukuRunning = shizukuInstalledAndRunning.value,
                                            shizukuPermission = shizukuPermissionGranted.value,
                                            onRequestShizukuPermission = { requestShizukuPermission() },
                                            onOpenShizukuApp = { openShizukuApp() },
                                            onRefreshShizukuStatus = { refreshShizukuStatus() },
                                            pcClientsCount = tcpClientsCountState.value,
                                            remoteSenderStatus = remoteSenderStatusState.value,
                                            remoteReceiverClientsCount = remoteReceiverClientsCountState.value,
                                            onConnectRemoteSender = { connectRemoteSender() },
                                            onResetConnection = { resetConnection() },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TaikoPad(
                                        settings = settings,
                                        activeInputs = activeInputs,
                                        onInputTriggered = { part, isPressed ->
                                            triggerInput(part, isPressed)
                                        },
                                        onMultiInputTriggered = { inputsList ->
                                            triggerMultiInputs(inputsList)
                                        },
                                        audioPlayer = audioPlayer,
                                        vibrateAction = { isBig -> triggerVibration(isBig) },
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    SettingsPanel(
                                        settings = settings,
                                        onSettingsChanged = { updateAndPersistSettings(it) },
                                        wsConnected = false,
                                        peerCount = 0,
                                        wsServerUrl = "",
                                        onWsServerUrlChanged = {},
                                        adbStatus = "disconnected",
                                        adbError = null,
                                        onAdbConnect = {},
                                        onAdbPair = {},
                                        onEnterFullScreen = { isFullScreen = true },
                                        shizukuRunning = shizukuInstalledAndRunning.value,
                                        shizukuPermission = shizukuPermissionGranted.value,
                                        onRequestShizukuPermission = { requestShizukuPermission() },
                                        onOpenShizukuApp = { openShizukuApp() },
                                        onRefreshShizukuStatus = { refreshShizukuStatus() },
                                        pcClientsCount = tcpClientsCountState.value,
                                        remoteSenderStatus = remoteSenderStatusState.value,
                                        remoteReceiverClientsCount = remoteReceiverClientsCountState.value,
                                        onConnectRemoteSender = { connectRemoteSender() },
                                        onResetConnection = { resetConnection() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Floating real-time log overlay at the top (drawn last to be on top of everything)
                    if (settings.showLogConsole) {
                        LogConsoleOverlay(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun triggerVibration(isBigNote: Boolean = false) {
        val settings = settingsState.value
        if (!settings.vibration || settings.vibrationStrengthPercent <= 0) return
        try {
            val baseDuration = if (isBigNote) 24L else 12L
            val strengthFactor = settings.vibrationStrengthPercent / 100f
            val duration = (baseDuration * strengthFactor).toLong().coerceAtLeast(1L)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val baseAmp = if (isBigNote) 255 else 180
                val amplitude = (baseAmp * strengthFactor).toInt().coerceIn(1, 255)
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Suppress error if vibration is disabled or not permitted
        }
    }

    private fun connectToWebSocket() {
        // Obsolete connection mode
    }

    private fun triggerAdbConnect() {
        val settings = settingsState.value
        val portInt = settings.webAdbPort.toIntOrNull() ?: 5555
        adbStatusState.value = "connecting"
        adbErrorState.value = null

        adbClient?.testConnection(settings.webAdbHost, portInt) { success, error ->
            runOnUiThread {
                if (success) {
                    adbStatusState.value = "connected"
                } else {
                    adbStatusState.value = "error"
                    adbErrorState.value = error ?: "接続に失敗しました。ワイヤレスデバッグを有効にしてください。"
                }
            }
        }
    }

    private fun triggerAdbPair() {
        val settings = settingsState.value
        val portInt = settings.webAdbPairingPort.toIntOrNull() ?: 5555
        adbStatusState.value = "connecting"
        adbErrorState.value = null

        adbClient?.pairDevice(settings.webAdbHost, portInt, settings.webAdbPairingCode) { success, error ->
            runOnUiThread {
                if (success) {
                    adbStatusState.value = "disconnected"
                    // Successful pairing callback prompt
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("ペアリング成功")
                        .setMessage("ペアリングに成功しました！次に接続ボタンを押してください。")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    adbStatusState.value = "error"
                    adbErrorState.value = error ?: "ペアリングに失敗しました"
                }
            }
        }
    }

    private fun dispatchPhysicalKey(part: String, key: String, isPressed: Boolean, fromTouch: Boolean) {
        val settings = settingsState.value

        when (settings.connectionMode) {
            "shizuku" -> {
                if (fromTouch && key.isNotEmpty()) {
                    val emulationMode = settings.shizukuEmulationMode
                    adbClient?.setEmulationMode(emulationMode)
                    adbClient?.setInjectionMethod(settings.injectionMethod)
                    adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                    adbClient?.sendKeyEvent(part, key, isPressed, settings.simultaneousGroupingMs)
                }
            }
            "usb-wired" -> {
                if (fromTouch && key.isNotEmpty()) {
                    tcpServer?.sendKeyEvent(key, isPressed)
                }
            }
            "another_android" -> {
                if (fromTouch && key.isNotEmpty()) {
                    if (settings.anotherAndroidRole == "sender") {
                        remoteSender?.sendKeyEvent(part, isPressed)
                    } else {
                        // Receiver mode: Inject locally on this device via Shizuku
                        val emulationMode = settings.shizukuEmulationMode
                        adbClient?.setEmulationMode(emulationMode)
                        adbClient?.setInjectionMethod(settings.injectionMethod)
                        adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                        adbClient?.sendKeyEvent(part, key, isPressed, settings.simultaneousGroupingMs)
                    }
                }
            }
            "local-demo" -> {
                // Free/Local play mode: Sound & Vibration only, no key injection or TCP transmission
            }
        }
    }

    private fun connectRemoteSender() {
        val settings = settingsState.value
        val isWired = settings.anotherAndroidConnectionType == "wired"
        val port = settings.anotherAndroidPort.toIntOrNull() ?: 60002

        remoteSenderStatusState.value = "connecting"
        remoteSender?.disconnect()

        val sender = TaikoAndroidRemoteSender()
        remoteSender = sender

        if (isWired) {
            // Collect USB Direct driver status
            lifecycleScope.launch {
                TaikoUsbDirectManager.isConnectedState.collect { isUsbConnected ->
                    if (isUsbConnected && settingsState.value.anotherAndroidConnectionType == "wired") {
                        remoteSenderStatusState.value = "connected"
                        TaikoLogManager.log("USB 有線直接通信 (AOA / USB Direct): 接続完了! (<1ms)")
                    }
                }
            }

            val ip = settings.anotherAndroidTargetIp.trim()
            if (ip.isNotEmpty()) {
                sender.connect(ip, port, object : TaikoAndroidRemoteSender.ConnectionListener {
                    override fun onConnected() {
                        runOnUiThread {
                            remoteSenderStatusState.value = "connected"
                            TaikoLogManager.log("USB 有線ネットワーク: 受信側 ($ip:$port) に接続完了")
                        }
                    }

                    override fun onDisconnected() {
                        runOnUiThread {
                            if (!TaikoUsbDirectManager.isConnectedState.value) {
                                remoteSenderStatusState.value = "disconnected"
                            }
                        }
                    }

                    override fun onError(error: String) {
                        runOnUiThread {
                            if (!TaikoUsbDirectManager.isConnectedState.value) {
                                remoteSenderStatusState.value = "error"
                                TaikoLogManager.log("USB 有線通信: $error")
                            }
                        }
                    }
                })
            } else {
                // Auto-detect IP on wired USB interfaces
                TaikoAndroidRemoteSender.scanAndFindReceiverIp(
                    targetPort = port,
                    connectionType = "wired",
                    onFound = { foundIp ->
                        runOnUiThread {
                            val currentSettings = settingsState.value
                            updateAndPersistSettings(currentSettings.copy(anotherAndroidTargetIp = foundIp))
                            sender.connect(foundIp, port, object : TaikoAndroidRemoteSender.ConnectionListener {
                                override fun onConnected() {
                                    runOnUiThread {
                                        remoteSenderStatusState.value = "connected"
                                        TaikoLogManager.log("USB 有線ネットワーク: 受信側 ($foundIp:$port) に接続完了")
                                    }
                                }
                                override fun onDisconnected() {
                                    runOnUiThread {
                                        if (!TaikoUsbDirectManager.isConnectedState.value) {
                                            remoteSenderStatusState.value = "disconnected"
                                        }
                                    }
                                }
                                override fun onError(error: String) {
                                    runOnUiThread {
                                        if (!TaikoUsbDirectManager.isConnectedState.value) {
                                            remoteSenderStatusState.value = "error"
                                        }
                                    }
                                }
                            })
                        }
                    },
                    onNotFound = {
                        runOnUiThread {
                            if (!TaikoUsbDirectManager.isConnectedState.value) {
                                remoteSenderStatusState.value = "connecting"
                                TaikoLogManager.log("USB 有線通信: AOA / USBテザリング接続の待機中...")
                            }
                        }
                    }
                )
            }
        } else {
            // Wireless Wi-Fi Mode
            val ip = settings.anotherAndroidTargetIp.trim()
            if (ip.isEmpty()) {
                remoteSenderStatusState.value = "error"
                TaikoLogManager.log("無線 (Wi-Fi) モード: 受信側 (ゲーム) のIPアドレスを入力するか、「自動検出」ボタンを押してください。")
                return
            }

            sender.connect(ip, port, object : TaikoAndroidRemoteSender.ConnectionListener {
                override fun onConnected() {
                    runOnUiThread {
                        remoteSenderStatusState.value = "connected"
                        TaikoLogManager.log("Remote Sender: Connected to receiver at $ip:$port")
                    }
                }

                override fun onDisconnected() {
                    runOnUiThread {
                        remoteSenderStatusState.value = "disconnected"
                        TaikoLogManager.log("Remote Sender: Disconnected")
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        remoteSenderStatusState.value = "error"
                        TaikoLogManager.log("Remote Sender Error: $error")
                    }
                }
            })
        }
    }

    private fun handleIncomingRemoteKeys(keys: List<String>, isPressed: Boolean) {
        val settingsCur = settingsState.value
        val emulationMode = settingsCur.shizukuEmulationMode
        adbClient?.setEmulationMode(emulationMode)
        adbClient?.setInjectionMethod(settingsCur.injectionMethod)
        adbClient?.setGamepadKeyConfig(settingsCur.gamepadKeyConfig)

        val parts = keys.mapNotNull { rawKeyOrPart ->
            when (rawKeyOrPart) {
                "leftDon", "leftKat", "rightDon", "rightKat" -> rawKeyOrPart
                "F", "f" -> "leftDon"
                "J", "j" -> "rightDon"
                "D", "d" -> "leftKat"
                "K", "k" -> "rightKat"
                else -> {
                    val keyUpper = rawKeyOrPart.uppercase()
                    when {
                        keyUpper == settingsCur.keyConfig.leftDon.uppercase() || keyUpper == settingsCur.gamepadKeyConfig.leftDon.uppercase() -> "leftDon"
                        keyUpper == settingsCur.keyConfig.rightDon.uppercase() || keyUpper == settingsCur.gamepadKeyConfig.rightDon.uppercase() -> "rightDon"
                        keyUpper == settingsCur.keyConfig.leftKat.uppercase() || keyUpper == settingsCur.gamepadKeyConfig.leftKat.uppercase() -> "leftKat"
                        keyUpper == settingsCur.keyConfig.rightKat.uppercase() || keyUpper == settingsCur.gamepadKeyConfig.rightKat.uppercase() -> "rightKat"
                        else -> "leftDon"
                    }
                }
            }
        }

        if (parts.isNotEmpty()) {
            runOnUiThread {
                triggerMultiInputs(parts.map { it to isPressed }, fromTouch = false)
            }
        }
    }

    private fun startRemoteReceiver() {
        val settings = settingsState.value
        val port = settings.anotherAndroidPort.toIntOrNull() ?: 60002

        remoteReceiver?.stop()
        val receiver = TaikoAndroidRemoteReceiver { keys: List<String>, isPressed: Boolean ->
            handleIncomingRemoteKeys(keys, isPressed)
        }

        remoteReceiver = receiver
        receiver.start(port) { activeCount ->
            runOnUiThread {
                remoteReceiverClientsCountState.value = activeCount
            }
        }
        TaikoLogManager.log("Remote Receiver: Listening on port $port")
    }

    private fun stopRemoteReceiver() {
        remoteReceiver?.stop()
        remoteReceiver = null
        remoteReceiverClientsCountState.value = 0
    }

    private fun stopRemoteSender() {
        remoteSender?.disconnect()
        remoteSender = null
        remoteSenderStatusState.value = "disconnected"
    }

    private fun onConnectionModeChanged(oldMode: String, newMode: String) {
        val settings = settingsState.value
        if (newMode == "usb-wired") {
            TaikoUsbDirectManager.stop(this)
            startTcpServer()
            stopRemoteReceiver()
            stopRemoteSender()
            TaikoLogManager.log("Switched to USB-Wired mode: Started TCP server on port 60001")
        } else if (newMode == "another_android") {
            stopTcpServer()
            // Force reset existing sockets when connection parameters change
            stopRemoteSender()
            stopRemoteReceiver()

            if (settings.anotherAndroidConnectionType == "wired") {
                TaikoUsbDirectManager.start(this)
            } else {
                TaikoUsbDirectManager.stop(this)
            }

            if (settings.anotherAndroidRole == "receiver") {
                startRemoteReceiver()
            } else {
                connectRemoteSender()
            }
            TaikoLogManager.log("Switched to Another Android mode (Role=${settings.anotherAndroidRole}, Type=${settings.anotherAndroidConnectionType})")
        } else {
            TaikoUsbDirectManager.stop(this)
            stopTcpServer()
            stopRemoteReceiver()
            stopRemoteSender()
            TaikoLogManager.log("Switched to $newMode mode: Stopped external connections")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        TaikoLogManager.log("App received new Intent (USB attach/permission event)")
        val settings = settingsState.value
        if (settings.anotherAndroidConnectionType == "wired") {
            TaikoUsbDirectManager.restart(this)
        }
    }

    private fun resetConnection() {
        TaikoLogManager.log("⚡ 通信・ポート再初期化を開始します...")
        val settings = settingsState.value

        stopTcpServer()
        stopRemoteSender()
        stopRemoteReceiver()
        TaikoUsbDirectManager.stop(this)

        lifecycleScope.launch {
            delay(150)
            if (settings.connectionMode == "usb-wired") {
                startTcpServer()
                TaikoLogManager.log("USB有線モード: TCP サーバー再起動完了")
            } else if (settings.connectionMode == "another_android") {
                if (settings.anotherAndroidConnectionType == "wired") {
                    TaikoUsbDirectManager.start(this@MainActivity)
                    delay(300)
                    TaikoUsbDirectManager.restart(this@MainActivity)
                }
                if (settings.anotherAndroidRole == "receiver") {
                    startRemoteReceiver()
                } else {
                    connectRemoteSender()
                }
                TaikoLogManager.log("Another Android モード (${settings.anotherAndroidRole}) 再接続完了")
            } else {
                stopTcpServer()
            }
            Toast.makeText(this@MainActivity, "⚡ 通信・ポートを再初期化しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTcpServer() {
        try {
            tcpServer?.start(60001)
        } catch (e: Exception) {
            TaikoLogManager.log("Failed to start TCP server: ${e.message}")
        }
    }

    private fun stopTcpServer() {
        try {
            tcpServer?.stop()
        } catch (e: Exception) {
            TaikoLogManager.log("Failed to stop TCP server: ${e.message}")
        }
    }

    // Main single-input router from the stage taps
    private fun triggerInput(part: String, isPressed: Boolean, fromTouch: Boolean = true) {
        val settings = settingsState.value

        // 1. Update UI Highlight State immediately
        updateActiveInputsState(part, isPressed)

        // 2. Generate key mapping code based on current active emulation mode
        val activeEmulationMode = settings.activeEmulationMode
        val keyChar = if (activeEmulationMode == "gamepad") {
            when (part) {
                "leftKat" -> settings.gamepadKeyConfig.leftKat
                "leftDon" -> settings.gamepadKeyConfig.leftDon
                "rightDon" -> settings.gamepadKeyConfig.rightDon
                "rightKat" -> settings.gamepadKeyConfig.rightKat
                else -> ""
            }
        } else {
            when (part) {
                "leftKat" -> settings.keyConfig.leftKat
                "leftDon" -> settings.keyConfig.leftDon
                "rightDon" -> settings.keyConfig.rightDon
                "rightKat" -> settings.keyConfig.rightKat
                else -> ""
            }
        }
        if (keyChar.isEmpty()) return

        if (isPressed) {
            val now = System.currentTimeMillis()
            lastPressTimestamps[part] = now
            activeRepeatJobs[part]?.cancel()
            activeRepeatJobs.remove(part)

            val pendingJob = pendingReleaseJobs[part]
            if (pendingJob != null) {
                pendingJob.cancel()
                pendingReleaseJobs.remove(part)
                TaikoLogManager.log("Touch Down Overlap: $part -> key=$keyChar. Canceling pending release, forcing release/re-press!")
                // Force an immediate release event, then repress after 5ms so the game registers separate hits
                dispatchPhysicalKey(part, keyChar, false, fromTouch)
                lifecycleScope.launch(Dispatchers.IO) {
                    delay(5)
                    dispatchPhysicalKey(part, keyChar, true, fromTouch)
                }
            } else {
                TaikoLogManager.log("Touch Down: $part -> key=$keyChar")
                dispatchPhysicalKey(part, keyChar, true, fromTouch)
            }
            
            // Handle repeat logic: Only repeat rapidly when Turbo (Auto-Repeat) is explicitly enabled.
            // For standard keyboard/gamepad behavior, we send a single Down event and stay pressed down.
            if (settings.isTurboEnabled) {
                TaikoLogManager.log("Turbo Enabled: auto-repeating $part every ${settings.turboIntervalMs}ms")
                activeRepeatJobs[part] = lifecycleScope.launch(Dispatchers.IO) {
                    val interval = settings.turboIntervalMs.toLong()
                    while (isActive) {
                        delay(interval)
                        dispatchPhysicalKey(part, keyChar, false, fromTouch)
                        delay(10L)
                        dispatchPhysicalKey(part, keyChar, true, fromTouch)
                    }
                }
            } else {
                TaikoLogManager.log("Standard Press & Hold Mode: single press held down for $part")
            }
        } else {
            activeRepeatJobs[part]?.cancel()
            activeRepeatJobs.remove(part)
            
            val pressTime = lastPressTimestamps[part] ?: 0L
            val elapsed = System.currentTimeMillis() - pressTime
            val minDuration = settings.minPressDurationMs.toLong()
            TaikoLogManager.log("Touch Up: $part (actual hold: ${elapsed}ms)")
            if (elapsed < minDuration) {
                val delayMs = minDuration - elapsed
                TaikoLogManager.log("Touch Up Hold: $part hold was ${elapsed}ms < minPress ${minDuration}ms. Delaying release by ${delayMs}ms to ensure registration.")
                val job = lifecycleScope.launch(Dispatchers.IO) {
                    delay(delayMs)
                    dispatchPhysicalKey(part, keyChar, false, fromTouch)
                    pendingReleaseJobs.remove(part)
                }
                pendingReleaseJobs[part] = job
            } else {
                dispatchPhysicalKey(part, keyChar, false, fromTouch)
            }
        }
    }

    // Main multi-input router (handles big note simultaneity)
    private fun triggerMultiInputs(inputs: List<Pair<String, Boolean>>, fromTouch: Boolean = true) {
        val settings = settingsState.value

        // 1. Update UI highlights
        inputs.forEach { (part, isPressed) ->
            updateActiveInputsState(part, isPressed)
        }

        val activeEmulationMode = settings.activeEmulationMode

        // Fast-path batch multi-key dispatch for big notes
        if (fromTouch && inputs.isNotEmpty()) {
            val isAllSameAction = inputs.all { it.second == inputs[0].second }
            val actionIsPressed = inputs[0].second

            if (isAllSameAction) {
                if (settings.connectionMode == "another_android" && settings.anotherAndroidRole == "sender") {
                    val parts = inputs.map { it.first }
                    if (parts.isNotEmpty()) {
                        remoteSender?.sendMultiKeyEvents(parts, actionIsPressed)
                        return
                    }
                } else if (settings.connectionMode == "usb-wired") {
                    val keys = inputs.map { (part, _) ->
                        when (part) {
                            "leftKat" -> settings.keyConfig.leftKat
                            "leftDon" -> settings.keyConfig.leftDon
                            "rightDon" -> settings.keyConfig.rightDon
                            "rightKat" -> settings.keyConfig.rightKat
                            else -> ""
                        }
                    }.filter { it.isNotEmpty() }
                    if (keys.isNotEmpty()) {
                        tcpServer?.sendMultiKeyEvents(keys, actionIsPressed)
                        return
                    }
                } else if (settings.connectionMode == "shizuku" || (settings.connectionMode == "another_android" && settings.anotherAndroidRole == "receiver")) {
                    val items = inputs.mapNotNull { (part, _) ->
                        val keyChar = if (activeEmulationMode == "gamepad") {
                            when (part) {
                                "leftKat" -> settings.gamepadKeyConfig.leftKat
                                "leftDon" -> settings.gamepadKeyConfig.leftDon
                                "rightDon" -> settings.gamepadKeyConfig.rightDon
                                "rightKat" -> settings.gamepadKeyConfig.rightKat
                                else -> ""
                            }
                        } else {
                            when (part) {
                                "leftKat" -> settings.keyConfig.leftKat
                                "leftDon" -> settings.keyConfig.leftDon
                                "rightDon" -> settings.keyConfig.rightDon
                                "rightKat" -> settings.keyConfig.rightKat
                                else -> ""
                            }
                        }
                        if (keyChar.isNotEmpty()) part to keyChar else null
                    }
                    if (items.isNotEmpty()) {
                        val emulationMode = settings.shizukuEmulationMode
                        adbClient?.setEmulationMode(emulationMode)
                        adbClient?.setInjectionMethod(settings.injectionMethod)
                        adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                        adbClient?.sendMultiKeyEvents(items, actionIsPressed, settings.simultaneousGroupingMs)
                        return
                    }
                }
            }
        }

        // 2. Map and delegate individually
        inputs.forEach { (part, isPressed) ->
            val keyChar = if (activeEmulationMode == "gamepad") {
                when (part) {
                    "leftKat" -> settings.gamepadKeyConfig.leftKat
                    "leftDon" -> settings.gamepadKeyConfig.leftDon
                    "rightDon" -> settings.gamepadKeyConfig.rightDon
                    "rightKat" -> settings.gamepadKeyConfig.rightKat
                    else -> ""
                }
            } else {
                when (part) {
                    "leftKat" -> settings.keyConfig.leftKat
                    "leftDon" -> settings.keyConfig.leftDon
                    "rightDon" -> settings.keyConfig.rightDon
                    "rightKat" -> settings.keyConfig.rightKat
                    else -> ""
                }
            }
            if (keyChar.isNotEmpty()) {
                if (isPressed) {
                    lastPressTimestamps[part] = System.currentTimeMillis()
                    activeRepeatJobs[part]?.cancel()
                    activeRepeatJobs.remove(part)

                    val pendingJob = pendingReleaseJobs[part]
                    if (pendingJob != null) {
                        pendingJob.cancel()
                        pendingReleaseJobs.remove(part)
                        // Force an immediate release event, then repress after 5ms so the game registers separate hits
                        dispatchPhysicalKey(part, keyChar, false, fromTouch)
                        lifecycleScope.launch(Dispatchers.IO) {
                            delay(5)
                            dispatchPhysicalKey(part, keyChar, true, fromTouch)
                        }
                    } else {
                        dispatchPhysicalKey(part, keyChar, true, fromTouch)
                    }
                    
                    // Handle repeat logic: Only repeat rapidly when Turbo (Auto-Repeat) is explicitly enabled.
                    // For standard keyboard/gamepad behavior, we send a single Down event and stay pressed down.
                    if (settings.isTurboEnabled) {
                        activeRepeatJobs[part] = lifecycleScope.launch(Dispatchers.IO) {
                            val interval = settings.turboIntervalMs.toLong()
                            while (isActive) {
                                delay(interval)
                                dispatchPhysicalKey(part, keyChar, false, fromTouch)
                                delay(10L)
                                dispatchPhysicalKey(part, keyChar, true, fromTouch)
                            }
                        }
                    }
                } else {
                    activeRepeatJobs[part]?.cancel()
                    activeRepeatJobs.remove(part)
                    
                    val pressTime = lastPressTimestamps[part] ?: 0L
                    val elapsed = System.currentTimeMillis() - pressTime
                    val minDuration = settings.minPressDurationMs.toLong()
                    if (elapsed < minDuration) {
                        val job = lifecycleScope.launch(Dispatchers.IO) {
                            delay(minDuration - elapsed)
                            dispatchPhysicalKey(part, keyChar, false, fromTouch)
                            pendingReleaseJobs.remove(part)
                        }
                        pendingReleaseJobs[part] = job
                    } else {
                        dispatchPhysicalKey(part, keyChar, false, fromTouch)
                    }
                }
            }
        }
    }

    private fun updateActiveInputsState(part: String, isPressed: Boolean) {
        val prev = activeInputsState.value
        activeInputsState.value = when (part) {
            "leftKat" -> prev.copy(leftKat = isPressed)
            "leftDon" -> prev.copy(leftDon = isPressed)
            "rightDon" -> prev.copy(rightDon = isPressed)
            "rightKat" -> prev.copy(rightKat = isPressed)
            else -> prev
        }
    }

    // --- Support Physical Key Bindings (Zero-latency Keyboard/Gamepad integration) ---
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = settingsState.value
        val part = when (keyCode) {
            KeyEvent.KEYCODE_D -> "leftKat"
            KeyEvent.KEYCODE_F -> "leftDon"
            KeyEvent.KEYCODE_J -> "rightDon"
            KeyEvent.KEYCODE_K -> "rightKat"
            else -> null
        }

        if (part != null) {
            if (event?.repeatCount == 0) {
                // Play audio local feedback
                if (settings.soundEffects) {
                    if (part.contains("Don")) {
                        audioPlayer?.playDon(settings.soundVolume)
                    } else {
                        audioPlayer?.playKat(settings.soundVolume)
                    }
                }
                // Play vibe
                if (settings.vibration) {
                    triggerVibration()
                }

                // Split big note assistance
                if (settings.singleHandBigNotes) {
                    if (part.contains("Don")) {
                        triggerMultiInputs(listOf("leftDon" to true, "rightDon" to true), fromTouch = false)
                    } else {
                        triggerMultiInputs(listOf("leftKat" to true, "rightKat" to true), fromTouch = false)
                    }
                } else {
                    triggerInput(part, true, fromTouch = false)
                }
            }
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = settingsState.value
        val part = when (keyCode) {
            KeyEvent.KEYCODE_D -> "leftKat"
            KeyEvent.KEYCODE_F -> "leftDon"
            KeyEvent.KEYCODE_J -> "rightDon"
            KeyEvent.KEYCODE_K -> "rightKat"
            else -> null
        }

        if (part != null) {
            if (settings.singleHandBigNotes) {
                if (part.contains("Don")) {
                    triggerMultiInputs(listOf("leftDon" to false, "rightDon" to false), fromTouch = false)
                } else {
                    triggerMultiInputs(listOf("leftKat" to false, "rightKat" to false), fromTouch = false)
                }
            } else {
                triggerInput(part, false, fromTouch = false)
            }
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    private fun updateAndPersistSettings(newSettings: ControllerSettings) {
        val oldSettings = settingsState.value
        settingsState.value = newSettings
        saveSettings(newSettings)

        if (oldSettings.connectionMode != newSettings.connectionMode ||
            oldSettings.anotherAndroidConnectionType != newSettings.anotherAndroidConnectionType ||
            oldSettings.anotherAndroidRole != newSettings.anotherAndroidRole) {

            onConnectionModeChanged(oldSettings.connectionMode, newSettings.connectionMode)
        }

        TaikoLogManager.log("Settings updated & auto-saved: mode=${newSettings.connectionMode}, connType=${newSettings.anotherAndroidConnectionType}, role=${newSettings.anotherAndroidRole}")
    }

    private fun loadPersistedSettings() {
        try {
            val prefs = getSharedPreferences("taiko_controller_settings", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("settings_json", null)
            if (!jsonStr.isNullOrEmpty()) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val loaded = json.decodeFromString<ControllerSettings>(jsonStr)
                settingsState.value = loaded
                TaikoLogManager.log("Persisted settings loaded successfully (mode: ${loaded.connectionMode}, shizukuEmulation: ${loaded.shizukuEmulationMode}, usbEmulation: ${loaded.usbEmulationMode}).")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load settings from prefs", e)
        }
    }

    private fun saveSettings(settings: ControllerSettings) {
        try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val jsonStr = json.encodeToString(ControllerSettings.serializer(), settings)
            val prefs = getSharedPreferences("taiko_controller_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("settings_json", jsonStr).apply()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to save settings to prefs", e)
        }
    }

    override fun onStart() {
        super.onStart()
        val settings = settingsState.value
        if (settings.connectionMode == "usb-wired") {
            startTcpServer()
        } else if (settings.connectionMode == "another_android") {
            if (settings.anotherAndroidRole == "receiver") {
                startRemoteReceiver()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopTcpServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
            Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
            Shizuku.removeRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {}
        TaikoUsbDirectManager.stop(this)
        activeRepeatJobs.values.forEach { it.cancel() }
        activeRepeatJobs.clear()
        audioPlayer?.release()
        audioPlayer = null
        webSocketClient?.disconnect()
        webSocketClient = null
        adbClient?.release()
        adbClient = null
        tcpServer?.stop()
        tcpServer = null
        stopRemoteReceiver()
        stopRemoteSender()
    }
}
