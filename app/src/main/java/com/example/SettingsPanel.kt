package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPanel(
    settings: ControllerSettings,
    onSettingsChanged: (ControllerSettings) -> Unit,
    wsConnected: Boolean,
    peerCount: Int,
    wsServerUrl: String,
    onWsServerUrlChanged: (String) -> Unit,
    adbStatus: String, // "disconnected", "connecting", "connected", "error"
    adbError: String?,
    onAdbConnect: () -> Unit,
    onAdbPair: () -> Unit,
    onEnterFullScreen: () -> Unit,
    onStartOverlay: () -> Unit = {},
    shizukuRunning: Boolean = false,
    shizukuPermission: Boolean = false,
    onRequestShizukuPermission: () -> Unit = {},
    onOpenShizukuApp: () -> Unit = {},
    onRefreshShizukuStatus: () -> Unit = {},
    pcClientsCount: Int = 0,
    remoteSenderStatus: String = "disconnected",
    remoteReceiverClientsCount: Int = 0,
    onConnectRemoteSender: () -> Unit = {},
    bluetoothSenderStatus: String = "disconnected",
    bluetoothConnectedDeviceName: String? = null,
    bluetoothReceiverConnectedDevice: String? = null,
    bluetoothPairedDevices: List<TaikoBluetoothManager.BluetoothDeviceInfo> = emptyList(),
    onConnectBluetoothDevice: (address: String, name: String) -> Unit = { _, _ -> },
    onDisconnectBluetooth: () -> Unit = {},
    onRefreshBluetoothDevices: () -> Unit = {},
    onOpenBluetoothSettings: () -> Unit = {},
    onResetConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var isAllExpanded by remember { mutableStateOf(false) }

    var expandSizeCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandConnCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandShizukuCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandUsbCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandDrumCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandPresetCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandKeyCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
    var expandThemeCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }

    val isDark = resolveIsDarkTheme(settings.themeMode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Top Bar with Expand All / Collapse All ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚙️ コントローラー設定項目",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78350F).invertIfDark(isDark)
            )

            TextButton(
                onClick = {
                    isAllExpanded = !isAllExpanded
                    expandSizeCard = isAllExpanded
                    expandConnCard = isAllExpanded
                    expandShizukuCard = isAllExpanded
                    expandUsbCard = isAllExpanded
                    expandDrumCard = isAllExpanded
                    expandPresetCard = isAllExpanded
                    expandKeyCard = isAllExpanded
                    expandThemeCard = isAllExpanded
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (isAllExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                    contentDescription = null,
                    tint = Color(0xFFD97706).invertIfDark(isDark),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAllExpanded) "すべて折りたたむ" else "すべて展開",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706).invertIfDark(isDark)
                )
            }
        }

        // --- PC Connected Active Banner ---
        if (settings.connectionMode == "usb-wired" && pcClientsCount > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)),
                border = BorderStroke(1.5.dp, if (isDark) Color(0xFF10B981) else Color(0xFF10B981)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF34D399) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "💻 PCとの接続完了 (${pcClientsCount}台接続中)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = "PC側スクリプト(ポート60001)と正常に接続中です。太鼓を叩くとPCへ入力が送信されます。",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                        )
                    }
                }
            }
        }

        // --- 0. Display Mode (全画面 & オーバーレイ表示) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F5EB).invertIfDark(isDark)),
            border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.15f).invertIfDark(isDark)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📱 表示モード切り替え",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Text(
                            text = "全画面表示または他アプリの上に重ねて表示",
                            fontSize = 10.sp,
                            color = if (isDark) Color.White else Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fullscreen Button
                    Button(
                        onClick = onEnterFullScreen,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706).invertIfDark(isDark)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "📱 全画面",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = false
                        )
                    }

                    // Overlay Mode Button
                    Button(
                        onClick = onStartOverlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).invertIfDark(isDark)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Overlay",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "🪟 オーバーレイ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            softWrap = false
                        )
                    }
                }
            }
        }



        val context = LocalContext.current
        var anotherAndroidTapCount by remember { mutableIntStateOf(0) }
        var lastAnotherAndroidTapTime by remember { mutableLongStateOf(0L) }

        val onAnotherAndroidTapped: () -> Unit = {
            if (settings.connectionMode != "another_android") {
                // 他のモードから「別のAndroid」に切り替えるタップはカウントせず、モード切り替えのみ行う
                anotherAndroidTapCount = 0
                onSettingsChanged(settings.copy(connectionMode = "another_android"))
            } else {
                val now = System.currentTimeMillis()
                if (now - lastAnotherAndroidTapTime > 3500L) {
                    anotherAndroidTapCount = 0
                }
                lastAnotherAndroidTapTime = now
                anotherAndroidTapCount++

                if (anotherAndroidTapCount >= 10) {
                    val newShow = !settings.showWirelessOptions
                    anotherAndroidTapCount = 0
                    val updated = if (!newShow && settings.anotherAndroidConnectionType != "wired") {
                        settings.copy(connectionMode = "another_android", showWirelessOptions = newShow, anotherAndroidConnectionType = "wired")
                    } else {
                        settings.copy(connectionMode = "another_android", showWirelessOptions = newShow)
                    }
                    onSettingsChanged(updated)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(
                            context.applicationContext,
                            if (newShow) "🔓 隠し設定解放: 無線通信モード (Wi-Fi / Bluetooth) を出現させました！" else "🔒 無線通信モードを非表示にしました (USB有線固定)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    TaikoLogManager.log(if (newShow) "無線通信モード: 解放" else "無線通信モード: 非表示")
                }
            }
        }

        // --- Connection Mode / Destination Card ---
        CollapsibleSettingCard(
            title = "🌐 接続先設定",
            subtitle = when (settings.connectionMode) {
                "shizuku" -> "この端末 (Shizuku)"
                "usb-wired" -> "PC (USB)"
                "another_android" -> "別のAndroid (${if (settings.anotherAndroidRole == "sender") "送信側" else "受信側"})"
                else -> "なし (ローカル)"
            },
            badgeText = when (settings.connectionMode) {
                "shizuku" -> "この端末"
                "usb-wired" -> "PC"
                "another_android" -> "別のAndroid"
                else -> "なし"
            },
            isExpanded = expandConnCard,
            onExpandedChange = { expandConnCard = it },
            isDarkTheme = isDark
        ) {
            val modes = listOf(
                "shizuku" to "この端末",
                "usb-wired" to "PC",
                "another_android" to "別のAndroid",
                "local-demo" to "なし"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                modes.forEach { (modeVal, label) ->
                    val isSelected = settings.connectionMode == modeVal
                    Button(
                        onClick = {
                            if (modeVal == "another_android") {
                                onAnotherAndroidTapped()
                            } else {
                                onSettingsChanged(settings.copy(connectionMode = modeVal))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                            contentColor = if (isSelected) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- 2.1 Shizuku Local Key Injection Card ("この端末") ---
        if (settings.connectionMode == "shizuku") {
            CollapsibleSettingCard(
                title = "⚡ この端末 (Shizuku) 設定",
                subtitle = if (shizukuRunning && shizukuPermission) "動作形態: ${if (settings.shizukuEmulationMode == "gamepad") "ゲームパッド" else "キーボード"}" else "Shizuku 未起動 / 権限なし",
                badgeText = if (shizukuRunning && shizukuPermission) "稼働中" else "要確認",
                badgeColor = if (shizukuRunning && shizukuPermission) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                badgeTextColor = if (shizukuRunning && shizukuPermission) Color(0xFF065F46) else Color(0xFF991B1B),
                isExpanded = expandShizukuCard,
                onExpandedChange = { expandShizukuCard = it },
                isDarkTheme = isDark
            ) {
                ShizukuSettingsContent(
                    settings = settings,
                    shizukuRunning = shizukuRunning,
                    shizukuPermission = shizukuPermission,
                    onOpenShizukuApp = onOpenShizukuApp,
                    onRefreshShizukuStatus = onRefreshShizukuStatus,
                    onRequestShizukuPermission = onRequestShizukuPermission,
                    onSettingsChanged = onSettingsChanged,
                    isDark = isDark
                )
            }
        }

        // --- 2.1.2 Another Android Remote Connection Card ---
        if (settings.connectionMode == "another_android") {
            var expandAnotherAndroidCard by remember(isAllExpanded) { mutableStateOf(isAllExpanded) }
            var isScanningByAutoDiscovery by remember { mutableStateOf(false) }
            var autoDiscoveryMessage by remember { mutableStateOf<String?>(null) }
            var showManualInput by remember { mutableStateOf(false) }

            val connTypeTitle = when (settings.anotherAndroidConnectionType) {
                "bluetooth" -> "無線 Bluetooth"
                "wireless" -> "無線 Wi-Fi"
                else -> "有線 USB通信"
            }

            CollapsibleSettingCard(
                title = "📱 別のAndroid連携設定 ($connTypeTitle)",
                subtitle = if (settings.anotherAndroidRole == "sender") {
                    "役割: 送信側 (太鼓) | 方式: $connTypeTitle"
                } else {
                    "役割: 受信側 (ゲーム) | 方式: $connTypeTitle"
                },
                badgeText = if (settings.anotherAndroidRole == "sender") "送信側" else "受信側",
                badgeColor = if (settings.anotherAndroidRole == "sender") Color(0xFFDBEAFE) else Color(0xFFDCFCE7),
                badgeTextColor = if (settings.anotherAndroidRole == "sender") Color(0xFF1E40AF) else Color(0xFF166534),
                isExpanded = expandAnotherAndroidCard,
                onExpandedChange = { expandAnotherAndroidCard = it },
                onHeaderClick = onAnotherAndroidTapped,
                isDarkTheme = isDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isUsbDirectConnected by TaikoUsbDirectManager.isConnectedState.collectAsState()

                    if (settings.anotherAndroidConnectionType == "wired") {
                        Surface(
                            color = if (isUsbDirectConnected) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUsbDirectConnected) "⚡ USB Direct (AOA) 超極小遅延通信 接続完了 (<1ms Latency)" else "🔌 USBケーブルを繋ぐだけで自動認識されます (テザリング設定は不要です)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUsbDirectConnected) Color(0xFF166534) else Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Text(
                        text = when (settings.anotherAndroidConnectionType) {
                            "bluetooth" -> "📶 Bluetoothで2台のAndroidを直接ワイヤレス接続します。外部Wi-Fiルーターやテザリング、IPアドレスの入力は不要！端末同士をペアリングするだけで接続できます。"
                            "wired" -> "2台のAndroid端末をType-C - Type-C ケーブル（またはUSB OTGケーブル）で繋ぐだけ！USB AOAダイレクト通信により、ネットワーク遅延ゼロ・1ms未満の最高速入力レスポンスを実現します。"
                            else -> "2台のAndroid端末を同じWi-Fi（またはネットワーク）に接続し、一方を「送信側（太鼓）」、もう一方を「受信側（ゲーム）」として通信させます。"
                        },
                        fontSize = 10.sp,
                        color = if (isDark) Color.White else Color.DarkGray
                    )

                    // Wireless Warning Banner (Only shown when wireless options are unlocked and wireless/bluetooth is selected)
                    if (settings.showWirelessOptions && (settings.anotherAndroidConnectionType == "wireless" || settings.anotherAndroidConnectionType == "bluetooth")) {
                        Surface(
                            color = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFB45309) else Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "⚠️", fontSize = 15.sp)
                                    Text(
                                        text = "無線通信（${if (settings.anotherAndroidConnectionType == "bluetooth") "Bluetooth" else "Wi-Fi"}）使用時の注意",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = "スマートフォンの無線通信は、Android OSの省電力制御（Sniff Mode/スリープ）、パケットバッファリング、電波干渉等の影響を受けるため、入力の遅延（レイテンシ）や打鍵の欠落（抜け）が発生しやすくなります。\n\n高精度な判定や高速連打の安定性を求める場合は、Type-Cケーブル直結の「🔌 USB有線」接続を推奨します。",
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                                )
                            }
                        }
                    }

                    // Connection Type Selector (Hidden mode: only visible when showWirelessOptions is true)
                    if (settings.showWirelessOptions) {
                        Text(
                            text = "接続方式を選択:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Button layout order: USB有線, Wi-Fi, Bluetooth
                            val connTypes = listOf(
                                "wired" to "🔌 USB有線",
                                "wireless" to "🌐 Wi-Fi",
                                "bluetooth" to "📶 Bluetooth"
                            )
                            connTypes.forEach { (typeVal, label) ->
                                val isSelected = settings.anotherAndroidConnectionType == typeVal
                                Button(
                                    onClick = {
                                        onSettingsChanged(settings.copy(anotherAndroidConnectionType = typeVal))
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                                        contentColor = if (isSelected) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // When wireless options are hidden, show USB wired indicator
                        Surface(
                            color = Color(0xFFFEF3C7).invertIfDark(isDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAnotherAndroidTapped() }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔌 接続方式: USB有線 (Type-C直結)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E).invertIfDark(isDark)
                                )
                            }
                        }
                    }

                    // Role Selector
                    Text(
                        text = "この端末の役割を選択:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            "sender" to "送信側 (太鼓)",
                            "receiver" to "受信側 (ゲーム)"
                        )
                        roles.forEach { (roleVal, label) ->
                            val isSelected = settings.anotherAndroidRole == roleVal
                            Button(
                                onClick = {
                                    onSettingsChanged(settings.copy(anotherAndroidRole = roleVal))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                                    contentColor = if (isSelected) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF78350F).copy(alpha = 0.1f).invertIfDark(isDark))

                    if (settings.anotherAndroidRole == "sender") {
                        // SENDER
                        val headerText = when (settings.anotherAndroidConnectionType) {
                            "bluetooth" -> "【送信側の設定 (Bluetooth直接接続)】"
                            "wired" -> "【送信側の設定 (有線 USB通信)】"
                            else -> "【送信側の設定 (無線 Wi-Fi)】"
                        }
                        Text(
                            text = headerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )

                        if (settings.anotherAndroidConnectionType == "bluetooth") {
                            // BLUETOOTH SENDER
                            val isConnected = bluetoothSenderStatus == "connected"
                            val isConnecting = bluetoothSenderStatus == "connecting"
                            val isError = bluetoothSenderStatus == "error"

                            val statusBg = when {
                                isConnected -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                isConnecting -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                                isError -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                                else -> if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6)
                            }
                            val statusTextColor = when {
                                isConnected -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                isConnecting -> if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                isError -> if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                                else -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151)
                            }
                            val statusText = when {
                                isConnected -> "🟢 Bluetooth接続中: ${bluetoothConnectedDeviceName ?: settings.anotherAndroidBluetoothDeviceName.ifEmpty { "ゲーム端末" }}"
                                isConnecting -> "🟡 ゲーム端末にBluetooth接続中..."
                                isError -> "🔴 Bluetooth接続エラー (相手端末でアプリが起動しているか確認してください)"
                                else -> "⚪ Bluetooth未接続 (下の端末一覧からゲーム端末をタップして接続)"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isConnected) {
                                    TextButton(
                                        onClick = onDisconnectBluetooth,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("切断", fontSize = 10.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            }

                            // Paired Devices List Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📱 ペアリング済み端末一覧 (タップして接続)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
                                        )
                                        TextButton(
                                            onClick = onRefreshBluetoothDevices,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("更新", fontSize = 10.sp, color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF))
                                        }
                                    }

                                    if (bluetoothPairedDevices.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "ペアリング済みのBluetooth端末が見つかりません。\n下のボタンからゲーム端末とBluetoothペアリングしてください。",
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            bluetoothPairedDevices.forEach { dev ->
                                                val isThisConnected = isConnected && (settings.anotherAndroidBluetoothDeviceAddress == dev.address || bluetoothConnectedDeviceName == dev.name)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isThisConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)))
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = dev.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                                        )
                                                        Text(
                                                            text = dev.address,
                                                            fontSize = 9.sp,
                                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                                        )
                                                    }

                                                    Button(
                                                        onClick = {
                                                            onConnectBluetoothDevice(dev.address, dev.name)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isThisConnected) Color(0xFF16A34A) else (if (isDark) Color(0xFF2563EB) else Color(0xFF3B82F6))
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isThisConnected) "🟢 接続中" else "接続",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = onOpenBluetoothSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📱 AndroidのBluetooth設定を開く (新規ペアリング)", fontSize = 11.sp, color = Color.White)
                            }

                            // Bluetooth Instructions Tip Card
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "💡 Bluetooth接続の簡単3ステップ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                    )
                                    Text(
                                        text = "① 受信側（ゲーム端末）とこの端末（太鼓端末）の両方でBluetoothをONにしてペアリングします。\n" +
                                               "② ゲーム端末でこのアプリを起動し、「受信側 (ゲーム)」にして待機します。\n" +
                                               "③ 上の一覧に表示されたゲーム端末の名前をタップするだけで即接続完了！\n" +
                                               "※Wi-Fiルーターやテザリング、IPアドレス指定が一切不要で、最速・超低遅延で快適に遊べます！",
                                        fontSize = 9.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF14532D),
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                        } else if (settings.anotherAndroidConnectionType == "wireless") {
                            Text(
                                text = "「自動検出」を押すか、受信側（ゲーム）画面に表示されているIPアドレスを入力して接続してください。",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White else Color.Gray
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        isScanningByAutoDiscovery = true
                                        autoDiscoveryMessage = "🔍 ゲーム側 (受信機) を自動検出中..."
                                        val portInt = settings.anotherAndroidPort.toIntOrNull() ?: 60002
                                        TaikoAndroidRemoteSender.scanAndFindReceiverIp(
                                            targetPort = portInt,
                                            udpDiscoveryPort = portInt + 200,
                                            connectionType = settings.anotherAndroidConnectionType,
                                            onFound = { foundIp ->
                                                isScanningByAutoDiscovery = false
                                                val isDirectHotspot = foundIp == "192.168.43.1" || foundIp == "192.168.49.1" || foundIp.startsWith("192.168.42.")
                                                val typeLabel = if (isDirectHotspot) " (🔥 テザリング直接接続)" else " (🌐 Wi-Fi経由)"
                                                autoDiscoveryMessage = "✅ 発見しました: $foundIp$typeLabel"
                                                onSettingsChanged(settings.copy(anotherAndroidTargetIp = foundIp))
                                                onConnectRemoteSender()
                                            },
                                            onNotFound = {
                                                isScanningByAutoDiscovery = false
                                                autoDiscoveryMessage = "❌ 受信機が見つかりませんでした。受信側（ゲーム）でアプリを起動しているか確認してください。"
                                            }
                                        )
                                    },
                                    enabled = !isScanningByAutoDiscovery,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD97706).invertIfDark(isDark),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isScanningByAutoDiscovery) "🔍 探索中..." else "🔍 受信機 (ゲーム) を自動検出して接続",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (autoDiscoveryMessage != null) {
                                Text(
                                    text = autoDiscoveryMessage ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoDiscoveryMessage?.startsWith("✅") == true) Color(0xFF059669) else Color(0xFFD97706)
                                )
                            }

                            // Toggle for Manual Connection Settings
                            TextButton(
                                onClick = { showManualInput = !showManualInput },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = if (showManualInput) "⚙️ 手動設定 (IP/ポート指定) を隠す ▴" else "⚙️ 手動設定 (IP/ポート指定) を表示 ▾",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F).invertIfDark(isDark),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (showManualInput) {
                                OutlinedTextField(
                                    value = settings.anotherAndroidTargetIp,
                                    onValueChange = { onSettingsChanged(settings.copy(anotherAndroidTargetIp = it)) },
                                    label = { Text("受信側 (ゲーム) AndroidのIPアドレス") },
                                    placeholder = { Text("192.168.43.1 または 192.168.1.100") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = customTextFieldColors(isDark)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = settings.anotherAndroidPort,
                                        onValueChange = { onSettingsChanged(settings.copy(anotherAndroidPort = it)) },
                                        label = { Text("ポート番号") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        colors = customTextFieldColors(isDark)
                                    )

                                    Button(
                                        onClick = onConnectRemoteSender,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F).invertIfDark(isDark)),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("接続", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Status Banner
                            val statusText = when (remoteSenderStatus) {
                                "connected" -> "接続完了: 受信側 (ゲーム) へ入力を送信可能です"
                                "connecting" -> "接続試行中..."
                                "error" -> "接続エラー: 受信側IP・ポート番号を確認してください"
                                else -> "未接続"
                            }
                            val statusBg = when (remoteSenderStatus) {
                                "connected" -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                "connecting" -> if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                                "error" -> if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                                else -> if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6)
                            }
                            val statusTextColor = when (remoteSenderStatus) {
                                "connected" -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                "connecting" -> if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                "error" -> if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                                else -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }

                            // Wireless Speedup Tip Card
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFFCD34D)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "🚀 Wi-Fiテザリング直接接続の手順",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFDE047) else Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "① 受信側（ゲーム端末）で「Wi-Fiテザリング（アクセスポイント）」をONにします。\n" +
                                               "② 送信側（太鼓端末）のWi-Fi設定を開き、受信側のWi-Fiスポットに接続します。\n" +
                                               "③ この画面で「🔍 受信機 (ゲーム) を自動検出して接続」を押します。（または手動で 192.168.43.1 を入力）\n" +
                                               "※ルーターを介さず端末同士が直接通信するため、安定してプレイできます。",
                                        fontSize = 9.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF78350F),
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        } else {
                            // WIRED SENDER
                            val context = LocalContext.current
                            val isConnected = isUsbDirectConnected || remoteSenderStatus == "connected"

                            val statusBg = if (isConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6))
                            val statusTextColor = if (isConnected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151))
                            val statusText = if (isConnected) "⚡ USB 有線通信: 接続完了" else "🔌 USB ケーブル接続を待機中..."

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }

                            Text(
                                text = "USB Type-Cケーブルでゲーム側Androidと接続します。接続許可ダイアログが出たら「許可」を選択してください。\n※自動接続されない場合は、下のボタンからAndroidのUSB設定を開き、USB制御元を「このデバイス」に変更してください。",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White else Color.Gray
                            )

                            Button(
                                onClick = { openUsbSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFFEA580C)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📱 AndroidのUSB設定を開く (制御元の切替)", fontSize = 11.sp, color = Color.White)
                            }
                        }

                    } else {
                        // RECEIVER
                        val headerText = when (settings.anotherAndroidConnectionType) {
                            "bluetooth" -> "【受信側 (ゲーム) の設定 (Bluetooth直接接続)】"
                            "wired" -> "【受信側 (ゲーム) の設定 (有線 USB通信)】"
                            else -> "【受信側 (ゲーム) の設定 (無線 Wi-Fi)】"
                        }
                        Text(
                            text = headerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )

                        if (settings.anotherAndroidConnectionType == "bluetooth") {
                            // BLUETOOTH RECEIVER
                            val context = LocalContext.current
                            val localBtName = remember { TaikoBluetoothManager.getLocalDeviceName(context) }
                            val isConnected = bluetoothReceiverConnectedDevice != null

                            val statusBg = if (isConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6))
                            val statusTextColor = if (isConnected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151))
                            val statusText = if (isConnected) "🟢 太鼓側端末「${bluetoothReceiverConnectedDevice}」が接続中 (入力受信待機中)" else "🟡 太鼓側 (送信側) からのBluetooth接続を待機中..."

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFF93C5FD)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "この端末のBluetooth名",
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                    )
                                    Text(
                                        text = localBtName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1E3A8A)
                                    )
                                    Text(
                                        text = "太鼓側端末（送信機）の画面で、この名前「$localBtName」をタップして接続してください。",
                                        fontSize = 9.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF3B82F6)
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenBluetoothSettings,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📱 AndroidのBluetooth設定を開く (ペアリング用)", fontSize = 11.sp, color = Color.White)
                            }

                            // Receiver Guide Card
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF0FDF4)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "💡 受信側 (ゲーム端末) の準備手順",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                    )
                                    Text(
                                        text = "① AndroidのBluetoothがONになっていることを確認します。\n" +
                                               "② 太鼓側端末と一度ペアリングします（設定ボタンから可能）。\n" +
                                               "③ このアプリでShizukuを起動した状態で太鼓側から接続すれば、受信した打鍵がゲームに即座に入力されます！",
                                        fontSize = 9.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF14532D),
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                        } else if (settings.anotherAndroidConnectionType == "wireless") {
                            val clipboardManager = LocalClipboardManager.current
                            val detailedIps = remember { NetworkUtils.getDetailedLocalIpAddresses() }
                            val primaryIp = detailedIps.firstOrNull()?.ip ?: NetworkUtils.getLocalIpAddress()

                            Text(
                                text = "この端末のIPアドレスを太鼓側 (送信側) に入力するか、太鼓側で「自動検出」を実行してください。",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White else Color.Gray
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0C4A6E) else Color(0xFFFFF3E0)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF0284C7) else Color(0xFFFFB74D)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "この端末のIPアドレス (接続先)",
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFFC2410C)
                                            )
                                            Text(
                                                text = primaryIp,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF7C2D12)
                                            )
                                            Text(
                                                text = "待受ポート: ${settings.anotherAndroidPort}",
                                                fontSize = 10.sp,
                                                color = if (isDark) Color(0xFFBAE6FD) else Color(0xFFEA580C)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(primaryIp))
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFFEA580C))
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("IPコピー", fontSize = 10.sp, color = Color.White)
                                        }
                                    }

                                    if (detailedIps.size > 1) {
                                        Divider(color = if (isDark) Color(0xFF0369A1) else Color(0xFFFFCC80), thickness = 1.dp)
                                        Text(
                                            text = "検出されたすべてのネットワーク:",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFFC2410C)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            detailedIps.forEach { netIp ->
                                                val isTether = netIp.ip == "192.168.43.1" || netIp.ip == "192.168.49.1" || netIp.isHotspot
                                                val badge = if (isTether) "🔥 [テザリング直接]" else "🌐"
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$badge ${netIp.displayName}: ${netIp.ip}",
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isTether) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isTether) (if (isDark) Color(0xFF38BDF8) else Color(0xFFC2410C)) else (if (isDark) Color.White else Color(0xFF431407))
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            clipboardManager.setText(AnnotatedString(netIp.ip))
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("コピー", fontSize = 9.5.sp, color = if (isDark) Color(0xFF38BDF8) else Color(0xFFEA580C))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (remoteReceiverClientsCount > 0) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6)))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (remoteReceiverClientsCount > 0) "接続中の送信側Android: ${remoteReceiverClientsCount}台 (入力受信待機中)" else "送信側Androidからの接続を待機中...",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remoteReceiverClientsCount > 0) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151))
                                )
                            }
                        } else {
                            // WIRED RECEIVER
                            val context = LocalContext.current
                            val isConnected = isUsbDirectConnected || remoteReceiverClientsCount > 0

                            val statusBg = if (isConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6))
                            val statusTextColor = if (isConnected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151))
                            val statusText = if (isConnected) "⚡ USB 有線通信: 送信側から接続中 (入力受信待機中)" else "🔌 太鼓側 (送信側) からのUSB接続を待機中..."

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }

                            Text(
                                text = "USB Type-Cケーブルで太鼓側 (送信側) Androidと繋いでおくだけで受信準備完了です。接続許可ダイアログが出たら「許可」を選択してください。",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White else Color.Gray
                            )

                            Button(
                                onClick = { openUsbSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFFEA580C)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📱 AndroidのUSB設定を開く", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Universal Port & Connection Reset Button
                        Button(
                            onClick = onResetConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF059669) else Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ 通信・ポート再初期化 (1タップ再接続)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                        ShizukuSettingsContent(
                            settings = settings,
                            shizukuRunning = shizukuRunning,
                            shizukuPermission = shizukuPermission,
                            onOpenShizukuApp = onOpenShizukuApp,
                            onRefreshShizukuStatus = onRefreshShizukuStatus,
                            onRequestShizukuPermission = onRequestShizukuPermission,
                            onSettingsChanged = onSettingsChanged,
                            isDark = isDark
                        )
                    }
                }
            }
        }

        // --- 2.2 USB PC-Wired Connection Card ---
        if (settings.connectionMode == "usb-wired") {
            val linuxScript = String(android.util.Base64.decode("IyEvdXNyL2Jpbi9lbnYgcHl0aG9uMwojIExpbnV4IFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIG5hbWU6IFRUQy1yZWNlaXZlci1saW51eC5zaAojIFVzYWdlOiBjaG1vZCAreCBUVEMtcmVjZWl2ZXItbGludXguc2ggJiYgLi9UVEMtcmVjZWl2ZXItbGludXguc2gKCmltcG9ydCBzb2NrZXQKaW1wb3J0IHN1YnByb2Nlc3MKaW1wb3J0IHRpbWUKaW1wb3J0IHN5cwppbXBvcnQgb3MKaW1wb3J0IHVybGxpYi5yZXF1ZXN0CmltcG9ydCB6aXBmaWxlCmltcG9ydCB0cmFjZWJhY2sKClBPUlQgPSA2MDAwMQoKZGVmIGxvZyhtc2cpOgogICAgcHJpbnQobXNnLCBmbHVzaD1UcnVlKQoKZGVmIGVuc3VyZV9zeXN0ZW1fcGFja2FnZShwa2dfbmFtZV9hcHQsIHBrZ19uYW1lX2RuZj1Ob25lLCBwa2dfbmFtZV9wYWNtYW49Tm9uZSwgcGtnX25hbWVfenlwcGVyPU5vbmUpOgogICAgIyBBdHRlbXB0IHRvIGluc3RhbGwgYSBzeXN0ZW0gcGFja2FnZSB1c2luZyBhdmFpbGFibGUgcGFja2FnZSBtYW5hZ2VyIHdpdGggc3VkbyBwcm9tcHQKICAgIGlmIG9zLnBhdGguZXhpc3RzKCIvdXNyL2Jpbi9hcHQtZ2V0Iik6CiAgICAgICAgbG9nKGYiSW5zdGFsbGluZyB7cGtnX25hbWVfYXB0fSB2aWEgYXB0IChhZG1pbmlzdHJhdG9yIHBhc3N3b3JkIG1heSBiZSByZXF1ZXN0ZWQpLi4uIikKICAgICAgICB0cnk6CiAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsic3VkbyIsICJhcHQtZ2V0IiwgInVwZGF0ZSJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oWyJzdWRvIiwgImFwdC1nZXQiLCAiaW5zdGFsbCIsICIteSIsIHBrZ19uYW1lX2FwdF0sIGNoZWNrPVRydWUpCiAgICAgICAgICAgIHJldHVybiBUcnVlCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICBsb2coZiJhcHQtZ2V0IGluc3RhbGwgZmFpbGVkOiB7ZX0iKQogICAgZWxpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vZG5mIik6CiAgICAgICAgcGtnID0gcGtnX25hbWVfZG5mIG9yIHBrZ19uYW1lX2FwdAogICAgICAgIGxvZyhmIkluc3RhbGxpbmcge3BrZ30gdmlhIGRuZiAoYWRtaW5pc3RyYXRvciBwYXNzd29yZCBtYXkgYmUgcmVxdWVzdGVkKS4uLiIpCiAgICAgICAgdHJ5OgogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbInN1ZG8iLCAiZG5mIiwgImluc3RhbGwiLCAiLXkiLCBwa2ddLCBjaGVjaz1UcnVlKQogICAgICAgICAgICByZXR1cm4gVHJ1ZQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiZG5mIGluc3RhbGwgZmFpbGVkOiB7ZX0iKQogICAgZWxpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vcGFjbWFuIik6CiAgICAgICAgcGtnID0gcGtnX25hbWVfcGFjbWFuIG9yIHBrZ19uYW1lX2FwdAogICAgICAgIGxvZyhmIkluc3RhbGxpbmcge3BrZ30gdmlhIHBhY21hbiAoYWRtaW5pc3RyYXRvciBwYXNzd29yZCBtYXkgYmUgcmVxdWVzdGVkKS4uLiIpCiAgICAgICAgdHJ5OgogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbInN1ZG8iLCAicGFjbWFuIiwgIi1TeSIsICItLW5vY29uZmlybSIsIHBrZ10sIGNoZWNrPVRydWUpCiAgICAgICAgICAgIHJldHVybiBUcnVlCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICBsb2coZiJwYWNtYW4gaW5zdGFsbCBmYWlsZWQ6IHtlfSIpCiAgICBlbGlmIG9zLnBhdGguZXhpc3RzKCIvdXNyL2Jpbi96eXBwZXIiKToKICAgICAgICBwa2cgPSBwa2dfbmFtZV96eXBwZXIgb3IgcGtnX25hbWVfYXB0CiAgICAgICAgbG9nKGYiSW5zdGFsbGluZyB7cGtnfSB2aWEgenlwcGVyIChhZG1pbmlzdHJhdG9yIHBhc3N3b3JkIG1heSBiZSByZXF1ZXN0ZWQpLi4uIikKICAgICAgICB0cnk6CiAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsic3VkbyIsICJ6eXBwZXIiLCAiLS1ub24taW50ZXJhY3RpdmUiLCAiaW4iLCBwa2ddLCBjaGVjaz1UcnVlKQogICAgICAgICAgICByZXR1cm4gVHJ1ZQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYienlwcGVyIGluc3RhbGwgZmFpbGVkOiB7ZX0iKQogICAgcmV0dXJuIEZhbHNlCgpkZWYgbWFpbigpOgogICAgbG9nKCI9PT0gVGFpa28gQ29udHJvbGxlciBSZWNlaXZlciBmb3IgTGludXggPT09IikKICAgIAogICAgYWRiX2NtZCA9ICJhZGIiCgogICAgIyAxLiBWZXJpZnkgYW5kIHNldHVwIEFEQgogICAgdHJ5OgogICAgICAgIHN1YnByb2Nlc3MucnVuKFsiYWRiIiwgInZlcnNpb24iXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgIGV4Y2VwdCBGaWxlTm90Rm91bmRFcnJvcjoKICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cygiLi9wbGF0Zm9ybS10b29scy9hZGIiKToKICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICBlbHNlOgogICAgICAgICAgICBsb2coIkFEQiBub3QgZm91bmQgaW4gUEFUSC4gQXR0ZW1wdGluZyBhdXRvbWF0aWMgaW5zdGFsbGF0aW9uLi4uIikKICAgICAgICAgICAgaW5zdGFsbGVkID0gZW5zdXJlX3N5c3RlbV9wYWNrYWdlKAogICAgICAgICAgICAgICAgcGtnX25hbWVfYXB0PSJhZGIiLAogICAgICAgICAgICAgICAgcGtnX25hbWVfZG5mPSJhbmRyb2lkLXRvb2xzIiwKICAgICAgICAgICAgICAgIHBrZ19uYW1lX3BhY21hbj0iYW5kcm9pZC10b29scyIsCiAgICAgICAgICAgICAgICBwa2dfbmFtZV96eXBwZXI9ImFuZHJvaWQtdG9vbHMiCiAgICAgICAgICAgICkKICAgICAgICAgICAgaWYgaW5zdGFsbGVkOgogICAgICAgICAgICAgICAgYWRiX2NtZCA9ICJhZGIiCiAgICAgICAgICAgIGVsc2U6CiAgICAgICAgICAgICAgICBsb2coIkRvd25sb2FkaW5nIG9mZmljaWFsIHN0YW5kYWxvbmUgQW5kcm9pZCBTREsgUGxhdGZvcm0gVG9vbHMuLi4iKQogICAgICAgICAgICAgICAgdXJsID0gImh0dHBzOi8vZGwuZ29vZ2xlLmNvbS9hbmRyb2lkL3JlcG9zaXRvcnkvcGxhdGZvcm0tdG9vbHMtbGF0ZXN0LWxpbnV4LnppcCIKICAgICAgICAgICAgICAgIHppcF9wYXRoID0gIi4vcGxhdGZvcm0tdG9vbHMuemlwIgogICAgICAgICAgICAgICAgdHJ5OgogICAgICAgICAgICAgICAgICAgIHVybGxpYi5yZXF1ZXN0LnVybHJldHJpZXZlKHVybCwgemlwX3BhdGgpCiAgICAgICAgICAgICAgICAgICAgd2l0aCB6aXBmaWxlLlppcEZpbGUoemlwX3BhdGgsICJyIikgYXMgemlwX3JlZjoKICAgICAgICAgICAgICAgICAgICAgICAgemlwX3JlZi5leHRyYWN0YWxsKCIuIikKICAgICAgICAgICAgICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cyh6aXBfcGF0aCk6CiAgICAgICAgICAgICAgICAgICAgICAgIG9zLnJlbW92ZSh6aXBfcGF0aCkKICAgICAgICAgICAgICAgICAgICBvcy5jaG1vZCgiLi9wbGF0Zm9ybS10b29scy9hZGIiLCAwbzc1NSkKICAgICAgICAgICAgICAgICAgICBhZGJfY21kID0gIi4vcGxhdGZvcm0tdG9vbHMvYWRiIgogICAgICAgICAgICAgICAgICAgIGxvZygiQURCIGRvd25sb2FkZWQgYW5kIGV4dHJhY3RlZCBzdWNjZXNzZnVsbHkgdG8gLi9wbGF0Zm9ybS10b29scy8iKQogICAgICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICAgICAgICAgIGxvZyhmIkVycm9yIGRvd25sb2FkaW5nIHBsYXRmb3JtLXRvb2xzOiB7ZX0iKQogICAgICAgICAgICAgICAgICAgIGxvZygiUGxlYXNlIGluc3RhbGwgJ2FkYicgb3IgJ2FuZHJvaWQtdG9vbHMnIHZpYSB5b3VyIHBhY2thZ2UgbWFuYWdlciBtYW51YWxseS4iKQogICAgICAgICAgICAgICAgICAgIHJldHVybgoKICAgICMgQ2hlY2sgZm9yIEFEQiB1cGRhdGVzCiAgICB0cnk6CiAgICAgICAgbG9nKCJbQURCXSBDaGVja2luZyBmb3IgQURCIHVwZGF0ZXMuLi4iKQogICAgICAgIGlmIG9zLnBhdGguZXhpc3RzKCIuL3BsYXRmb3JtLXRvb2xzL2FkYiIpOgogICAgICAgICAgICB1cmwgPSAiaHR0cHM6Ly9kbC5nb29nbGUuY29tL2FuZHJvaWQvcmVwb3NpdG9yeS9wbGF0Zm9ybS10b29scy1sYXRlc3QtbGludXguemlwIgogICAgICAgICAgICBldGFnX2ZpbGUgPSAiLi9wbGF0Zm9ybS10b29scy8uZXRhZyIKICAgICAgICAgICAgc2F2ZWRfZXRhZyA9ICIiCiAgICAgICAgICAgIGlmIG9zLnBhdGguZXhpc3RzKGV0YWdfZmlsZSk6CiAgICAgICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICAgICAgd2l0aCBvcGVuKGV0YWdfZmlsZSwgInIiLCBlbmNvZGluZz0idXRmLTgiKSBhcyBmOgogICAgICAgICAgICAgICAgICAgICAgICBzYXZlZF9ldGFnID0gZi5yZWFkKCkuc3RyaXAoKQogICAgICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgICAgICBwYXNzCiAgICAgICAgICAgIHJlcSA9IHVybGxpYi5yZXF1ZXN0LlJlcXVlc3QodXJsLCBtZXRob2Q9IkhFQUQiKQogICAgICAgICAgICB3aXRoIHVybGxpYi5yZXF1ZXN0LnVybG9wZW4ocmVxLCB0aW1lb3V0PTQpIGFzIHJlc3A6CiAgICAgICAgICAgICAgICByZW1vdGVfZXRhZyA9IHJlc3AuaGVhZGVycy5nZXQoIkVUYWciKSBvciByZXNwLmhlYWRlcnMuZ2V0KCJMYXN0LU1vZGlmaWVkIikKICAgICAgICAgICAgaWYgcmVtb3RlX2V0YWcgYW5kIHNhdmVkX2V0YWcgYW5kIHJlbW90ZV9ldGFnID09IHNhdmVkX2V0YWc6CiAgICAgICAgICAgICAgICBsb2coIltBREJdIExvY2FsIHBsYXRmb3JtLXRvb2xzIGlzIGFscmVhZHkgdXAgdG8gZGF0ZS4iKQogICAgICAgICAgICBlbGlmIHJlbW90ZV9ldGFnOgogICAgICAgICAgICAgICAgbG9nKCJbQURCXSBOZXcgdmVyc2lvbiBhdmFpbGFibGUuIFVwZGF0aW5nIEFEQiBwbGF0Zm9ybS10b29scy4uLiIpCiAgICAgICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbYWRiX2NtZCwgImtpbGwtc2VydmVyIl0sIGNoZWNrPUZhbHNlLCBzdGRvdXQ9c3VicHJvY2Vzcy5ERVZOVUxMLCBzdGRlcnI9c3VicHJvY2Vzcy5ERVZOVUxMKQogICAgICAgICAgICAgICAgemlwX3BhdGggPSAiLi9wbGF0Zm9ybS10b29scy11cGRhdGUuemlwIgogICAgICAgICAgICAgICAgdXJsbGliLnJlcXVlc3QudXJscmV0cmlldmUodXJsLCB6aXBfcGF0aCkKICAgICAgICAgICAgICAgIHdpdGggemlwZmlsZS5aaXBGaWxlKHppcF9wYXRoLCAiciIpIGFzIHppcF9yZWY6CiAgICAgICAgICAgICAgICAgICAgemlwX3JlZi5leHRyYWN0YWxsKCIuIikKICAgICAgICAgICAgICAgIGlmIG9zLnBhdGguZXhpc3RzKHppcF9wYXRoKToKICAgICAgICAgICAgICAgICAgICBvcy5yZW1vdmUoemlwX3BhdGgpCiAgICAgICAgICAgICAgICBvcy5jaG1vZCgiLi9wbGF0Zm9ybS10b29scy9hZGIiLCAwbzc1NSkKICAgICAgICAgICAgICAgIHdpdGggb3BlbihldGFnX2ZpbGUsICJ3IiwgZW5jb2Rpbmc9InV0Zi04IikgYXMgZjoKICAgICAgICAgICAgICAgICAgICBmLndyaXRlKHJlbW90ZV9ldGFnKQogICAgICAgICAgICAgICAgbG9nKCJbQURCXSBQbGF0Zm9ybS10b29scyB1cGRhdGVkIHN1Y2Nlc3NmdWxseSEiKQogICAgICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICBlbHNlOgogICAgICAgICAgICBsb2coIltBREJdIEFEQiBpcyBhY3RpdmUgYW5kIHJlYWR5LiIpCiAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgIGxvZygiW0FEQl0gVXBkYXRlIGNoZWNrIGZpbmlzaGVkLiIpCgogICAgIyAyLiBLZXkgc2ltdWxhdGlvbiBsaWJyYXJ5IChweW5wdXQpCiAgICB0cnk6CiAgICAgICAgZnJvbSBweW5wdXQua2V5Ym9hcmQgaW1wb3J0IEtleSwgQ29udHJvbGxlcgogICAgZXhjZXB0IEltcG9ydEVycm9yOgogICAgICAgIGxvZygicHlucHV0IGxpYnJhcnkgbm90IGZvdW5kLiBJbnN0YWxsaW5nIHN5c3RlbSBwYWNrYWdlLi4uIikKICAgICAgICBpbnN0YWxsZWQgPSBlbnN1cmVfc3lzdGVtX3BhY2thZ2UoCiAgICAgICAgICAgIHBrZ19uYW1lX2FwdD0icHl0aG9uMy1weW5wdXQiLAogICAgICAgICAgICBwa2dfbmFtZV9kbmY9InB5dGhvbjMtcHlucHV0IiwKICAgICAgICAgICAgcGtnX25hbWVfcGFjbWFuPSJweXRob24tcHlucHV0IiwKICAgICAgICAgICAgcGtnX25hbWVfenlwcGVyPSJweXRob24zLXB5bnB1dCIKICAgICAgICApCiAgICAgICAgaWYgbm90IGluc3RhbGxlZDoKICAgICAgICAgICAgbG9nKCJUcnlpbmcgcGlwIGluc3RhbGwgd2l0aCAtLXVzZXIgZmxhZy4uLiIpCiAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFtzeXMuZXhlY3V0YWJsZSwgIi1tIiwgInBpcCIsICJpbnN0YWxsIiwgIi0tdXNlciIsICJweW5wdXQiXSwgY2hlY2s9VHJ1ZSkKICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICAgICAgbG9nKGYicGlwIGluc3RhbGwgZmFpbGVkOiB7ZX0iKQogICAgICAgICAgICAgICAgbG9nKCJQbGVhc2UgcnVuOiBzdWRvIGFwdCBpbnN0YWxsIHB5dGhvbjMtcHlucHV0IChvciBlcXVpdmFsZW50IGZvciB5b3VyIExpbnV4IGRpc3RyaWJ1dGlvbikiKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgdHJ5OgogICAgICAgICAgICBmcm9tIHB5bnB1dC5rZXlib2FyZCBpbXBvcnQgS2V5LCBDb250cm9sbGVyCiAgICAgICAgZXhjZXB0IEltcG9ydEVycm9yOgogICAgICAgICAgICBsb2coIkNvdWxkIG5vdCBsb2FkIHB5bnB1dC4gUGxlYXNlIHJlc3RhcnQgdGhlIHRlcm1pbmFsIG9yIGluc3RhbGwgcHl0aG9uMy1weW5wdXQuIikKICAgICAgICAgICAgcmV0dXJuCgogICAga2V5Ym9hcmQgPSBDb250cm9sbGVyKCkKCiAgICBkZWYgc2VuZF9kb3duKGtleV9jaGFyKToKICAgICAgICB0cnk6CiAgICAgICAgICAgIGtleWJvYXJkLnByZXNzKGtleV9jaGFyKQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiS2V5IHByZXNzIGVycm9yOiB7ZX0iKQoKICAgIGRlZiBzZW5kX3VwKGtleV9jaGFyKToKICAgICAgICB0cnk6CiAgICAgICAgICAgIGtleWJvYXJkLnJlbGVhc2Uoa2V5X2NoYXIpCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICBsb2coZiJLZXkgcmVsZWFzZSBlcnJvcjoge2V9IikKCiAgICBkZWYgbG9nX2JpKGVuLCBqYT1Ob25lKToKICAgICAgICBsb2coZW4pCiAgICAgICAgaWYgamE6CiAgICAgICAgICAgIGxvZyhmIiAgLT4ge2phfSIpCgogICAgZGVmIHJlc2V0X2FkYl9zZXJ2ZXIocmVhc29uPSIiKToKICAgICAgICBpZiByZWFzb246CiAgICAgICAgICAgIGxvZ19iaShmIltBREJdIFJlc2V0dGluZyBBREIgc2VydmVyOiB7cmVhc29ufSAoYWRiIGtpbGwtc2VydmVyKS4uLiIsIGYiQURC44K144O844OQ44O844KS5YaN6LW35YuV5LitOiB7cmVhc29ufSAoYWRiIGtpbGwtc2VydmVyKS4uLiIpCiAgICAgICAgdHJ5OgogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbYWRiX2NtZCwgImtpbGwtc2VydmVyIl0sIHN0ZG91dD1zdWJwcm9jZXNzLkRFVk5VTEwsIHN0ZGVycj1zdWJwcm9jZXNzLkRFVk5VTEwpCiAgICAgICAgICAgIHRpbWUuc2xlZXAoMC40KQogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbYWRiX2NtZCwgInN0YXJ0LXNlcnZlciJdLCBzdGRvdXQ9c3VicHJvY2Vzcy5ERVZOVUxMLCBzdGRlcnI9c3VicHJvY2Vzcy5ERVZOVUxMKQogICAgICAgICAgICB0aW1lLnNsZWVwKDAuNCkKICAgICAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgICAgICBwYXNzCgogICAgbG9nX2JpKCJbQURCXSBJbml0aWFsaXppbmcgY2xlYW4gQURCIHNlcnZlciBzdGF0ZSAoYWRiIGtpbGwtc2VydmVyKS4uLiIsICLjgq/jg6rjg7zjg7PjgapBRELjgrXjg7zjg5Djg7zjgpLliJ3mnJ/ljJbkuK0gKGFkYiBraWxsLXNlcnZlcikuLi4iKQogICAgcmVzZXRfYWRiX3NlcnZlcigiU3RhcnR1cCBpbml0aWFsaXphdGlvbiIpCgogICAgY29uc2VjdXRpdmVfd2FpdHMgPSAwCgogICAgZGVmIGVuc3VyZV9hZGJfZm9yd2FyZCgpOgogICAgICAgIG5vbmxvY2FsIGNvbnNlY3V0aXZlX3dhaXRzCiAgICAgICAgdHJ5OgogICAgICAgICAgICAjIENoZWNrIGRldmljZXMKICAgICAgICAgICAgcmVzID0gc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICJkZXZpY2VzIiwgIi1sIl0sIGNhcHR1cmVfb3V0cHV0PVRydWUsIHRleHQ9VHJ1ZSkKICAgICAgICAgICAgbGluZXMgPSByZXMuc3Rkb3V0LnN0cmlwKCkuc3BsaXRsaW5lcygpCiAgICAgICAgICAgIG9ubGluZV9zZXJpYWxzID0gW10KICAgICAgICAgICAgdW5hdXRob3JpemVkID0gRmFsc2UKICAgICAgICAgICAgb2ZmbGluZSA9IEZhbHNlCiAgICAgICAgICAgIGZvciBsaW5lIGluIGxpbmVzWzE6XToKICAgICAgICAgICAgICAgIGxpbmUgPSBsaW5lLnN0cmlwKCkKICAgICAgICAgICAgICAgIGlmIG5vdCBsaW5lOgogICAgICAgICAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgICAgICAgICBpZiAidW5hdXRob3JpemVkIiBpbiBsaW5lOgogICAgICAgICAgICAgICAgICAgIHVuYXV0aG9yaXplZCA9IFRydWUKICAgICAgICAgICAgICAgIGVsaWYgIm9mZmxpbmUiIGluIGxpbmU6CiAgICAgICAgICAgICAgICAgICAgb2ZmbGluZSA9IFRydWUKICAgICAgICAgICAgICAgIGVsaWYgImRldmljZSIgaW4gbGluZToKICAgICAgICAgICAgICAgICAgICBwYXJ0cyA9IGxpbmUuc3BsaXQoKQogICAgICAgICAgICAgICAgICAgIGlmIGxlbihwYXJ0cykgPiAwOgogICAgICAgICAgICAgICAgICAgICAgICBvbmxpbmVfc2VyaWFscy5hcHBlbmQocGFydHNbMF0pCgogICAgICAgICAgICBpZiBvZmZsaW5lIGFuZCBub3Qgb25saW5lX3NlcmlhbHM6CiAgICAgICAgICAgICAgICBsb2dfYmkoIltBREJdIERldmljZSBpbiBvZmZsaW5lIHN0YXRlLiBSZXNldHRpbmcgQURCIHNlcnZlci4uLiIsICLjgqrjg5Xjg6njgqTjg7Pnq6/mnKvjgpLmpJzlh7rjgIJBRELjgrXjg7zjg5Djg7zjgpLjg6rjg5Xjg6zjg4Pjgrfjg6XkuK0uLi4iKQogICAgICAgICAgICAgICAgcmVzZXRfYWRiX3NlcnZlcigiT2ZmbGluZSBkZXZpY2UgcmVjb3ZlcnkiKQogICAgICAgICAgICAgICAgcmV0dXJuIEZhbHNlCgogICAgICAgICAgICBpZiBub3Qgb25saW5lX3NlcmlhbHM6CiAgICAgICAgICAgICAgICBjb25zZWN1dGl2ZV93YWl0cyArPSAxCiAgICAgICAgICAgICAgICBpZiBjb25zZWN1dGl2ZV93YWl0cyA+PSAzOgogICAgICAgICAgICAgICAgICAgIGxvZ19iaSgiW0FEQl0gRGV2aWNlIG5vdCBkZXRlY3RlZCBhZnRlciByZWNvbm5lY3QuIFJlc2V0dGluZyBBREIgKGFkYiBraWxsLXNlcnZlcikuLi4iLCAi56uv5pyr44GM6KqN6K2Y44GV44KM44Gq44GE44GL5YiH5pat44GV44KM44G+44GX44Gf44CCYWRiIGtpbGwtc2VydmVyIOOCkuWun+ihjOOBl+OBpuWGjeippuihjOS4rS4uLiIpCiAgICAgICAgICAgICAgICAgICAgcmVzZXRfYWRiX3NlcnZlcigiUmVjb25uZWN0IHJldHJ5IikKICAgICAgICAgICAgICAgICAgICBjb25zZWN1dGl2ZV93YWl0cyA9IDAKICAgICAgICAgICAgICAgICAgICByZXR1cm4gRmFsc2UKCiAgICAgICAgICAgICAgICBpZiB1bmF1dGhvcml6ZWQ6CiAgICAgICAgICAgICAgICAgICAgbG9nX2JpKCJbV0FJVF0gQW5kcm9pZCBkZXZpY2UgZGV0ZWN0ZWQsIGJ1dCB1bmF1dGhvcml6ZWQuIiwgIkFuZHJvaWTnq6/mnKvjgYzmpJzlh7rjgZXjgozjgb7jgZfjgZ/jgYzjgIHmnKroqLHlj6/jgafjgZnjgILnlLvpnaLjg63jg4Pjgq/jgpLop6PpmaTjgZfjgabjgIxVU0Ljg4fjg5Djg4PjgrDjgpLoqLHlj6/jgI3jgpLjgr/jg4Pjg5fjgZfjgabjgY/jgaDjgZXjgYTjgIIiKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBsb2dfYmkoIltXQUlUXSBObyBBbmRyb2lkIGRldmljZSBkZXRlY3RlZC4iLCAiQW5kcm9pZOerr+acq+OBjOimi+OBpOOBi+OCiuOBvuOBm+OCk+OAglVTQuOCseODvOODluODq+aOpee2muOBqOOAjFVTQuODh+ODkOODg+OCsOOAjeOBruacieWKueWMluOCkueiuuiqjeOBl+OBpuOBj+OBoOOBleOBhOOAgiIpCiAgICAgICAgICAgICAgICByZXR1cm4gRmFsc2UKCiAgICAgICAgICAgIGNvbnNlY3V0aXZlX3dhaXRzID0gMAogICAgICAgICAgICBjaG9zZW5fc2VyaWFsID0gb25saW5lX3NlcmlhbHNbMF0KICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICItcyIsIGNob3Nlbl9zZXJpYWwsICJmb3J3YXJkIiwgIi0tcmVtb3ZlIiwgZiJ0Y3A6e1BPUlR9Il0sIHN0ZG91dD1zdWJwcm9jZXNzLkRFVk5VTEwsIHN0ZGVycj1zdWJwcm9jZXNzLkRFVk5VTEwpCiAgICAgICAgICAgIGZ3ZCA9IHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAiLXMiLCBjaG9zZW5fc2VyaWFsLCAiZm9yd2FyZCIsIGYidGNwOntQT1JUfSIsIGYidGNwOntQT1JUfSJdLCBjYXB0dXJlX291dHB1dD1UcnVlLCB0ZXh0PVRydWUpCiAgICAgICAgICAgIHJldHVybiBmd2QucmV0dXJuY29kZSA9PSAwCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgcmV0dXJuIEZhbHNlCgogICAgbG9nX2JpKGYiUmVhZHkuIFN0YXJ0aW5nIGF1dG8tY29ubmVjdGlvbiBsb29wICh0YXJnZXQgcG9ydDoge1BPUlR9KS4uLiIsIGYi5rqW5YKZ5a6M5LqG44CC6Ieq5YuV5o6l57aa44Or44O844OX44KS6ZaL5aeL44GX44G+44GZICjjg53jg7zjg4g6IHtQT1JUfSkuLi4iKQogICAgbGFzdF9md2Rfb2sgPSBGYWxzZQoKICAgIHdoaWxlIFRydWU6CiAgICAgICAgdHJ5OgogICAgICAgICAgICBpZiBub3QgZW5zdXJlX2FkYl9mb3J3YXJkKCk6CiAgICAgICAgICAgICAgICBsYXN0X2Z3ZF9vayA9IEZhbHNlCiAgICAgICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgICAgICAgICBjb250aW51ZQoKICAgICAgICAgICAgaWYgbm90IGxhc3RfZndkX29rOgogICAgICAgICAgICAgICAgbG9nX2JpKGYiW09LXSBBREIgcG9ydCBmb3J3YXJkaW5nIGFjdGl2ZSAocG9ydCB7UE9SVH0pLiIsIGYiQURC44Od44O844OI44OV44Kp44Ov44O844OJ56K656uLICjjg53jg7zjg4gge1BPUlR9KeOAguOCueODnuODm+OCouODl+ODquOBp+OAjFBD5o6l57aaIChVU0Ip44CN44Oi44O844OJ44KS6ZaL44GE44Gm44GP44Gg44GV44GE44CCIikKICAgICAgICAgICAgICAgIGxhc3RfZndkX29rID0gVHJ1ZQoKICAgICAgICAgICAgcyA9IHNvY2tldC5zb2NrZXQoc29ja2V0LkFGX0lORVQsIHNvY2tldC5TT0NLX1NUUkVBTSkKICAgICAgICAgICAgcy5zZXR0aW1lb3V0KDIuMCkKICAgICAgICAgICAgcy5jb25uZWN0KCgiMTI3LjAuMC4xIiwgUE9SVCkpCiAgICAgICAgICAgIAogICAgICAgICAgICBmID0gcy5tYWtlZmlsZSgiciIsIGVuY29kaW5nPSJ1dGYtOCIpCiAgICAgICAgICAgIGJhbm5lciA9IGYucmVhZGxpbmUoKQogICAgICAgICAgICBpZiBub3QgYmFubmVyOgogICAgICAgICAgICAgICAgcy5jbG9zZSgpCiAgICAgICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgICAgICAgICBjb250aW51ZQoKICAgICAgICAgICAgcy5zZXR0aW1lb3V0KE5vbmUpCiAgICAgICAgICAgIGxvZygiPT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PSIpCiAgICAgICAgICAgIGxvZ19iaSgiICoqKiBUYWlrbyBDb250cm9sbGVyIENvbm5lY3RlZCBTdWNjZXNzZnVsbHkhICoqKiIsICLimIXimIXimIUg5aSq6byT44Kz44Oz44OI44Ot44O844Op44O8ICjjgqLjg5fjg6opIOOBqOaOpee2muWujOS6hu+8gSDimIXimIXimIUiKQogICAgICAgICAgICBsb2dfYmkoIiBTZW5kaW5nIGtleXMgKEQgLyBGIC8gSiAvIEspIHRvIFBDIGdhbWVzIGluIHJlYWwtdGltZS4iLCAiUEPjgrLjg7zjg6Djgbjjgq3jg7zjgpLjg6rjgqLjg6vjgr/jgqTjg6DpgIHkv6HjgZfjgb7jgZnjgIIiKQogICAgICAgICAgICBsb2coIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT0iKQogICAgICAgICAgICAKICAgICAgICAgICAgZm9yIGxpbmUgaW4gZjoKICAgICAgICAgICAgICAgIGxpbmUgPSBsaW5lLnN0cmlwKCkKICAgICAgICAgICAgICAgIGlmIG5vdCBsaW5lIG9yIGxpbmUgPT0gIlBJTkciOgogICAgICAgICAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgICAgICAgICBwYXJ0cyA9IGxpbmUuc3BsaXQoIiAiKQogICAgICAgICAgICAgICAgaWYgbGVuKHBhcnRzKSA+PSAyOgogICAgICAgICAgICAgICAgICAgIGFjdGlvbiA9IHBhcnRzWzBdCiAgICAgICAgICAgICAgICAgICAgZm9yIGtleV9zdHIgaW4gcGFydHNbMTpdOgogICAgICAgICAgICAgICAgICAgICAgICBrZXlfY2hhciA9IGtleV9zdHIubG93ZXIoKQogICAgICAgICAgICAgICAgICAgICAgICBpZiBub3Qga2V5X2NoYXI6CiAgICAgICAgICAgICAgICAgICAgICAgICAgICBjb250aW51ZQogICAgICAgICAgICAgICAgICAgICAgICBsb2coZiJbS0VZXSB7YWN0aW9ufSAtPiB7a2V5X2NoYXJ9IikKICAgICAgICAgICAgICAgICAgICAgICAgaWYgYWN0aW9uID09ICJET1dOIjoKICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlbmRfZG93bihrZXlfY2hhcikKICAgICAgICAgICAgICAgICAgICAgICAgZWxpZiBhY3Rpb24gPT0gIlVQIjoKICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlbmRfdXAoa2V5X2NoYXIpCgogICAgICAgICAgICBsb2dfYmkoIltJTkZPXSBDb25uZWN0aW9uIGNsb3NlZCBieSBBbmRyb2lkIGFwcC4gV2FpdGluZyB0byByZWNvbm5lY3QuLi4iLCAi44Ki44OX44Oq44Go44Gu5o6l57aa44GM5YiH5pat44GV44KM44G+44GX44Gf44CC5YaN5o6l57aa5b6F5qmf5LitLi4uIikKICAgICAgICAgICAgcy5jbG9zZSgpCiAgICAgICAgICAgIHJlc2V0X2FkYl9zZXJ2ZXIoIlJlY29ubmVjdGlvbiBjbGVhbnVwIikKICAgICAgICAgICAgdGltZS5zbGVlcCgyKQogICAgICAgIGV4Y2VwdCAoc29ja2V0LnRpbWVvdXQsIENvbm5lY3Rpb25SZWZ1c2VkRXJyb3IsIE9TRXJyb3IpOgogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgZXhjZXB0IEtleWJvYXJkSW50ZXJydXB0OgogICAgICAgICAgICBsb2coIkV4aXRpbmcuLi4iKQogICAgICAgICAgICBicmVhawogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgdGltZS5zbGVlcCgyKQoKaWYgX19uYW1lX18gPT0gIl9fbWFpbl9fIjoKICAgIHRyeToKICAgICAgICBtYWluKCkKICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZXJyOgogICAgICAgIHByaW50KGYiRmF0YWwgRXJyb3I6IHtlcnJ9IiwgZmx1c2g9VHJ1ZSkKICAgICAgICB0cmFjZWJhY2sucHJpbnRfZXhjKCkKICAgICAgICBpbnB1dCgiUHJlc3MgRW50ZXIgdG8gZXhpdC4uLiIpCg==", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)
            val macOSScript = String(android.util.Base64.decode("IyEvdXNyL2Jpbi9lbnYgcHl0aG9uMwojIG1hY09TIFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIG5hbWU6IFRUQy1yZWNlaXZlci1tYWNvcy5jb21tYW5kCiMgVXNhZ2U6IGNobW9kICt4IFRUQy1yZWNlaXZlci1tYWNvcy5jb21tYW5kICYmIC4vVFRDLXJlY2VpdmVyLW1hY29zLmNvbW1hbmQKCmltcG9ydCBzb2NrZXQKaW1wb3J0IHN1YnByb2Nlc3MKaW1wb3J0IHRpbWUKaW1wb3J0IHN5cwppbXBvcnQgb3MKaW1wb3J0IHVybGxpYi5yZXF1ZXN0CmltcG9ydCB6aXBmaWxlCmltcG9ydCB0cmFjZWJhY2sKClBPUlQgPSA2MDAwMQoKZGVmIGxvZyhtc2cpOgogICAgcHJpbnQobXNnLCBmbHVzaD1UcnVlKQoKZGVmIG1haW4oKToKICAgIGxvZygiPT09IFRhaWtvIENvbnRyb2xsZXIgUmVjZWl2ZXIgZm9yIG1hY09TID09PSIpCiAgICAKICAgIGFkYl9jbWQgPSAiYWRiIgoKICAgICMgVmVyaWZ5IGFuZCBjaGVjayBBREIgdXBkYXRlcwogICAgdHJ5OgogICAgICAgIHN1YnByb2Nlc3MucnVuKFsiYWRiIiwgInZlcnNpb24iXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgIGV4Y2VwdCBGaWxlTm90Rm91bmRFcnJvcjoKICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cygiLi9wbGF0Zm9ybS10b29scy9hZGIiKToKICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICBlbHNlOgogICAgICAgICAgICBsb2coIkFEQiBub3QgZm91bmQgaW4gUEFUSC4gQ2hlY2tpbmcgSG9tZWJyZXcgb3Igc3RhbmRhbG9uZSB0b29scy4uLiIpCiAgICAgICAgICAgIGluc3RhbGxlZCA9IEZhbHNlCiAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgIGxvZygiVHJ5aW5nIHRvIGluc3RhbGwgYW5kcm9pZC1wbGF0Zm9ybS10b29scyB2aWEgSG9tZWJyZXcuLi4iKQogICAgICAgICAgICAgICAgcmVzID0gc3VicHJvY2Vzcy5ydW4oWyJicmV3IiwgImluc3RhbGwiLCAiYW5kcm9pZC1wbGF0Zm9ybS10b29scyJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgIGlmIHJlcy5yZXR1cm5jb2RlID09IDA6CiAgICAgICAgICAgICAgICAgICAgaW5zdGFsbGVkID0gVHJ1ZQogICAgICAgICAgICAgICAgICAgIGFkYl9jbWQgPSAiYWRiIgogICAgICAgICAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgICAgICAgICAgcGFzcwogICAgICAgICAgICAKICAgICAgICAgICAgaWYgbm90IGluc3RhbGxlZDoKICAgICAgICAgICAgICAgIGxvZygiRG93bmxvYWRpbmcgb2ZmaWNpYWwgQW5kcm9pZCBTREsgUGxhdGZvcm0gVG9vbHMuLi4iKQogICAgICAgICAgICAgICAgdXJsID0gImh0dHBzOi8vZGwuZ29vZ2xlLmNvbS9hbmRyb2lkL3JlcG9zaXRvcnkvcGxhdGZvcm0tdG9vbHMtbGF0ZXN0LWRhcndpbi56aXAiCiAgICAgICAgICAgICAgICB6aXBfcGF0aCA9ICIuL3BsYXRmb3JtLXRvb2xzLnppcCIKICAgICAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgICAgICB1cmxsaWIucmVxdWVzdC51cmxyZXRyaWV2ZSh1cmwsIHppcF9wYXRoKQogICAgICAgICAgICAgICAgICAgIHdpdGggemlwZmlsZS5aaXBGaWxlKHppcF9wYXRoLCAiciIpIGFzIHppcF9yZWY6CiAgICAgICAgICAgICAgICAgICAgICAgIHppcF9yZWYuZXh0cmFjdGFsbCgiLiIpCiAgICAgICAgICAgICAgICAgICAgaWYgb3MucGF0aC5leGlzdHMoemlwX3BhdGgpOgogICAgICAgICAgICAgICAgICAgICAgICBvcy5yZW1vdmUoemlwX3BhdGgpCiAgICAgICAgICAgICAgICAgICAgb3MuY2htb2QoIi4vcGxhdGZvcm0tdG9vbHMvYWRiIiwgMG83NTUpCiAgICAgICAgICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICAgICAgICAgICAgICBsb2coIkFEQiBkb3dubG9hZGVkIGFuZCBleHRyYWN0ZWQgc3VjY2Vzc2Z1bGx5IHRvIC4vcGxhdGZvcm0tdG9vbHMvIikKICAgICAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgICAgICAgICBsb2coZiJFcnJvciBkb3dubG9hZGluZyBwbGF0Zm9ybS10b29sczoge2V9IikKICAgICAgICAgICAgICAgICAgICBsb2coIlBsZWFzZSBpbnN0YWxsIEFEQiBtYW51YWxseS4iKQogICAgICAgICAgICAgICAgICAgIHJldHVybgoKICAgICMgQ2hlY2sgZm9yIEFEQiB1cGRhdGVzCiAgICB0cnk6CiAgICAgICAgbG9nKCJbQURCXSBDaGVja2luZyBmb3IgQURCIHVwZGF0ZXMuLi4iKQogICAgICAgIGlmIG9zLnBhdGguZXhpc3RzKCIuL3BsYXRmb3JtLXRvb2xzL2FkYiIpOgogICAgICAgICAgICB1cmwgPSAiaHR0cHM6Ly9kbC5nb29nbGUuY29tL2FuZHJvaWQvcmVwb3NpdG9yeS9wbGF0Zm9ybS10b29scy1sYXRlc3QtZGFyd2luLnppcCIKICAgICAgICAgICAgZXRhZ19maWxlID0gIi4vcGxhdGZvcm0tdG9vbHMvLmV0YWciCiAgICAgICAgICAgIHNhdmVkX2V0YWcgPSAiIgogICAgICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cyhldGFnX2ZpbGUpOgogICAgICAgICAgICAgICAgdHJ5OgogICAgICAgICAgICAgICAgICAgIHdpdGggb3BlbihldGFnX2ZpbGUsICJyIiwgZW5jb2Rpbmc9InV0Zi04IikgYXMgZjoKICAgICAgICAgICAgICAgICAgICAgICAgc2F2ZWRfZXRhZyA9IGYucmVhZCgpLnN0cmlwKCkKICAgICAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgICAgICAgICAgcGFzcwogICAgICAgICAgICByZXEgPSB1cmxsaWIucmVxdWVzdC5SZXF1ZXN0KHVybCwgbWV0aG9kPSJIRUFEIikKICAgICAgICAgICAgd2l0aCB1cmxsaWIucmVxdWVzdC51cmxvcGVuKHJlcSwgdGltZW91dD00KSBhcyByZXNwOgogICAgICAgICAgICAgICAgcmVtb3RlX2V0YWcgPSByZXNwLmhlYWRlcnMuZ2V0KCJFVGFnIikgb3IgcmVzcC5oZWFkZXJzLmdldCgiTGFzdC1Nb2RpZmllZCIpCiAgICAgICAgICAgIGlmIHJlbW90ZV9ldGFnIGFuZCBzYXZlZF9ldGFnIGFuZCByZW1vdGVfZXRhZyA9PSBzYXZlZF9ldGFnOgogICAgICAgICAgICAgICAgbG9nKCJbQURCXSBMb2NhbCBwbGF0Zm9ybS10b29scyBpcyBhbHJlYWR5IHVwIHRvIGRhdGUuIikKICAgICAgICAgICAgZWxpZiByZW1vdGVfZXRhZzoKICAgICAgICAgICAgICAgIGxvZygiW0FEQl0gTmV3IHZlcnNpb24gYXZhaWxhYmxlLiBVcGRhdGluZyBBREIgcGxhdGZvcm0tdG9vbHMuLi4iKQogICAgICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICJraWxsLXNlcnZlciJdLCBjaGVjaz1GYWxzZSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgICAgICAgICAgICAgIHppcF9wYXRoID0gIi4vcGxhdGZvcm0tdG9vbHMtdXBkYXRlLnppcCIKICAgICAgICAgICAgICAgIHVybGxpYi5yZXF1ZXN0LnVybHJldHJpZXZlKHVybCwgemlwX3BhdGgpCiAgICAgICAgICAgICAgICB3aXRoIHppcGZpbGUuWmlwRmlsZSh6aXBfcGF0aCwgInIiKSBhcyB6aXBfcmVmOgogICAgICAgICAgICAgICAgICAgIHppcF9yZWYuZXh0cmFjdGFsbCgiLiIpCiAgICAgICAgICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cyh6aXBfcGF0aCk6CiAgICAgICAgICAgICAgICAgICAgb3MucmVtb3ZlKHppcF9wYXRoKQogICAgICAgICAgICAgICAgb3MuY2htb2QoIi4vcGxhdGZvcm0tdG9vbHMvYWRiIiwgMG83NTUpCiAgICAgICAgICAgICAgICB3aXRoIG9wZW4oZXRhZ19maWxlLCAidyIsIGVuY29kaW5nPSJ1dGYtOCIpIGFzIGY6CiAgICAgICAgICAgICAgICAgICAgZi53cml0ZShyZW1vdGVfZXRhZykKICAgICAgICAgICAgICAgIGxvZygiW0FEQl0gUGxhdGZvcm0tdG9vbHMgdXBkYXRlZCBzdWNjZXNzZnVsbHkhIikKICAgICAgICAgICAgICAgIGFkYl9jbWQgPSAiLi9wbGF0Zm9ybS10b29scy9hZGIiCiAgICAgICAgZWxzZToKICAgICAgICAgICAgdHJ5OgogICAgICAgICAgICAgICAgIyBJZiBpbnN0YWxsZWQgdmlhIEhvbWVicmV3LCBjaGVjayBvdXRkYXRlZAogICAgICAgICAgICAgICAgb3V0ID0gc3VicHJvY2Vzcy5ydW4oWyJicmV3IiwgIm91dGRhdGVkIiwgImFuZHJvaWQtcGxhdGZvcm0tdG9vbHMiXSwgY2FwdHVyZV9vdXRwdXQ9VHJ1ZSwgdGV4dD1UcnVlLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgIGlmIG91dC5yZXR1cm5jb2RlID09IDAgYW5kICJhbmRyb2lkLXBsYXRmb3JtLXRvb2xzIiBpbiBvdXQuc3Rkb3V0OgogICAgICAgICAgICAgICAgICAgIGxvZygiW0FEQl0gVXBkYXRpbmcgYW5kcm9pZC1wbGF0Zm9ybS10b29scyB2aWEgSG9tZWJyZXcuLi4iKQogICAgICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAia2lsbC1zZXJ2ZXIiXSwgY2hlY2s9RmFsc2UsIHN0ZG91dD1zdWJwcm9jZXNzLkRFVk5VTEwsIHN0ZGVycj1zdWJwcm9jZXNzLkRFVk5VTEwpCiAgICAgICAgICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oWyJicmV3IiwgInVwZ3JhZGUiLCAiYW5kcm9pZC1wbGF0Zm9ybS10b29scyJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgICAgICBsb2coIltBREJdIFVwZGF0ZWQgdmlhIEhvbWVicmV3ISIpCiAgICAgICAgICAgICAgICBlbHNlOgogICAgICAgICAgICAgICAgICAgIGxvZygiW0FEQl0gQURCIGlzIHVwIHRvIGRhdGUuIikKICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgIGxvZygiW0FEQl0gQURCIGlzIGFjdGl2ZSBhbmQgcmVhZHkuIikKICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgbG9nKCJbQURCXSBVcGRhdGUgY2hlY2sgZmluaXNoZWQuIikKCiAgICAjIEtleSBzaW11bGF0aW9uIGxpYnJhcmllcwogICAgdHJ5OgogICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgIGV4Y2VwdCBJbXBvcnRFcnJvcjoKICAgICAgICBsb2coIkluc3RhbGxpbmcgcHlucHV0IGxpYnJhcnkgZm9yIGtleWJvYXJkIHNpbXVsYXRpb24uLi4iKQogICAgICAgIGluc3RhbGxlZCA9IEZhbHNlCiAgICAgICAgdHJ5OgogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbc3lzLmV4ZWN1dGFibGUsICItbSIsICJwaXAiLCAiaW5zdGFsbCIsICItLXVzZXIiLCAicHlucHV0Il0sIGNoZWNrPVRydWUpCiAgICAgICAgICAgIGluc3RhbGxlZCA9IFRydWUKICAgICAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbc3lzLmV4ZWN1dGFibGUsICItbSIsICJwaXAiLCAiaW5zdGFsbCIsICJweW5wdXQiXSwgY2hlY2s9VHJ1ZSkKICAgICAgICAgICAgICAgIGluc3RhbGxlZCA9IFRydWUKICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgIHBhc3MKICAgICAgICB0cnk6CiAgICAgICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgICAgICBleGNlcHQgSW1wb3J0RXJyb3I6CiAgICAgICAgICAgIGxvZygiQ291bGQgbm90IGxvYWQgcHlucHV0LiBQbGVhc2UgcnVuOiBwaXAzIGluc3RhbGwgLS11c2VyIHB5bnB1dCIpCiAgICAgICAgICAgIHJldHVybgoKICAgIGtleWJvYXJkID0gQ29udHJvbGxlcigpCgogICAgZGVmIHNlbmRfZG93bihrZXlfY2hhcik6CiAgICAgICAgdHJ5OgogICAgICAgICAgICBrZXlib2FyZC5wcmVzcyhrZXlfY2hhcikKICAgICAgICBleGNlcHQgRXhjZXB0aW9uIGFzIGU6CiAgICAgICAgICAgIGxvZyhmIktleSBwcmVzcyBlcnJvcjoge2V9IikKCiAgICBkZWYgc2VuZF91cChrZXlfY2hhcik6CiAgICAgICAgdHJ5OgogICAgICAgICAgICBrZXlib2FyZC5yZWxlYXNlKGtleV9jaGFyKQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiS2V5IHJlbGVhc2UgZXJyb3I6IHtlfSIpCgogICAgZGVmIGxvZ19iaShlbiwgamE9Tm9uZSk6CiAgICAgICAgbG9nKGVuKQogICAgICAgIGlmIGphOgogICAgICAgICAgICBsb2coZiIgIC0+IHtqYX0iKQoKICAgIGRlZiByZXNldF9hZGJfc2VydmVyKHJlYXNvbj0iIik6CiAgICAgICAgaWYgcmVhc29uOgogICAgICAgICAgICBsb2dfYmkoZiJbQURCXSBSZXNldHRpbmcgQURCIHNlcnZlcjoge3JlYXNvbn0gKGFkYiBraWxsLXNlcnZlcikuLi4iLCBmIkFEQuOCteODvOODkOODvOOCkuWGjei1t+WLleS4rToge3JlYXNvbn0gKGFkYiBraWxsLXNlcnZlcikuLi4iKQogICAgICAgIHRyeToKICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICJraWxsLXNlcnZlciJdLCBzdGRvdXQ9c3VicHJvY2Vzcy5ERVZOVUxMLCBzdGRlcnI9c3VicHJvY2Vzcy5ERVZOVUxMKQogICAgICAgICAgICB0aW1lLnNsZWVwKDAuNCkKICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICJzdGFydC1zZXJ2ZXIiXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgICAgICAgICAgdGltZS5zbGVlcCgwLjQpCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgcGFzcwoKICAgIGxvZ19iaSgiW0FEQl0gSW5pdGlhbGl6aW5nIGNsZWFuIEFEQiBzZXJ2ZXIgc3RhdGUgKGFkYiBraWxsLXNlcnZlcikuLi4iLCAi44Kv44Oq44O844Oz44GqQURC44K144O844OQ44O844KS5Yid5pyf5YyW5LitIChhZGIga2lsbC1zZXJ2ZXIpLi4uIikKICAgIHJlc2V0X2FkYl9zZXJ2ZXIoIlN0YXJ0dXAgaW5pdGlhbGl6YXRpb24iKQoKICAgIGNvbnNlY3V0aXZlX3dhaXRzID0gMAoKICAgIGRlZiBlbnN1cmVfYWRiX2ZvcndhcmQoKToKICAgICAgICBub25sb2NhbCBjb25zZWN1dGl2ZV93YWl0cwogICAgICAgIHRyeToKICAgICAgICAgICAgIyBDaGVjayBkZXZpY2VzCiAgICAgICAgICAgIHJlcyA9IHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAiZGV2aWNlcyIsICItbCJdLCBjYXB0dXJlX291dHB1dD1UcnVlLCB0ZXh0PVRydWUpCiAgICAgICAgICAgIGxpbmVzID0gcmVzLnN0ZG91dC5zdHJpcCgpLnNwbGl0bGluZXMoKQogICAgICAgICAgICBvbmxpbmVfc2VyaWFscyA9IFtdCiAgICAgICAgICAgIHVuYXV0aG9yaXplZCA9IEZhbHNlCiAgICAgICAgICAgIG9mZmxpbmUgPSBGYWxzZQogICAgICAgICAgICBmb3IgbGluZSBpbiBsaW5lc1sxOl06CiAgICAgICAgICAgICAgICBsaW5lID0gbGluZS5zdHJpcCgpCiAgICAgICAgICAgICAgICBpZiBub3QgbGluZToKICAgICAgICAgICAgICAgICAgICBjb250aW51ZQogICAgICAgICAgICAgICAgaWYgInVuYXV0aG9yaXplZCIgaW4gbGluZToKICAgICAgICAgICAgICAgICAgICB1bmF1dGhvcml6ZWQgPSBUcnVlCiAgICAgICAgICAgICAgICBlbGlmICJvZmZsaW5lIiBpbiBsaW5lOgogICAgICAgICAgICAgICAgICAgIG9mZmxpbmUgPSBUcnVlCiAgICAgICAgICAgICAgICBlbGlmICJkZXZpY2UiIGluIGxpbmU6CiAgICAgICAgICAgICAgICAgICAgcGFydHMgPSBsaW5lLnNwbGl0KCkKICAgICAgICAgICAgICAgICAgICBpZiBsZW4ocGFydHMpID4gMDoKICAgICAgICAgICAgICAgICAgICAgICAgb25saW5lX3NlcmlhbHMuYXBwZW5kKHBhcnRzWzBdKQoKICAgICAgICAgICAgaWYgb2ZmbGluZSBhbmQgbm90IG9ubGluZV9zZXJpYWxzOgogICAgICAgICAgICAgICAgbG9nX2JpKCJbQURCXSBEZXZpY2UgaW4gb2ZmbGluZSBzdGF0ZS4gUmVzZXR0aW5nIEFEQiBzZXJ2ZXIuLi4iLCAi44Kq44OV44Op44Kk44Oz56uv5pyr44KS5qSc5Ye644CCQURC44K144O844OQ44O844KS44Oq44OV44Os44OD44K344Ol5LitLi4uIikKICAgICAgICAgICAgICAgIHJlc2V0X2FkYl9zZXJ2ZXIoIk9mZmxpbmUgZGV2aWNlIHJlY292ZXJ5IikKICAgICAgICAgICAgICAgIHJldHVybiBGYWxzZQoKICAgICAgICAgICAgaWYgbm90IG9ubGluZV9zZXJpYWxzOgogICAgICAgICAgICAgICAgY29uc2VjdXRpdmVfd2FpdHMgKz0gMQogICAgICAgICAgICAgICAgaWYgY29uc2VjdXRpdmVfd2FpdHMgPj0gMzoKICAgICAgICAgICAgICAgICAgICBsb2dfYmkoIltBREJdIERldmljZSBub3QgZGV0ZWN0ZWQgYWZ0ZXIgcmVjb25uZWN0LiBSZXNldHRpbmcgQURCIChhZGIga2lsbC1zZXJ2ZXIpLi4uIiwgIuerr+acq+OBjOiqjeitmOOBleOCjOOBquOBhOOBi+WIh+aWreOBleOCjOOBvuOBl+OBn+OAgmFkYiBraWxsLXNlcnZlciDjgpLlrp/ooYzjgZfjgablho3oqabooYzkuK0uLi4iKQogICAgICAgICAgICAgICAgICAgIHJlc2V0X2FkYl9zZXJ2ZXIoIlJlY29ubmVjdCByZXRyeSIpCiAgICAgICAgICAgICAgICAgICAgY29uc2VjdXRpdmVfd2FpdHMgPSAwCiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIEZhbHNlCgogICAgICAgICAgICAgICAgaWYgdW5hdXRob3JpemVkOgogICAgICAgICAgICAgICAgICAgIGxvZ19iaSgiW1dBSVRdIEFuZHJvaWQgZGV2aWNlIGRldGVjdGVkLCBidXQgdW5hdXRob3JpemVkLiIsICJBbmRyb2lk56uv5pyr44GM5qSc5Ye644GV44KM44G+44GX44Gf44GM44CB5pyq6Kix5Y+v44Gn44GZ44CC55S76Z2i44Ot44OD44Kv44KS6Kej6Zmk44GX44Gm44CMVVNC44OH44OQ44OD44Kw44KS6Kix5Y+v44CN44KS44K/44OD44OX44GX44Gm44GP44Gg44GV44GE44CCIikKICAgICAgICAgICAgICAgIGVsc2U6CiAgICAgICAgICAgICAgICAgICAgbG9nX2JpKCJbV0FJVF0gTm8gQW5kcm9pZCBkZXZpY2UgZGV0ZWN0ZWQuIiwgIkFuZHJvaWTnq6/mnKvjgYzopovjgaTjgYvjgorjgb7jgZvjgpPjgIJVU0LjgrHjg7zjg5bjg6vmjqXntprjgajjgIxVU0Ljg4fjg5Djg4PjgrDjgI3jga7mnInlirnljJbjgpLnorroqo3jgZfjgabjgY/jgaDjgZXjgYTjgIIiKQogICAgICAgICAgICAgICAgcmV0dXJuIEZhbHNlCgogICAgICAgICAgICBjb25zZWN1dGl2ZV93YWl0cyA9IDAKICAgICAgICAgICAgY2hvc2VuX3NlcmlhbCA9IG9ubGluZV9zZXJpYWxzWzBdCiAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAiLXMiLCBjaG9zZW5fc2VyaWFsLCAiZm9yd2FyZCIsICItLXJlbW92ZSIsIGYidGNwOntQT1JUfSJdLCBzdGRvdXQ9c3VicHJvY2Vzcy5ERVZOVUxMLCBzdGRlcnI9c3VicHJvY2Vzcy5ERVZOVUxMKQogICAgICAgICAgICBmd2QgPSBzdWJwcm9jZXNzLnJ1bihbYWRiX2NtZCwgIi1zIiwgY2hvc2VuX3NlcmlhbCwgImZvcndhcmQiLCBmInRjcDp7UE9SVH0iLCBmInRjcDp7UE9SVH0iXSwgY2FwdHVyZV9vdXRwdXQ9VHJ1ZSwgdGV4dD1UcnVlKQogICAgICAgICAgICByZXR1cm4gZndkLnJldHVybmNvZGUgPT0gMAogICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgIHJldHVybiBGYWxzZQoKICAgIGxvZ19iaShmIlJlYWR5LiBTdGFydGluZyBhdXRvLWNvbm5lY3Rpb24gbG9vcCAodGFyZ2V0IHBvcnQ6IHtQT1JUfSkuLi4iLCBmIua6luWCmeWujOS6huOAguiHquWLleaOpee2muODq+ODvOODl+OCkumWi+Wni+OBl+OBvuOBmSAo44Od44O844OIOiB7UE9SVH0pLi4uIikKICAgIGxvZ19iaSgiTm90ZTogRW5zdXJlIFRlcm1pbmFsL0FwcCBoYXMgQWNjZXNzaWJpbGl0eSBwZXJtaXNzaW9uIGluIFN5c3RlbSBTZXR0aW5ncyAtPiBQcml2YWN5ICYgU2VjdXJpdHkgLT4gQWNjZXNzaWJpbGl0eS4iLCAi5rOo5oSPOiDliJ3lm57jga/jgIzjgrfjgrnjg4bjg6DoqK3lrpogLT4g44OX44Op44Kk44OQ44K344O844Go44K744Kt44Ol44Oq44OG44KjIC0+IOOCouOCr+OCu+OCt+ODk+ODquODhuOCo+OAjeOBp+OCv+ODvOODn+ODiuODq+OBruOCreODvOmAgeS/oeioseWPr+OBjOW/heimgeOBp+OBmeOAgiIpCiAgICBsYXN0X2Z3ZF9vayA9IEZhbHNlCgogICAgd2hpbGUgVHJ1ZToKICAgICAgICB0cnk6CiAgICAgICAgICAgIGlmIG5vdCBlbnN1cmVfYWRiX2ZvcndhcmQoKToKICAgICAgICAgICAgICAgIGxhc3RfZndkX29rID0gRmFsc2UKICAgICAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKICAgICAgICAgICAgICAgIGNvbnRpbnVlCgogICAgICAgICAgICBpZiBub3QgbGFzdF9md2Rfb2s6CiAgICAgICAgICAgICAgICBsb2dfYmkoZiJbT0tdIEFEQiBwb3J0IGZvcndhcmRpbmcgYWN0aXZlIChwb3J0IHtQT1JUfSkuIiwgZiJBRELjg53jg7zjg4jjg5Xjgqnjg6/jg7zjg4nnorrnq4sgKOODneODvOODiCB7UE9SVH0p44CC44K544Oe44Ob44Ki44OX44Oq44Gn44CMUEPmjqXntpogKFVTQinjgI3jg6Ljg7zjg4njgpLplovjgYTjgabjgY/jgaDjgZXjgYTjgIIiKQogICAgICAgICAgICAgICAgbGFzdF9md2Rfb2sgPSBUcnVlCgogICAgICAgICAgICBzID0gc29ja2V0LnNvY2tldChzb2NrZXQuQUZfSU5FVCwgc29ja2V0LlNPQ0tfU1RSRUFNKQogICAgICAgICAgICBzLnNldHRpbWVvdXQoMi4wKQogICAgICAgICAgICBzLmNvbm5lY3QoKCIxMjcuMC4wLjEiLCBQT1JUKSkKICAgICAgICAgICAgCiAgICAgICAgICAgIGYgPSBzLm1ha2VmaWxlKCJyIiwgZW5jb2Rpbmc9InV0Zi04IikKICAgICAgICAgICAgYmFubmVyID0gZi5yZWFkbGluZSgpCiAgICAgICAgICAgIGlmIG5vdCBiYW5uZXI6CiAgICAgICAgICAgICAgICBzLmNsb3NlKCkKICAgICAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKICAgICAgICAgICAgICAgIGNvbnRpbnVlCgogICAgICAgICAgICBzLnNldHRpbWVvdXQoTm9uZSkKICAgICAgICAgICAgbG9nKCI9PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09IikKICAgICAgICAgICAgbG9nX2JpKCIgKioqIFRhaWtvIENvbnRyb2xsZXIgQ29ubmVjdGVkIFN1Y2Nlc3NmdWxseSEgKioqIiwgIuKYheKYheKYhSDlpKrpvJPjgrPjg7Pjg4jjg63jg7zjg6njg7wgKOOCouODl+ODqikg44Go5o6l57aa5a6M5LqG77yBIOKYheKYheKYhSIpCiAgICAgICAgICAgIGxvZ19iaSgiIFNlbmRpbmcga2V5cyAoRCAvIEYgLyBKIC8gSykgdG8gUEMgZ2FtZXMgaW4gcmVhbC10aW1lLiIsICJQQ+OCsuODvOODoOOBuOOCreODvOOCkuODquOCouODq+OCv+OCpOODoOmAgeS/oeOBl+OBvuOBmeOAgiIpCiAgICAgICAgICAgIGxvZygiPT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PSIpCiAgICAgICAgICAgIAogICAgICAgICAgICBmb3IgbGluZSBpbiBmOgogICAgICAgICAgICAgICAgbGluZSA9IGxpbmUuc3RyaXAoKQogICAgICAgICAgICAgICAgaWYgbm90IGxpbmUgb3IgbGluZSA9PSAiUElORyI6CiAgICAgICAgICAgICAgICAgICAgY29udGludWUKICAgICAgICAgICAgICAgIHBhcnRzID0gbGluZS5zcGxpdCgiICIpCiAgICAgICAgICAgICAgICBpZiBsZW4ocGFydHMpID49IDI6CiAgICAgICAgICAgICAgICAgICAgYWN0aW9uID0gcGFydHNbMF0KICAgICAgICAgICAgICAgICAgICBmb3Iga2V5X3N0ciBpbiBwYXJ0c1sxOl06CiAgICAgICAgICAgICAgICAgICAgICAgIGtleV9jaGFyID0ga2V5X3N0ci5sb3dlcigpCiAgICAgICAgICAgICAgICAgICAgICAgIGlmIG5vdCBrZXlfY2hhcjoKICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgICAgICAgICAgICAgICAgIGxvZyhmIltLRVldIHthY3Rpb259IC0+IHtrZXlfY2hhcn0iKQogICAgICAgICAgICAgICAgICAgICAgICBpZiBhY3Rpb24gPT0gIkRPV04iOgogICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VuZF9kb3duKGtleV9jaGFyKQogICAgICAgICAgICAgICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAiVVAiOgogICAgICAgICAgICAgICAgICAgICAgICAgICAgc2VuZF91cChrZXlfY2hhcikKCiAgICAgICAgICAgIGxvZ19iaSgiW0lORk9dIENvbm5lY3Rpb24gY2xvc2VkIGJ5IEFuZHJvaWQgYXBwLiBXYWl0aW5nIHRvIHJlY29ubmVjdC4uLiIsICLjgqLjg5fjg6rjgajjga7mjqXntprjgYzliIfmlq3jgZXjgozjgb7jgZfjgZ/jgILlho3mjqXntprlvoXmqZ/kuK0uLi4iKQogICAgICAgICAgICBzLmNsb3NlKCkKICAgICAgICAgICAgcmVzZXRfYWRiX3NlcnZlcigiUmVjb25uZWN0aW9uIGNsZWFudXAiKQogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgZXhjZXB0IChzb2NrZXQudGltZW91dCwgQ29ubmVjdGlvblJlZnVzZWRFcnJvciwgT1NFcnJvcik6CiAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKICAgICAgICBleGNlcHQgS2V5Ym9hcmRJbnRlcnJ1cHQ6CiAgICAgICAgICAgIGxvZygiRXhpdGluZy4uLiIpCiAgICAgICAgICAgIGJyZWFrCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCgppZiBfX25hbWVfXyA9PSAiX19tYWluX18iOgogICAgdHJ5OgogICAgICAgIG1haW4oKQogICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlcnI6CiAgICAgICAgcHJpbnQoZiJGYXRhbCBFcnJvcjoge2Vycn0iLCBmbHVzaD1UcnVlKQogICAgICAgIHRyYWNlYmFjay5wcmludF9leGMoKQogICAgICAgIGlucHV0KCJQcmVzcyBFbnRlciB0byBleGl0Li4uIikK", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)
            val powerShellScript = String(android.util.Base64.decode("IyBXaW5kb3dzIFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIG5hbWU6IFRUQy1yZWNlaXZlci13aW5kb3dzLnBzMQojIFVzYWdlOiBSaWdodC1jbGljayB0aGUgc2F2ZWQgZmlsZSBhbmQgc2VsZWN0ICJSdW4gd2l0aCBQb3dlclNoZWxsIgoKcGFyYW0oCiAgICBbaW50XSRwb3J0ID0gNjAwMDEsCiAgICBbc3RyaW5nXSRhZGJUYXJnZXQgPSAiIgopCgokYWRiQ21kID0gImFkYiIKCmZ1bmN0aW9uIEFkZC1QYXRoVG9Vc2VyRW52aXJvbm1lbnQoJGRpclRvQWRkKSB7CiAgICB0cnkgewogICAgICAgICRyZXNvbHZlZERpciA9IChSZXNvbHZlLVBhdGggJGRpclRvQWRkKS5QYXRoCiAgICAgICAgJHVzZXJQYXRoID0gW1N5c3RlbS5FbnZpcm9ubWVudF06OkdldEVudmlyb25tZW50VmFyaWFibGUoIlBhdGgiLCAiVXNlciIpCiAgICAgICAgaWYgKCRudWxsIC1lcSAkdXNlclBhdGgpIHsgJHVzZXJQYXRoID0gIiIgfQogICAgICAgICRwYXRocyA9ICR1c2VyUGF0aC5TcGxpdCgnOycsIFtTeXN0ZW0uU3RyaW5nU3BsaXRPcHRpb25zXTo6UmVtb3ZlRW1wdHlFbnRyaWVzKQogICAgICAgIGlmICgkcGF0aHMgLW5vdGNvbnRhaW5zICRyZXNvbHZlZERpcikgewogICAgICAgICAgICBXcml0ZS1Ib3N0ICJSZWdpc3RlcmluZyBBREIgdG8gVXNlciBQQVRIOiAkcmVzb2x2ZWREaXIiIC1Gb3JlZ3JvdW5kQ29sb3IgQ3lhbgogICAgICAgICAgICAkbmV3UGF0aCA9IGlmICgkdXNlclBhdGguVHJpbSgpLkxlbmd0aCAtZ3QgMCkgeyAiJHVzZXJQYXRoOyRyZXNvbHZlZERpciIgfSBlbHNlIHsgJHJlc29sdmVkRGlyIH0KICAgICAgICAgICAgW1N5c3RlbS5FbnZpcm9ubWVudF06OlNldEVudmlyb25tZW50VmFyaWFibGUoIlBhdGgiLCAkbmV3UGF0aCwgIlVzZXIiKQogICAgICAgICAgICAkZW52OlBhdGggPSBbU3lzdGVtLkVudmlyb25tZW50XTo6R2V0RW52aXJvbm1lbnRWYXJpYWJsZSgiUGF0aCIsIk1hY2hpbmUiKSArICI7IiArIFtTeXN0ZW0uRW52aXJvbm1lbnRdOjpHZXRFbnZpcm9ubWVudFZhcmlhYmxlKCJQYXRoIiwiVXNlciIpCiAgICAgICAgICAgIFdyaXRlLUhvc3QgIkFEQiBzdWNjZXNzZnVsbHkgYWRkZWQgdG8gUEFUSCEiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICB9CiAgICB9IGNhdGNoIHsKICAgICAgICBXcml0ZS1Ib3N0ICJOb3RlOiBDb3VsZCBub3QgYXV0b21hdGljYWxseSB1cGRhdGUgVXNlciBQQVRIOiAkXyIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgIH0KfQoKIyBFbnN1cmUgQURCIGlzIGF2YWlsYWJsZSBhbmQgY2hlY2sgZm9yIHVwZGF0ZXMKZnVuY3Rpb24gRW5zdXJlLUFuZC1VcGRhdGUtQWRiIHsKICAgICRmb3VuZEFkYiA9ICRudWxsCgogICAgaWYgKEdldC1Db21tYW5kIGFkYiAtRXJyb3JBY3Rpb24gU2lsZW50bHlDb250aW51ZSkgewogICAgICAgICRmb3VuZEFkYiA9ICJhZGIiCiAgICB9IGVsc2VpZiAoVGVzdC1QYXRoICIuXHBsYXRmb3JtLXRvb2xzXGFkYi5leGUiKSB7CiAgICAgICAgJGZvdW5kQWRiID0gIi5ccGxhdGZvcm0tdG9vbHNcYWRiLmV4ZSIKICAgICAgICBBZGQtUGF0aFRvVXNlckVudmlyb25tZW50ICIuXHBsYXRmb3JtLXRvb2xzIgogICAgfSBlbHNlIHsKICAgICAgICBXcml0ZS1Ib3N0ICJBREIgaXMgbm90IGZvdW5kIGluIFBBVEggb3IgcGxhdGZvcm0tdG9vbHMuIENoZWNraW5nIHdpbmdldC4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICBpZiAoR2V0LUNvbW1hbmQgd2luZ2V0IC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSB7CiAgICAgICAgICAgIHRyeSB7CiAgICAgICAgICAgICAgICB3aW5nZXQgaW5zdGFsbCBHb29nbGUuQWRiIC0tc2lsZW50IC0tYWNjZXB0LXNvdXJjZS1hZ3JlZW1lbnRzIC0tYWNjZXB0LXBhY2thZ2UtYWdyZWVtZW50cyB8IE91dC1OdWxsCiAgICAgICAgICAgICAgICAkZW52OlBhdGggPSBbU3lzdGVtLkVudmlyb25tZW50XTo6R2V0RW52aXJvbm1lbnRWYXJpYWJsZSgiUGF0aCIsIk1hY2hpbmUiKSArICI7IiArIFtTeXN0ZW0uRW52aXJvbm1lbnRdOjpHZXRFbnZpcm9ubWVudFZhcmlhYmxlKCJQYXRoIiwiVXNlciIpCiAgICAgICAgICAgICAgICBpZiAoR2V0LUNvbW1hbmQgYWRiIC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSB7CiAgICAgICAgICAgICAgICAgICAgJGZvdW5kQWRiID0gImFkYiIKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJBREIgaW5zdGFsbGVkIHZpYSB3aW5nZXQgc3VjY2Vzc2Z1bGx5ISIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9IGNhdGNoIHt9CiAgICAgICAgfQoKICAgICAgICBpZiAoJG51bGwgLWVxICRmb3VuZEFkYikgewogICAgICAgICAgICBXcml0ZS1Ib3N0ICJEb3dubG9hZGluZyBvZmZpY2lhbCBBbmRyb2lkIFNESyBQbGF0Zm9ybSBUb29scyBmcm9tIEdvb2dsZS4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgJHVybCA9ICJodHRwczovL2RsLmdvb2dsZS5jb20vYW5kcm9pZC9yZXBvc2l0b3J5L3BsYXRmb3JtLXRvb2xzLWxhdGVzdC13aW5kb3dzLnppcCIKICAgICAgICAgICAgJG91dHB1dCA9ICIuXHBsYXRmb3JtLXRvb2xzLnppcCIKICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIEludm9rZS1XZWJSZXF1ZXN0IC1VcmkgJHVybCAtT3V0RmlsZSAkb3V0cHV0CiAgICAgICAgICAgICAgICBFeHBhbmQtQXJjaGl2ZSAtUGF0aCAkb3V0cHV0IC1EZXN0aW5hdGlvblBhdGggIi4iIC1Gb3JjZQogICAgICAgICAgICAgICAgUmVtb3ZlLUl0ZW0gJG91dHB1dCAtRm9yY2UgLUVycm9yQWN0aW9uIFNpbGVudGx5Q29udGludWUKICAgICAgICAgICAgICAgIGlmIChUZXN0LVBhdGggIi5ccGxhdGZvcm0tdG9vbHNcYWRiLmV4ZSIpIHsKICAgICAgICAgICAgICAgICAgICAkZm91bmRBZGIgPSAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIgogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkFEQiBkb3dubG9hZGVkIGFuZCBleHRyYWN0ZWQgc3VjY2Vzc2Z1bGx5ISIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgICAgICAgICAgICAgIEFkZC1QYXRoVG9Vc2VyRW52aXJvbm1lbnQgIi5ccGxhdGZvcm0tdG9vbHMiCiAgICAgICAgICAgICAgICB9IGVsc2UgewogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkVycm9yOiBGYWlsZWQgdG8gZXh0cmFjdCBwbGF0Zm9ybS10b29scy4iIC1Gb3JlZ3JvdW5kQ29sb3IgUmVkCiAgICAgICAgICAgICAgICAgICAgUGF1c2UKICAgICAgICAgICAgICAgICAgICBFeGl0CiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgIH0gY2F0Y2ggewogICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiRXJyb3I6IENvdWxkIG5vdCBkb3dubG9hZCBBREIuIFBsZWFzZSBjaGVjayB5b3VyIGludGVybmV0IGNvbm5lY3Rpb24uIiAtRm9yZWdyb3VuZENvbG9yIFJlZAogICAgICAgICAgICAgICAgUGF1c2UKICAgICAgICAgICAgICAgIEV4aXQKICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KCiAgICAjIENoZWNrIGZvciBBREIgdXBkYXRlcyBhbmQgZXhlY3V0ZSB1cGRhdGUgaWYgYXZhaWxhYmxlCiAgICBXcml0ZS1Ib3N0ICJbQURCXSBDaGVja2luZyBmb3IgQURCIHVwZGF0ZXMuLi4iIC1Gb3JlZ3JvdW5kQ29sb3IgQ3lhbgogICAgJHVwZGF0ZUNvbXBsZXRlZCA9ICRmYWxzZQoKICAgICMgMS4gSWYgd2luZ2V0IGlzIGF2YWlsYWJsZSBhbmQgbWFuYWdlcyBHb29nbGUuQWRiCiAgICBpZiAoR2V0LUNvbW1hbmQgd2luZ2V0IC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSB7CiAgICAgICAgdHJ5IHsKICAgICAgICAgICAgJHdpbmdldExpc3QgPSB3aW5nZXQgbGlzdCAtLWlkIEdvb2dsZS5BZGIgLS1leGFjdCAyPiYxCiAgICAgICAgICAgIGlmICgkd2luZ2V0TGlzdCAtbWF0Y2ggIkdvb2dsZVwuQWRiIikgewogICAgICAgICAgICAgICAgJHdpbmdldFVwZ3JhZGUgPSB3aW5nZXQgdXBncmFkZSAtLWlkIEdvb2dsZS5BZGIgLS1leGFjdCAyPiYxCiAgICAgICAgICAgICAgICBpZiAoJHdpbmdldFVwZ3JhZGUgLW1hdGNoICJHb29nbGVcLkFkYiIpIHsKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJbQURCXSBVcGRhdGUgZm91bmQgdmlhIHdpbmdldC4gVXBkYXRpbmcgQURCLi4uIiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgICAgICAgICAgICAgICAgIHRyeSB7ICYgJGZvdW5kQWRiIGtpbGwtc2VydmVyIDI+JG51bGwgfSBjYXRjaCB7fQogICAgICAgICAgICAgICAgICAgIFN0YXJ0LVNsZWVwIC1NaWxsaXNlY29uZHMgNDAwCiAgICAgICAgICAgICAgICAgICAgd2luZ2V0IHVwZ3JhZGUgLS1pZCBHb29nbGUuQWRiIC0tZXhhY3QgLS1zaWxlbnQgLS1hY2NlcHQtc291cmNlLWFncmVlbWVudHMgLS1hY2NlcHQtcGFja2FnZS1hZ3JlZW1lbnRzIHwgT3V0LU51bGwKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJbQURCXSBBREIgdXBkYXRlZCBzdWNjZXNzZnVsbHkgdmlhIHdpbmdldCEiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICAgICAgICAgICAgICAkZm91bmRBZGIgPSAiYWRiIgogICAgICAgICAgICAgICAgICAgICR1cGRhdGVDb21wbGV0ZWQgPSAkdHJ1ZQogICAgICAgICAgICAgICAgfSBlbHNlIHsKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJbQURCXSBBREIgaXMgYWxyZWFkeSB1cCB0byBkYXRlICh3aW5nZXQpLiIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgICAgICAgICAgICAgICR1cGRhdGVDb21wbGV0ZWQgPSAkdHJ1ZQogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9CiAgICAgICAgfSBjYXRjaCB7fQogICAgfQoKICAgICMgMi4gSWYgdXNpbmcgbG9jYWwgcGxhdGZvcm0tdG9vbHMgb3Igd2luZ2V0IHdhcyBub3QgdXNlZCwgY2hlY2sgR29vZ2xlIHJlcG9zaXRvcnkgZm9yIHVwZGF0ZXMKICAgIGlmICghJHVwZGF0ZUNvbXBsZXRlZCAtYW5kIChUZXN0LVBhdGggIi5ccGxhdGZvcm0tdG9vbHNcYWRiLmV4ZSIpKSB7CiAgICAgICAgdHJ5IHsKICAgICAgICAgICAgJHVybCA9ICJodHRwczovL2RsLmdvb2dsZS5jb20vYW5kcm9pZC9yZXBvc2l0b3J5L3BsYXRmb3JtLXRvb2xzLWxhdGVzdC13aW5kb3dzLnppcCIKICAgICAgICAgICAgJGV0YWdGaWxlID0gIi5ccGxhdGZvcm0tdG9vbHNcLmV0YWciCiAgICAgICAgICAgICRzYXZlZEV0YWcgPSAiIgogICAgICAgICAgICBpZiAoVGVzdC1QYXRoICRldGFnRmlsZSkgewogICAgICAgICAgICAgICAgdHJ5IHsgJHNhdmVkRXRhZyA9IChHZXQtQ29udGVudCAkZXRhZ0ZpbGUgLVJhdyAtRXJyb3JBY3Rpb24gU2lsZW50bHlDb250aW51ZSkuVHJpbSgpIH0gY2F0Y2gge30KICAgICAgICAgICAgfQoKICAgICAgICAgICAgIyBIVFRQIEhFQUQgdG8gY2hlY2sgcmVtb3RlIHZlcnNpb24vZXRhZyB3aXRob3V0IGRvd25sb2FkaW5nIGZ1bGwgemlwCiAgICAgICAgICAgICRyZXEgPSBbU3lzdGVtLk5ldC5IdHRwV2ViUmVxdWVzdF06OkNyZWF0ZSgkdXJsKQogICAgICAgICAgICAkcmVxLk1ldGhvZCA9ICJIRUFEIgogICAgICAgICAgICAkcmVxLlRpbWVvdXQgPSA0MDAwCiAgICAgICAgICAgICRyZXNwID0gJHJlcS5HZXRSZXNwb25zZSgpCiAgICAgICAgICAgICRyZW1vdGVFdGFnID0gJHJlc3AuSGVhZGVyc1siRVRhZyJdCiAgICAgICAgICAgICRsYXN0TW9kaWZpZWQgPSAkcmVzcC5IZWFkZXJzWyJMYXN0LU1vZGlmaWVkIl0KICAgICAgICAgICAgJHJlc3AuQ2xvc2UoKQoKICAgICAgICAgICAgJHJlbW90ZVRva2VuID0gaWYgKCRyZW1vdGVFdGFnKSB7ICRyZW1vdGVFdGFnIH0gZWxzZSB7ICRsYXN0TW9kaWZpZWQgfQoKICAgICAgICAgICAgaWYgKCRyZW1vdGVUb2tlbiAtYW5kICRzYXZlZEV0YWcgLW5lICIiIC1hbmQgJHNhdmVkRXRhZyAtZXEgJHJlbW90ZVRva2VuKSB7CiAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJbQURCXSBMb2NhbCBwbGF0Zm9ybS10b29scyBpcyBhbHJlYWR5IHVwIHRvIGRhdGUuIiAtRm9yZWdyb3VuZENvbG9yIEdyZWVuCiAgICAgICAgICAgIH0gZWxzZWlmICgkcmVtb3RlVG9rZW4pIHsKICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIltBREJdIE5ldyB2ZXJzaW9uIG9mIEFuZHJvaWQgUGxhdGZvcm0gVG9vbHMgYXZhaWxhYmxlLiBVcGRhdGluZy4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgICAgIHRyeSB7ICYgJGZvdW5kQWRiIGtpbGwtc2VydmVyIDI+JG51bGwgfSBjYXRjaCB7fQogICAgICAgICAgICAgICAgU3RhcnQtU2xlZXAgLU1pbGxpc2Vjb25kcyA0MDAKCiAgICAgICAgICAgICAgICAkdXBkYXRlWmlwID0gIi5ccGxhdGZvcm0tdG9vbHMtdXBkYXRlLnppcCIKICAgICAgICAgICAgICAgIEludm9rZS1XZWJSZXF1ZXN0IC1VcmkgJHVybCAtT3V0RmlsZSAkdXBkYXRlWmlwCiAgICAgICAgICAgICAgICBFeHBhbmQtQXJjaGl2ZSAtUGF0aCAkdXBkYXRlWmlwIC1EZXN0aW5hdGlvblBhdGggIi4iIC1Gb3JjZQogICAgICAgICAgICAgICAgUmVtb3ZlLUl0ZW0gJHVwZGF0ZVppcCAtRm9yY2UgLUVycm9yQWN0aW9uIFNpbGVudGx5Q29udGludWUKCiAgICAgICAgICAgICAgICB0cnkgeyBTZXQtQ29udGVudCAtUGF0aCAkZXRhZ0ZpbGUgLVZhbHVlICRyZW1vdGVUb2tlbiAtRm9yY2UgfSBjYXRjaCB7fQogICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiW0FEQl0gUGxhdGZvcm0gVG9vbHMgdXBkYXRlZCB0byBsYXRlc3QgdmVyc2lvbiEiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICAgICAgICAgICRmb3VuZEFkYiA9ICIuXHBsYXRmb3JtLXRvb2xzXGFkYi5leGUiCiAgICAgICAgICAgIH0KICAgICAgICB9IGNhdGNoIHsKICAgICAgICAgICAgV3JpdGUtSG9zdCAiW0FEQl0gVXBkYXRlIGNoZWNrIHNraXBwZWQgKHVzaW5nIGN1cnJlbnQgQURCKS4iIC1Gb3JlZ3JvdW5kQ29sb3IgRGFya0dyYXkKICAgICAgICB9CiAgICB9IGVsc2VpZiAoISR1cGRhdGVDb21wbGV0ZWQpIHsKICAgICAgICBXcml0ZS1Ib3N0ICJbQURCXSBDdXJyZW50IEFEQiBpcyBhY3RpdmUgYW5kIHJlYWR5LiIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgfQoKICAgIHJldHVybiAkZm91bmRBZGIKfQoKJGFkYkNtZCA9IEVuc3VyZS1BbmQtVXBkYXRlLUFkYgoKIyBBZGQgQyMgaGVscGVyIGZvciBXaW4zMiBsb3ctbGF0ZW5jeSBrZXkgZXZlbnRzIHdpdGggRGlyZWN0WC9EaXJlY3RJbnB1dCBIYXJkd2FyZSBTY2FuQ29kZSBzdXBwb3J0CmlmICghKCJUYWlrb0tleWJvYXJkIiAtYXMgW3R5cGVdKSkgewogICAgJFNpZ25hdHVyZSA9IEAiCnVzaW5nIFN5c3RlbTsKdXNpbmcgU3lzdGVtLlJ1bnRpbWUuSW50ZXJvcFNlcnZpY2VzOwoKcHVibGljIGNsYXNzIFRhaWtvS2V5Ym9hcmQgewogICAgW0RsbEltcG9ydCgidXNlcjMyLmRsbCIpXQogICAgcHVibGljIHN0YXRpYyBleHRlcm4gdm9pZCBrZXliZF9ldmVudChieXRlIGJWaywgYnl0ZSBiU2NhbiwgdWludCBkd0ZsYWdzLCBVSW50UHRyIGR3RXh0cmFJbmZvKTsKCiAgICBbRGxsSW1wb3J0KCJ1c2VyMzIuZGxsIildCiAgICBwdWJsaWMgc3RhdGljIGV4dGVybiB1aW50IE1hcFZpcnR1YWxLZXkodWludCB1Q29kZSwgdWludCB1TWFwVHlwZSk7CgogICAgW0RsbEltcG9ydCgidXNlcjMyLmRsbCIsIFNldExhc3RFcnJvciA9IHRydWUpXQogICAgcHVibGljIHN0YXRpYyBleHRlcm4gdWludCBTZW5kSW5wdXQodWludCBuSW5wdXRzLCBJbnRQdHIgcElucHV0cywgaW50IGNiU2l6ZSk7CgogICAgcHJpdmF0ZSBjb25zdCB1aW50IEtFWUVWRU5URl9LRVlVUCA9IDB4MDAwMjsKCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIFNlbmREb3duUmF3KGJ5dGUgdmtleSwgdXNob3J0IHNjYW5Db2RlKSB7CiAgICAgICAga2V5YmRfZXZlbnQodmtleSwgKGJ5dGUpc2NhbkNvZGUsIDAsIFVJbnRQdHIuWmVybyk7CgogICAgICAgIHRyeSB7CiAgICAgICAgICAgIGludCBjYlNpemUgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA0MCA6IDI4OwogICAgICAgICAgICBieXRlW10gaW5wdXRCeXRlcyA9IG5ldyBieXRlW2NiU2l6ZV07CiAgICAgICAgICAgIEJpdENvbnZlcnRlci5HZXRCeXRlcygodWludCkxKS5Db3B5VG8oaW5wdXRCeXRlcywgMCk7IC8vIElOUFVUX0tFWUJPQVJECiAgICAgICAgICAgIGludCBraU9mZnNldCA9IChJbnRQdHIuU2l6ZSA9PSA4KSA/IDggOiA0OwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoc2NhbkNvZGUpLkNvcHlUbyhpbnB1dEJ5dGVzLCBraU9mZnNldCArIDIpOyAvLyB3U2NhbgogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMHgwMDA4KS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyA0KTsgLy8gS0VZRVZFTlRGX1NDQU5DT0RFCiAgICAgICAgICAgIEdDSGFuZGxlIGhhbmRsZSA9IEdDSGFuZGxlLkFsbG9jKGlucHV0Qnl0ZXMsIEdDSGFuZGxlVHlwZS5QaW5uZWQpOwogICAgICAgICAgICBTZW5kSW5wdXQoMSwgaGFuZGxlLkFkZHJPZlBpbm5lZE9iamVjdCgpLCBjYlNpemUpOwogICAgICAgICAgICBoYW5kbGUuRnJlZSgpOwogICAgICAgIH0gY2F0Y2gge30KICAgIH0KCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIFNlbmRVcFJhdyhieXRlIHZrZXksIHVzaG9ydCBzY2FuQ29kZSkgewogICAgICAgIGtleWJkX2V2ZW50KHZrZXksIChieXRlKXNjYW5Db2RlLCBLRVlFVkVOVEZfS0VZVVAsIFVJbnRQdHIuWmVybyk7CgogICAgICAgIHRyeSB7CiAgICAgICAgICAgIGludCBjYlNpemUgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA0MCA6IDI4OwogICAgICAgICAgICBieXRlW10gaW5wdXRCeXRlcyA9IG5ldyBieXRlW2NiU2l6ZV07CiAgICAgICAgICAgIEJpdENvbnZlcnRlci5HZXRCeXRlcygodWludCkxKS5Db3B5VG8oaW5wdXRCeXRlcywgMCk7IC8vIElOUFVUX0tFWUJPQVJECiAgICAgICAgICAgIGludCBraU9mZnNldCA9IChJbnRQdHIuU2l6ZSA9PSA4KSA/IDggOiA0OwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoc2NhbkNvZGUpLkNvcHlUbyhpbnB1dEJ5dGVzLCBraU9mZnNldCArIDIpOyAvLyB3U2NhbgogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMHgwMDBhKS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyA0KTsgLy8gS0VZRVZFTlRGX1NDQU5DT0RFIHwgS0VZRVZFTlRGX0tFWVVQCiAgICAgICAgICAgIEdDSGFuZGxlIGhhbmRsZSA9IEdDSGFuZGxlLkFsbG9jKGlucHV0Qnl0ZXMsIEdDSGFuZGxlVHlwZS5QaW5uZWQpOwogICAgICAgICAgICBTZW5kSW5wdXQoMSwgaGFuZGxlLkFkZHJPZlBpbm5lZE9iamVjdCgpLCBjYlNpemUpOwogICAgICAgICAgICBoYW5kbGUuRnJlZSgpOwogICAgICAgIH0gY2F0Y2gge30KICAgIH0KCiAgICBwdWJsaWMgc3RhdGljIHZvaWQgRG93bihieXRlIHZrZXkpIHsKICAgICAgICB1c2hvcnQgc2NhbkNvZGUgPSAodXNob3J0KU1hcFZpcnR1YWxLZXkodmtleSwgMCk7CiAgICAgICAgU2VuZERvd25SYXcodmtleSwgc2NhbkNvZGUpOwogICAgfQoKICAgIHB1YmxpYyBzdGF0aWMgdm9pZCBVcChieXRlIHZrZXkpIHsKICAgICAgICB1c2hvcnQgc2NhbkNvZGUgPSAodXNob3J0KU1hcFZpcnR1YWxLZXkodmtleSwgMCk7CiAgICAgICAgU2VuZFVwUmF3KHZrZXksIHNjYW5Db2RlKTsKICAgIH0KfQoiQAogICAgdHJ5IHsKICAgICAgICBBZGQtVHlwZSAtVHlwZURlZmluaXRpb24gJFNpZ25hdHVyZSAtRXJyb3JBY3Rpb24gU3RvcAogICAgfSBjYXRjaCB7CiAgICAgICAgV3JpdGUtSG9zdCAiRmFpbGVkIHRvIGNvbXBpbGUga2V5Ym9hcmQgaGVscGVyOiAkXyIgLUZvcmVncm91bmRDb2xvciBSZWQKICAgIH0KfQoKdHJ5IHsgW0NvbnNvbGVdOjpPdXRwdXRFbmNvZGluZyA9IFtTeXN0ZW0uVGV4dC5FbmNvZGluZ106OlVURjggfSBjYXRjaCB7fQoKZnVuY3Rpb24gTG9nLUJpKCRjb2xvciwgW3N0cmluZ10kZW4sIFtzdHJpbmddJGphQjY0ID0gIiIpIHsKICAgIFdyaXRlLUhvc3QgJGVuIC1Gb3JlZ3JvdW5kQ29sb3IgJGNvbG9yCiAgICBpZiAoJGphQjY0IC1uZSAiIikgewogICAgICAgIHRyeSB7CiAgICAgICAgICAgICRieXRlcyA9IFtTeXN0ZW0uQ29udmVydF06OkZyb21CYXNlNjRTdHJpbmcoJGphQjY0KQogICAgICAgICAgICAkamEgPSBbU3lzdGVtLlRleHQuRW5jb2RpbmddOjpVVEY4LkdldFN0cmluZygkYnl0ZXMpCiAgICAgICAgICAgIFdyaXRlLUhvc3QgIiAgLT4gJGphIiAtRm9yZWdyb3VuZENvbG9yICRjb2xvcgogICAgICAgIH0gY2F0Y2gge30KICAgIH0KfQoKTG9nLUJpIEdyZWVuICI9PT0gVGFpa28gQ29udHJvbGxlciBSZWNlaXZlciBmb3IgV2luZG93cyA9PT0iICI1YVNxNmJ5VDQ0S3o0NE96NDRPSTQ0T3Q0NE84NDRPcDQ0TzhJRmRwYm1SdmQzUG5sS2psajVma3Y2SGpncm5qZ3Evamc2cmpnNWZqZzRnPSIKTG9nLUJpIEN5YW4gIkluaXRpYWxpemluZyBjb25uZWN0aW9uIGhlbHBlci4uLiIgIjVvNmw1N2FhNDRPWTQ0T3I0NE9SNDRPODQ0S1M1WWlkNXB5ZjVZeVc1TGl0TGk0dSIKCmZ1bmN0aW9uIFJlc2V0LUFkYlNlcnZlcihbc3RyaW5nXSRyZWFzb24gPSAiIikgewogICAgaWYgKCRyZWFzb24gLW5lICIiKSB7CiAgICAgICAgTG9nLUJpIFllbGxvdyAiW0FEQl0gUmVzZXR0aW5nIEFEQiBzZXJ2ZXI6ICRyZWFzb24gKGFkYiBraWxsLXNlcnZlcikuLi4iICI0NEt2NDRPcTQ0Tzg0NE96NDRHcVFVUkM1NHEyNW9XTDQ0S1M1TDJjNW9pUTQ0R1o0NEtMNDRHZjQ0S0I0NENCUVVSQzQ0SzE0NE84NDRPUTQ0Tzg0NEtTNVlhTjZMVzM1WXVWNDRHWDQ0Rys0NEdaTGk0dSIKICAgIH0KICAgIHRyeSB7CiAgICAgICAgJiAkYWRiQ21kIGtpbGwtc2VydmVyIDI+JG51bGwKICAgICAgICBTdGFydC1TbGVlcCAtTWlsbGlzZWNvbmRzIDQwMAogICAgICAgICYgJGFkYkNtZCBzdGFydC1zZXJ2ZXIgMj4kbnVsbAogICAgICAgIFN0YXJ0LVNsZWVwIC1NaWxsaXNlY29uZHMgNDAwCiAgICB9IGNhdGNoIHt9Cn0KCiMgMS4gQ2xlYW4gdXAgYW55IHJvZ3VlIG9yIGxvY2tlZCBBREIgc3RhdGUgYXQgc3RhcnR1cApMb2ctQmkgQ3lhbiAiW0FEQl0gSW5pdGlhbGl6aW5nIGNsZWFuIEFEQiBzZXJ2ZXIgc3RhdGUgKGFkYiBraWxsLXNlcnZlcikuLi4iICI0NEt2NDRPcTQ0Tzg0NE96NDRHcVFVUkM0NEsxNDRPODQ0T1E0NE84NDRLUzVZaWQ1cHlmNVl5VzVMaXRJQ2hoWkdJZ2EybHNiQzF6WlhKMlpYSXBMaTR1IgpSZXNldC1BZGJTZXJ2ZXIgIkluaXRpYWwgc3RhcnR1cCIKCiRzY3JpcHQ6Y29uc2VjdXRpdmVXYWl0Q291bnQgPSAwCgpmdW5jdGlvbiBFbnN1cmUtQWRiRm9yd2FyZCgkdGFyZ2V0UG9ydCkgewogICAgIyBDaGVjayBjb25uZWN0ZWQgZGV2aWNlcwogICAgJHJhd0RldmljZXMgPSAmICRhZGJDbWQgZGV2aWNlcyAtbCAyPiYxCiAgICAkb25saW5lU2VyaWFscyA9IEAoKQogICAgJHVuYXV0aG9yaXplZEZvdW5kID0gJGZhbHNlCiAgICAkb2ZmbGluZUZvdW5kID0gJGZhbHNlCgogICAgZm9yZWFjaCAoJGxpbmUgaW4gJHJhd0RldmljZXMpIHsKICAgICAgICAkdHJpbW1lZCA9ICRsaW5lLlRyaW0oKQogICAgICAgIGlmICgkdHJpbW1lZC5MZW5ndGggLWVxIDAgLW9yICR0cmltbWVkLlN0YXJ0c1dpdGgoIkxpc3Qgb2YgZGV2aWNlcyIpKSB7IGNvbnRpbnVlIH0KICAgICAgICBpZiAoJHRyaW1tZWQgLW1hdGNoICJeKFteXHNdKylccyt1bmF1dGhvcml6ZWQiKSB7CiAgICAgICAgICAgICR1bmF1dGhvcml6ZWRGb3VuZCA9ICR0cnVlCiAgICAgICAgfSBlbHNlaWYgKCR0cmltbWVkIC1tYXRjaCAiXihbXlxzXSspXHMrb2ZmbGluZSIpIHsKICAgICAgICAgICAgJG9mZmxpbmVGb3VuZCA9ICR0cnVlCiAgICAgICAgfSBlbHNlaWYgKCR0cmltbWVkIC1tYXRjaCAiXihbXlxzXSspXHMrZGV2aWNlIikgewogICAgICAgICAgICAkb25saW5lU2VyaWFscyArPSAkbWF0Y2hlc1sxXQogICAgICAgIH0KICAgIH0KCiAgICAjIElmIG9mZmxpbmUgZGV2aWNlIGRldGVjdGVkLCByZXNldCBBREIgY29ubmVjdGlvbgogICAgaWYgKCRvZmZsaW5lRm91bmQgLWFuZCAkb25saW5lU2VyaWFscy5Db3VudCAtZXEgMCkgewogICAgICAgIExvZy1CaSBZZWxsb3cgIltBREJdIERldmljZSBpbiBvZmZsaW5lIHN0YXRlLiBSZXNldHRpbmcgQURCIHNlcnZlci4uLiIgIjQ0S3E0NE9WNDRPcDQ0S2s0NE96NTZ1djVweXI0NEtTNXFTYzVZZTY0NENDUVVSQzQ0SzE0NE84NDRPUTQ0Tzg0NEtTNDRPcTQ0T1Y0NE9zNDRPRDQ0SzM0NE9sNUxpdExpNHUiCiAgICAgICAgUmVzZXQtQWRiU2VydmVyICJPZmZsaW5lIGRldmljZSByZWNvdmVyeSIKICAgICAgICByZXR1cm4gJGZhbHNlCiAgICB9CgogICAgIyBJZiB1c2VyIHBhc3NlZCBhbiBleHBsaWNpdCBhZGIgdGFyZ2V0IChlLmcuIC1hZGJUYXJnZXQgIjEyNy4wLjAuMTo1ODUyNiIpCiAgICBpZiAoJGFkYlRhcmdldCAtbmUgIiIgLWFuZCAkb25saW5lU2VyaWFscyAtbm90Y29udGFpbnMgJGFkYlRhcmdldCkgewogICAgICAgICRjb25uT3V0ID0gJiAkYWRiQ21kIGNvbm5lY3QgJGFkYlRhcmdldCAyPiYxCiAgICAgICAgaWYgKCRjb25uT3V0IC1tYXRjaCAiY29ubmVjdGVkIHRvIikgewogICAgICAgICAgICAkb25saW5lU2VyaWFscyArPSAkYWRiVGFyZ2V0CiAgICAgICAgfQogICAgfQoKICAgICMgSWYgbm8gZGV2aWNlcyBmb3VuZCwgYXR0ZW1wdCBhdXRvLWNvbm5lY3RpbmcgdG8gV1NBIChXaW5kb3dzIFN1YnN5c3RlbSBmb3IgQW5kcm9pZCkKICAgIGlmICgkb25saW5lU2VyaWFscy5Db3VudCAtZXEgMCkgewogICAgICAgICR3c2FSdW5uaW5nID0gR2V0LVByb2Nlc3MgLU5hbWUgIldzYUNsaWVudCIsIldzYVNlcnZpY2UiLCJ2bW1lbVdTQSIgLUVycm9yQWN0aW9uIFNpbGVudGx5Q29udGludWUKCiAgICAgICAgIyBTdGFuZGFyZCBXU0EgcG9ydHM6IDU4NTI2IChzdGFuZGFyZCksIDU1NTUgKGRlZmF1bHQgQURCKQogICAgICAgICRjYW5kaWRhdGVXc2FQb3J0cyA9IEAoNTg1MjYsIDU1NTUpCiAgICAgICAgdHJ5IHsKICAgICAgICAgICAgJGxpc3RlbmVycyA9IEdldC1OZXRUQ1BDb25uZWN0aW9uIC1TdGF0ZSBMaXN0ZW4gLUxvY2FsQWRkcmVzcyAiMTI3LjAuMC4xIiAtRXJyb3JBY3Rpb24gU2lsZW50bHlDb250aW51ZSB8CiAgICAgICAgICAgICAgICBXaGVyZS1PYmplY3QgeyAoJF8uTG9jYWxQb3J0IC1nZSA1ODUyMCAtYW5kICRfLkxvY2FsUG9ydCAtbGUgNTg1MzUpIH0gfAogICAgICAgICAgICAgICAgU2VsZWN0LU9iamVjdCAtRXhwYW5kUHJvcGVydHkgTG9jYWxQb3J0IC1VbmlxdWUKICAgICAgICAgICAgZm9yZWFjaCAoJHAgaW4gJGxpc3RlbmVycykgewogICAgICAgICAgICAgICAgaWYgKCRjYW5kaWRhdGVXc2FQb3J0cyAtbm90Y29udGFpbnMgJHApIHsKICAgICAgICAgICAgICAgICAgICAkY2FuZGlkYXRlV3NhUG9ydHMgPSBAKCRwKSArICRjYW5kaWRhdGVXc2FQb3J0cwogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9CiAgICAgICAgfSBjYXRjaCB7fQoKICAgICAgICAjIEZhc3QgVENQIHByZS1jaGVjayAoODBtcyk6IG9ubHkgY2FsbCAnYWRiIGNvbm5lY3QnIGlmIHRoZSBwb3J0IGlzIGFjdHVhbGx5IG9wZW4KICAgICAgICAjIFRoaXMgY29tcGxldGVseSBhdm9pZHMgYmxvY2tpbmcgZm9yIDUtMTAgc2Vjb25kcyBwZXIgcG9ydCEKICAgICAgICAkd3NhQ29ubmVjdGVkID0gJGZhbHNlCiAgICAgICAgZm9yZWFjaCAoJHdzYVBvcnQgaW4gJGNhbmRpZGF0ZVdzYVBvcnRzKSB7CiAgICAgICAgICAgICRpc09wZW4gPSAkZmFsc2UKICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgICR0Y3BUZXN0ID0gTmV3LU9iamVjdCBTeXN0ZW0uTmV0LlNvY2tldHMuVGNwQ2xpZW50CiAgICAgICAgICAgICAgICAkaWFyID0gJHRjcFRlc3QuQmVnaW5Db25uZWN0KCIxMjcuMC4wLjEiLCAkd3NhUG9ydCwgJG51bGwsICRudWxsKQogICAgICAgICAgICAgICAgaWYgKCRpYXIuQXN5bmNXYWl0SGFuZGxlLldhaXRPbmUoODAsICRmYWxzZSkpIHsKICAgICAgICAgICAgICAgICAgICAkdGNwVGVzdC5FbmRDb25uZWN0KCRpYXIpCiAgICAgICAgICAgICAgICAgICAgJGlzT3BlbiA9ICR0cnVlCiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgICAgICAkdGNwVGVzdC5DbG9zZSgpCiAgICAgICAgICAgIH0gY2F0Y2gge30KCiAgICAgICAgICAgIGlmICgkaXNPcGVuKSB7CiAgICAgICAgICAgICAgICAkdGFyZ2V0RW5kcG9pbnQgPSAiMTI3LjAuMC4xOiR3c2FQb3J0IgogICAgICAgICAgICAgICAgJGNvbm5PdXQgPSAmICRhZGJDbWQgY29ubmVjdCAkdGFyZ2V0RW5kcG9pbnQgMj4mMQogICAgICAgICAgICAgICAgaWYgKCRjb25uT3V0IC1tYXRjaCAiY29ubmVjdGVkIHRvIiAtYW5kICRjb25uT3V0IC1ub3RtYXRjaCAiY2Fubm90IGNvbm5lY3QiIC1hbmQgJGNvbm5PdXQgLW5vdG1hdGNoICJmYWlsZWQiKSB7CiAgICAgICAgICAgICAgICAgICAgJG9ubGluZVNlcmlhbHMgKz0gJHRhcmdldEVuZHBvaW50CiAgICAgICAgICAgICAgICAgICAgJHdzYUNvbm5lY3RlZCA9ICR0cnVlCiAgICAgICAgICAgICAgICAgICAgTG9nLUJpIEdyZWVuICJbV1NBXSBTdWNjZXNzZnVsbHkgY29ubmVjdGVkIHRvIFdTQSBBREIgKCR0YXJnZXRFbmRwb2ludCkuIiAiVjFOQklFRkVRdU9CcU9PQnJ1YU9wZWUybXVPQnErYUlrT1dLbitPQmwrT0J2dU9CbCtPQm4rT0FnaUE9IgogICAgICAgICAgICAgICAgICAgIGJyZWFrCiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgIH0KICAgICAgICB9CgogICAgICAgIGlmICghJHdzYUNvbm5lY3RlZCAtYW5kICRudWxsIC1uZSAkd3NhUnVubmluZyAtYW5kICR3c2FSdW5uaW5nLkNvdW50IC1ndCAwKSB7CiAgICAgICAgICAgIExvZy1CaSBZZWxsb3cgIltXU0FdIFdTQSAoV2luZG93cyBTdWJzeXN0ZW0gZm9yIEFuZHJvaWQpIGRldGVjdGVkLCBidXQgQURCIHBvcnQgaXMgbm90IG9wZW4uIiAiVjJsdVpHOTNjeUJUZFdKemVYTjBaVzBnWm05eUlFRnVaSEp2YVdRZ0tGZFRRU2tnNDRLUzVxU2M1WWU2NDRHWDQ0Rys0NEdYNDRHZjQ0R000NENCUVVSQzQ0T2Q0NE84NDRPSTQ0R002WmFMNDRHRTQ0R200NEdFNDRHKzQ0R2I0NEtUNDRDQyIKICAgICAgICAgICAgTG9nLUJpIFllbGxvdyAiICAgICAgT3BlbiAnV2luZG93cyBTdWJzeXN0ZW0gZm9yIEFuZHJvaWQgU2V0dGluZ3MnIC0+ICdEZXZlbG9wZXInIC0+IGVuYWJsZSAnRGV2ZWxvcGVyIG1vZGUnLiIgIlYxTkI2S2l0NWE2YTQ0S2k0NE9YNDRPcTQ0R3U0NENNNlphTDU1bTY2SUNGNDRDTjQ0Sy80NE9XNDRHbjQ0Q002WmFMNTVtNjZJQ0Y0NE9pNDRPRDQ0T0o0NENONDRLUzQ0S3E0NE96NDRHcjQ0R1g0NEdtNDRHUDQ0R2c0NEdWNDRHRTQ0Q0MiCiAgICAgICAgfQogICAgfQoKICAgIGlmICgkb25saW5lU2VyaWFscy5Db3VudCAtZXEgMCkgewogICAgICAgICRzY3JpcHQ6Y29uc2VjdXRpdmVXYWl0Q291bnQrKwogICAgICAgICMgSWYgd2FpdGluZyByZXBlYXRlZGx5IHdpdGhvdXQgZmluZGluZyBkZXZpY2UgKGUuZy4gYWZ0ZXIgVVNCIHJlY29ubmVjdCB3aGVuIGFub3RoZXIgcHJvZ3JhbSBncmFiYmVkIEFEQik6CiAgICAgICAgIyBSdW4gYWRiIGtpbGwtc2VydmVyIHRvIHJlc2V0IEFEQiBzdGF0ZQogICAgICAgIGlmICgkc2NyaXB0OmNvbnNlY3V0aXZlV2FpdENvdW50IC1nZSAzKSB7CiAgICAgICAgICAgIExvZy1CaSBZZWxsb3cgIltBREJdIERldmljZSBub3QgZGV0ZWN0ZWQgYWZ0ZXIgcmVjb25uZWN0LiBSZXNldHRpbmcgQURCIChhZGIga2lsbC1zZXJ2ZXIpLi4uIiAiNTZ1djVweXI0NEdNNktxTjZLMlk0NEdWNDRLTTQ0R3E0NEdFNDRHTDVZaUg1cGF0NDRHVjQ0S000NEcrNDRHWDQ0R2Y0NENDWVdSaUlHdHBiR3d0YzJWeWRtVnlJT09Da3VXdW4raWhqT09CbCtPQnB1V0dqZWlwcHVpaGpPUzRyUzR1TGc9PSIKICAgICAgICAgICAgUmVzZXQtQWRiU2VydmVyICJSZWNvbm5lY3QgcmV0cnkiCiAgICAgICAgICAgICRzY3JpcHQ6Y29uc2VjdXRpdmVXYWl0Q291bnQgPSAwCiAgICAgICAgICAgIHJldHVybiAkZmFsc2UKICAgICAgICB9CgogICAgICAgIGlmICgkdW5hdXRob3JpemVkRm91bmQpIHsKICAgICAgICAgICAgTG9nLUJpIFllbGxvdyAiW1dBSVRdIEFuZHJvaWQgZGV2aWNlIGRldGVjdGVkLCBidXQgdW5hdXRob3JpemVkLiIgIlFXNWtjbTlwWk9lcnIrYWNxK09Cak9ha25PV0h1dU9CbGVPQ2pPT0J2dU9CbCtPQm4rT0JqT09BZ2VhY3F1aW9zZVdQcitPQnArT0JtZU9BZ2c9PSIKICAgICAgICAgICAgTG9nLUJpIFllbGxvdyAiICAgICAgIFBsZWFzZSB1bmxvY2sgcGhvbmUgc2NyZWVuIGFuZCB0YXAgJ0FsbG93IFVTQiBkZWJ1Z2dpbmcnLiIgIjQ0SzU0NE9lNDRPYjU1Uzc2WjJpNDRHdTQ0T3Q0NE9ENDRLdjQ0S1M2S2VqNlptazQ0R1g0NENCNDRDTVZWTkM0NE9INDRPUTQ0T0Q0NEt3NDRLUzZLaXg1WSt2NDRDTjQ0S1M0NEsvNDRPRDQ0T1g0NEdYNDRHbTQ0R1A0NEdnNDRHVjQ0R0U0NENDIgogICAgICAgIH0gZWxzZSB7CiAgICAgICAgICAgIExvZy1CaSBZZWxsb3cgIltXQUlUXSBObyBBbmRyb2lkIGRldmljZSBkZXRlY3RlZC4iICJRVzVrY205cFpPZXJyK2FjcStPQmpPaW1pK09CcE9PQmkrT0NpdU9CdnVPQm0rT0NrK09BZ2c9PSIKICAgICAgICAgICAgTG9nLUJpIFllbGxvdyAiICAgICAgIDEuIENvbm5lY3QgcGhvbmUgdG8gUEMgdmlhIFVTQiBjYWJsZSAob3Igb3BlbiBXU0EpLiIgIk1TNGc0NEs1NDRPZTQ0T2I0NEtTVlZOQzQ0S3g0NE84NDRPVzQ0T3I0NEduVUVQamdhdm1qcVhudHByamdaZmpnYWJqZ1kvamdhRGpnWlhqZ1lRZ0tPV1FpT2FJa0ZkVFFTaz0iCiAgICAgICAgICAgIExvZy1CaSBZZWxsb3cgIiAgICAgICAyLiBFbmFibGUgJ1VTQiBkZWJ1Z2dpbmcnIGluIERldmVsb3BlciBvcHRpb25zLiIgIk1pNGc1NnV2NXB5cjQ0R3U2WmFMNTVtNjZJQ0Y1WkNSNDRHUjQ0S3E0NE9YNDRLMzQ0T240NE96NDRHbjQ0Q01WVk5DNDRPSDQ0T1E0NE9ENDRLdzQ0Q040NEtTVDA3amdhdmpnWmZqZ2FiamdZL2pnYURqZ1pYamdZVGpnSUk9IgogICAgICAgIH0KICAgICAgICByZXR1cm4gJGZhbHNlCiAgICB9CgogICAgJHNjcmlwdDpjb25zZWN1dGl2ZVdhaXRDb3VudCA9IDAKCiAgICAjIFBpY2sgYmVzdCB0YXJnZXQgc2VyaWFsCiAgICAkY2hvc2VuU2VyaWFsID0gJG9ubGluZVNlcmlhbHNbMF0KICAgIAogICAgIyBDaGVjayBpZiBwb3J0IGZvcndhcmQgaXMgYWxyZWFkeSBhY3RpdmUgZm9yIGNob3NlbiBkZXZpY2UgYW5kIHRhcmdldCBwb3J0CiAgICAkZXhpc3RpbmdGd2QgPSAmICRhZGJDbWQgZm9yd2FyZCAtLWxpc3QgMj4mMQogICAgJGlzQWxyZWFkeUZvcndhcmRlZCA9ICRmYWxzZQogICAgZm9yZWFjaCAoJGZ3ZExpbmUgaW4gJGV4aXN0aW5nRndkKSB7CiAgICAgICAgaWYgKCRmd2RMaW5lIC1tYXRjaCBbcmVnZXhdOjpFc2NhcGUoJGNob3NlblNlcmlhbCkgLWFuZCAkZndkTGluZSAtbWF0Y2ggInRjcDokdGFyZ2V0UG9ydFxzK3RjcDokdGFyZ2V0UG9ydCIpIHsKICAgICAgICAgICAgJGlzQWxyZWFkeUZvcndhcmRlZCA9ICR0cnVlCiAgICAgICAgICAgIGJyZWFrCiAgICAgICAgfQogICAgfQoKICAgIGlmICghJGlzQWxyZWFkeUZvcndhcmRlZCkgewogICAgICAgIHRyeSB7ICYgJGFkYkNtZCAtcyAkY2hvc2VuU2VyaWFsIGZvcndhcmQgLS1yZW1vdmUgInRjcDokdGFyZ2V0UG9ydCIgMj4kbnVsbCB9IGNhdGNoIHt9CiAgICAgICAgJGZ3ZE91dCA9ICYgJGFkYkNtZCAtcyAkY2hvc2VuU2VyaWFsIGZvcndhcmQgInRjcDokdGFyZ2V0UG9ydCIgInRjcDokdGFyZ2V0UG9ydCIgMj4mMQogICAgICAgIGlmICgkTEFTVEVYSVRDT0RFIC1uZSAwKSB7CiAgICAgICAgICAgIFdyaXRlLUhvc3QgIltOT1RJQ0VdIEFEQiBQb3J0IEZvcndhcmRpbmcgbm90aWNlOiAkZndkT3V0IiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgICAgICAgICByZXR1cm4gJGZhbHNlCiAgICAgICAgfQogICAgfQogICAgcmV0dXJuICR0cnVlCn0KCkxvZy1CaSBDeWFuICJSZWFkeS4gU3RhcnRpbmcgYXV0by1jb25uZWN0aW9uIGxvb3AgKHRhcmdldCBwb3J0OiAkcG9ydCkuLi4iICI1cnFXNVlLWjVhNk01THFHNDRDQzZJZXE1WXVWNW82bDU3YWE0NE9yNDRPODQ0T1g0NEtTNlphTDVhZUw0NEdYNDRHKzQ0R1oiCgokbGFzdEZvcndhcmRPayA9ICRmYWxzZQoKd2hpbGUgKCR0cnVlKSB7CiAgICAkY2xpZW50ID0gJG51bGwKICAgICRzdHJlYW0gPSAkbnVsbAogICAgJHJlYWRlciA9ICRudWxsCiAgICAkY29ubmVjdGVkQW5ub3VuY2VkID0gJGZhbHNlCiAgICB0cnkgewogICAgICAgICMgQ2hlY2sgYW5kIGVuc3VyZSBBREIgcG9ydCBmb3J3YXJkIGlzIGFjdGl2ZQogICAgICAgICRmb3J3YXJkT2sgPSBFbnN1cmUtQWRiRm9yd2FyZCAtdGFyZ2V0UG9ydCAkcG9ydAogICAgICAgIGlmICghJGZvcndhcmRPaykgewogICAgICAgICAgICAkbGFzdEZvcndhcmRPayA9ICRmYWxzZQogICAgICAgICAgICBTdGFydC1TbGVlcCAtU2Vjb25kcyAyCiAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgfQoKICAgICAgICBpZiAoISRsYXN0Rm9yd2FyZE9rKSB7CiAgICAgICAgICAgIExvZy1CaSBHcmVlbiAiW09LXSBBREIgcG9ydCBmb3J3YXJkaW5nIGFjdGl2ZSAocG9ydCAkcG9ydCkuIiAiUVVSQzQ0T2Q0NE84NDRPSTQ0T1Y0NEtwNDRPdjQ0Tzg0NE9KNTZLNjU2dUxJQ2pqZzUzamc3empnNGdnSkhCdmNuUXAiCiAgICAgICAgICAgIExvZy1CaSBDeWFuICJbSU5GT10gSW4gQW5kcm9pZCBhcHAsIHNlbGVjdCAnUEMgQ29ubmVjdGlvbiAoVVNCKScgbW9kZS4uLiIgIjQ0SzU0NE9lNDRPYjQ0S2k0NE9YNDRPcTQ0R240NENNVUVQbWpxWG50cG9nS0ZWVFFpbmpnSTNqZzZMamc3empnNG5qZ3BMcGdiam1pcDdqZ1pmamdhYmpnWS9qZ2FEamdaWGpnWVF1TGk0PSIKICAgICAgICAgICAgJGxhc3RGb3J3YXJkT2sgPSAkdHJ1ZQogICAgICAgIH0KCiAgICAgICAgIyBBdHRlbXB0IFRDUCBjb25uZWN0aW9uIHRvIEFuZHJvaWQgYXBwIHZpYSBmb3J3YXJkZWQgbG9jYWxob3N0IHBvcnQKICAgICAgICAkY2xpZW50ID0gTmV3LU9iamVjdCBTeXN0ZW0uTmV0LlNvY2tldHMuVGNwQ2xpZW50CiAgICAgICAgJGNsaWVudC5Ob0RlbGF5ID0gJHRydWUKICAgICAgICAkY29ubmVjdFJlc3VsdCA9ICRjbGllbnQuQmVnaW5Db25uZWN0KCIxMjcuMC4wLjEiLCAkcG9ydCwgJG51bGwsICRudWxsKQogICAgICAgICRjb25uZWN0U3VjY2VzcyA9ICRjb25uZWN0UmVzdWx0LkFzeW5jV2FpdEhhbmRsZS5XYWl0T25lKDQwMDAsICRmYWxzZSkKICAgICAgICBpZiAoISRjb25uZWN0U3VjY2VzcykgewogICAgICAgICAgICAkY2xpZW50LkNsb3NlKCkKICAgICAgICAgICAgU3RhcnQtU2xlZXAgLVNlY29uZHMgMQogICAgICAgICAgICBjb250aW51ZQogICAgICAgIH0KICAgICAgICAkY2xpZW50LkVuZENvbm5lY3QoJGNvbm5lY3RSZXN1bHQpCgogICAgICAgICRzdHJlYW0gPSAkY2xpZW50LkdldFN0cmVhbSgpCiAgICAgICAgJHJlYWRlciA9IE5ldy1PYmplY3QgU3lzdGVtLklPLlN0cmVhbVJlYWRlcigkc3RyZWFtLCBbU3lzdGVtLlRleHQuRW5jb2RpbmddOjpVVEY4KQoKICAgICAgICB3aGlsZSAoJHRydWUpIHsKICAgICAgICAgICAgJGxpbmUgPSAkcmVhZGVyLlJlYWRMaW5lKCkKICAgICAgICAgICAgaWYgKCRudWxsIC1lcSAkbGluZSkgewogICAgICAgICAgICAgICAgaWYgKCRjb25uZWN0ZWRBbm5vdW5jZWQpIHsKICAgICAgICAgICAgICAgICAgICBMb2ctQmkgWWVsbG93ICJbSU5GT10gQ29ubmVjdGlvbiBjbG9zZWQgYnkgQW5kcm9pZCBhcHAuIFdhaXRpbmcgdG8gcmVjb25uZWN0Li4uIiAiNDRLaTQ0T1g0NE9xNDRHbzQ0R3U1bzZsNTdhYTQ0R001WWlINXBhdDQ0R1Y0NEtNNDRHKzQ0R1g0NEdmNDRDQzVZYU41bzZsNTdhYTViNkY1cW1mNUxpdExpNHUiCiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgICAgICBicmVhawogICAgICAgICAgICB9CiAgICAgICAgICAgIAogICAgICAgICAgICAkbGluZSA9ICRsaW5lLlRyaW0oKQogICAgICAgICAgICBpZiAoJGxpbmUuTGVuZ3RoIC1lcSAwKSB7IGNvbnRpbnVlIH0KCiAgICAgICAgICAgIGlmICgkbGluZSAtZXEgIk9LIiAtb3IgJGxpbmUgLWVxICJQSU5HIikgewogICAgICAgICAgICAgICAgaWYgKCEkY29ubmVjdGVkQW5ub3VuY2VkKSB7CiAgICAgICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiIgogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT0iIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICAgICAgICAgICAgICBMb2ctQmkgR3JlZW4gIiAqKiogVGFpa28gQ29udHJvbGxlciBDb25uZWN0ZWQgU3VjY2Vzc2Z1bGx5ISAqKioiICI0cGlGNHBpRjRwaUZJT1drcXVtOGsrT0NzK09EcytPRGlPT0RyZU9Edk9PRHFlT0R2Q0FvNDRLaTQ0T1g0NE9xS1NEamdham1qcVhudHBybHJvemt1b2J2dklFZzRwaUY0cGlGNHBpRiIKICAgICAgICAgICAgICAgICAgICBMb2ctQmkgR3JlZW4gIiBTZW5kaW5nIGtleXMgKEQgLyBGIC8gSiAvIEspIHRvIFBDIGdhbWVzIGluIHJlYWwtdGltZS4iICJVRVBqZ3JMamc3empnNkRqZ2JqamdxM2pnN3pqZ3BMamc2cmpncUxqZzZ2amdyL2pncVRqZzZEcGdJSGt2NkhqZ1pmamdiN2pnWm5qZ0lJPSIKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICI9PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09IiAtRm9yZWdyb3VuZENvbG9yIEdyZWVuCiAgICAgICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiIgogICAgICAgICAgICAgICAgICAgICRjb25uZWN0ZWRBbm5vdW5jZWQgPSAkdHJ1ZQogICAgICAgICAgICAgICAgfQogICAgICAgICAgICAgICAgY29udGludWUKICAgICAgICAgICAgfQoKICAgICAgICAgICAgJHBhcnRzID0gJGxpbmUuU3BsaXQoJyAnKQogICAgICAgICAgICBpZiAoJHBhcnRzLkxlbmd0aCAtZ2UgMikgewogICAgICAgICAgICAgICAgaWYgKCEkY29ubmVjdGVkQW5ub3VuY2VkKSB7CiAgICAgICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiIgogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT0iIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICAgICAgICAgICAgICBMb2ctQmkgR3JlZW4gIiAqKiogVGFpa28gQ29udHJvbGxlciBDb25uZWN0ZWQgU3VjY2Vzc2Z1bGx5ISAqKioiICI0cGlGNHBpRjRwaUZJT1drcXVtOGsrT0NzK09EcytPRGlPT0RyZU9Edk9PRHFlT0R2Q0FvNDRLaTQ0T1g0NE9xS1NEamdham1qcVhudHBybHJvemt1b2J2dklFZzRwaUY0cGlGNHBpRiIKICAgICAgICAgICAgICAgICAgICBMb2ctQmkgR3JlZW4gIiBTZW5kaW5nIGtleXMgKEQgLyBGIC8gSiAvIEspIHRvIFBDIGdhbWVzIGluIHJlYWwtdGltZS4iICJVRVBqZ3JMamc3empnNkRqZ2JqamdxM2pnN3pqZ3BMamc2cmpncUxqZzZ2amdyL2pncVRqZzZEcGdJSGt2NkhqZ1pmamdiN2pnWm5qZ0lJPSIKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICI9PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09IiAtRm9yZWdyb3VuZENvbG9yIEdyZWVuCiAgICAgICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiIgogICAgICAgICAgICAgICAgICAgICRjb25uZWN0ZWRBbm5vdW5jZWQgPSAkdHJ1ZQogICAgICAgICAgICAgICAgfQoKICAgICAgICAgICAgICAgICRhY3Rpb24gPSAkcGFydHNbMF0KICAgICAgICAgICAgICAgIGZvciAoJGkgPSAxOyAkaSAtbHQgJHBhcnRzLkxlbmd0aDsgJGkrKykgewogICAgICAgICAgICAgICAgICAgICRrZXkgPSAkcGFydHNbJGldLlRvVXBwZXIoKQogICAgICAgICAgICAgICAgICAgIGlmICgka2V5Lkxlbmd0aCAtZ3QgMCkgewogICAgICAgICAgICAgICAgICAgICAgICAkdmtleSA9IFtieXRlXVtjaGFyXSRrZXlbMF0KICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICMgSW5zdGFudCBpbmplY3Rpb24gZmlyc3QgZm9yIHN1Yi1taWxsaXNlY29uZCBnYW1lIHJlc3BvbnNlCiAgICAgICAgICAgICAgICAgICAgICAgIGlmICgkYWN0aW9uIC1lcSAiRE9XTiIpIHsKICAgICAgICAgICAgICAgICAgICAgICAgICAgIFtUYWlrb0tleWJvYXJkXTo6RG93bigkdmtleSkKICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlaWYgKCRhY3Rpb24gLWVxICJVUCIpIHsKICAgICAgICAgICAgICAgICAgICAgICAgICAgIFtUYWlrb0tleWJvYXJkXTo6VXAoJHZrZXkpCiAgICAgICAgICAgICAgICAgICAgICAgIH0KCiAgICAgICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIltLRVldICRhY3Rpb24gLT4gJGtleSIgLUZvcmVncm91bmRDb2xvciBDeWFuCiAgICAgICAgICAgICAgICAgICAgfQogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9CiAgICAgICAgfQogICAgfSBjYXRjaCB7CiAgICAgICAgaWYgKCRjb25uZWN0ZWRBbm5vdW5jZWQpIHsKICAgICAgICAgICAgJGVyck1zZyA9ICRfLkV4Y2VwdGlvbi5NZXNzYWdlCiAgICAgICAgICAgIExvZy1CaSBZZWxsb3cgIltJTkZPXSBDb25uZWN0aW9uIGRyb3BwZWQgKCRlcnJNc2cpLiBSZWNvbm5lY3RpbmcuLi4iICI0NEs3NDRPRDQ0SzM0NE9uNDRPejQ0R001WWlINXBhdDQ0R1Y0NEtNNDRHKzQ0R1g0NEdmNDRDQzVZYU41bzZsNTdhYTQ0R1g0NEcrNDRHWkxpNHUiCiAgICAgICAgfQogICAgfSBmaW5hbGx5IHsKICAgICAgICAkbGFzdEZvcndhcmRPayA9ICRmYWxzZQogICAgICAgIGlmICgkbnVsbCAtbmUgJHJlYWRlcikgeyB0cnkgeyAkcmVhZGVyLkNsb3NlKCkgfSBjYXRjaCB7fSB9CiAgICAgICAgaWYgKCRudWxsIC1uZSAkc3RyZWFtKSB7IHRyeSB7ICRzdHJlYW0uQ2xvc2UoKSB9IGNhdGNoIHt9IH0KICAgICAgICBpZiAoJG51bGwgLW5lICRjbGllbnQpIHsgdHJ5IHsgJGNsaWVudC5DbG9zZSgpIH0gY2F0Y2gge30gfQogICAgfQogICAgU3RhcnQtU2xlZXAgLVNlY29uZHMgMQp9Cg==", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)

            var activeScriptTab by remember { mutableStateOf(0) } // 0 = Windows, 1 = macOS, 2 = Linux
            var showManualScriptCopy by remember { mutableStateOf(false) }
            val clipboardManager = LocalClipboardManager.current
            val uriHandler = LocalUriHandler.current
            val scriptText = when (activeScriptTab) {
                0 -> powerShellScript
                1 -> macOSScript
                else -> linuxScript
            }

            CollapsibleSettingCard(
                title = "💻 USB PC接続設定",
                subtitle = if (pcClientsCount > 0) "${pcClientsCount}台のPC接続中" else "PC接続待機中 (ポート60001)",
                badgeText = if (pcClientsCount > 0) "接続中" else "待機中",
                badgeColor = if (pcClientsCount > 0) Color(0xFFD1FAE5) else Color(0xFFFEF3C7),
                badgeTextColor = if (pcClientsCount > 0) Color(0xFF065F46) else Color(0xFF92400E),
                isExpanded = expandUsbCard,
                onExpandedChange = { expandUsbCard = it },
                isDarkTheme = isDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "端末をUSBケーブルでPCに接続し、PC側からADBポートフォワーディングを行うことで、PC上のゲームへ超低遅延・root化不要で入力を送信します。",
                        fontSize = 10.sp,
                        color = if (isDark) Color.White else Color.DarkGray
                    )

                    // PC Connection Status indicator
                    val isConnected = pcClientsCount > 0
                    val statusBg = if (isConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7))
                    val statusTxt = if (isConnected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E))
                    val statusLabel = if (isConnected) {
                        "PC接続状態: 接続完了 (${pcClientsCount}台のPCが接続中)"
                    } else {
                        "PC接続状態: 接続待機中 (TCPサーバー起動中: ポート 60001)"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusTxt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                    Column {
                        Text("USB接続時入力形式: ⌨️ キーボード (固定)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                        Text(
                            text = "※ USB PC接続時はPC側スクリプトへキーボード入力のみ送信されます。",
                            fontSize = 10.sp,
                            color = if (isDark) Color.White else Color.DarkGray
                        )
                    }

                    Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                    Column {
                        Text(
                            text = "📖 接続手順 (推奨: GitHub Releases からダウンロード):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. 端末の「USBデバッグ」を有効にしてPCにUSB接続します。\n" +
                                   "2. 下のボタンからGitHub Releasesを開き、お使いのPC環境に合わせたスクリプトをダウンロードします：\n" +
                                   "   ・Windows用: TTC-receiver-windows.ps1\n" +
                                   "   ・macOS用: TTC-receiver-macos.command\n" +
                                   "   ・Linux用: TTC-receiver-linux.sh\n" +
                                   "3. ダウンロードしたスクリプトを実行すると、ADB環境の自動構築・ポート転送・キー入力ツールの準備・接続まで全自動で行われます！\n" +
                                   "   ※ Windows版: スクリプトを右クリックして「PowerShell で実行」を選択します。\n" +
                                   "   ※ macOS / Linux版: 初回実行時にファイルの実行権限（chmod +x）の設定が必要です。\n" +
                                   "   ※ macOS版: 初回実行時にキー入力送信のためアクセシビリティ権限の許可が必要です。\n\n" +
                                   "※ テキストから手動でコピーして作成する場合は、下の「手動作成用スクリプトを表示」を展開してください。",
                            fontSize = 10.sp,
                            color = if (isDark) Color.White else Color.DarkGray,
                            lineHeight = 14.sp
                        )
                    }

                    // GitHub Releases Download Button
                    Button(
                        onClick = {
                            uriHandler.openUri("https://github.com/Sango916/TaikoTouchController-Android/releases/latest")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF374151) else Color(0xFF1F2937),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp)
                    ) {
                        Text(
                            text = "📥 GitHub Releases からスクリプトをダウンロード",
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                    // Collapsible manual script copy section
                    OutlinedButton(
                        onClick = { showManualScriptCopy = !showManualScriptCopy },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF78350F).invertIfDark(isDark)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.25f).invertIfDark(isDark)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📋 テキストからスクリプトを手動コピーして作成する",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showManualScriptCopy) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showManualScriptCopy) "閉じる" else "展開する",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showManualScriptCopy) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Script tabs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf("Windows (.ps1)", "macOS (.command)", "Linux (.sh)")
                                tabs.forEachIndexed { index, label ->
                                    val isSelected = activeScriptTab == index
                                    Button(
                                        onClick = { activeScriptTab = index },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                                            contentColor = if (isSelected) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 32.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Code Viewer box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark))
                                    .border(1.dp, Color(0xFF78350F).copy(alpha = 0.1f).invertIfDark(isDark), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = scriptText,
                                        color = Color(0xFF4B2E1E).invertIfDark(isDark),
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        lineHeight = 12.sp
                                    )
                                }
                            }

                            // Copy Button
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scriptText))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF78350F).invertIfDark(isDark),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy script",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val scriptFileName = when (activeScriptTab) {
                                    0 -> "TTC-receiver-windows.ps1"
                                    1 -> "TTC-receiver-macos.command"
                                    else -> "TTC-receiver-linux.sh"
                                }
                                Text(
                                    text = "$scriptFileName の内容をコピー",
                                    fontSize = 11.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }


        // --- Taiko Size & Position Adjustment Card (太鼓の位置・サイズ設定) ---
        TaikoSizeSettingCard(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            expandSizeCard = expandSizeCard,
            onExpandSizeCardChange = { expandSizeCard = it },
            isDark = isDark
        )

        // --- 3. Drum Settings Options (太鼓の動作設定) ---
        CollapsibleSettingCard(
            title = "🥁 太鼓の動作設定 (振動・大音符・ログ)",
            subtitle = "バイブ: ${if (settings.vibration) "${settings.vibrationStrengthPercent}%" else "OFF"} / 大音符DS: ${if (settings.singleHandBigNotes) "ON" else "OFF"}",
            isExpanded = expandDrumCard,
            onExpandedChange = { expandDrumCard = it },
            isDarkTheme = isDark
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Vibration switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "バイブレーション",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Text(
                            text = "太鼓を叩くと振動し、迫力のある演奏体験を実現します",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = if (isDark) Color.White else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.vibration,
                        onCheckedChange = { onSettingsChanged(settings.copy(vibration = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF78350F).invertIfDark(isDark),
                            checkedTrackColor = Color(0xFFFED7AA).invertIfDark(isDark)
                        )
                    )
                }

                if (settings.vibration) {
                    Spacer(modifier = Modifier.height(10.dp))

                    val currentVibe = settings.vibrationStrengthPercent
                    var vibeText by remember(currentVibe) { mutableStateOf(currentVibe.toString()) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "バイブレーションの強さ: ${currentVibe}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F).invertIfDark(isDark)
                            )

                            // Reset button
                            OutlinedButton(
                                onClick = {
                                    onSettingsChanged(settings.copy(vibrationStrengthPercent = 100))
                                },
                                border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.3f).invertIfDark(isDark)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("デフォルト", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val newVibe = (currentVibe - 10).coerceIn(0, 200)
                                        onSettingsChanged(settings.copy(vibrationStrengthPercent = newVibe))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("-10%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val newVibe = (currentVibe + 10).coerceIn(0, 200)
                                        onSettingsChanged(settings.copy(vibrationStrengthPercent = newVibe))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+10%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = vibeText,
                                    onValueChange = { newValue ->
                                        vibeText = newValue
                                        val parsed = newValue.toIntOrNull()
                                        if (parsed != null) {
                                            val clamped = parsed.coerceIn(0, 200)
                                            onSettingsChanged(settings.copy(vibrationStrengthPercent = clamped))
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(70.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                    colors = customTextFieldColors(isDark)
                                )
                                Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                // Single Hand Big Note Assist Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "大音符DS風モード",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Text(
                            text = "太鼓の中心/フチ端をタップ時、自動で両手同時押しに変換",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = if (isDark) Color.White else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.singleHandBigNotes,
                        onCheckedChange = { onSettingsChanged(settings.copy(singleHandBigNotes = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF78350F).invertIfDark(isDark),
                            checkedTrackColor = Color(0xFFFED7AA).invertIfDark(isDark)
                        )
                    )
                }

                // Big Note DS Detection Area Adjustment (面とフチそれぞれの広さ設定: 縦横別)
                if (settings.singleHandBigNotes) {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    val orientationLabel = if (isLandscape) "横画面" else "縦画面"
                    val currentDonBig = if (isLandscape) settings.landscapeDonBigNotePercent else settings.portraitDonBigNotePercent
                    var donBigText by remember(currentDonBig) { mutableStateOf(currentDonBig.toString()) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎯 大音符DS 判定の広さ設定",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F).invertIfDark(isDark)
                            )
                            Surface(
                                color = if (isDark) Color(0xFF78350F) else Color(0xFFFED7AA),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = orientationLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF78350F),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 1. 面 (ドン) の大音符判定の広さ
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔴 面 (ドン) の判定範囲: ${currentDonBig}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F).invertIfDark(isDark)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (isLandscape) {
                                            onSettingsChanged(settings.copy(landscapeDonBigNotePercent = 40, donBigNotePercent = 40))
                                        } else {
                                            onSettingsChanged(settings.copy(portraitDonBigNotePercent = 40, donBigNotePercent = 40))
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.3f).invertIfDark(isDark)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("初期値 (40%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val newVal = (currentDonBig - 5).coerceIn(10, 300)
                                            if (isLandscape) {
                                                onSettingsChanged(settings.copy(landscapeDonBigNotePercent = newVal, donBigNotePercent = newVal))
                                            } else {
                                                onSettingsChanged(settings.copy(portraitDonBigNotePercent = newVal, donBigNotePercent = newVal))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("-5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val newVal = (currentDonBig + 5).coerceIn(10, 300)
                                            if (isLandscape) {
                                                onSettingsChanged(settings.copy(landscapeDonBigNotePercent = newVal, donBigNotePercent = newVal))
                                            } else {
                                                onSettingsChanged(settings.copy(portraitDonBigNotePercent = newVal, donBigNotePercent = newVal))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = donBigText,
                                        onValueChange = { newValue ->
                                            donBigText = newValue
                                            val parsed = newValue.toIntOrNull()
                                            if (parsed != null) {
                                                val clamped = parsed.coerceIn(10, 300)
                                                if (isLandscape) {
                                                    onSettingsChanged(settings.copy(landscapeDonBigNotePercent = clamped, donBigNotePercent = clamped))
                                                } else {
                                                    onSettingsChanged(settings.copy(portraitDonBigNotePercent = clamped, donBigNotePercent = clamped))
                                                }
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(65.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                        colors = customTextFieldColors(isDark)
                                    )
                                    Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = Color(0xFF78350F).copy(alpha = 0.1f).invertIfDark(isDark))

                        // 2. フチ (カッ) の大音符判定の広さ
                        val currentKatBig = if (isLandscape) settings.landscapeKatBigNotePercent else settings.portraitKatBigNotePercent
                        var katBigText by remember(currentKatBig) { mutableStateOf(currentKatBig.toString()) }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔵 フチ (カッ) の判定範囲: ${currentKatBig}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F).invertIfDark(isDark)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (isLandscape) {
                                            onSettingsChanged(settings.copy(landscapeKatBigNotePercent = 100, katBigNotePercent = 100))
                                        } else {
                                            onSettingsChanged(settings.copy(portraitKatBigNotePercent = 100, katBigNotePercent = 100))
                                        }
                                    },
                                    border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.3f).invertIfDark(isDark)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("初期値 (100%)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            val newVal = (currentKatBig - 5).coerceIn(10, 300)
                                            if (isLandscape) {
                                                onSettingsChanged(settings.copy(landscapeKatBigNotePercent = newVal, katBigNotePercent = newVal))
                                            } else {
                                                onSettingsChanged(settings.copy(portraitKatBigNotePercent = newVal, katBigNotePercent = newVal))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("-5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            val newVal = (currentKatBig + 5).coerceIn(10, 300)
                                            if (isLandscape) {
                                                onSettingsChanged(settings.copy(landscapeKatBigNotePercent = newVal, katBigNotePercent = newVal))
                                            } else {
                                                onSettingsChanged(settings.copy(portraitKatBigNotePercent = newVal, katBigNotePercent = newVal))
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("+5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = katBigText,
                                        onValueChange = { newValue ->
                                            katBigText = newValue
                                            val parsed = newValue.toIntOrNull()
                                            if (parsed != null) {
                                                val clamped = parsed.coerceIn(10, 300)
                                                if (isLandscape) {
                                                    onSettingsChanged(settings.copy(landscapeKatBigNotePercent = clamped, katBigNotePercent = clamped))
                                                } else {
                                                    onSettingsChanged(settings.copy(portraitKatBigNotePercent = clamped, katBigNotePercent = clamped))
                                                }
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(65.dp),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                        colors = customTextFieldColors(isDark)
                                    )
                                    Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(
                            text = "※ 面は中心から指定%内、フチは内側(面との境界)から指定%内をタップすると大音符(両手)になります (100%超えも設定可能)",
                            fontSize = 9.sp,
                            color = if (isDark) Color.LightGray else Color(0xFF92400E)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                // Lightweight Rendering Mode (Effect OFF) Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "描画軽量モード (エフェクトOFF)",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Text(
                            text = "波紋エフェクトや毎フレームの回転アニメーション描画を省略し、高速連打時のCPU負荷とフレーム落ちを最小化します",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = if (isDark) Color.White else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.lightweightRenderingMode,
                        onCheckedChange = { onSettingsChanged(settings.copy(lightweightRenderingMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF78350F).invertIfDark(isDark),
                            checkedTrackColor = Color(0xFFFED7AA).invertIfDark(isDark)
                        )
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

                // Log Console Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ログコンソールを表示",
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Text(
                            text = "入力履歴やデバイス接続状態のリアルタイムログを上部にオーバーレイ表示します",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = if (isDark) Color.White else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.showLogConsole,
                        onCheckedChange = { onSettingsChanged(settings.copy(showLogConsole = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF78350F).invertIfDark(isDark),
                            checkedTrackColor = Color(0xFFFED7AA).invertIfDark(isDark)
                        )
                    )
                }
            }
        }

        // --- 4. Drum Presets Management Card (太鼓の設定の下に配置) ---
        TaikoPresetSettingCard(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            expandPresetCard = expandPresetCard,
            onExpandPresetCardChange = { expandPresetCard = it },
            isDark = isDark
        )

        // --- 5. Custom Key Configuration Card ---
        val activeEmulationMode = settings.activeEmulationMode
        CollapsibleSettingCard(
            title = if (activeEmulationMode == "gamepad") "🎮 キーマッピング設定 (ゲームパッド)" else "⌨️ キーマッピング設定 (キーボード)",
            subtitle = if (activeEmulationMode == "gamepad") {
                "左カッ:${settings.gamepadKeyConfig.leftKat} / 左ドン:${settings.gamepadKeyConfig.leftDon} / 右ドン:${settings.gamepadKeyConfig.rightDon} / 右カッ:${settings.gamepadKeyConfig.rightKat}"
            } else {
                "左カッ:${settings.keyConfig.leftKat} / 左ドン:${settings.keyConfig.leftDon} / 右ドン:${settings.keyConfig.rightDon} / 右カッ:${settings.keyConfig.rightKat}"
            },
            isExpanded = expandKeyCard,
            onExpandedChange = { expandKeyCard = it },
            isDarkTheme = isDark,
            headerTrailingContent = {
                OutlinedButton(
                    onClick = {
                        if (activeEmulationMode == "gamepad") {
                            onSettingsChanged(settings.copy(gamepadKeyConfig = GamepadKeyConfig()))
                        } else {
                            onSettingsChanged(settings.copy(keyConfig = KeyConfig()))
                        }
                    },
                    border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.3f).invertIfDark(isDark)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset",
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF78350F).invertIfDark(isDark)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("初期化", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (settings.activeEmulationMode == "gamepad") {
                    val gpConfig = settings.gamepadKeyConfig

                    BoxWithConstraints {
                        if (maxWidth < 450.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GamepadButtonDropdown(
                                        label = "左カッ",
                                        selectedButtonId = gpConfig.leftKat,
                                        onButtonSelected = { selected ->
                                            onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(leftKat = selected)))
                                        },
                                        isDarkTheme = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    GamepadButtonDropdown(
                                        label = "左ドン",
                                        selectedButtonId = gpConfig.leftDon,
                                        onButtonSelected = { selected ->
                                            onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(leftDon = selected)))
                                        },
                                        isDarkTheme = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GamepadButtonDropdown(
                                        label = "右ドン",
                                        selectedButtonId = gpConfig.rightDon,
                                        onButtonSelected = { selected ->
                                            onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(rightDon = selected)))
                                        },
                                        isDarkTheme = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    GamepadButtonDropdown(
                                        label = "右カッ",
                                        selectedButtonId = gpConfig.rightKat,
                                        onButtonSelected = { selected ->
                                            onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(rightKat = selected)))
                                        },
                                        isDarkTheme = isDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GamepadButtonDropdown(
                                    label = "左カッ",
                                    selectedButtonId = gpConfig.leftKat,
                                    onButtonSelected = { selected ->
                                        onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(leftKat = selected)))
                                    },
                                    isDarkTheme = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                                GamepadButtonDropdown(
                                    label = "左ドン",
                                    selectedButtonId = gpConfig.leftDon,
                                    onButtonSelected = { selected ->
                                        onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(leftDon = selected)))
                                    },
                                    isDarkTheme = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                                GamepadButtonDropdown(
                                    label = "右ドン",
                                    selectedButtonId = gpConfig.rightDon,
                                    onButtonSelected = { selected ->
                                        onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(rightDon = selected)))
                                    },
                                    isDarkTheme = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                                GamepadButtonDropdown(
                                    label = "右カッ",
                                    selectedButtonId = gpConfig.rightKat,
                                    onButtonSelected = { selected ->
                                        onSettingsChanged(settings.copy(gamepadKeyConfig = gpConfig.copy(rightKat = selected)))
                                    },
                                    isDarkTheme = isDark,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    BoxWithConstraints {
                        if (maxWidth < 450.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = settings.keyConfig.leftKat,
                                        onValueChange = {
                                            val newConf = settings.keyConfig.copy(leftKat = it.uppercase().take(1))
                                            onSettingsChanged(settings.copy(keyConfig = newConf))
                                        },
                                        label = { Text("左カッ", fontSize = 10.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = customTextFieldColors(isDark)
                                    )

                                    OutlinedTextField(
                                        value = settings.keyConfig.leftDon,
                                        onValueChange = {
                                            val newConf = settings.keyConfig.copy(leftDon = it.uppercase().take(1))
                                            onSettingsChanged(settings.copy(keyConfig = newConf))
                                        },
                                        label = { Text("左ドン", fontSize = 10.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = customTextFieldColors(isDark)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = settings.keyConfig.rightDon,
                                        onValueChange = {
                                            val newConf = settings.keyConfig.copy(rightDon = it.uppercase().take(1))
                                            onSettingsChanged(settings.copy(keyConfig = newConf))
                                        },
                                        label = { Text("右ドン", fontSize = 10.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = customTextFieldColors(isDark)
                                    )

                                    OutlinedTextField(
                                        value = settings.keyConfig.rightKat,
                                        onValueChange = {
                                            val newConf = settings.keyConfig.copy(rightKat = it.uppercase().take(1))
                                            onSettingsChanged(settings.copy(keyConfig = newConf))
                                        },
                                        label = { Text("右カッ", fontSize = 10.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = customTextFieldColors(isDark)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = settings.keyConfig.leftKat,
                                    onValueChange = {
                                        val newConf = settings.keyConfig.copy(leftKat = it.uppercase().take(1))
                                        onSettingsChanged(settings.copy(keyConfig = newConf))
                                    },
                                    label = { Text("左カッ", fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = customTextFieldColors(isDark)
                                )

                                OutlinedTextField(
                                    value = settings.keyConfig.leftDon,
                                    onValueChange = {
                                        val newConf = settings.keyConfig.copy(leftDon = it.uppercase().take(1))
                                        onSettingsChanged(settings.copy(keyConfig = newConf))
                                    },
                                    label = { Text("左ドン", fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = customTextFieldColors(isDark)
                                )

                                OutlinedTextField(
                                    value = settings.keyConfig.rightDon,
                                    onValueChange = {
                                        val newConf = settings.keyConfig.copy(rightDon = it.uppercase().take(1))
                                        onSettingsChanged(settings.copy(keyConfig = newConf))
                                    },
                                    label = { Text("右ドン", fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = customTextFieldColors(isDark)
                                )

                                OutlinedTextField(
                                    value = settings.keyConfig.rightKat,
                                    onValueChange = {
                                        val newConf = settings.keyConfig.copy(rightKat = it.uppercase().take(1))
                                        onSettingsChanged(settings.copy(keyConfig = newConf))
                                    },
                                    label = { Text("右カッ", fontSize = 10.sp, maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = customTextFieldColors(isDark)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Theme Selection Card (Auto OS / Light / Dark) ---
        CollapsibleSettingCard(
            title = "🎨 カラーテーマ設定",
            subtitle = when (settings.themeMode) {
                "system" -> "テーマ: 自動"
                "dark" -> "テーマ: ダーク"
                else -> "テーマ: ライト"
            },
            badgeText = if (isDark) "ダーク" else "ライト",
            isExpanded = expandThemeCard,
            onExpandedChange = { expandThemeCard = it },
            isDarkTheme = isDark
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val themeOptions = listOf(
                    "system" to "⚙️ 自動 (OS)",
                    "light" to "☀️ ライト",
                    "dark" to "🌙 ダーク"
                )
                themeOptions.forEach { (modeVal, label) ->
                    val isSel = settings.themeMode == modeVal
                    Button(
                        onClick = {
                            onSettingsChanged(
                                settings.copy(
                                    themeMode = modeVal,
                                    isDarkTheme = (modeVal == "dark")
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSel) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                            contentColor = if (isSel) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 36.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GamepadButtonDropdown(
    label: String,
    selectedButtonId: String,
    onButtonSelected: (String) -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "L1" to "L1 (LB)",
        "R1" to "R1 (RB)",
        "L2" to "L2 (LT)",
        "R2" to "R2 (RT)",
        "DPAD_LEFT" to "DPad 左",
        "DPAD_RIGHT" to "DPad 右",
        "DPAD_UP" to "DPad 上",
        "DPAD_DOWN" to "DPad 下",
        "A" to "A ボタン",
        "B" to "B ボタン",
        "X" to "X ボタン",
        "Y" to "Y ボタン",
        "SELECT" to "Select",
        "START" to "Start",
        "L3" to "L3",
        "R3" to "R3"
    )
    val currentLabel = options.find { it.first == selectedButtonId }?.second ?: selectedButtonId

    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDarkTheme))
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF78350F).copy(alpha = 0.4f).invertIfDark(isDarkTheme), RoundedCornerShape(8.dp))
                .background(Color.White.invertIfDark(isDarkTheme))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F).invertIfDark(isDarkTheme),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select button",
                    tint = Color(0xFF78350F).invertIfDark(isDarkTheme),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFFF9F5EB).invertIfDark(isDarkTheme))
                    .heightIn(max = 240.dp)
            ) {
                options.forEach { (id, optLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optLabel,
                                fontSize = 11.sp,
                                fontWeight = if (id == selectedButtonId) FontWeight.Bold else FontWeight.Normal,
                                color = if (id == selectedButtonId) Color(0xFFD97706).invertIfDark(isDarkTheme) else Color(0xFF78350F).invertIfDark(isDarkTheme)
                            )
                        },
                        onClick = {
                            onButtonSelected(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsibleSettingCard(
    title: String,
    subtitle: String? = null,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFFED7AA),
    badgeTextColor: Color = Color(0xFF78350F),
    isDarkTheme: Boolean = false,
    headerTrailingContent: (@Composable () -> Unit)? = null,
    onHeaderClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F5EB).invertIfDark(isDarkTheme)),
        border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.15f).invertIfDark(isDarkTheme)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onHeaderClick?.invoke()
                        onExpandedChange(!isExpanded)
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = true),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F).invertIfDark(isDarkTheme)
                            )
                            if (badgeText != null) {
                                val effectiveBadgeBg = if (isDarkTheme) {
                                    if (badgeColor == Color(0xFFD1FAE5)) Color(0xFF064E3B)
                                    else if (badgeColor == Color(0xFFFEE2E2)) Color(0xFF7F1D1D)
                                    else if (badgeColor == Color(0xFFFEF3C7) || badgeColor == Color(0xFFFED7AA)) Color(0xFF78350F)
                                    else badgeColor.invertIfDark(isDarkTheme)
                                } else badgeColor

                                val effectiveBadgeTxt = if (isDarkTheme) {
                                    if (badgeTextColor == Color(0xFF065F46)) Color(0xFFA7F3D0)
                                    else if (badgeTextColor == Color(0xFF991B1B)) Color(0xFFFECACA)
                                    else if (badgeTextColor == Color(0xFF92400E) || badgeTextColor == Color(0xFF78350F)) Color(0xFFFDE68A)
                                    else badgeTextColor.invertIfDark(isDarkTheme)
                                } else badgeTextColor

                                Surface(
                                    color = effectiveBadgeBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = effectiveBadgeTxt,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (!subtitle.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color.White else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    headerTrailingContent?.invoke()

                    IconButton(
                        onClick = { onExpandedChange(!isExpanded) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "折りたたむ" else "展開する",
                            tint = Color(0xFF78350F).invertIfDark(isDarkTheme)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDarkTheme))
                    content()
                }
            }
        }
    }
}

@Composable
fun TaikoSizeSettingCard(
    settings: ControllerSettings,
    onSettingsChanged: (ControllerSettings) -> Unit,
    expandSizeCard: Boolean,
    onExpandSizeCardChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val currentSize = if (isLandscape) settings.landscapeSizePercent else settings.portraitSizePercent
    val currentPos = if (isLandscape) settings.landscapeVerticalPosPercent else settings.portraitVerticalPosPercent

    var sizeText by remember(currentSize) { mutableStateOf(currentSize.toString()) }
    var posText by remember(currentPos) { mutableStateOf(currentPos.toString()) }

    val currentAlpha = settings.overlayAlphaPercent
    var alphaText by remember(currentAlpha) { mutableStateOf(currentAlpha.toString()) }

    CollapsibleSettingCard(
        title = "✥ 太鼓のサイズ・位置・透明度調整",
        subtitle = if (isLandscape) "横画面 (${currentSize}% / 位置${currentPos}% / 透過${currentAlpha}%)" else "縦画面 (${currentSize}% / 位置${currentPos}% / 透過${currentAlpha}%)",
        isExpanded = expandSizeCard,
        onExpandedChange = onExpandSizeCardChange,
        isDarkTheme = isDark,
        headerTrailingContent = {
            OutlinedButton(
                onClick = {
                    if (isLandscape) {
                        onSettingsChanged(settings.copy(landscapeSizePercent = 100, landscapeVerticalPosPercent = 55, overlayAlphaPercent = 80))
                    } else {
                        onSettingsChanged(settings.copy(portraitSizePercent = 100, portraitVerticalPosPercent = 50, overlayAlphaPercent = 80))
                    }
                },
                border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.3f).invertIfDark(isDark)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset",
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF78350F).invertIfDark(isDark)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "初期化",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F).invertIfDark(isDark)
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "太鼓サイズ: ${currentSize}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F).invertIfDark(isDark)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val newSize = (currentSize - 5).coerceIn(10, 300)
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(landscapeSizePercent = newSize))
                                } else {
                                    onSettingsChanged(settings.copy(portraitSizePercent = newSize))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val newSize = (currentSize + 5).coerceIn(10, 300)
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(landscapeSizePercent = newSize))
                                } else {
                                    onSettingsChanged(settings.copy(portraitSizePercent = newSize))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = sizeText,
                            onValueChange = { newValue: String ->
                                sizeText = newValue
                                val parsed = newValue.toIntOrNull()
                                if (parsed != null) {
                                    val clamped = parsed.coerceIn(10, 300)
                                    if (isLandscape) {
                                        onSettingsChanged(settings.copy(landscapeSizePercent = clamped))
                                    } else {
                                        onSettingsChanged(settings.copy(portraitSizePercent = clamped))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(70.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            colors = customTextFieldColors(isDark)
                        )
                        Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "上下位置: ${currentPos}% (0%=上端, 50%=中央, 100%=下端 / -50%〜150%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78350F).invertIfDark(isDark)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val newPos = (currentPos - 5).coerceIn(-50, 150)
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(landscapeVerticalPosPercent = newPos))
                                } else {
                                    onSettingsChanged(settings.copy(portraitVerticalPosPercent = newPos))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val newPos = (currentPos + 5).coerceIn(-50, 150)
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(landscapeVerticalPosPercent = newPos))
                                } else {
                                    onSettingsChanged(settings.copy(portraitVerticalPosPercent = newPos))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = posText,
                            onValueChange = { newValue: String ->
                                posText = newValue
                                val parsed = newValue.toIntOrNull()
                                if (parsed != null) {
                                    val clamped = parsed.coerceIn(-50, 150)
                                    if (isLandscape) {
                                        onSettingsChanged(settings.copy(landscapeVerticalPosPercent = clamped))
                                    } else {
                                        onSettingsChanged(settings.copy(portraitVerticalPosPercent = clamped))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(70.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            colors = customTextFieldColors(isDark)
                        )
                        Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Overlay-only Transparency (不透明度) Setting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪟 オーバーレイ時の不透明度: ${currentAlpha}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark)
                    )
                    Text(
                        text = "(オーバーレイ専用)",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFFFED7AA) else Color(0xFFB45309),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val newAlpha = (currentAlpha - 5).coerceIn(10, 100)
                                onSettingsChanged(settings.copy(overlayAlphaPercent = newAlpha))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("-5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val newAlpha = (currentAlpha + 5).coerceIn(10, 100)
                                onSettingsChanged(settings.copy(overlayAlphaPercent = newAlpha))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADCC9).invertIfDark(isDark)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+5%", color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("入力:", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = alphaText,
                            onValueChange = { newValue: String ->
                                alphaText = newValue
                                val parsed = newValue.toIntOrNull()
                                if (parsed != null) {
                                    val clamped = parsed.coerceIn(10, 100)
                                    onSettingsChanged(settings.copy(overlayAlphaPercent = clamped))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(70.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            colors = customTextFieldColors(isDark)
                        )
                        Text("%", fontSize = 11.sp, color = Color(0xFF78350F).invertIfDark(isDark), fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "※ オーバーレイ表示モード時の透け具合を設定します (通常アプリ画面には影響しません)",
                    fontSize = 10.sp,
                    color = if (isDark) Color.LightGray else Color(0xFF92400E)
                )
            }
        }
    }
}

@Composable
fun TaikoPresetSettingCard(
    settings: ControllerSettings,
    onSettingsChanged: (ControllerSettings) -> Unit,
    expandPresetCard: Boolean,
    onExpandPresetCardChange: (Boolean) -> Unit,
    isDark: Boolean
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val preset1 = if (isLandscape) settings.landscapePreset1 else settings.portraitPreset1
    val preset2 = if (isLandscape) settings.landscapePreset2 else settings.portraitPreset2
    val prefix = if (isLandscape) "横画面" else "縦画面"

    CollapsibleSettingCard(
        title = "💾 プリセット保存 (${prefix})",
        subtitle = "配置・サイズ・大音符範囲の保存と一括切り替え",
        isExpanded = expandPresetCard,
        onExpandedChange = onExpandPresetCardChange,
        isDarkTheme = isDark
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (isLandscape) "📍 横画面用 プリセット (サイズ・位置・面/フチ大音符)" else "📍 縦画面用 プリセット (サイズ・位置・面/フチ大音符)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78350F).invertIfDark(isDark)
            )

            // Preset 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$prefix プリセット 1",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(
                                        landscapeSizePercent = preset1.sizePercent,
                                        landscapeVerticalPosPercent = preset1.verticalPositionPercent,
                                        landscapeDonBigNotePercent = preset1.donBigPercent,
                                        landscapeKatBigNotePercent = preset1.katBigPercent,
                                        donBigNotePercent = preset1.donBigPercent,
                                        katBigNotePercent = preset1.katBigPercent
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitSizePercent = preset1.sizePercent,
                                        portraitVerticalPosPercent = preset1.verticalPositionPercent,
                                        portraitDonBigNotePercent = preset1.donBigPercent,
                                        portraitKatBigNotePercent = preset1.katBigPercent,
                                        donBigNotePercent = preset1.donBigPercent,
                                        katBigNotePercent = preset1.katBigPercent
                                    ))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F).invertIfDark(isDark)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("適用", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(
                                        landscapePreset1 = DrumPreset(
                                            sizePercent = settings.landscapeSizePercent,
                                            verticalPositionPercent = settings.landscapeVerticalPosPercent,
                                            donBigPercent = settings.landscapeDonBigNotePercent,
                                            katBigPercent = settings.landscapeKatBigNotePercent
                                        )
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitPreset1 = DrumPreset(
                                            sizePercent = settings.portraitSizePercent,
                                            verticalPositionPercent = settings.portraitVerticalPosPercent,
                                            donBigPercent = settings.portraitDonBigNotePercent,
                                            katBigPercent = settings.portraitKatBigNotePercent
                                        )
                                    ))
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.4f).invertIfDark(isDark)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("保存", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                        }
                    }
                }
                Text(
                    text = "サイズ: ${preset1.sizePercent}% / 位置: ${preset1.verticalPositionPercent}% / 面大音符: ${preset1.donBigPercent}% / フチ大音符: ${preset1.katBigPercent}%",
                    fontSize = 10.sp,
                    color = if (isDark) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Preset 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEADCC9).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$prefix プリセット 2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(
                                        landscapeSizePercent = preset2.sizePercent,
                                        landscapeVerticalPosPercent = preset2.verticalPositionPercent,
                                        landscapeDonBigNotePercent = preset2.donBigPercent,
                                        landscapeKatBigNotePercent = preset2.katBigPercent,
                                        donBigNotePercent = preset2.donBigPercent,
                                        katBigNotePercent = preset2.katBigPercent
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitSizePercent = preset2.sizePercent,
                                        portraitVerticalPosPercent = preset2.verticalPositionPercent,
                                        portraitDonBigNotePercent = preset2.donBigPercent,
                                        portraitKatBigNotePercent = preset2.katBigPercent,
                                        donBigNotePercent = preset2.donBigPercent,
                                        katBigNotePercent = preset2.katBigPercent
                                    ))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F).invertIfDark(isDark)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("適用", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                if (isLandscape) {
                                    onSettingsChanged(settings.copy(
                                        landscapePreset2 = DrumPreset(
                                            sizePercent = settings.landscapeSizePercent,
                                            verticalPositionPercent = settings.landscapeVerticalPosPercent,
                                            donBigPercent = settings.landscapeDonBigNotePercent,
                                            katBigPercent = settings.landscapeKatBigNotePercent
                                        )
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitPreset2 = DrumPreset(
                                            sizePercent = settings.portraitSizePercent,
                                            verticalPositionPercent = settings.portraitVerticalPosPercent,
                                            donBigPercent = settings.portraitDonBigNotePercent,
                                            katBigPercent = settings.portraitKatBigNotePercent
                                        )
                                    ))
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.4f).invertIfDark(isDark)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("保存", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
                        }
                    }
                }
                Text(
                    text = "サイズ: ${preset2.sizePercent}% / 位置: ${preset2.verticalPositionPercent}% / 面大音符: ${preset2.donBigPercent}% / フチ大音符: ${preset2.katBigPercent}%",
                    fontSize = 10.sp,
                    color = if (isDark) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShizukuSettingsContent(
    settings: ControllerSettings,
    shizukuRunning: Boolean,
    shizukuPermission: Boolean,
    onOpenShizukuApp: () -> Unit,
    onRefreshShizukuStatus: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onSettingsChanged: (ControllerSettings) -> Unit,
    isDark: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Shizuku（ADB権限実行環境）を使用して、端末ローカルでダイレクトにキーボード/ゲームパッド信号を注入します。Root化不要で動作します。",
            fontSize = 10.sp,
            color = if (isDark) Color.White else Color.DarkGray
        )

        // Shizuku Service Status
        val shizukuBg = if (shizukuRunning) {
            if (shizukuPermission) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7))
        } else {
            if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
        }
        val shizukuTxt = if (shizukuRunning) {
            if (shizukuPermission) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E))
        } else {
            if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
        }
        val shizukuLabel = if (shizukuRunning) {
            if (shizukuPermission) "Shizuku: 実行中・許可済み (接続成功)" else "Shizuku: 実行中・許可保留中"
        } else {
            "Shizuku: 停止中 (Shizukuアプリを起動してください)"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(shizukuBg)
                .padding(8.dp)
        ) {
            Text(
                text = shizukuLabel,
                color = shizukuTxt,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (!shizukuRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenShizukuApp,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706).invertIfDark(isDark),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.1f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = "Shizukuアプリを起動",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onRefreshShizukuStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78350F).invertIfDark(isDark),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.weight(0.9f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = "再確認する",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (shizukuRunning && !shizukuPermission) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onRequestShizukuPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF78350F).invertIfDark(isDark),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1.1f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = "Shizukuの使用を許可",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onRefreshShizukuStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEADCC9).invertIfDark(isDark),
                        contentColor = Color(0xFF78350F).invertIfDark(isDark)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.weight(0.9f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = "再確認する",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

        // --- Injection Method & Emulation Mode Selectors ---
        Text(
            text = "⚙️ インジェクション設定",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF78350F).invertIfDark(isDark)
        )

        Text("インジェクション方式:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "inject" to "Direct API (推奨)",
                "uinput" to "uinput (仮想デバイス)"
            ).forEach { (method, label) ->
                val isSel = settings.injectionMethod == method
                Button(
                    onClick = { onSettingsChanged(settings.copy(injectionMethod = method)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                        contentColor = if (isSel) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

        Text("デバイス種別 (エミュレーション):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F).invertIfDark(isDark))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "gamepad" to "ゲームパッド (推奨)",
                "keyboard" to "キーボード"
            ).forEach { (mode, label) ->
                val isSel = settings.shizukuEmulationMode == mode
                Button(
                    onClick = { onSettingsChanged(settings.copy(shizukuEmulationMode = mode, emulationMode = mode)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) Color(0xFF78350F).invertIfDark(isDark) else Color(0xFFEADCC9).invertIfDark(isDark),
                        contentColor = if (isSel) Color.White else Color(0xFF78350F).invertIfDark(isDark)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 34.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

        var dolphinExpanded by remember { mutableStateOf(false) }

        // 1. Non-device-discriminating Emulators Guide Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background((if (isDark) Color(0xFFE0F2FE) else Color(0xFFFEF3C7)).invertIfDark(isDark))
                .border(1.dp, (if (isDark) Color(0xFF0284C7) else Color(0xFFD97706)).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = "🎮 ゲームパッドのポイント",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = (if (isDark) Color(0xFF0369A1) else Color(0xFF92400E)).invertIfDark(isDark)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ARMSX2などデバイスの種類を識別しないエミュレータでは、本アプリのデバイス種別を「ゲームパッド (推奨)」に設定することで、物理コントローラーと本アプリの入力を同じボタン入力として共有でき便利です。",
                    fontSize = 10.sp,
                    color = (if (isDark) Color(0xFF0C4A6E) else Color(0xFF78350F)).invertIfDark(isDark),
                    lineHeight = 14.sp
                )
            }
        }

        // 2. Dolphin Setup Guide Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background((if (isDark) Color(0xFFFEF3C7) else Color(0xFFE0F2FE)).invertIfDark(isDark))
                .border(1.dp, (if (isDark) Color(0xFFD97706) else Color(0xFF0284C7)).copy(alpha = 0.3f).invertIfDark(isDark), RoundedCornerShape(8.dp))
                .clickable { dolphinExpanded = !dolphinExpanded }
                .padding(8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐬 Dolphinエミュレータ設定のポイント",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color(0xFF92400E) else Color(0xFF0369A1)).invertIfDark(isDark)
                    )
                    Icon(
                        imageVector = if (dolphinExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Dolphin guide",
                        tint = (if (isDark) Color(0xFF92400E) else Color(0xFF0369A1)).invertIfDark(isDark),
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (dolphinExpanded) {
                    val uriHandler = LocalUriHandler.current
                    val dolphinGuideUrl = "https://ja.dolphin-emu.org/docs/guides/controlling-global-user-directory/#Android"

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "【基本設定】\n" +
                               "1. インジェクション方式: 「Direct API (推奨)」を選択（動作不安定時は「uinput」）\n" +
                               "2. デバイス種別: 「ゲームパッド (推奨)」を選択\n" +
                               "3. Dolphin設定: 『コントローラー設定』の「Create Mappings for Other Devices」をオンにしてボタンを割り当てます。\n\n" +
                               "【タタコン拡張設定 (Extension)】\n" +
                               "• Dolphin Android版でタタコンをExtensionに設定するには、設定プロファイル (.ini) を直接編集する必要があります。\n" +
                               "• Dolphinの『Wii Input』各コントローラー設定内にある『Profiles』から設定を保存すると.iniファイルが作成されます。\n" +
                               "• 保存先: Dolphinユーザーフォルダ内の『Config/Profiles/Wiimote/』配下\n" +
                               "• .iniファイル内に「Extension = TaTaCon」と記入してください（元からExtensionの行があれば書き換え、無ければ追加）。\n\n" +
                               "【Android 11以降のフォルダ制限と回避策】\n" +
                               "• Android 11以降でDolphinのユーザーフォルダにアクセスするには、特殊なファイルマネージャーを使用するか、Dolphinの機能で毎回「エクスポート / インポート」を行う必要があります。\n" +
                               "• 【回避方法】旧来の「dolphin-emu」フォルダを使用したい場合は、一度バージョン 5.0-15341 以前のDolphinをインストールして起動（フォルダ生成）させてから、最新版にアップデートすることで制限を回避できます。\n" +
                               "※ 既にDolphinをインストールして使用している場合は、作業前に必ずDolphinの機能でエクスポートしてバックアップを取ってください。\n\n" +
                               "詳しく知りたい方は以下の公式ガイドをご覧ください:",
                        fontSize = 10.sp,
                        color = (if (isDark) Color(0xFF78350F) else Color(0xFF0C4A6E)).invertIfDark(isDark),
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dolphinGuideUrl,
                        fontSize = 10.sp,
                        color = (if (isDark) Color(0xFFD97706) else Color(0xFF0284C7)).invertIfDark(isDark),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            try {
                                uriHandler.openUri(dolphinGuideUrl)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun openUsbSettings(context: android.content.Context) {
    val intents = listOf(
        // Direct USB details / preferences (Android 10+)
        android.content.Intent("android.settings.USB_PREFERENCES_SETTINGS"),
        android.content.Intent("android.settings.USB_DETAILS_SETTINGS"),
        android.content.Intent().apply { setClassName("com.android.settings", "com.android.settings.Settings\$UsbDetailsActivity") },
        android.content.Intent().apply { setClassName("com.android.settings", "com.android.settings.usb.UsbDetailsActivity") },
        // Connected devices & connection preferences
        android.content.Intent("android.settings.CONNECTED_DEVICE_SETTINGS"),
        android.content.Intent().apply { setClassName("com.android.settings", "com.android.settings.Settings\$ConnectedDeviceDashboardActivity") },
        // Fallback to main Android settings
        android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
    )
    for (intent in intents) {
        try {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
}
