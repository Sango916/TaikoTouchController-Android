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
            // Linux Receiver Script (.sh / Python wrapper)
            val linuxScript = """#!/usr/bin/env python3
# Linux PC-side Receiver for Taiko Controller
# File extension to save as: controller.sh (or controller.py)
# Usage: chmod +x controller.sh && ./controller.sh

import socket
import subprocess
import time
import sys
import os
import urllib.request
import zipfile
import traceback

PORT = 60001

def log(msg):
    print(msg, flush=True)

def ensure_system_package(pkg_name_apt, pkg_name_dnf=None, pkg_name_pacman=None, pkg_name_zypper=None):
    # Attempt to install a system package using available package manager with sudo prompt
    if os.path.exists("/usr/bin/apt-get"):
        log(f"Installing {pkg_name_apt} via apt (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "apt-get", "update"], check=False)
            subprocess.run(["sudo", "apt-get", "install", "-y", pkg_name_apt], check=True)
            return True
        except Exception as e:
            log(f"apt-get install failed: {e}")
    elif os.path.exists("/usr/bin/dnf"):
        pkg = pkg_name_dnf or pkg_name_apt
        log(f"Installing {pkg} via dnf (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "dnf", "install", "-y", pkg], check=True)
            return True
        except Exception as e:
            log(f"dnf install failed: {e}")
    elif os.path.exists("/usr/bin/pacman"):
        pkg = pkg_name_pacman or pkg_name_apt
        log(f"Installing {pkg} via pacman (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "pacman", "-Sy", "--noconfirm", pkg], check=True)
            return True
        except Exception as e:
            log(f"pacman install failed: {e}")
    elif os.path.exists("/usr/bin/zypper"):
        pkg = pkg_name_zypper or pkg_name_apt
        log(f"Installing {pkg} via zypper (administrator password may be requested)...")
        try:
            subprocess.run(["sudo", "zypper", "--non-interactive", "in", pkg], check=True)
            return True
        except Exception as e:
            log(f"zypper install failed: {e}")
    return False

def main():
    log("=== Taiko Controller Receiver for Linux ===")
    
    adb_cmd = "adb"

    # 1. Verify and setup ADB
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Attempting automatic installation...")
            installed = ensure_system_package(
                pkg_name_apt="adb",
                pkg_name_dnf="android-tools",
                pkg_name_pacman="android-tools",
                pkg_name_zypper="android-tools"
            )
            if installed:
                adb_cmd = "adb"
            else:
                log("Downloading official standalone Android SDK Platform Tools...")
                url = "https://dl.google.com/android/repository/platform-tools-latest-linux.zip"
                zip_path = "./platform-tools.zip"
                try:
                    urllib.request.urlretrieve(url, zip_path)
                    with zipfile.ZipFile(zip_path, "r") as zip_ref:
                        zip_ref.extractall(".")
                    if os.path.exists(zip_path):
                        os.remove(zip_path)
                    os.chmod("./platform-tools/adb", 0o755)
                    adb_cmd = "./platform-tools/adb"
                    log("ADB downloaded and extracted successfully to ./platform-tools/")
                except Exception as e:
                    log(f"Error downloading platform-tools: {e}")
                    log("Please install 'adb' or 'android-tools' via your package manager manually.")
                    return

    # 2. Key simulation library (pynput)
    try:
        from pynput.keyboard import Key, Controller
    except ImportError:
        log("pynput library not found. Installing system package...")
        installed = ensure_system_package(
            pkg_name_apt="python3-pynput",
            pkg_name_dnf="python3-pynput",
            pkg_name_pacman="python-pynput",
            pkg_name_zypper="python3-pynput"
        )
        if not installed:
            log("Trying pip install with --user flag...")
            try:
                subprocess.run([sys.executable, "-m", "pip", "install", "--user", "pynput"], check=True)
            except Exception as e:
                log(f"pip install failed: {e}")
                log("Please run: sudo apt install python3-pynput (or equivalent for your Linux distribution)")
                return
        try:
            from pynput.keyboard import Key, Controller
        except ImportError:
            log("Could not load pynput. Please restart the terminal or install python3-pynput.")
            return

    keyboard = Controller()

    def send_down(key_char):
        try:
            keyboard.press(key_char)
        except Exception as e:
            log(f"Key press error: {e}")

    def send_up(key_char):
        try:
            keyboard.release(key_char)
        except Exception as e:
            log(f"Key release error: {e}")

    log(f"Setting up ADB port forwarding (tcp:{PORT})...")
    subprocess.run([adb_cmd, "forward", "--remove", f"tcp:{PORT}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    fwd_proc = subprocess.run([adb_cmd, "forward", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True, text=True)
    if fwd_proc.returncode != 0:
        log(f"ADB forward message: {fwd_proc.stderr.strip() or fwd_proc.stdout.strip()}")
        log("Ensure your Android device is USB connected with USB debugging enabled!")

    log(f"Connecting to Android Taiko controller on localhost:{PORT}...")

    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(3.0)
            s.connect(("127.0.0.1", PORT))
            
            f = s.makefile("r", encoding="utf-8")
            banner = f.readline()
            if not banner:
                s.close()
                log("Waiting for Android app response... (retrying in 2s)")
                time.sleep(2)
                continue

            s.settimeout(None)
            log("Connected successfully to Taiko App! Start your game now!")
            
            for line in f:
                line = line.strip()
                if not line:
                    continue
                parts = line.split(" ")
                if len(parts) >= 2:
                    action = parts[0]
                    for key_str in parts[1:]:
                        key_char = key_str.lower()
                        if not key_char:
                            continue
                        log(f"[KEY] {action} -> {key_char}")
                        if action == "DOWN":
                            send_down(key_char)
                        elif action == "UP":
                            send_up(key_char)

            log("Disconnected by Android app. Reconnecting in 2 seconds...")
            s.close()
            time.sleep(2)
        except (socket.timeout, ConnectionRefusedError, OSError):
            log("Waiting for Android app connection... (retrying in 2s)")
            time.sleep(2)
        except KeyboardInterrupt:
            log("Exiting...")
            break
        except Exception as e:
            log(f"Error: {e}. Reconnecting in 2 seconds...")
            time.sleep(2)

if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"Fatal Error: {err}", flush=True)
        traceback.print_exc()
        input("Press Enter to exit...")
"""

            // macOS Receiver Script (.command / Python wrapper)
            val macOSScript = """#!/usr/bin/env python3
# macOS PC-side Receiver for Taiko Controller
# File extension to save as: controller.command (or controller.py)
# Usage: chmod +x controller.command && ./controller.command

import socket
import subprocess
import time
import sys
import os
import urllib.request
import zipfile
import traceback

PORT = 60001

def log(msg):
    print(msg, flush=True)

def main():
    log("=== Taiko Controller Receiver for macOS ===")
    
    adb_cmd = "adb"

    # Verify ADB
    try:
        subprocess.run(["adb", "version"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except FileNotFoundError:
        if os.path.exists("./platform-tools/adb"):
            adb_cmd = "./platform-tools/adb"
        else:
            log("ADB not found in PATH. Checking Homebrew or standalone tools...")
            installed = False
            try:
                # Homebrew does not require sudo
                log("Trying to install android-platform-tools via Homebrew...")
                res = subprocess.run(["brew", "install", "android-platform-tools"], check=False)
                if res.returncode == 0:
                    installed = True
                    adb_cmd = "adb"
            except Exception:
                pass
            
            if not installed:
                log("Downloading official Android SDK Platform Tools...")
                url = "https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"
                zip_path = "./platform-tools.zip"
                try:
                    urllib.request.urlretrieve(url, zip_path)
                    with zipfile.ZipFile(zip_path, "r") as zip_ref:
                        zip_ref.extractall(".")
                    if os.path.exists(zip_path):
                        os.remove(zip_path)
                    os.chmod("./platform-tools/adb", 0o755)
                    adb_cmd = "./platform-tools/adb"
                    log("ADB downloaded and extracted successfully to ./platform-tools/")
                except Exception as e:
                    log(f"Error downloading platform-tools: {e}")
                    log("Please install ADB manually.")
                    return

    # Key simulation libraries
    try:
        from pynput.keyboard import Key, Controller
    except ImportError:
        log("Installing pynput library for keyboard simulation...")
        installed = False
        try:
            subprocess.run([sys.executable, "-m", "pip", "install", "--user", "pynput"], check=True)
            installed = True
        except Exception:
            try:
                subprocess.run([sys.executable, "-m", "pip", "install", "pynput"], check=True)
                installed = True
            except Exception:
                pass
        try:
            from pynput.keyboard import Key, Controller
        except ImportError:
            log("Could not load pynput. Please run: pip3 install --user pynput")
            return

    keyboard = Controller()

    def send_down(key_char):
        try:
            keyboard.press(key_char)
        except Exception as e:
            log(f"Key press error: {e}")

    def send_up(key_char):
        try:
            keyboard.release(key_char)
        except Exception as e:
            log(f"Key release error: {e}")

    log(f"Setting up ADB port forwarding (tcp:{PORT})...")
    subprocess.run([adb_cmd, "forward", "--remove", f"tcp:{PORT}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    fwd_proc = subprocess.run([adb_cmd, "forward", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True, text=True)
    if fwd_proc.returncode != 0:
        log(f"ADB forward message: {fwd_proc.stderr.strip() or fwd_proc.stdout.strip()}")
        log("Ensure your Android device is USB connected with USB debugging enabled.")

    log(f"Connecting to Android Taiko controller on localhost:{PORT}...")
    log("Note: Ensure Terminal/App has Accessibility permission in System Settings -> Privacy & Security -> Accessibility.")

    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.settimeout(3.0)
            s.connect(("127.0.0.1", PORT))
            
            f = s.makefile("r", encoding="utf-8")
            banner = f.readline()
            if not banner:
                s.close()
                log("Waiting for Android app response... (retrying in 2s)")
                time.sleep(2)
                continue

            s.settimeout(None)
            log("Connected successfully to Taiko App! Start your game now!")
            
            for line in f:
                line = line.strip()
                if not line:
                    continue
                parts = line.split(" ")
                if len(parts) >= 2:
                    action = parts[0]
                    for key_str in parts[1:]:
                        key_char = key_str.lower()
                        if not key_char:
                            continue
                        log(f"[KEY] {action} -> {key_char}")
                        if action == "DOWN":
                            send_down(key_char)
                        elif action == "UP":
                            send_up(key_char)

            log("Disconnected by Android app. Reconnecting in 2 seconds...")
            s.close()
            time.sleep(2)
        except (socket.timeout, ConnectionRefusedError, OSError):
            log("Waiting for Android app connection... (retrying in 2s)")
            time.sleep(2)
        except KeyboardInterrupt:
            log("Exiting...")
            break
        except Exception as e:
            log(f"Error: {e}. Reconnecting in 2 seconds...")
            time.sleep(2)

if __name__ == "__main__":
    try:
        main()
    except Exception as err:
        print(f"Fatal Error: {err}", flush=True)
        traceback.print_exc()
        input("Press Enter to exit...")
"""

            val powerShellScript = String(android.util.Base64.decode("IyBXaW5kb3dzIFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIGV4dGVuc2lvbiB0byBzYXZlIGFzOiBjb250cm9sbGVyLnBzMQojIFVzYWdlOiBSaWdodC1jbGljayB0aGUgc2F2ZWQgZmlsZSBhbmQgc2VsZWN0ICJSdW4gd2l0aCBQb3dlclNoZWxsIgoKJHBvcnQgPSA2MDAwMQokYWRiQ21kID0gImFkYiIKCmZ1bmN0aW9uIEFkZC1QYXRoVG9Vc2VyRW52aXJvbm1lbnQoJGRpclRvQWRkKSB7CiAgICB0cnkgewogICAgICAgICRyZXNvbHZlZERpciA9IChSZXNvbHZlLVBhdGggJGRpclRvQWRkKS5QYXRoCiAgICAgICAgJHVzZXJQYXRoID0gW1N5c3RlbS5FbnZpcm9ubWVudF06OkdldEVudmlyb25tZW50VmFyaWFibGUoIlBhdGgiLCAiVXNlciIpCiAgICAgICAgaWYgKCRudWxsIC1lcSAkdXNlclBhdGgpIHsgJHVzZXJQYXRoID0gIiIgfQogICAgICAgICRwYXRocyA9ICR1c2VyUGF0aC5TcGxpdCgnOycsIFtTeXN0ZW0uU3RyaW5nU3BsaXRPcHRpb25zXTo6UmVtb3ZlRW1wdHlFbnRyaWVzKQogICAgICAgIGlmICgkcGF0aHMgLW5vdGNvbnRhaW5zICRyZXNvbHZlZERpcikgewogICAgICAgICAgICBXcml0ZS1Ib3N0ICJSZWdpc3RlcmluZyBBREIgdG8gVXNlciBQQVRIOiAkcmVzb2x2ZWREaXIiIC1Gb3JlZ3JvdW5kQ29sb3IgQ3lhbgogICAgICAgICAgICAkbmV3UGF0aCA9IGlmICgkdXNlclBhdGguVHJpbSgpLkxlbmd0aCAtZ3QgMCkgeyAiJHVzZXJQYXRoOyRyZXNvbHZlZERpciIgfSBlbHNlIHsgJHJlc29sdmVkRGlyIH0KICAgICAgICAgICAgW1N5c3RlbS5FbnZpcm9ubWVudF06OlNldEVudmlyb25tZW50VmFyaWFibGUoIlBhdGgiLCAkbmV3UGF0aCwgIlVzZXIiKQogICAgICAgICAgICAkZW52OlBhdGggPSBbU3lzdGVtLkVudmlyb25tZW50XTo6R2V0RW52aXJvbm1lbnRWYXJpYWJsZSgiUGF0aCIsIk1hY2hpbmUiKSArICI7IiArIFtTeXN0ZW0uRW52aXJvbm1lbnRdOjpHZXRFbnZpcm9ubWVudFZhcmlhYmxlKCJQYXRoIiwiVXNlciIpCiAgICAgICAgICAgIFdyaXRlLUhvc3QgIkFEQiBzdWNjZXNzZnVsbHkgYWRkZWQgdG8gUEFUSCEiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICB9CiAgICB9IGNhdGNoIHsKICAgICAgICBXcml0ZS1Ib3N0ICJOb3RlOiBDb3VsZCBub3QgYXV0b21hdGljYWxseSB1cGRhdGUgVXNlciBQQVRIOiAkXyIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgIH0KfQoKIyBDaGVjayBpZiBhZGIgaXMgaW4gUEFUSAppZiAoIShHZXQtQ29tbWFuZCBhZGIgLUVycm9yQWN0aW9uIFNpbGVudGx5Q29udGludWUpKSB7CiAgICBXcml0ZS1Ib3N0ICJBREIgaXMgbm90IGluIFBBVEguIENoZWNraW5nIGxvY2FsIHBsYXRmb3JtLXRvb2xzLi4uIiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgCiAgICBpZiAoVGVzdC1QYXRoICIuXHBsYXRmb3JtLXRvb2xzXGFkYi5leGUiKSB7CiAgICAgICAgJGFkYkNtZCA9ICIuXHBsYXRmb3JtLXRvb2xzXGFkYi5leGUiCiAgICAgICAgV3JpdGUtSG9zdCAiRm91bmQgbG9jYWwgQURCIGluIHBsYXRmb3JtLXRvb2xzIGZvbGRlci4iIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICBBZGQtUGF0aFRvVXNlckVudmlyb25tZW50ICIuXHBsYXRmb3JtLXRvb2xzIgogICAgfSBlbHNlIHsKICAgICAgICBXcml0ZS1Ib3N0ICJUcnlpbmcgdG8gaW5zdGFsbCBBREIgdmlhIHdpbmdldC4uLiIgLUZvcmVncm91bmRDb2xvciBDeWFuCiAgICAgICAgaWYgKEdldC1Db21tYW5kIHdpbmdldCAtRXJyb3JBY3Rpb24gU2lsZW50bHlDb250aW51ZSkgewogICAgICAgICAgICB0cnkgewogICAgICAgICAgICAgICAgd2luZ2V0IGluc3RhbGwgR29vZ2xlLkFkYiAtLXNpbGVudCAtLWFjY2VwdC1zb3VyY2UtYWdyZWVtZW50cyAtLWFjY2VwdC1wYWNrYWdlLWFncmVlbWVudHMgfCBPdXQtTnVsbAogICAgICAgICAgICAgICAgJGVudjpQYXRoID0gW1N5c3RlbS5FbnZpcm9ubWVudF06OkdldEVudmlyb25tZW50VmFyaWFibGUoIlBhdGgiLCJNYWNoaW5lIikgKyAiOyIgKyBbU3lzdGVtLkVudmlyb25tZW50XTo6R2V0RW52aXJvbm1lbnRWYXJpYWJsZSgiUGF0aCIsIlVzZXIiKQogICAgICAgICAgICB9IGNhdGNoIHt9CiAgICAgICAgfQoKICAgICAgICBpZiAoR2V0LUNvbW1hbmQgYWRiIC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSB7CiAgICAgICAgICAgICRhZGJDbWQgPSAiYWRiIgogICAgICAgICAgICBXcml0ZS1Ib3N0ICJBREIgaW5zdGFsbGVkIHZpYSB3aW5nZXQgc3VjY2Vzc2Z1bGx5ISIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgIH0gZWxzZSB7CiAgICAgICAgICAgIFdyaXRlLUhvc3QgIndpbmdldCB1bmF2YWlsYWJsZSBvciBwZW5kaW5nLiBEb3dubG9hZGluZyBvZmZpY2lhbCBBbmRyb2lkIFNESyBQbGF0Zm9ybSBUb29scy4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgJHVybCA9ICJodHRwczovL2RsLmdvb2dsZS5jb20vYW5kcm9pZC9yZXBvc2l0b3J5L3BsYXRmb3JtLXRvb2xzLWxhdGVzdC13aW5kb3dzLnppcCIKICAgICAgICAgICAgJG91dHB1dCA9ICIuXHBsYXRmb3JtLXRvb2xzLnppcCIKICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIEludm9rZS1XZWJSZXF1ZXN0IC1VcmkgJHVybCAtT3V0RmlsZSAkb3V0cHV0CiAgICAgICAgICAgICAgICBFeHBhbmQtQXJjaGl2ZSAtUGF0aCAkb3V0cHV0IC1EZXN0aW5hdGlvblBhdGggIi4iIC1Gb3JjZQogICAgICAgICAgICAgICAgUmVtb3ZlLUl0ZW0gJG91dHB1dAogICAgICAgICAgICAgICAgaWYgKFRlc3QtUGF0aCAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIikgewogICAgICAgICAgICAgICAgICAgICRhZGJDbWQgPSAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIgogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkFEQiBkb3dubG9hZGVkIGFuZCBleHRyYWN0ZWQgc3VjY2Vzc2Z1bGx5ISIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgICAgICAgICAgICAgIEFkZC1QYXRoVG9Vc2VyRW52aXJvbm1lbnQgIi5ccGxhdGZvcm0tdG9vbHMiCiAgICAgICAgICAgICAgICB9IGVsc2UgewogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkVycm9yOiBGYWlsZWQgdG8gZXh0cmFjdCBwbGF0Zm9ybS10b29scy4iIC1Gb3JlZ3JvdW5kQ29sb3IgUmVkCiAgICAgICAgICAgICAgICAgICAgUGF1c2UKICAgICAgICAgICAgICAgICAgICBFeGl0CiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgIH0gY2F0Y2ggewogICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiRXJyb3I6IENvdWxkIG5vdCBkb3dubG9hZCBBREIuIFBsZWFzZSBpbnN0YWxsIEFEQiBvciBwbGF0Zm9ybS10b29scyBtYW51YWxseS4iIC1Gb3JlZ3JvdW5kQ29sb3IgUmVkCiAgICAgICAgICAgICAgICBQYXVzZQogICAgICAgICAgICAgICAgRXhpdAogICAgICAgICAgICB9CiAgICAgICAgfQogICAgfQp9IGVsc2UgewogICAgaWYgKFRlc3QtUGF0aCAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIikgewogICAgICAgIEFkZC1QYXRoVG9Vc2VyRW52aXJvbm1lbnQgIi5ccGxhdGZvcm0tdG9vbHMiCiAgICB9Cn0KCiMgQWRkIEMjIGhlbHBlciBmb3IgV2luMzIgbG93LWxhdGVuY3kga2V5IGV2ZW50cyB3aXRoIERpcmVjdFgvRGlyZWN0SW5wdXQgSGFyZHdhcmUgU2NhbkNvZGUgc3VwcG9ydAppZiAoISgiVGFpa29LZXlib2FyZCIgLWFzIFt0eXBlXSkpIHsKICAgICRTaWduYXR1cmUgPSBAIgp1c2luZyBTeXN0ZW07CnVzaW5nIFN5c3RlbS5SdW50aW1lLkludGVyb3BTZXJ2aWNlczsKCnB1YmxpYyBjbGFzcyBUYWlrb0tleWJvYXJkIHsKICAgIFtEbGxJbXBvcnQoInVzZXIzMi5kbGwiKV0KICAgIHB1YmxpYyBzdGF0aWMgZXh0ZXJuIHZvaWQga2V5YmRfZXZlbnQoYnl0ZSBiVmssIGJ5dGUgYlNjYW4sIHVpbnQgZHdGbGFncywgVUludFB0ciBkd0V4dHJhSW5mbyk7CgogICAgW0RsbEltcG9ydCgidXNlcjMyLmRsbCIpXQogICAgcHVibGljIHN0YXRpYyBleHRlcm4gdWludCBNYXBWaXJ0dWFsS2V5KHVpbnQgdUNvZGUsIHVpbnQgdU1hcFR5cGUpOwoKICAgIFtEbGxJbXBvcnQoInVzZXIzMi5kbGwiLCBTZXRMYXN0RXJyb3IgPSB0cnVlKV0KICAgIHB1YmxpYyBzdGF0aWMgZXh0ZXJuIHVpbnQgU2VuZElucHV0KHVpbnQgbklucHV0cywgSW50UHRyIHBJbnB1dHMsIGludCBjYlNpemUpOwoKICAgIHByaXZhdGUgY29uc3QgdWludCBLRVlFVkVOVEZfS0VZVVAgPSAweDAwMDI7CgogICAgcHJpdmF0ZSBzdGF0aWMgdm9pZCBTZW5kRG93blJhdyhieXRlIHZrZXksIHVzaG9ydCBzY2FuQ29kZSkgewogICAgICAgIGtleWJkX2V2ZW50KHZrZXksIChieXRlKXNjYW5Db2RlLCAwLCBVSW50UHRyLlplcm8pOwoKICAgICAgICB0cnkgewogICAgICAgICAgICBpbnQgY2JTaXplID0gKEludFB0ci5TaXplID09IDgpID8gNDAgOiAyODsKICAgICAgICAgICAgYnl0ZVtdIGlucHV0Qnl0ZXMgPSBuZXcgYnl0ZVtjYlNpemVdOwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMSkuQ29weVRvKGlucHV0Qnl0ZXMsIDApOyAvLyBJTlBVVF9LRVlCT0FSRAogICAgICAgICAgICBpbnQga2lPZmZzZXQgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA4IDogNDsKICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKHNjYW5Db2RlKS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyAyKTsgLy8gd1NjYW4KICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKCh1aW50KTB4MDAwOCkuQ29weVRvKGlucHV0Qnl0ZXMsIGtpT2Zmc2V0ICsgNCk7IC8vIEtFWUVWRU5URl9TQ0FOQ09ERQogICAgICAgICAgICBHQ0hhbmRsZSBoYW5kbGUgPSBHQ0hhbmRsZS5BbGxvYyhpbnB1dEJ5dGVzLCBHQ0hhbmRsZVR5cGUuUGlubmVkKTsKICAgICAgICAgICAgU2VuZElucHV0KDEsIGhhbmRsZS5BZGRyT2ZQaW5uZWRPYmplY3QoKSwgY2JTaXplKTsKICAgICAgICAgICAgaGFuZGxlLkZyZWUoKTsKICAgICAgICB9IGNhdGNoIHt9CiAgICB9CgogICAgcHJpdmF0ZSBzdGF0aWMgdm9pZCBTZW5kVXBSYXcoYnl0ZSB2a2V5LCB1c2hvcnQgc2NhbkNvZGUpIHsKICAgICAgICBrZXliZF9ldmVudCh2a2V5LCAoYnl0ZSlzY2FuQ29kZSwgS0VZRVZFTlRGX0tFWVVQLCBVSW50UHRyLlplcm8pOwoKICAgICAgICB0cnkgewogICAgICAgICAgICBpbnQgY2JTaXplID0gKEludFB0ci5TaXplID09IDgpID8gNDAgOiAyODsKICAgICAgICAgICAgYnl0ZVtdIGlucHV0Qnl0ZXMgPSBuZXcgYnl0ZVtjYlNpemVdOwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMSkuQ29weVRvKGlucHV0Qnl0ZXMsIDApOyAvLyBJTlBVVF9LRVlCT0FSRAogICAgICAgICAgICBpbnQga2lPZmZzZXQgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA4IDogNDsKICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKHNjYW5Db2RlKS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyAyKTsgLy8gd1NjYW4KICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKCh1aW50KTB4MDAwYSkuQ29weVRvKGlucHV0Qnl0ZXMsIGtpT2Zmc2V0ICsgNCk7IC8vIEtFWUVWRU5URl9TQ0FOQ09ERSB8IEtFWUVWRU5URl9LRVlVUAogICAgICAgICAgICBHQ0hhbmRsZSBoYW5kbGUgPSBHQ0hhbmRsZS5BbGxvYyhpbnB1dEJ5dGVzLCBHQ0hhbmRsZVR5cGUuUGlubmVkKTsKICAgICAgICAgICAgU2VuZElucHV0KDEsIGhhbmRsZS5BZGRyT2ZQaW5uZWRPYmplY3QoKSwgY2JTaXplKTsKICAgICAgICAgICAgaGFuZGxlLkZyZWUoKTsKICAgICAgICB9IGNhdGNoIHt9CiAgICB9CgogICAgcHVibGljIHN0YXRpYyB2b2lkIERvd24oYnl0ZSB2a2V5KSB7CiAgICAgICAgdXNob3J0IHNjYW5Db2RlID0gKHVzaG9ydClNYXBWaXJ0dWFsS2V5KHZrZXksIDApOwogICAgICAgIFNlbmREb3duUmF3KHZrZXksIHNjYW5Db2RlKTsKICAgIH0KCiAgICBwdWJsaWMgc3RhdGljIHZvaWQgVXAoYnl0ZSB2a2V5KSB7CiAgICAgICAgdXNob3J0IHNjYW5Db2RlID0gKHVzaG9ydClNYXBWaXJ0dWFsS2V5KHZrZXksIDApOwogICAgICAgIFNlbmRVcFJhdyh2a2V5LCBzY2FuQ29kZSk7CiAgICB9Cn0KIkAKICAgIHRyeSB7CiAgICAgICAgQWRkLVR5cGUgLVR5cGVEZWZpbml0aW9uICRTaWduYXR1cmUgLUVycm9yQWN0aW9uIFN0b3AKICAgIH0gY2F0Y2ggewogICAgICAgIFdyaXRlLUhvc3QgIkZhaWxlZCB0byBjb21waWxlIGtleWJvYXJkIGhlbHBlcjogJF8iIC1Gb3JlZ3JvdW5kQ29sb3IgUmVkCiAgICB9Cn0KCldyaXRlLUhvc3QgIj09PSBUYWlrbyBDb250cm9sbGVyIFJlY2VpdmVyIGZvciBXaW5kb3dzID09PSIgLUZvcmVncm91bmRDb2xvciBHcmVlbgpXcml0ZS1Ib3N0ICJTZXR0aW5nIHVwIEFEQiBwb3J0IGZvcndhcmRpbmcgKHRjcDokcG9ydCkuLi4iIC1Gb3JlZ3JvdW5kQ29sb3IgQ3lhbgp0cnkgeyAmICRhZGJDbWQgZm9yd2FyZCAtLXJlbW92ZSB0Y3A6JHBvcnQgMj4kbnVsbCB9IGNhdGNoIHt9CgokZndkT3V0ID0gJiAkYWRiQ21kIGZvcndhcmQgdGNwOiRwb3J0IHRjcDokcG9ydCAyPiYxCmlmICgkTEFTVEVYSVRDT0RFIC1uZSAwKSB7CiAgICBXcml0ZS1Ib3N0ICJOb3RpY2UgZnJvbSBBREI6ICRmd2RPdXQiIC1Gb3JlZ3JvdW5kQ29sb3IgWWVsbG93CiAgICBXcml0ZS1Ib3N0ICJFbnN1cmUgQW5kcm9pZCBkZXZpY2UgaXMgY29ubmVjdGVkIHZpYSBVU0IgYW5kIFVTQiBkZWJ1Z2dpbmcgaXMgZW5hYmxlZCEiIC1Gb3JlZ3JvdW5kQ29sb3IgWWVsbG93Cn0KCldyaXRlLUhvc3QgIkNvbm5lY3RpbmcgdG8gQW5kcm9pZCBUYWlrbyBjb250cm9sbGVyIG9uIGxvY2FsaG9zdDokcG9ydC4uLiIgLUZvcmVncm91bmRDb2xvciBDeWFuCgp3aGlsZSAoJHRydWUpIHsKICAgICRjbGllbnQgPSAkbnVsbAogICAgdHJ5IHsKICAgICAgICAkY2xpZW50ID0gTmV3LU9iamVjdCBTeXN0ZW0uTmV0LlNvY2tldHMuVGNwQ2xpZW50CiAgICAgICAgJGNvbm5lY3RSZXN1bHQgPSAkY2xpZW50LkJlZ2luQ29ubmVjdCgiMTI3LjAuMC4xIiwgJHBvcnQsICRudWxsLCAkbnVsbCkKICAgICAgICAkc3VjY2VzcyA9ICRjb25uZWN0UmVzdWx0LkFzeW5jV2FpdEhhbmRsZS5XYWl0T25lKDMwMDAsICRmYWxzZSkKICAgICAgICBpZiAoISRzdWNjZXNzKSB7CiAgICAgICAgICAgICRjbGllbnQuQ2xvc2UoKQogICAgICAgICAgICB0aHJvdyAiQ29ubmVjdGlvbiB0aW1lb3V0IgogICAgICAgIH0KICAgICAgICAkY2xpZW50LkVuZENvbm5lY3QoJGNvbm5lY3RSZXN1bHQpCgogICAgICAgICRzdHJlYW0gPSAkY2xpZW50LkdldFN0cmVhbSgpCiAgICAgICAgJHN0cmVhbS5SZWFkVGltZW91dCA9IDMwMDAKICAgICAgICAkcmVhZGVyID0gTmV3LU9iamVjdCBTeXN0ZW0uSU8uU3RyZWFtUmVhZGVyKCRzdHJlYW0pCgogICAgICAgICMgUmVhZCBiYW5uZXIgdG8gdmVyaWZ5IHJlYWwgYXBwIGNvbm5lY3Rpb24KICAgICAgICAkYmFubmVyID0gJHJlYWRlci5SZWFkTGluZSgpCiAgICAgICAgaWYgKCRudWxsIC1lcSAkYmFubmVyKSB7CiAgICAgICAgICAgICRjbGllbnQuQ2xvc2UoKQogICAgICAgICAgICBXcml0ZS1Ib3N0ICJXYWl0aW5nIGZvciBBbmRyb2lkIGFwcCBjb25uZWN0aW9uLi4uIChyZXRyeWluZyBpbiAyIHNlY29uZHMpIiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgICAgICAgICBTdGFydC1TbGVlcCAtU2Vjb25kcyAyCiAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgfQoKICAgICAgICAkc3RyZWFtLlJlYWRUaW1lb3V0ID0gLTEKICAgICAgICBXcml0ZS1Ib3N0ICJDb25uZWN0ZWQgc3VjY2Vzc2Z1bGx5IHRvIFRhaWtvIEFwcCEgU3RhcnQgeW91ciBnYW1lIG5vdyEiIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KICAgICAgICAKICAgICAgICB3aGlsZSAoJGNsaWVudC5Db25uZWN0ZWQpIHsKICAgICAgICAgICAgJGxpbmUgPSAkcmVhZGVyLlJlYWRMaW5lKCkKICAgICAgICAgICAgaWYgKCRudWxsIC1lcSAkbGluZSkgewogICAgICAgICAgICAgICAgV3JpdGUtSG9zdCAiRGlzY29ubmVjdGVkIGJ5IEFuZHJvaWQgYXBwLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgICAgIGJyZWFrCiAgICAgICAgICAgIH0KICAgICAgICAgICAgCiAgICAgICAgICAgICRsaW5lID0gJGxpbmUuVHJpbSgpCiAgICAgICAgICAgIGlmICgkbGluZS5MZW5ndGggLWVxIDApIHsgY29udGludWUgfQoKICAgICAgICAgICAgJHBhcnRzID0gJGxpbmUuU3BsaXQoJyAnKQogICAgICAgICAgICBpZiAoJHBhcnRzLkxlbmd0aCAtZ2UgMikgewogICAgICAgICAgICAgICAgJGFjdGlvbiA9ICRwYXJ0c1swXQogICAgICAgICAgICAgICAgZm9yICgkaSA9IDE7ICRpIC1sdCAkcGFydHMuTGVuZ3RoOyAkaSsrKSB7CiAgICAgICAgICAgICAgICAgICAgJGtleSA9ICRwYXJ0c1skaV0uVG9VcHBlcigpCiAgICAgICAgICAgICAgICAgICAgaWYgKCRrZXkuTGVuZ3RoIC1ndCAwKSB7CiAgICAgICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIltLRVldICRhY3Rpb24gLT4gJGtleSIgLUZvcmVncm91bmRDb2xvciBDeWFuCiAgICAgICAgICAgICAgICAgICAgICAgICR2a2V5ID0gW2J5dGVdW2NoYXJdJGtleVswXQogICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCRhY3Rpb24gLWVxICJET1dOIikgewogICAgICAgICAgICAgICAgICAgICAgICAgICAgW1RhaWtvS2V5Ym9hcmRdOjpEb3duKCR2a2V5KQogICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2VpZiAoJGFjdGlvbiAtZXEgIlVQIikgewogICAgICAgICAgICAgICAgICAgICAgICAgICAgW1RhaWtvS2V5Ym9hcmRdOjpVcCgkdmtleSkKICAgICAgICAgICAgICAgICAgICAgICAgfQogICAgICAgICAgICAgICAgICAgIH0KICAgICAgICAgICAgICAgIH0KICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0gY2F0Y2ggewogICAgICAgIFdyaXRlLUhvc3QgIldhaXRpbmcgZm9yIEFuZHJvaWQgYXBwIGNvbm5lY3Rpb24uLi4gKHJldHJ5aW5nIGluIDIgc2Vjb25kcykiIC1Gb3JlZ3JvdW5kQ29sb3IgWWVsbG93CiAgICB9IGZpbmFsbHkgewogICAgICAgIGlmICgkbnVsbCAtbmUgJGNsaWVudCkgewogICAgICAgICAgICB0cnkgeyAkY2xpZW50LkNsb3NlKCkgfSBjYXRjaCB7fQogICAgICAgIH0KICAgIH0KICAgIFN0YXJ0LVNsZWVwIC1TZWNvbmRzIDIKfQo=", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)

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
