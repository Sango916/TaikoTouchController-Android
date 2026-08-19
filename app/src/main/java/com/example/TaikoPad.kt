package com.example

import android.os.SystemClock
import android.view.MotionEvent
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class VisualRipple(
    val id: Long,
    val x: Float,
    val y: Float,
    val isDon: Boolean,
    val maxRadius: Float,
    val creationTime: Long
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TaikoPad(
    settings: ControllerSettings,
    activeInputs: RecordActiveInputs,
    onInputTriggered: (String, Boolean) -> Unit,
    onMultiInputTriggered: (List<Pair<String, Boolean>>) -> Unit,
    audioPlayer: TaikoAudioPlayer?,
    vibrateAction: (Boolean) -> Unit,
    isFullScreen: Boolean = false,
    isOverlay: Boolean = false,
    overlayAlpha: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(Size.Zero) }
    val ripples = remember { mutableStateListOf<VisualRipple>() }
    var rippleIdCounter by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    // Animation ticker state to drive frame-by-frame star rotation and fade transitions smoothly
    var animationTick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(ripples.size, settings.lightweightRenderingMode) {
        if (!settings.lightweightRenderingMode && ripples.isNotEmpty()) {
            while (ripples.isNotEmpty()) {
                animationTick = SystemClock.uptimeMillis()
                delay(16)
            }
        }
    }

    // Dimensions: Determine orientation & aspect ratio from the actual container size (supporting split-screen)
    val containerWidth = size.width
    val containerHeight = size.height
    val isLandscape = if (containerWidth > 0f && containerHeight > 0f) {
        containerWidth >= containerHeight
    } else {
        val configuration = LocalConfiguration.current
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val customSizePercent = if (isLandscape) settings.landscapeSizePercent else settings.portraitSizePercent
    val customVerticalPosPercent = if (isLandscape) settings.landscapeVerticalPosPercent else settings.portraitVerticalPosPercent

    // Base scale is consistent (1.0f in landscape, 0.90f in portrait) across fullscreen and preview
    val baseScale = if (isLandscape) 1.00f else 0.90f
    val drumScale = baseScale * (customSizePercent / 100f)

    val width = size.width
    val height = size.height
    // Ensure the drum is always a perfect circle and fits beautifully inside the container without overflowing
    val drumDiameter = Math.min(width, height) * drumScale
    val drumRadius = drumDiameter / 2f
    val centerX = width * 0.5f
    val centerY = height * (customVerticalPosPercent / 100f)
    val centerYOffset = centerY - (height * 0.5f)

    // Touch boundaries ratios (based on precise percentages)
    val donMaxRadius = drumRadius * 0.6864f
    val katRimRadius = drumRadius

    // Dynamic Big Note DS Radii (面: 内側0~donBigRadius, フチ: 内側donMaxRadius~外側katBigOuterRadius)
    val donBigFactor = (settings.donBigNotePercent / 100f).coerceAtLeast(0.10f)
    val donBigRadius = donMaxRadius * donBigFactor

    val katBigFactor = (settings.katBigNotePercent / 100f).coerceAtLeast(0.10f)
    // Kat big note zone & visual Kat light effect extends from the inner boundary (donMaxRadius) outwards to katBigOuterRadius
    val katBigOuterRadius = donMaxRadius + (drumRadius - donMaxRadius) * katBigFactor

    // Track active parts per touch pointer ID
    val pointerPartsMap = remember { mutableMapOf<Int, List<String>>() }
    // Debounce tracker for rapid-fire / chatter prevention (e.g., AYN Thor Android 13 mapping)
    val lastTriggerTimeMap = remember { mutableMapOf<String, Long>() }

    // Decay visual inputs slightly to prevent flickering during rapid rolling (連打)
    val visualLeftKat = rememberDecayedState(activeInputs.leftKat, 35L)
    val visualRightKat = rememberDecayedState(activeInputs.rightKat, 35L)
    val visualLeftDon = rememberDecayedState(activeInputs.leftDon, 35L)
    val visualRightDon = rememberDecayedState(activeInputs.rightDon, 35L)

    // Track actual real-time simultaneous presses (Big Notes)
    val realTimeBigKat = activeInputs.leftKat && activeInputs.rightKat
    val realTimeBigDon = activeInputs.leftDon && activeInputs.rightDon

    // Decay the big note states as well so they fade out smoothly during a true big note hit
    val visualBigKat = rememberDecayedState(realTimeBigKat, 35L)
    val visualBigDon = rememberDecayedState(realTimeBigDon, 35L)

    // When in overlay mode, always render the drum using light theme colors as requested
    val isDark = if (isOverlay) false else resolveIsDarkTheme(settings.themeMode)

    val targetBgColor = if (isDark) {
        Color.Black
    } else {
        when {
            visualLeftKat || visualRightKat -> Color(0xFF6EDCFF) // Kat: Fresh Blue
            else -> Color(0xFFFFEE77) // Default & Don: Traditional Japanese Warm Yellow
        }
    }
    // Smooth transition for background color changes
    val stageBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )

    val isKatActive = visualLeftKat || visualRightKat
    val targetStrokeColor = if (isDark) {
        Color(0xFF333333) // Dark subtle mesh on Black background
    } else if (isKatActive) {
        Color(0xFF0284C7).copy(alpha = 0.22f) // Light Blue mesh on Blue background
    } else {
        Color(0xFFD97706).copy(alpha = 0.22f) // Amber/Gold mesh on Yellow background
    }
    // Smooth transition for the lattice line color
    val strokeColor by animateColorAsState(
        targetValue = targetStrokeColor,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )

    // Pre-calculate animated colors for drum segments to keep Canvas drawings extremely efficient
    val leftKatColorTarget = if (visualLeftKat) {
        if (visualBigKat) Color(0xFF00B2FF) else Color(0xFF5CD6FF)
    } else {
        val baseColor = if (visualBigKat) Color(0xFF00B2FF) else Color(0xFF5CD6FF)
        baseColor.copy(alpha = 0f)
    }
    val leftKatColorAnimated by animateColorAsState(
        targetValue = leftKatColorTarget,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )
    val leftKatColor = if (settings.lightweightRenderingMode) leftKatColorTarget else leftKatColorAnimated

    val rightKatColorTarget = if (visualRightKat) {
        if (visualBigKat) Color(0xFF00B2FF) else Color(0xFF5CD6FF)
    } else {
        val baseColor = if (visualBigKat) Color(0xFF00B2FF) else Color(0xFF5CD6FF)
        baseColor.copy(alpha = 0f)
    }
    val rightKatColorAnimated by animateColorAsState(
        targetValue = rightKatColorTarget,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )
    val rightKatColor = if (settings.lightweightRenderingMode) rightKatColorTarget else rightKatColorAnimated

    val leftDonColorTarget = if (visualLeftDon) {
        Color(0xFFFF5A14)
    } else {
        Color(0xFFFF5A14).copy(alpha = 0f)
    }
    val leftDonColorAnimated by animateColorAsState(
        targetValue = leftDonColorTarget,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )
    val leftDonColor = if (settings.lightweightRenderingMode) leftDonColorTarget else leftDonColorAnimated

    val rightDonColorTarget = if (visualRightDon) {
        Color(0xFFFF5A14)
    } else {
        Color(0xFFFF5A14).copy(alpha = 0f)
    }
    val rightDonColorAnimated by animateColorAsState(
        targetValue = rightDonColorTarget,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )
    val rightDonColor = if (settings.lightweightRenderingMode) rightDonColorTarget else rightDonColorAnimated

    val bigDonColorTarget = if (visualBigDon) {
        Color(0xFFD03800)
    } else {
        Color(0xFFD03800).copy(alpha = 0f)
    }
    val bigDonColorAnimated by animateColorAsState(
        targetValue = bigDonColorTarget,
        animationSpec = tween(durationMillis = 80, easing = LinearOutSlowInEasing)
    )
    val bigDonColor = if (settings.lightweightRenderingMode) bigDonColorTarget else bigDonColorAnimated

    val effectiveBgColor = if (isOverlay) {
        Color.Transparent
    } else {
        stageBgColor
    }

    val boxModifier = if (isOverlay) {
        modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer {
                alpha = overlayAlpha.coerceIn(0.1f, 1.0f)
            }
    } else if (isFullScreen) {
        modifier
            .fillMaxSize()
            .background(effectiveBgColor)
    } else {
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .shadow(8.dp)
            .background(effectiveBgColor)
    }

    Box(
        modifier = boxModifier
            .onGloballyPositioned {
                size = Size(it.size.width.toFloat(), it.size.height.toFloat())
            }
            .drawBehind {
                if (!isOverlay) {
                    // Draw Japanese Lattice (Kagome lattice triple-weave pattern)
                    val strokeWidth = 1.8f
                    
                    val hSpacing = 36.dp.toPx()
                    val wSpacing = hSpacing * 2f / sqrt(3f)
                    
                    // 1. Draw horizontal lines
                    var y = 0f
                    while (y < size.height + hSpacing) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = strokeWidth
                        )
                        y += hSpacing
                    }
                    
                    // 2. Draw dual diagonal lines to form the Kagome hexagonal mesh
                    val dx = wSpacing / 2f
                    val xRangeMin = -size.height - wSpacing
                    val xRangeMax = size.width + size.height + wSpacing
                    
                    var x = xRangeMin
                    while (x < xRangeMax) {
                        drawLine(
                            color = strokeColor,
                            start = Offset(x, 0f),
                            end = Offset(x + (size.height / hSpacing) * dx, size.height),
                            strokeWidth = strokeWidth
                        )
                        
                        drawLine(
                            color = strokeColor,
                            start = Offset(x, 0f),
                            end = Offset(x - (size.height / hSpacing) * dx, size.height),
                            strokeWidth = strokeWidth
                        )
                        
                        x += wSpacing
                    }
                }
            }
            .pointerInteropFilter { event ->
                val action = event.actionMasked
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                val pX = event.getX(index)
                val pY = event.getY(index)

                when (action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        // 1. Avoid duplicate processing if pointerId is already down (protecting against double-fires)
                        if (pointerPartsMap.containsKey(pointerId)) {
                            return@pointerInteropFilter true
                        }

                        // Calculate touch location on drum relative to center
                        val dx = pX - centerX
                        val dy = pY - centerY
                        val distance = sqrt(dx * dx + dy * dy)

                        val parts = mutableListOf<String>()
                        val isDon: Boolean

                        val isLeftHand = pX < centerX

                        if (distance < donMaxRadius) {
                            isDon = true
                            if (settings.singleHandBigNotes && distance < donBigRadius) {
                                parts.add("leftDon")
                                parts.add("rightDon")
                            } else {
                                parts.add(if (isLeftHand) "leftDon" else "rightDon")
                            }
                        } else {
                            isDon = false
                            if (settings.singleHandBigNotes && distance >= donMaxRadius && distance <= katBigOuterRadius) {
                                parts.add("leftKat")
                                parts.add("rightKat")
                            } else {
                                parts.add(if (isLeftHand) "leftKat" else "rightKat")
                            }
                        }

                        // 2. Touchscreen taps are processed instantly without artificial debounce
                        val now = SystemClock.uptimeMillis()
                        val validParts = parts

                        if (validParts.isEmpty()) {
                            return@pointerInteropFilter true
                        }

                        // Update successfully triggered parts with the new timestamp
                        validParts.forEach { part ->
                            lastTriggerTimeMap[part] = now
                        }

                        // Register active pointers per part
                        pointerPartsMap[pointerId] = validParts

                        // Trigger inputs
                        if (validParts.size == 1) {
                            onInputTriggered(validParts[0], true)
                        } else {
                            onMultiInputTriggered(validParts.map { it to true })
                        }

                        // Play local audio feedback
                        if (settings.soundEffects && audioPlayer != null) {
                            if (isDon) {
                                audioPlayer.playDon(settings.soundVolume)
                            } else {
                                audioPlayer.playKat(settings.soundVolume)
                            }
                        }

                        val isBigNote = validParts.size > 1

                        // Play haptic vibration feedback
                        if (settings.vibration) {
                            vibrateAction(isBigNote)
                        }

                        // Create visual hit ripple (only when lightweight rendering mode is OFF)
                        if (!settings.lightweightRenderingMode) {
                            val rippleId = rippleIdCounter++
                            val rippleRadius = if (isBigNote) 240f else 150f
                            if (ripples.size >= 6) {
                                ripples.removeAt(0)
                            }
                            ripples.add(
                                VisualRipple(
                                    id = rippleId,
                                    x = pX,
                                    y = pY,
                                    isDon = isDon,
                                    maxRadius = rippleRadius,
                                    creationTime = now
                                )
                            )

                            // Remove ripple after 350ms
                            scope.launch {
                                delay(350)
                                ripples.removeAll { it.id == rippleId }
                            }
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        pointerPartsMap.remove(pointerId)?.let { parts ->
                            if (parts.size == 1) {
                                onInputTriggered(parts[0], false)
                            } else {
                                onMultiInputTriggered(parts.map { it to false })
                            }
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // Keep active buttons engaged where they started.
                    }
                }
                true
            }
    ) {
        // --- 1. Wooden Legs / Stand Base (太鼓の台座・台形) ---
        if (drumDiameter > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val standTopY = centerY + drumRadius * 0.05f
                val standBottomY = centerY + drumRadius * 0.80f
                val topHalfW = drumRadius * 0.80f
                val bottomHalfW = drumRadius * 0.70f
                val cornerR = 12f

                val standPath = Path().apply {
                    // 上辺 (左 -> 右)
                    moveTo(centerX - topHalfW + cornerR, standTopY)
                    lineTo(centerX + topHalfW - cornerR, standTopY)
                    // 右上角
                    quadraticBezierTo(centerX + topHalfW, standTopY, centerX + topHalfW - 2f, standTopY + 6f)
                    // 右斜め辺 (上 -> 下)
                    lineTo(centerX + bottomHalfW + 2f, standBottomY - 6f)
                    // 右下角 (丸みを抑えた適度な角丸)
                    quadraticBezierTo(centerX + bottomHalfW, standBottomY, centerX + bottomHalfW - cornerR, standBottomY)
                    // 下辺 (右 -> 左)
                    lineTo(centerX - bottomHalfW + cornerR, standBottomY)
                    // 左下角
                    quadraticBezierTo(centerX - bottomHalfW, standBottomY, centerX - bottomHalfW - 2f, standBottomY - 6f)
                    // 左斜め辺 (下 -> 上)
                    lineTo(centerX - topHalfW + 2f, standTopY + 6f)
                    // 左上角
                    quadraticBezierTo(centerX - topHalfW, standTopY, centerX - topHalfW + cornerR, standTopY)
                    close()
                }

                // 台座の塗り（グラデーション）
                drawPath(
                    path = standPath,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF0F172A))
                        } else {
                            listOf(Color(0xFFB45309), Color(0xFF78350F), Color(0xFF3B1A03))
                        },
                        startY = standTopY,
                        endY = standBottomY
                    )
                )

                // 台座の輪郭線
                drawPath(
                    path = standPath,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF2E2520),
                    style = Stroke(width = 6f)
                )
            }
        }

        // --- 2. Master Taiko Drum Graphics (太鼓本体 - Single Canvas to prevent ellipse distortions) ---
        if (drumDiameter > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Bounding boxes for guaranteed perfect circles
                val drumSize = Size(drumDiameter, drumDiameter)
                val drumTopLeft = Offset(centerX - drumRadius, centerY - drumRadius)

                // Define the visual drum radius (thinner rim for elegant top-down balance: 80% of drumRadius)
                val visualDrumRadius = drumRadius * 0.80f
                val visualDrumSize = Size(visualDrumRadius * 2f, visualDrumRadius * 2f)
                val visualDrumTopLeft = Offset(centerX - visualDrumRadius, centerY - visualDrumRadius)

                // 1. Draw solid idle gray/beige/metal drum rim up to visualDrumRadius
                val rimColor = if (isDark) Color(0xFF475569) else Color(0xFFE2DAD1)
                drawArc(
                    color = rimColor,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = visualDrumTopLeft,
                    size = visualDrumSize
                )
                drawArc(
                    color = rimColor,
                    startAngle = 270f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = visualDrumTopLeft,
                    size = visualDrumSize
                )

                // 2. Active Kat highlight background (Lights up in beautiful Cyan-Blue)
                // The Kat light effect and Big Kat zone extend outwards together to katBigOuterRadius
                val katDrawRadius = katBigOuterRadius
                val katDrawSize = Size(katDrawRadius * 2f, katDrawRadius * 2f)
                val katDrawTopLeft = Offset(centerX - katDrawRadius, centerY - katDrawRadius)

                if (leftKatColor.alpha > 0f) {
                    drawArc(
                        color = leftKatColor,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = katDrawTopLeft,
                        size = katDrawSize
                    )
                }

                if (rightKatColor.alpha > 0f) {
                    drawArc(
                        color = rightKatColor,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = katDrawTopLeft,
                        size = katDrawSize
                    )
                }

                // 3. Uniform 360-degree outer crisp border for a perfect top-down view (at visualDrumRadius)
                drawCircle(
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF2E2520),
                    radius = visualDrumRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 6f)
                )

                // 4. Place individual 3D rivets/鋲 uniformly spaced around the entire 360-degree circumference.
                // Placed perfectly in the center of the thinner rim (between skinRadius = 0.6864f and visualDrumRadius = 0.80f).
                val rivetRadius = drumRadius * 0.7432f // Halfway between 0.6864f and 0.80f
                for (deg in 0 until 360 step 20) { // Symmetrical 18 rivets
                    val angleRad = Math.toRadians(deg.toDouble())
                    val rx = centerX + rivetRadius * cos(angleRad).toFloat()
                    val ry = centerY + rivetRadius * sin(angleRad).toFloat()
                    
                    // Draw rivet outer thin dark shadow border
                    drawCircle(
                        color = if (isDark) Color(0xFF020617) else Color(0xFF1E1510),
                        radius = drumRadius * 0.022f,
                        center = Offset(rx, ry)
                    )
                    // Draw rivet body (metallic chrome in dark mode vs charcoal in light mode)
                    drawCircle(
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF4E4540),
                        radius = drumRadius * 0.018f,
                        center = Offset(rx, ry)
                    )
                    // Draw a shiny metallic 3D highlight
                    drawCircle(
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFFFFFDF9).copy(alpha = 0.65f),
                        radius = drumRadius * 0.007f,
                        center = Offset(rx - drumRadius * 0.004f, ry - drumRadius * 0.004f)
                    )
                }

                // 4. Main Drum Face/Skin (Large center circle representing the Don skin, matched precisely to touch boundaries at 68.64%)
                val skinRadius = drumRadius * 0.6864f
                val skinDiameter = skinRadius * 2f
                val skinSize = Size(skinDiameter, skinDiameter)
                val skinTopLeft = Offset(centerX - skinRadius, centerY - skinRadius)

                // Base drum face color (Dark Titanium Steel face in dark theme vs Cream/white paper skin in light theme)
                val skinBaseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF6E5)
                drawCircle(
                    color = skinBaseColor,
                    radius = skinRadius,
                    center = Offset(centerX, centerY)
                )

                // Left Don skin split highlights (Always use vibrant peach/orange for the surrounding skin)
                if (leftDonColor.alpha > 0f) {
                    drawArc(
                        color = leftDonColor,
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = skinTopLeft,
                        size = skinSize
                    )
                }

                // Right Don skin split highlights (Always use vibrant peach/orange for the surrounding skin)
                if (rightDonColor.alpha > 0f) {
                    drawArc(
                        color = rightDonColor,
                        startAngle = 270f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = skinTopLeft,
                        size = skinSize
                    )
                }

                // 5. Draw the Big Don decision circle (inner circle of radius donBigRadius = drumRadius * 0.2746f)
                // If Big Don is active, draw it in the darker orange color.
                if (bigDonColor.alpha > 0f) {
                    drawCircle(
                        color = bigDonColor,
                        radius = donBigRadius,
                        center = Offset(centerX, centerY)
                    )
                }

                // Elegant thin outline for the Big Don decision circle (makes the target area visible)
                drawCircle(
                    color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.4f) else Color(0xFF78350F).copy(alpha = 0.25f),
                    radius = donBigRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )

                // Elegant outline for the drum skin
                drawCircle(
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF222222),
                    radius = skinRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 6f)
                )
                
                // Soft inner border for premium visual aesthetics
                drawCircle(
                    color = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFEADCC9).copy(alpha = 0.4f),
                    radius = skinRadius - 3f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 4f)
                )
            }
        }

        // --- 3. Tap Ripples layer (タップ波紋星のエフェクト) ---
        if (!settings.lightweightRenderingMode && ripples.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Read animationTick state to trigger recomposition on every frame
                val tick = animationTick
                ripples.forEach { ripple ->
                    val elapsed = SystemClock.uptimeMillis() - ripple.creationTime
                    val duration = 350.0f
                    val progress = (elapsed / duration).coerceIn(0f, 1f)
                    
                    val rotationAngle = progress * 140f // Smooth rotation up to 140 degrees
                    val alpha = 1.0f - progress // Elegant fade out
                    val currentRadius = ripple.maxRadius * (0.4f + 0.6f * progress) // Scale up from 40% to 100% size
                    
                    rotate(degrees = rotationAngle, pivot = Offset(ripple.x, ripple.y)) {
                        if (ripple.isDon) {
                            // Don ripple (Red/Orange Star)
                            drawCircle(
                                color = Color(0xFFF87171).copy(alpha = 0.5f * alpha),
                                radius = currentRadius * 0.7f,
                                center = Offset(ripple.x, ripple.y),
                                style = Stroke(width = 8f)
                            )
                            drawCircle(
                                color = Color(0xFFFB923C).copy(alpha = 0.3f * alpha),
                                radius = currentRadius,
                                center = Offset(ripple.x, ripple.y),
                                style = Stroke(width = 4f)
                            )
                            
                            // Draw Star path inside
                            drawStar(Offset(ripple.x, ripple.y), currentRadius * 0.45f, Color(0xFFEA580C).copy(alpha = alpha))
                        } else {
                            // Kat ripple (Sky Blue Cross)
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.5f * alpha),
                                radius = currentRadius * 0.7f,
                                center = Offset(ripple.x, ripple.y),
                                style = Stroke(width = 8f)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.4f * alpha),
                                radius = currentRadius,
                                center = Offset(ripple.x, ripple.y),
                                style = Stroke(width = 4f)
                            )
                            
                            // Draw Cross Star
                            drawCrossStar(Offset(ripple.x, ripple.y), currentRadius * 0.4f, Color(0xFFBAE6FD).copy(alpha = alpha))
                        }
                    }
                }
            }
        }

        // Split Guide dotted line
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
                .background(Color.Transparent)
                .drawBehind {
                    drawLine(
                        color = if (isDark) Color(0xFF94A3B8).copy(alpha = 0.3f) else Color(0xFF78350F).copy(alpha = 0.15f),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
        )


    }
}

// Helper structures for inputs
data class RecordActiveInputs(
    val leftKat: Boolean = false,
    val leftDon: Boolean = false,
    val rightDon: Boolean = false,
    val rightKat: Boolean = false
)

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val sections = 5
    val angleBetween = (2f * kotlin.math.PI.toFloat() / sections)
    
    path.moveTo(
        center.x + radius * sin(0f),
        center.y - radius * cos(0f)
    )
    for (i in 1..sections) {
        val angle = i.toFloat() * angleBetween
        val innerAngle = angle - angleBetween / 2f
        val innerRadius = radius * 0.4f
        
        path.lineTo(
            center.x + innerRadius * sin(innerAngle),
            center.y - innerRadius * cos(innerAngle)
        )
        path.lineTo(
            center.x + radius * sin(angle),
            center.y - radius * cos(angle)
        )
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawCrossStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val sections = 4
    val angleBetween = (2f * kotlin.math.PI.toFloat() / sections)
    
    path.moveTo(
        center.x + radius * sin(0f),
        center.y - radius * cos(0f)
    )
    for (i in 1..sections) {
        val angle = i.toFloat() * angleBetween
        val innerAngle = angle - angleBetween / 2f
        val innerRadius = radius * 0.18f
        
        path.lineTo(
            center.x + innerRadius * sin(innerAngle),
            center.y - innerRadius * cos(innerAngle)
        )
        path.lineTo(
            center.x + radius * sin(angle),
            center.y - radius * cos(angle)
        )
    }
    path.close()
    drawPath(path, color)
}

@Composable
private fun rememberDecayedState(active: Boolean, decayMs: Long = 80L): Boolean {
    var visualActive by remember { mutableStateOf(active) }
    LaunchedEffect(active) {
        if (active) {
            visualActive = true
        } else {
            delay(decayMs)
            visualActive = false
        }
    }
    return visualActive
}
