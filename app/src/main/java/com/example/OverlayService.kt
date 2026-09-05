package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import kotlin.math.hypot
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Service to show Taiko drum pad and a floating bubble menu as a system overlay.
 */
class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.example.ACTION_START_OVERLAY"
        const val ACTION_STOP = "com.example.ACTION_STOP_OVERLAY"
        const val EXTRA_TARGET_DISPLAY_ID = "com.example.EXTRA_TARGET_DISPLAY_ID"
        const val CHANNEL_ID = "taiko_overlay_channel"
        const val NOTIFICATION_ID = 9021

        var instance: OverlayService? = null
            private set

        var isOverlayRunning = false
            private set

        fun updateSettings(newSettings: ControllerSettings) {
            instance?.updateSettingsInternal(newSettings)
        }

        fun start(context: Context, targetDisplayId: Int? = null) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START
                if (targetDisplayId != null) {
                    putExtra(EXTRA_TARGET_DISPLAY_ID, targetDisplayId)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var targetDisplayId: Int = Display.DEFAULT_DISPLAY
    private var displayContext: Context? = null
    private var currentDisplay: Display? = null
    private val availableDisplayIdsState = mutableStateListOf<Int>()

    private var windowManager: WindowManager? = null
    private var padComposeView: ComposeView? = null
    private var bubbleComposeView: ComposeView? = null

    private var padLayoutParams: WindowManager.LayoutParams? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null

    // Overlay state
    private val isTouchEnabledState = mutableStateOf(false) // Initial state: Touch OFF (Pass-through)
    private val isMenuExpandedState = mutableStateOf(false)
    private val isPlacedOnRightState = mutableStateOf(true)
    private val isPlacedOnTopState = mutableStateOf(false)
    private var bubblePosX = 0f
    private var bubblePosY = 0f

    private val settingsState = mutableStateOf(ControllerSettings())
    private val activeInputsState = mutableStateOf(RecordActiveInputs())

    private var audioPlayer: TaikoAudioPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        initDisplayContext(Display.DEFAULT_DISPLAY)
        initVibrator()
        try {
            audioPlayer = TaikoAudioPlayer(this)
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Audio player init failed", e)
        }

        createNotificationChannel()
    }

    private fun initDisplayContext(displayId: Int) {
        targetDisplayId = displayId
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        availableDisplayIdsState.clear()
        if (displayManager != null) {
            for (d in displayManager.displays) {
                availableDisplayIdsState.add(d.displayId)
            }
        }

        val target = displayManager?.getDisplay(displayId)
            ?: displayManager?.displays?.firstOrNull { it.displayId == displayId }
            ?: displayManager?.displays?.firstOrNull()

        currentDisplay = target

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // On Android 12+ (API 31+), createWindowContext creates a genuine WindowContext
        // bound specifically to the target display, with its own independent Configuration and WindowManager.
        val dContext = if (target != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val baseDisplayContext = createDisplayContext(target)
                    baseDisplayContext.createWindowContext(layoutType, null)
                } catch (e: Exception) {
                    createDisplayContext(target)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                createDisplayContext(target)
            } else {
                this
            }
        } else {
            this
        }
        displayContext = dContext
        windowManager = dContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Get the true physical/logical pixel resolution of the target Display object directly.
     * We MUST NOT use WindowMetrics.bounds or Activity/Service resources because they reflect
     * the screen where the main Activity is located rather than the overlay's target screen.
     */
    private fun getTargetDisplaySize(): Pair<Int, Int> {
        val d = currentDisplay ?: run {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            dm?.getDisplay(targetDisplayId) ?: dm?.displays?.firstOrNull()
        }

        if (d != null) {
            // 1. Direct real metrics of the target display
            val dm = DisplayMetrics()
            try {
                @Suppress("DEPRECATION")
                d.getRealMetrics(dm)
                if (dm.widthPixels > 0 && dm.heightPixels > 0) {
                    return Pair(dm.widthPixels, dm.heightPixels)
                }
            } catch (e: Exception) {
                // Fallback
            }

            // 2. Direct real size of the target display
            try {
                val point = android.graphics.Point()
                @Suppress("DEPRECATION")
                d.getRealSize(point)
                if (point.x > 0 && point.y > 0) {
                    return Pair(point.x, point.y)
                }
            } catch (e: Exception) {
                // Fallback
            }

            // 3. Display mode hardware resolution (accounting for rotation)
            try {
                val mode = d.mode
                val rotation = d.rotation
                val isLandscape = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
                val w = if (isLandscape) maxOf(mode.physicalWidth, mode.physicalHeight) else minOf(mode.physicalWidth, mode.physicalHeight)
                val h = if (isLandscape) minOf(mode.physicalWidth, mode.physicalHeight) else maxOf(mode.physicalWidth, mode.physicalHeight)
                if (w > 0 && h > 0) {
                    return Pair(w, h)
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        val resDm = (displayContext ?: this).resources.displayMetrics
        return Pair(resDm.widthPixels, resDm.heightPixels)
    }

    /**
     * Get density specifically for the target display.
     */
    private fun getTargetDensity(): Float {
        val d = currentDisplay
        if (d != null) {
            val dm = DisplayMetrics()
            try {
                @Suppress("DEPRECATION")
                d.getRealMetrics(dm)
                if (dm.density > 0f) return dm.density
            } catch (e: Exception) {
                // Fallback
            }
        }
        return (displayContext ?: this).resources.displayMetrics.density
    }

    /**
     * Switch overlay display on multi-screen devices (e.g., AYN Thor: Upper ⇔ Lower screen).
     */
    fun switchDisplay(newDisplayId: Int) {
        if (newDisplayId == targetDisplayId && isOverlayRunning) return
        try {
            padComposeView?.let { windowManager?.removeViewImmediate(it) }
        } catch (e: Exception) {
            try { padComposeView?.let { windowManager?.removeView(it) } } catch (e2: Exception) {}
        }
        try {
            bubbleComposeView?.let { windowManager?.removeViewImmediate(it) }
        } catch (e: Exception) {
            try { bubbleComposeView?.let { windowManager?.removeView(it) } } catch (e2: Exception) {}
        }
        padComposeView = null
        bubbleComposeView = null

        initDisplayContext(newDisplayId)

        // Recalculate bubble coordinates within the bounds of the new display
        val (dispW, dispH) = getTargetDisplaySize()
        val density = getTargetDensity()
        val bubbleSize = (56 * density).toInt()
        val margin = (12 * density).toInt()
        bubblePosX = (dispW - bubbleSize - margin).toFloat().coerceAtLeast(margin.toFloat())
        bubblePosY = (dispH * 0.25f).coerceAtLeast(margin.toFloat())
        isPlacedOnRightState.value = true
        isPlacedOnTopState.value = true

        setupOverlays()
        val isSub = newDisplayId != Display.DEFAULT_DISPLAY
        val label = if (isSub) "下画面 (サブ画面)" else "上画面 (メイン画面)"
        Toast.makeText(this, "太鼓オーバーレイを $label に移動しました", Toast.LENGTH_SHORT).show()
    }

    private fun updateSettingsInternal(newSettings: ControllerSettings) {
        settingsState.value = newSettings
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val requestedDisplayId = intent?.getIntExtra(EXTRA_TARGET_DISPLAY_ID, Display.DEFAULT_DISPLAY)
            ?: Display.DEFAULT_DISPLAY

        if (!isOverlayRunning || targetDisplayId != requestedDisplayId) {
            initDisplayContext(requestedDisplayId)
        }

        startForegroundServiceWithNotification()
        loadSettings()

        if (!isOverlayRunning) {
            isOverlayRunning = true
            setupOverlays()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            val isSub = targetDisplayId != Display.DEFAULT_DISPLAY
            val screenLabel = if (isSub) "下画面" else "画面"
            Toast.makeText(this, "太鼓オーバーレイ起動 ($screenLabel / 初期状態: 判定OFF)\nバブルメニューから判定をONにできます", Toast.LENGTH_LONG).show()
        }

        return START_STICKY
    }

    private fun loadSettings() {
        try {
            val mainSettings = MainActivity.instance?.getSettings()
            if (mainSettings != null) {
                settingsState.value = mainSettings
                return
            }
            val prefs = getSharedPreferences("taiko_controller_settings", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("settings_json", null)
            if (!jsonStr.isNullOrEmpty()) {
                val json = Json { ignoreUnknownKeys = true }
                settingsState.value = json.decodeFromString(ControllerSettings.serializer(), jsonStr)
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to load settings in service", e)
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
            // ignore
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "太鼓オーバーレイ表示",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "太鼓コントローラーのオーバーレイ表示を維持します"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("太鼓コントローラー (オーバーレイ表示中)")
                .setContentText("バブルメニューから判定のON/OFFやアプリ復帰が可能です")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "オーバーレイ終了", stopPendingIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("太鼓コントローラー (オーバーレイ表示中)")
                .setContentText("バブルメニューから判定のON/OFFやアプリ復帰が可能です")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "オーバーレイ終了", stopPendingIntent)
                .setOngoing(true)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupOverlays() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (dispWidth, dispHeight) = getTargetDisplaySize()
        val dContext = displayContext ?: this
        val targetDensity = getTargetDensity()

        // 1. Pad Overlay Window (Full Screen on Target Display)
        // Initial flag includes FLAG_NOT_TOUCHABLE to allow interaction with background apps
        val padFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        padLayoutParams = WindowManager.LayoutParams(
            dispWidth,
            dispHeight,
            layoutType,
            padFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            width = dispWidth
            height = dispHeight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        padComposeView = ComposeView(dContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setupComposeOwners(this)
            setContent {
                val settings by remember { settingsState }
                val activeInputs by remember { activeInputsState }

                // Constant overlay transparency regardless of touch enabled/disabled state
                val currentAlpha = (settings.overlayAlphaPercent / 100f).coerceIn(0.1f, 1.0f)

                // Explicitly provide target display size in DP to guarantee correct scale and centering
                // regardless of where the main application activity is currently located
                val densityScope = androidx.compose.ui.unit.Density(targetDensity)
                val widthDp = with(densityScope) { dispWidth.toDp() }
                val heightDp = with(densityScope) { dispHeight.toDp() }

                Box(
                    modifier = Modifier.size(widthDp, heightDp),
                    contentAlignment = Alignment.Center
                ) {
                    TaikoPad(
                        settings = settings,
                        activeInputs = activeInputs,
                        onInputTriggered = { part, isPressed ->
                            handleOverlayInput(part, isPressed)
                        },
                        onMultiInputTriggered = { inputsList ->
                            handleOverlayMultiInputs(inputsList)
                        },
                        audioPlayer = audioPlayer,
                        vibrateAction = { isBig -> triggerVibration(isBig) },
                        isFullScreen = true,
                        isOverlay = true,
                        overlayAlpha = currentAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2. Floating Bubble Overlay Window
        // Use FLAG_NOT_FOCUSABLE and FLAG_NOT_TOUCH_MODAL without FLAG_LAYOUT_NO_LIMITS
        // to ensure stable WindowManager InputChannel on Freeform/WSA environments.
        val bubbleFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        val density = targetDensity
        val bubbleSize = (56 * density).toInt()
        val margin = (12 * density).toInt()

        bubblePosX = (dispWidth - bubbleSize - margin).toFloat().coerceAtLeast(margin.toFloat())
        bubblePosY = (dispHeight * 0.25f).coerceAtLeast(margin.toFloat())

        isPlacedOnRightState.value = bubblePosX > (dispWidth / 2)
        isPlacedOnTopState.value = bubblePosY <= (dispHeight / 2)

        bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            bubbleFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubblePosX.toInt()
            y = bubblePosY.toInt()
        }

        bubbleComposeView = ComposeView(dContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setupComposeOwners(this)
            setContent {
                val isTouchEnabled by remember { isTouchEnabledState }
                val isMenuExpanded by remember { isMenuExpandedState }
                val isPlacedOnRight by remember { isPlacedOnRightState }
                val isPlacedOnTop by remember { isPlacedOnTopState }
                val availableDisplayCount = availableDisplayIdsState.size
                val isSub = targetDisplayId != Display.DEFAULT_DISPLAY

                FloatingBubbleMenu(
                    isTouchEnabled = isTouchEnabled,
                    isMenuExpanded = isMenuExpanded,
                    isPlacedOnRight = isPlacedOnRight,
                    isPlacedOnTop = isPlacedOnTop,
                    hasMultipleDisplays = availableDisplayCount > 1,
                    isSubDisplay = isSub,
                    onToggleMenu = {
                        isMenuExpandedState.value = !isMenuExpandedState.value
                        applyBubbleLayout()
                    },
                    onToggleTouch = {
                        togglePadTouch()
                    },
                    onSwitchDisplay = {
                        val otherDisplay = availableDisplayIdsState.find { it != targetDisplayId }
                            ?: Display.DEFAULT_DISPLAY
                        switchDisplay(otherDisplay)
                    },
                    onOpenApp = {
                        openMainApp()
                    },
                    onCloseOverlay = {
                        stopSelf()
                    },
                    onDragDelta = { dx, dy ->
                        updateBubblePosition(dx, dy)
                    }
                )
            }
        }

        try {
            windowManager?.addView(padComposeView, padLayoutParams)
            windowManager?.addView(bubbleComposeView, bubbleLayoutParams)
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to add overlay views to WindowManager", e)
            Toast.makeText(this, "オーバーレイの追加に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupComposeOwners(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    private fun togglePadTouch() {
        val newTouchState = !isTouchEnabledState.value
        isTouchEnabledState.value = newTouchState

        val params = padLayoutParams ?: return
        val view = padComposeView ?: return

        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        // Re-enforce accurate target display dimensions and (0,0) offset
        // to prevent WindowManager from resizing or left-aligning the pad when touchable
        val (dispWidth, dispHeight) = getTargetDisplaySize()
        params.width = dispWidth
        params.height = dispHeight
        params.x = 0
        params.y = 0
        params.gravity = Gravity.TOP or Gravity.START

        params.flags = if (newTouchState) {
            baseFlags // Touch enabled (Taiko will receive touch events)
        } else {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE // Touch disabled (Passed through to underlying apps)
        }

        try {
            windowManager?.updateViewLayout(view, params)
            if (newTouchState) {
                Toast.makeText(this, "🥁 太鼓の判定: ON (プレイ中)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🛡️ 太鼓の判定: OFF (タッチ透過中)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to update pad touch flags", e)
        }
    }

    private fun updateBubblePosition(dx: Float, dy: Float) {
        bubblePosX += dx
        bubblePosY += dy
        applyBubbleLayout()
    }

    private fun applyBubbleLayout() {
        val params = bubbleLayoutParams ?: return
        val view = bubbleComposeView ?: return
        view.post {
            try {
                val (dispWidth, dispHeight) = getTargetDisplaySize()
                val density = getTargetDensity()

                val bubbleSize = (56 * density).toInt()
                val menuWidth = (220 * density).toInt()
                val margin = (12 * density).toInt()

                // Clamp bubble button position so it is always fully on-screen
                val maxBubbleX = (dispWidth - bubbleSize - margin).toFloat().coerceAtLeast(margin.toFloat())
                val maxBubbleY = (dispHeight - bubbleSize - margin).toFloat().coerceAtLeast(margin.toFloat())

                bubblePosX = bubblePosX.coerceIn(margin.toFloat(), maxBubbleX)
                bubblePosY = bubblePosY.coerceIn(margin.toFloat(), maxBubbleY)

                val isExpanded = isMenuExpandedState.value
                val isRight = (bubblePosX + bubbleSize / 2f) > (dispWidth / 2f)
                val isTop = (bubblePosY + bubbleSize / 2f) <= (dispHeight / 2f)

                isPlacedOnRightState.value = isRight
                isPlacedOnTopState.value = isTop

                if (!isExpanded) {
                    params.width = bubbleSize
                    params.height = bubbleSize
                    params.x = bubblePosX.toInt()
                    params.y = bubblePosY.toInt()
                } else {
                    params.width = menuWidth
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT

                    // Horizontal window position:
                    val targetX = if (isRight) {
                        (bubblePosX.toInt() + bubbleSize - menuWidth).coerceIn(margin, (dispWidth - menuWidth - margin).coerceAtLeast(margin))
                    } else {
                        bubblePosX.toInt().coerceIn(margin, (dispWidth - menuWidth - margin).coerceAtLeast(margin))
                    }

                    // Vertical window position:
                    val estimatedMenuHeight = (260 * density).toInt()
                    val totalHeight = bubbleSize + estimatedMenuHeight + (8 * density).toInt()
                    val targetY = if (isTop) {
                        bubblePosY.toInt().coerceIn(margin, (dispHeight - totalHeight - margin).coerceAtLeast(margin))
                    } else {
                        (bubblePosY.toInt() + bubbleSize - totalHeight).coerceIn(margin, (dispHeight - totalHeight - margin).coerceAtLeast(margin))
                    }

                    params.x = targetX.coerceAtLeast(0)
                    params.y = targetY.coerceAtLeast(0)
                }

                windowManager?.updateViewLayout(view, params)
            } catch (e: Exception) {
                android.util.Log.e("OverlayService", "Failed to update bubble layout", e)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyBubbleLayout()
    }

    private fun openMainApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Failed to open main activity", e)
        }
    }

    private fun handleOverlayInput(part: String, isPressed: Boolean) {
        val main = MainActivity.instance
        if (main != null) {
            main.triggerOverlayInput(part, isPressed)
        }
    }

    private fun handleOverlayMultiInputs(inputs: List<Pair<String, Boolean>>) {
        val main = MainActivity.instance
        if (main != null) {
            main.triggerOverlayMultiInputs(inputs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        isOverlayRunning = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()

        try {
            if (padComposeView != null) {
                windowManager?.removeView(padComposeView)
                padComposeView = null
            }
            if (bubbleComposeView != null) {
                windowManager?.removeView(bubbleComposeView)
                bubbleComposeView = null
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayService", "Error removing overlay views", e)
        }

        audioPlayer?.release()
        audioPlayer = null
        Toast.makeText(this, "太鼓オーバーレイを終了しました", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingBubbleMenu(
    isTouchEnabled: Boolean,
    isMenuExpanded: Boolean,
    isPlacedOnRight: Boolean,
    isPlacedOnTop: Boolean,
    hasMultipleDisplays: Boolean = false,
    isSubDisplay: Boolean = false,
    onToggleMenu: () -> Unit,
    onToggleTouch: () -> Unit,
    onSwitchDisplay: () -> Unit = {},
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val horizontalAlign = if (isPlacedOnRight) Alignment.End else Alignment.Start

    Column(
        horizontalAlignment = horizontalAlign,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = if (isMenuExpanded) Modifier.width(220.dp) else Modifier.wrapContentSize()
    ) {
        // If bubble is in bottom half (not top), Menu is placed ABOVE the bubble
        if (!isPlacedOnTop && isMenuExpanded) {
            BubbleMenuCard(
                isTouchEnabled = isTouchEnabled,
                hasMultipleDisplays = hasMultipleDisplays,
                isSubDisplay = isSubDisplay,
                onToggleTouch = onToggleTouch,
                onSwitchDisplay = onSwitchDisplay,
                onOpenApp = onOpenApp,
                onCloseOverlay = onCloseOverlay,
                onCloseMenu = onToggleMenu
            )
        }

        // Circular Floating Bubble Trigger
        BubbleButton(
            isTouchEnabled = isTouchEnabled,
            onClick = onToggleMenu,
            onDragDelta = onDragDelta
        )

        // If bubble is in top half, Menu is placed BELOW the bubble
        if (isPlacedOnTop && isMenuExpanded) {
            BubbleMenuCard(
                isTouchEnabled = isTouchEnabled,
                hasMultipleDisplays = hasMultipleDisplays,
                isSubDisplay = isSubDisplay,
                onToggleTouch = onToggleTouch,
                onSwitchDisplay = onSwitchDisplay,
                onOpenApp = onOpenApp,
                onCloseOverlay = onCloseOverlay,
                onCloseMenu = onToggleMenu
            )
        }
    }
}

@Composable
fun BubbleButton(
    isTouchEnabled: Boolean,
    onClick: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val bubbleColor = if (isTouchEnabled) {
        Brush.radialGradient(listOf(Color(0xFFF97316), Color(0xFFDC2626)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF3B82F6), Color(0xFF1E293B)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(bubbleColor)
            .border(2.dp, if (isTouchEnabled) Color(0xFFFDE68A) else Color(0xFF93C5FD), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isDrag = false
                    var totalDx = 0f
                    var totalDy = 0f
                    val touchSlop = viewConfiguration.touchSlop

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) {
                            if (!isDrag) {
                                onClick()
                            }
                            break
                        }
                        if (!change.pressed) break

                        val drag = change.positionChange()
                        totalDx += drag.x
                        totalDy += drag.y
                        if (hypot(totalDx.toDouble(), totalDy.toDouble()).toFloat() > touchSlop) {
                            isDrag = true
                            change.consume()
                            onDragDelta(drag.x, drag.y)
                        }
                    }
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isTouchEnabled) "🔥" else "🥁",
                fontSize = 18.sp
            )
            Text(
                text = if (isTouchEnabled) "ON" else "OFF",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun BubbleMenuCard(
    isTouchEnabled: Boolean,
    hasMultipleDisplays: Boolean = false,
    isSubDisplay: Boolean = false,
    onToggleTouch: () -> Unit,
    onSwitchDisplay: () -> Unit = {},
    onOpenApp: () -> Unit,
    onCloseOverlay: () -> Unit,
    onCloseMenu: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Status with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🥁 太鼓オーバーレイ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                IconButton(
                    onClick = onCloseMenu,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "メニューを閉じる",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.15f))

            // 1. Touch Status & Toggle Button (判定ON/OFF)
            Button(
                onClick = onToggleTouch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTouchEnabled) Color(0xFFEF4444) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isTouchEnabled) Icons.Default.TouchApp else Icons.Default.PanTool,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isTouchEnabled) "判定: ON (タップ中)" else "判定: OFF (透過中)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 1.5. Multi-screen Switch Button (for AYN Thor dual-screen devices)
            if (hasMultipleDisplays) {
                OutlinedButton(
                    onClick = onSwitchDisplay,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDE68A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSubDisplay) "🖥️ 上画面 (メイン) へ移動" else "📱 下画面 (サブ) へ移動",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }

            // 2. Open App Button
            OutlinedButton(
                onClick = onOpenApp,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "太鼓アプリを開く",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            // 3. Exit Overlay Button
            OutlinedButton(
                onClick = onCloseOverlay,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "オーバーレイ終了",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                }
            }
        }
    }
}
