package com.example

import android.Manifest
import android.provider.Settings
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import rikka.shizuku.Shizuku
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    companion object {
        var instance: MainActivity? = null
            private set
    }

    // Native audio synthesizer and player
    private var audioPlayer: TaikoAudioPlayer? = null
    
    // WebSocket client manager
    private var webSocketClient: TaikoWebSocketClient? = null
    
    // Local direct shell key injector
    private var adbClient: AdbWirelessClient? = null

    // Local TCP Server for USB-Wired PC Connection
    private var tcpServer: TaikoTcpServer? = null
    private val tcpClientsCountState = mutableStateOf(0)

    // Another Android Remote Connection (Wi-Fi & Wired)
    private var remoteSender: TaikoAndroidRemoteSender? = null
    private var remoteReceiver: TaikoAndroidRemoteReceiver? = null
    private val remoteSenderStatusState = mutableStateOf("disconnected")
    private val remoteReceiverClientsCountState = mutableStateOf(0)

    // Bluetooth Remote Connection
    private var bluetoothSender: TaikoBluetoothSender? = null
    private var bluetoothReceiver: TaikoBluetoothReceiver? = null
    private val bluetoothSenderStatusState = mutableStateOf("disconnected")
    private val bluetoothConnectedDeviceNameState = mutableStateOf<String?>(null)
    private val bluetoothReceiverConnectedDeviceState = mutableStateOf<String?>(null)
    private val bluetoothPairedDevicesState = mutableStateOf<List<TaikoBluetoothManager.BluetoothDeviceInfo>>(emptyList())

    // Vibration hardware service
    private var vibrator: Vibrator? = null

    // Last pressed timestamps to guarantee a minimum key-press duration of 40ms for high reliability
    private val lastPressTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // Active software-repeat jobs for non-root / WebSocket connections
    private val activeRepeatJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    // Active release jobs to prevent early releases on fast multi-tap overlaps
    private val pendingReleaseJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    // Track physically held parts on this device to prevent race conditions during rapid rolling
    private val physicallyHeldParts = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
        runOnUiThread {
            try {
                shizukuPermissionGranted.value = (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED)
                if (shizukuPermissionGranted.value) {
                    TaikoLogManager.log("Shizuku権限: 承認されました")
                } else {
                    TaikoLogManager.log("Shizuku権限: 拒否されました")
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Error in shizukuListener", e)
            }
        }
    }

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            try {
                val ping = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
                shizukuInstalledAndRunning.value = ping
                if (ping) {
                    shizukuPermissionGranted.value = try {
                        Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } catch (_: Throwable) { false }
                } else {
                    shizukuPermissionGranted.value = false
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Error in onBinderReceived", e)
            }
        }
    }

    private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            try {
                shizukuInstalledAndRunning.value = false
                shizukuPermissionGranted.value = false
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "Error in onBinderDead", e)
            }
        }
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            refreshBluetoothDevices()
            val settings = settingsState.value
            if (settings.connectionMode == "another_android" && settings.anotherAndroidConnectionType == "bluetooth") {
                if (settings.anotherAndroidRole == "receiver") {
                    startBluetoothReceiver()
                } else if (settings.anotherAndroidBluetoothDeviceAddress.isNotEmpty()) {
                    connectBluetoothSender(settings.anotherAndroidBluetoothDeviceAddress, settings.anotherAndroidBluetoothDeviceName)
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestBluetoothPermissions(onGranted: () -> Unit) {
        if (TaikoBluetoothManager.hasBluetoothPermissions(this)) {
            onGranted()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    )
                )
            } else {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            }
        }
    }

    private fun requestShizukuPermission() {
        try {
            val ping = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            if (ping) {
                val isGranted = try {
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) { false }
                if (isGranted) {
                    shizukuPermissionGranted.value = true
                    Toast.makeText(this, "Shizuku権限は既に承認されています", Toast.LENGTH_SHORT).show()
                } else {
                    Shizuku.requestPermission(1001)
                }
            } else {
                shizukuInstalledAndRunning.value = false
                shizukuPermissionGranted.value = false
                Toast.makeText(this, "Shizukuサービスが起動していません", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to request Shizuku permission", e)
            Toast.makeText(this, "Shizuku権限の要求に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
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
        } catch (e: Throwable) {
            try {
                val uri = android.net.Uri.parse("https://shizuku.rikka.app/")
                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            } catch (ex: Throwable) {
                android.util.Log.e("MainActivity", "Failed to open Shizuku app/website", ex)
            }
        }
    }

    private fun refreshShizukuStatus() {
        try {
            val ping = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            shizukuInstalledAndRunning.value = ping
            if (ping) {
                shizukuPermissionGranted.value = try {
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) { false }
            } else {
                shizukuPermissionGranted.value = false
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to manual check Shizuku status", e)
        }
    }

    private val showOverlayPermissionDialogState = mutableStateOf(false)

    fun getSettings(): ControllerSettings = settingsState.value

    fun triggerOverlayInput(part: String, isPressed: Boolean) {
        runOnUiThread {
            triggerInput(part, isPressed, fromTouch = true)
        }
    }

    fun triggerOverlayMultiInputs(inputs: List<Pair<String, Boolean>>) {
        runOnUiThread {
            triggerMultiInputs(inputs, fromTouch = true)
        }
    }

    private fun checkAndLaunchOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                showOverlayPermissionDialogState.value = true
                return
            }
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        OverlayService.start(this)
    }

    private fun openOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(this, "オーバーレイ設定画面を開けませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
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
            shizukuInstalledAndRunning.value = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            if (shizukuInstalledAndRunning.value) {
                shizukuPermissionGranted.value = try {
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                } catch (_: Throwable) { false }
                TaikoLogManager.log("Shizuku status: Installed & Running (Granted=${shizukuPermissionGranted.value})")
            } else {
                TaikoLogManager.log("Shizuku status: Not running or not installed")
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Shizuku initialization failed", e)
            TaikoLogManager.log("Shizuku init failed: ${e.message}")
        }

        // Active background polling routine to check Shizuku status every second
        lifecycleScope.launch {
            while (isActive) {
                try {
                    val ping = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
                    if (shizukuInstalledAndRunning.value != ping) {
                        shizukuInstalledAndRunning.value = ping
                    }
                    if (ping) {
                        val granted = try {
                            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } catch (_: Throwable) { false }
                        if (shizukuPermissionGranted.value != granted) {
                            shizukuPermissionGranted.value = granted
                        }
                    } else {
                        if (shizukuPermissionGranted.value) {
                            shizukuPermissionGranted.value = false
                        }
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MainActivity", "Error in Shizuku status polling", e)
                }
                delay(1000)
            }
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
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

            // 画面分割(Split Screen)時のフォーカス問題対策 & 全画面時のステータスバー格納:
            // 全画面コントローラーのときは FLAG_NOT_FOCUSABLE を設定し、タップしても上画面のエミュレーターからフォーカスを奪わないようにします。
            // また、ステータスバー・ナビゲーションバーを自動的に格納（Immersive Mode）して戻るボタンや画面端の操作を快適にします。
            LaunchedEffect(isFullScreen) {
                resetAllInputs()
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (isFullScreen) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                    Toast.makeText(context, "全画面モード: 終了ボタンを長押しで閉じます", Toast.LENGTH_SHORT).show()
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            var appWidthPx by remember { mutableFloatStateOf(0f) }
            var appHeightPx by remember { mutableFloatStateOf(0f) }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(if (isFullScreen) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing)
                    .onGloballyPositioned { coordinates ->
                        appWidthPx = coordinates.size.width.toFloat()
                        appHeightPx = coordinates.size.height.toFloat()
                    },
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

                            // Overlaid exit button with safe insets padding and clear visibility
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                                    .padding(top = 12.dp, end = 12.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                try {
                                                    // Trigger right kat immediately on press
                                                    triggerInput("rightKat", true)
                                                    if (settings.soundEffects && audioPlayer != null) {
                                                        audioPlayer?.playKat(settings.soundVolume)
                                                    }
                                                    triggerVibration(false)
                                                    tryAwaitRelease()
                                                } finally {
                                                    triggerInput("rightKat", false)
                                                }
                                            },
                                            onLongPress = {
                                                triggerInput("rightKat", false)
                                                resetAllInputs()
                                                isFullScreen = false
                                            }
                                        )
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "長押しで全画面を終了",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFDF6E2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                            contentDescription = "アプリアイコン",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
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
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val appAspect = if (appHeightPx > 0f) appWidthPx / appHeightPx else 16f / 9f
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
                                                modifier = Modifier
                                                    .aspectRatio(appAspect, matchHeightConstraintsFirst = false)
                                                    .fillMaxSize()
                                            )
                                        }
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
                                            onStartOverlay = { checkAndLaunchOverlay() },
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
                                            bluetoothSenderStatus = bluetoothSenderStatusState.value,
                                            bluetoothConnectedDeviceName = bluetoothConnectedDeviceNameState.value,
                                            bluetoothReceiverConnectedDevice = bluetoothReceiverConnectedDeviceState.value,
                                            bluetoothPairedDevices = bluetoothPairedDevicesState.value,
                                            onConnectBluetoothDevice = { addr, name -> connectBluetoothSender(addr, name) },
                                            onDisconnectBluetooth = { disconnectBluetoothSender() },
                                            onRefreshBluetoothDevices = { refreshBluetoothDevices() },
                                            onOpenBluetoothSettings = { openBluetoothSettings() },
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
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val appAspect = if (appHeightPx > 0f) appWidthPx / appHeightPx else 16f / 9f
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
                                            modifier = Modifier
                                                .aspectRatio(appAspect, matchHeightConstraintsFirst = true)
                                                .fillMaxSize()
                                        )
                                    }

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
                                        onStartOverlay = { checkAndLaunchOverlay() },
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
                                        bluetoothSenderStatus = bluetoothSenderStatusState.value,
                                        bluetoothConnectedDeviceName = bluetoothConnectedDeviceNameState.value,
                                        bluetoothReceiverConnectedDevice = bluetoothReceiverConnectedDeviceState.value,
                                        bluetoothPairedDevices = bluetoothPairedDevicesState.value,
                                        onConnectBluetoothDevice = { addr, name -> connectBluetoothSender(addr, name) },
                                        onDisconnectBluetooth = { disconnectBluetoothSender() },
                                        onRefreshBluetoothDevices = { refreshBluetoothDevices() },
                                        onOpenBluetoothSettings = { openBluetoothSettings() },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Overlay Permission Dialog
                    if (showOverlayPermissionDialogState.value) {
                        AlertDialog(
                            onDismissRequest = { showOverlayPermissionDialogState.value = false },
                            title = {
                                Text("🪟 「他のアプリの上に重ねて表示」の許可", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            },
                            text = {
                                Text(
                                    "太鼓コントローラーを他のアプリの画面上にオーバーレイ表示するため、システム設定で「他のアプリの上に重ねて表示」を許可してください。\n\n許可後に再度「オーバーレイ」ボタンを押すと起動します。",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showOverlayPermissionDialogState.value = false
                                        openOverlayPermissionSettings()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("設定を開く", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showOverlayPermissionDialogState.value = false }) {
                                    Text("キャンセル")
                                }
                            }
                        )
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
                if (key.isNotEmpty()) {
                    try {
                        val emulationMode = settings.shizukuEmulationMode
                        adbClient?.setEmulationMode(emulationMode)
                        adbClient?.setInjectionMethod(settings.injectionMethod)
                        adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                        adbClient?.sendKeyEvent(part, key, isPressed, settings.simultaneousGroupingMs)
                    } catch (t: Throwable) {
                        android.util.Log.e("MainActivity", "Error sending key event via Shizuku", t)
                    }
                }
            }
            "usb-wired" -> {
                if (key.isNotEmpty()) {
                    try {
                        tcpServer?.sendKeyEvent(key, isPressed)
                    } catch (t: Throwable) {
                        android.util.Log.e("MainActivity", "Error sending key event via TCP server", t)
                    }
                }
            }
            "another_android" -> {
                if (key.isNotEmpty()) {
                    try {
                        if (settings.anotherAndroidRole == "sender") {
                            if (settings.anotherAndroidConnectionType == "bluetooth") {
                                bluetoothSender?.sendKeyEvent(part, isPressed)
                            } else {
                                remoteSender?.sendKeyEvent(part, isPressed)
                            }
                        } else {
                            // Receiver mode: Inject locally on this device via Shizuku
                            val emulationMode = settings.shizukuEmulationMode
                            adbClient?.setEmulationMode(emulationMode)
                            adbClient?.setInjectionMethod(settings.injectionMethod)
                            adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                            adbClient?.sendKeyEvent(part, key, isPressed, settings.simultaneousGroupingMs)
                        }
                    } catch (t: Throwable) {
                        android.util.Log.e("MainActivity", "Error sending key event in another_android mode", t)
                    }
                }
            }
            "local-demo" -> {
                // Free/Local play mode: Sound & Vibration only, no key injection or TCP transmission
            }
        }
    }

    private fun connectRemoteSender(overrideIp: String? = null) {
        val settings = settingsState.value
        if (settings.anotherAndroidConnectionType == "bluetooth") {
            connectBluetoothSender(overrideIp)
            return
        }

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

            val ip = (overrideIp ?: settings.anotherAndroidTargetIp).trim()
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
            val ip = (overrideIp ?: settings.anotherAndroidTargetIp).trim()
            if (ip.isEmpty()) {
                remoteSenderStatusState.value = "connecting"
                TaikoLogManager.log("無線 (Wi-Fi) モード: 受信機 (ゲーム) を自動探索中...")
                TaikoAndroidRemoteSender.scanAndFindReceiverIp(
                    targetPort = port,
                    connectionType = "wireless",
                    onFound = { foundIp ->
                        runOnUiThread {
                            val currentSettings = settingsState.value
                            updateAndPersistSettings(currentSettings.copy(anotherAndroidTargetIp = foundIp))
                            sender.connect(foundIp, port, object : TaikoAndroidRemoteSender.ConnectionListener {
                                override fun onConnected() {
                                    runOnUiThread {
                                        remoteSenderStatusState.value = "connected"
                                        TaikoLogManager.log("Wi-Fi 無線通信: 受信側 ($foundIp:$port) に接続完了")
                                    }
                                }
                                override fun onDisconnected() {
                                    runOnUiThread {
                                        remoteSenderStatusState.value = "disconnected"
                                        TaikoLogManager.log("Wi-Fi 無線通信: 切断されました")
                                    }
                                }
                                override fun onError(error: String) {
                                    runOnUiThread {
                                        remoteSenderStatusState.value = "error"
                                        TaikoLogManager.log("Wi-Fi 無線通信エラー: $error")
                                    }
                                }
                            })
                        }
                    },
                    onNotFound = {
                        runOnUiThread {
                            remoteSenderStatusState.value = "error"
                            TaikoLogManager.log("無線 (Wi-Fi) モード: 受信機が見つかりませんでした。受信側IPを手動入力するか、受信側のアプリが起動しているか確認してください。")
                        }
                    }
                )
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

        if (keys.isEmpty() && !isPressed) {
            runOnUiThread {
                listOf("leftKat", "leftDon", "rightDon", "rightKat").forEach { part ->
                    updateActiveInputsState(part, false)
                }
            }
            val allItems = listOf("leftKat", "leftDon", "rightDon", "rightKat").mapNotNull { part ->
                val keyChar = if (emulationMode == "gamepad") {
                    when (part) {
                        "leftKat" -> settingsCur.gamepadKeyConfig.leftKat
                        "leftDon" -> settingsCur.gamepadKeyConfig.leftDon
                        "rightDon" -> settingsCur.gamepadKeyConfig.rightDon
                        "rightKat" -> settingsCur.gamepadKeyConfig.rightKat
                        else -> ""
                    }
                } else {
                    when (part) {
                        "leftKat" -> settingsCur.keyConfig.leftKat
                        "leftDon" -> settingsCur.keyConfig.leftDon
                        "rightDon" -> settingsCur.keyConfig.rightDon
                        "rightKat" -> settingsCur.keyConfig.rightKat
                        else -> ""
                    }
                }
                if (keyChar.isNotEmpty()) part to keyChar else null
            }
            if (allItems.isNotEmpty()) {
                adbClient?.sendMultiKeyEvents(allItems, false, settingsCur.simultaneousGroupingMs)
            }
            return
        }

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
            // 1. Update visual feedback in receiver UI
            runOnUiThread {
                parts.forEach { part ->
                    updateActiveInputsState(part, isPressed)
                }
            }

            // 2. Direct local Shizuku/Uinput key injection
            val items = parts.mapNotNull { part ->
                val keyChar = if (emulationMode == "gamepad") {
                    when (part) {
                        "leftKat" -> settingsCur.gamepadKeyConfig.leftKat
                        "leftDon" -> settingsCur.gamepadKeyConfig.leftDon
                        "rightDon" -> settingsCur.gamepadKeyConfig.rightDon
                        "rightKat" -> settingsCur.gamepadKeyConfig.rightKat
                        else -> ""
                    }
                } else {
                    when (part) {
                        "leftKat" -> settingsCur.keyConfig.leftKat
                        "leftDon" -> settingsCur.keyConfig.leftDon
                        "rightDon" -> settingsCur.keyConfig.rightDon
                        "rightKat" -> settingsCur.keyConfig.rightKat
                        else -> ""
                    }
                }
                if (keyChar.isNotEmpty()) part to keyChar else null
            }

            if (items.isNotEmpty()) {
                adbClient?.sendMultiKeyEvents(items, isPressed, settingsCur.simultaneousGroupingMs)
            }
        }
    }

    private fun connectBluetoothSender(deviceAddress: String? = null, deviceName: String? = null) {
        checkAndRequestBluetoothPermissions {
            val addr = (deviceAddress ?: settingsState.value.anotherAndroidBluetoothDeviceAddress).trim()
            val name = (deviceName ?: settingsState.value.anotherAndroidBluetoothDeviceName).ifEmpty { addr }
            if (addr.isEmpty()) {
                refreshBluetoothDevices()
                bluetoothSenderStatusState.value = "disconnected"
                TaikoLogManager.log("Bluetooth送信: ペアリング済みデバイスを選択してください")
                return@checkAndRequestBluetoothPermissions
            }

            if (deviceAddress != null && deviceAddress != settingsState.value.anotherAndroidBluetoothDeviceAddress) {
                updateAndPersistSettings(settingsState.value.copy(
                    anotherAndroidBluetoothDeviceAddress = addr,
                    anotherAndroidBluetoothDeviceName = name
                ))
            }

            bluetoothSenderStatusState.value = "connecting"
            bluetoothConnectedDeviceNameState.value = name

            val sender = TaikoBluetoothSender(this)
            bluetoothSender?.disconnect()
            bluetoothSender = sender

            sender.connect(addr, object : TaikoBluetoothSender.ConnectionListener {
                override fun onConnected(connectedDeviceName: String) {
                    runOnUiThread {
                        bluetoothSenderStatusState.value = "connected"
                        bluetoothConnectedDeviceNameState.value = connectedDeviceName
                        TaikoLogManager.log("Bluetooth送信: 受信側「$connectedDeviceName」に接続完了 (<2ms)")
                    }
                }

                override fun onDisconnected() {
                    runOnUiThread {
                        bluetoothSenderStatusState.value = "disconnected"
                        bluetoothConnectedDeviceNameState.value = null
                        TaikoLogManager.log("Bluetooth送信: 切断されました")
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        bluetoothSenderStatusState.value = "error"
                        TaikoLogManager.log("Bluetooth送信エラー: $error")
                    }
                }
            })
        }
    }

    private fun disconnectBluetoothSender() {
        bluetoothSender?.disconnect()
        bluetoothSender = null
        bluetoothSenderStatusState.value = "disconnected"
        bluetoothConnectedDeviceNameState.value = null
        TaikoLogManager.log("Bluetooth送信: 接続を解除しました")
    }

    private fun startBluetoothReceiver() {
        checkAndRequestBluetoothPermissions {
            bluetoothReceiver?.stop()
            val receiver = TaikoBluetoothReceiver(this) { keys, isPressed ->
                handleIncomingRemoteKeys(keys, isPressed)
            }
            bluetoothReceiver = receiver
            receiver.start { connected, devName ->
                runOnUiThread {
                    bluetoothReceiverConnectedDeviceState.value = if (connected) devName else null
                }
            }
        }
    }

    private fun stopBluetoothReceiver() {
        bluetoothReceiver?.stop()
        bluetoothReceiver = null
        bluetoothReceiverConnectedDeviceState.value = null
        TaikoLogManager.log("Bluetooth受信: 待機を停止しました")
    }

    private fun refreshBluetoothDevices() {
        if (TaikoBluetoothManager.hasBluetoothPermissions(this)) {
            val list = TaikoBluetoothManager.getPairedDevices(this)
            bluetoothPairedDevicesState.value = list
            TaikoLogManager.log("Bluetooth: ペアリング済みデバイス ${list.size} 件を取得しました")
        }
    }

    private fun openBluetoothSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Bluetooth設定画面を開けませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRemoteReceiver() {
        val settings = settingsState.value
        if (settings.anotherAndroidConnectionType == "bluetooth") {
            startBluetoothReceiver()
            return
        }

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
        stopBluetoothReceiver()
    }

    private fun stopRemoteSender() {
        remoteSender?.disconnect()
        remoteSender = null
        remoteSenderStatusState.value = "disconnected"
        disconnectBluetoothSender()
    }

    private fun onConnectionModeChanged(oldMode: String, newMode: String) {
        val settings = settingsState.value
        if (newMode == "usb-wired") {
            TaikoUsbDirectManager.stop(this)
            disconnectBluetoothSender()
            stopBluetoothReceiver()
            startTcpServer()
            stopRemoteReceiver()
            stopRemoteSender()
            TaikoLogManager.log("Switched to USB-Wired mode: Started TCP server on port 60001")
        } else if (newMode == "another_android") {
            stopTcpServer()
            // Force reset existing sockets when connection parameters change
            stopRemoteSender()
            stopRemoteReceiver()
            disconnectBluetoothSender()
            stopBluetoothReceiver()

            if (settings.anotherAndroidConnectionType == "wired") {
                TaikoUsbDirectManager.start(this)
            } else {
                TaikoUsbDirectManager.stop(this)
            }

            if (settings.anotherAndroidConnectionType == "bluetooth") {
                refreshBluetoothDevices()
                if (settings.anotherAndroidRole == "receiver") {
                    startBluetoothReceiver()
                } else {
                    if (settings.anotherAndroidBluetoothDeviceAddress.isNotEmpty()) {
                        connectBluetoothSender(settings.anotherAndroidBluetoothDeviceAddress, settings.anotherAndroidBluetoothDeviceName)
                    }
                }
            } else {
                if (settings.anotherAndroidRole == "receiver") {
                    startRemoteReceiver()
                } else {
                    connectRemoteSender()
                }
            }
            TaikoLogManager.log("Switched to Another Android mode (Role=${settings.anotherAndroidRole}, Type=${settings.anotherAndroidConnectionType})")
        } else {
            TaikoUsbDirectManager.stop(this)
            disconnectBluetoothSender()
            stopBluetoothReceiver()
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
        disconnectBluetoothSender()
        stopBluetoothReceiver()
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
                if (settings.anotherAndroidConnectionType == "bluetooth") {
                    refreshBluetoothDevices()
                    if (settings.anotherAndroidRole == "receiver") {
                        startBluetoothReceiver()
                    } else {
                        if (settings.anotherAndroidBluetoothDeviceAddress.isNotEmpty()) {
                            connectBluetoothSender(settings.anotherAndroidBluetoothDeviceAddress, settings.anotherAndroidBluetoothDeviceName)
                        }
                    }
                } else {
                    if (settings.anotherAndroidRole == "receiver") {
                        startRemoteReceiver()
                    } else {
                        connectRemoteSender()
                    }
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
            }

            val wasPhysicallyHeld = physicallyHeldParts.contains(part)
            if (wasPhysicallyHeld || pendingJob != null) {
                if (settings.showLogConsole) {
                    TaikoLogManager.log("Touch Overlap: $part -> key=$keyChar. Instant release and re-press!")
                }
                // Force an immediate synchronous release event, followed by press so the game registers separate hits without coroutine race
                dispatchPhysicalKey(part, keyChar, false, fromTouch)
                dispatchPhysicalKey(part, keyChar, true, fromTouch)
            } else {
                if (settings.showLogConsole) {
                    TaikoLogManager.log("Touch Down: $part -> key=$keyChar")
                }
                dispatchPhysicalKey(part, keyChar, true, fromTouch)
            }
            physicallyHeldParts.add(part)
            
            // Handle repeat logic: Only repeat rapidly when Turbo (Auto-Repeat) is explicitly enabled.
            if (settings.isTurboEnabled) {
                if (settings.showLogConsole) {
                    TaikoLogManager.log("Turbo Enabled: auto-repeating $part every ${settings.turboIntervalMs}ms")
                }
                activeRepeatJobs[part] = lifecycleScope.launch(Dispatchers.IO) {
                    val interval = settings.turboIntervalMs.toLong()
                    while (isActive) {
                        delay(interval)
                        dispatchPhysicalKey(part, keyChar, false, fromTouch)
                        delay(5L)
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
            if (settings.showLogConsole) {
                TaikoLogManager.log("Touch Up: $part (actual hold: ${elapsed}ms)")
            }
            if (elapsed < minDuration && minDuration > 0) {
                val delayMs = minDuration - elapsed
                if (settings.showLogConsole) {
                    TaikoLogManager.log("Touch Up Hold: $part hold was ${elapsed}ms < minPress ${minDuration}ms. Delaying release by ${delayMs}ms to ensure registration.")
                }
                val job = lifecycleScope.launch(Dispatchers.IO) {
                    delay(delayMs)
                    dispatchPhysicalKey(part, keyChar, false, fromTouch)
                    physicallyHeldParts.remove(part)
                    pendingReleaseJobs.remove(part)
                }
                pendingReleaseJobs[part] = job
            } else {
                dispatchPhysicalKey(part, keyChar, false, fromTouch)
                physicallyHeldParts.remove(part)
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

        // Fast-path batch multi-key dispatch for simultaneous big notes (inputs > 1)
        if (inputs.size > 1) {
            val isAllSameAction = inputs.all { it.second == inputs[0].second }
            val actionIsPressed = inputs[0].second

            if (isAllSameAction) {
                val parts = inputs.map { it.first }
                if (actionIsPressed) {
                    val now = System.currentTimeMillis()
                    parts.forEach { part ->
                        lastPressTimestamps[part] = now
                        activeRepeatJobs[part]?.cancel()
                        activeRepeatJobs.remove(part)
                        pendingReleaseJobs[part]?.cancel()
                        pendingReleaseJobs.remove(part)
                        physicallyHeldParts.add(part)
                    }
                } else {
                    parts.forEach { part ->
                        activeRepeatJobs[part]?.cancel()
                        activeRepeatJobs.remove(part)
                        physicallyHeldParts.remove(part)
                    }
                }

                if (settings.connectionMode == "another_android" && settings.anotherAndroidRole == "sender") {
                    if (parts.isNotEmpty()) {
                        if (settings.anotherAndroidConnectionType == "bluetooth") {
                            bluetoothSender?.sendMultiKeyEvents(parts, actionIsPressed)
                        } else {
                            remoteSender?.sendMultiKeyEvents(parts, actionIsPressed)
                        }
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
                        try {
                            val emulationMode = settings.shizukuEmulationMode
                            adbClient?.setEmulationMode(emulationMode)
                            adbClient?.setInjectionMethod(settings.injectionMethod)
                            adbClient?.setGamepadKeyConfig(settings.gamepadKeyConfig)
                            adbClient?.sendMultiKeyEvents(items, actionIsPressed, settings.simultaneousGroupingMs)
                        } catch (t: Throwable) {
                            android.util.Log.e("MainActivity", "Error sending multi key events via Shizuku", t)
                        }
                        return
                    }
                }
            }
        }

        // 2. Map and delegate single or non-uniform inputs individually
        inputs.forEach { (part, isPressed) ->
            triggerInput(part, isPressed, fromTouch)
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

    fun resetAllInputs() {
        activeRepeatJobs.values.forEach { it.cancel() }
        activeRepeatJobs.clear()
        pendingReleaseJobs.values.forEach { it.cancel() }
        pendingReleaseJobs.clear()
        physicallyHeldParts.clear()
        val settings = settingsState.value
        val activeEmulationMode = settings.activeEmulationMode
        listOf("leftKat", "leftDon", "rightDon", "rightKat").forEach { part ->
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
                dispatchPhysicalKey(part, keyChar, false, fromTouch = false)
            }
        }
        activeInputsState.value = RecordActiveInputs(leftKat = false, leftDon = false, rightDon = false, rightKat = false)
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
        OverlayService.updateSettings(newSettings)

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
        try {
            val settings = settingsState.value
            if (settings.connectionMode == "usb-wired") {
                startTcpServer()
            } else if (settings.connectionMode == "another_android") {
                if (settings.anotherAndroidConnectionType == "bluetooth") {
                    if (TaikoBluetoothManager.hasBluetoothPermissions(this)) {
                        refreshBluetoothDevices()
                        if (settings.anotherAndroidRole == "receiver") {
                            startBluetoothReceiver()
                        }
                    }
                } else if (settings.anotherAndroidRole == "receiver") {
                    startRemoteReceiver()
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Error in onStart", e)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!OverlayService.isOverlayRunning) {
            stopTcpServer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
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
        disconnectBluetoothSender()
        stopBluetoothReceiver()
    }
}
