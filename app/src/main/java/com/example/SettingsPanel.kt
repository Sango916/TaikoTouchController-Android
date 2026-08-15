package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
    shizukuRunning: Boolean = false,
    shizukuPermission: Boolean = false,
    onRequestShizukuPermission: () -> Unit = {},
    onOpenShizukuApp: () -> Unit = {},
    onRefreshShizukuStatus: () -> Unit = {},
    pcClientsCount: Int = 0,
    remoteSenderStatus: String = "disconnected",
    remoteReceiverClientsCount: Int = 0,
    onConnectRemoteSender: () -> Unit = {},
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

        // --- 0. Display Mode (全画面表示) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F5EB).invertIfDark(isDark)),
            border = BorderStroke(1.dp, Color(0xFF78350F).copy(alpha = 0.15f).invertIfDark(isDark)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📱 全画面コントローラー",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "画面いっぱいに太鼓画面を表示します",
                        fontSize = 10.sp,
                        color = if (isDark) Color.White else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onEnterFullScreen,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706).invertIfDark(isDark)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "全画面",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = false
                    )
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
                            onSettingsChanged(settings.copy(connectionMode = modeVal))
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

            CollapsibleSettingCard(
                title = "📱 別のAndroid連携設定 (${if (settings.anotherAndroidConnectionType == "wired") "有線 USB通信" else "無線 Wi-Fi"})",
                subtitle = if (settings.anotherAndroidRole == "sender") {
                    "役割: 送信側 (太鼓) | 方式: ${if (settings.anotherAndroidConnectionType == "wired") "有線 (USB通信)" else "無線 (Wi-Fi)"} | 接続先: ${settings.anotherAndroidTargetIp}:${settings.anotherAndroidPort}"
                } else {
                    "役割: 受信側 (ゲーム) | 方式: ${if (settings.anotherAndroidConnectionType == "wired") "有線 (USB通信)" else "無線 (Wi-Fi)"} | 待受ポート: ${settings.anotherAndroidPort}"
                },
                badgeText = if (settings.anotherAndroidRole == "sender") "送信側" else "受信側",
                badgeColor = if (settings.anotherAndroidRole == "sender") Color(0xFFDBEAFE) else Color(0xFFDCFCE7),
                badgeTextColor = if (settings.anotherAndroidRole == "sender") Color(0xFF1E40AF) else Color(0xFF166534),
                isExpanded = expandAnotherAndroidCard,
                onExpandedChange = { expandAnotherAndroidCard = it },
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
                        text = if (settings.anotherAndroidConnectionType == "wired") {
                            "2台のAndroid端末をType-C - Type-C ケーブル（またはUSB OTGケーブル）で繋ぐだけ！USB AOAダイレクト通信により、ネットワーク遅延ゼロ・1ms未満の最高速入力レスポンスを実現します。"
                        } else {
                            "2台のAndroid端末を同じWi-Fi（またはネットワーク）に接続し、一方を「送信側（太鼓）」、もう一方を「受信側（ゲーム）」として通信させます。"
                        },
                        fontSize = 10.sp,
                        color = if (isDark) Color.White else Color.DarkGray
                    )

                    // Connection Type Selector
                    Text(
                        text = "接続方式を選択:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F).invertIfDark(isDark)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val connTypes = listOf(
                            "wired" to "有線 (USB通信)",
                            "wireless" to "無線 (Wi-Fi)"
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
                        Text(
                            text = if (settings.anotherAndroidConnectionType == "wired") "【送信側の設定 (有線 USB通信)】" else "【送信側の設定 (無線 Wi-Fi)】",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )

                        if (settings.anotherAndroidConnectionType == "wireless") {
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
                                        val portInt = settings.anotherAndroidPort.toIntOrNull() ?: 60001
                                        TaikoAndroidRemoteSender.scanAndFindReceiverIp(
                                            targetPort = portInt,
                                            connectionType = settings.anotherAndroidConnectionType,
                                            onFound = { foundIp ->
                                                isScanningByAutoDiscovery = false
                                                autoDiscoveryMessage = "✅ 発見しました: $foundIp"
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

                            // Wireless Speedup Tip Card
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFBEB)),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFFCD34D)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "🚀 無線 (Wi-Fi) の遅延・抜けを最小化するコツ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFDE047) else Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "① 受信側 (ゲーム側) または送信側で「Wi-Fiテザリング（アクセスポイント）」をONにして2台を直接接続すると、外部ルーターを経由せず極小遅延でプレイできます\n" +
                                               "② 家庭内Wi-Fi利用時は、ルーターの近くで接続してください",
                                        fontSize = 9.5.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF78350F),
                                        lineHeight = 13.sp
                                    )
                                }
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
                                    placeholder = { Text("192.168.1.100 または 127.0.0.1") },
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
                        } else {
                            // WIRED SENDER
                            val context = LocalContext.current
                            val isConnected = isUsbDirectConnected || remoteSenderStatus == "connected"

                            val statusBg = if (isConnected) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6))
                            val statusTextColor = if (isConnected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else (if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151))
                            val statusText = if (isConnected) "⚡ USB 有線超低遅延通信: 接続完了 (<1ms)" else "🔌 USB ケーブル接続を待機中..."

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
                        Text(
                            text = if (settings.anotherAndroidConnectionType == "wired") "【受信側 (ゲーム) の設定 (有線 USB通信)】" else "【受信側 (ゲーム) の設定 (無線 Wi-Fi)】",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )

                        if (settings.anotherAndroidConnectionType == "wireless") {
                            val clipboardManager = LocalClipboardManager.current
                            val localIp = remember { NetworkUtils.getLocalIpAddress() }

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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "この端末のIPアドレス",
                                            fontSize = 10.sp,
                                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFFC2410C)
                                        )
                                        Text(
                                            text = localIp,
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
                                            clipboardManager.setText(AnnotatedString(localIp))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF0284C7) else Color(0xFFEA580C))
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("IPコピー", fontSize = 10.sp, color = Color.White)
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
            val pythonScript = String(android.util.Base64.decode("IyEvdXNyL2Jpbi9lbnYgcHl0aG9uMwojIG1hY09TIFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIGV4dGVuc2lvbiB0byBzYXZlIGFzOiBjb250cm9sbGVyLmNvbW1hbmQgKG9yIGNvbnRyb2xsZXIucHkpCgppbXBvcnQgc29ja2V0CmltcG9ydCBzdWJwcm9jZXNzCmltcG9ydCB0aW1lCmltcG9ydCBzeXMKaW1wb3J0IG9zCmltcG9ydCB1cmxsaWIucmVxdWVzdAppbXBvcnQgemlwZmlsZQppbXBvcnQgdHJhY2ViYWNrCgpQT1JUID0gNjAwMDEKCmRlZiBsb2cobXNnKToKICAgIHByaW50KG1zZywgZmx1c2g9VHJ1ZSkKCmRlZiBtYWluKCk6CiAgICBsb2coIj09PSBUYWlrbyBDb250cm9sbGVyIFJlY2VpdmVyIGZvciBtYWNPUyA9PT0iKQogICAgCiAgICBhZGJfY21kID0gImFkYiIKCiAgICAjIFZlcmlmeSBBREIKICAgIHRyeToKICAgICAgICBzdWJwcm9jZXNzLnJ1bihbImFkYiIsICJ2ZXJzaW9uIl0sIHN0ZG91dD1zdWJwcm9jZXNzLkRFVk5VTEwsIHN0ZGVycj1zdWJwcm9jZXNzLkRFVk5VTEwpCiAgICBleGNlcHQgRmlsZU5vdEZvdW5kRXJyb3I6CiAgICAgICAgaWYgb3MucGF0aC5leGlzdHMoIi4vcGxhdGZvcm0tdG9vbHMvYWRiIik6CiAgICAgICAgICAgIGFkYl9jbWQgPSAiLi9wbGF0Zm9ybS10b29scy9hZGIiCiAgICAgICAgZWxzZToKICAgICAgICAgICAgbG9nKCJBREIgbm90IGZvdW5kIGluIFBBVEguIENoZWNraW5nIGxvY2FsIHBsYXRmb3JtLXRvb2xzLi4uIikKICAgICAgICAgICAgdHJ5OgogICAgICAgICAgICAgICAgbG9nKCJUcnlpbmcgdG8gaW5zdGFsbCBhbmRyb2lkLXBsYXRmb3JtLXRvb2xzIHZpYSBIb21lYnJldy4uLiIpCiAgICAgICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbImJyZXciLCAiaW5zdGFsbCIsICJhbmRyb2lkLXBsYXRmb3JtLXRvb2xzIl0sIGNoZWNrPVRydWUpCiAgICAgICAgICAgICAgICBhZGJfY21kID0gImFkYiIKICAgICAgICAgICAgZXhjZXB0IChzdWJwcm9jZXNzLlN1YnByb2Nlc3NFcnJvciwgRmlsZU5vdEZvdW5kRXJyb3IpOgogICAgICAgICAgICAgICAgbG9nKCJIb21lYnJldyBub3QgYXZhaWxhYmxlLiBEb3dubG9hZGluZyBvZmZpY2lhbCBBbmRyb2lkIFNESyBQbGF0Zm9ybSBUb29scyBmb3IgbWFjT1MuLi4iKQogICAgICAgICAgICAgICAgdXJsID0gImh0dHBzOi8vZGwuZ29vZ2xlLmNvbS9hbmRyb2lkL3JlcG9zaXRvcnkvcGxhdGZvcm0tdG9vbHMtbGF0ZXN0LWRhcndpbi56aXAiCiAgICAgICAgICAgICAgICB6aXBfcGF0aCA9ICIuL3BsYXRmb3JtLXRvb2xzLnppcCIKICAgICAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgICAgICB1cmxsaWIucmVxdWVzdC51cmxyZXRyaWV2ZSh1cmwsIHppcF9wYXRoKQogICAgICAgICAgICAgICAgICAgIHdpdGggemlwZmlsZS5aaXBGaWxlKHppcF9wYXRoLCAiciIpIGFzIHppcF9yZWY6CiAgICAgICAgICAgICAgICAgICAgICAgIHppcF9yZWYuZXh0cmFjdGFsbCgiLiIpCiAgICAgICAgICAgICAgICAgICAgaWYgb3MucGF0aC5leGlzdHMoemlwX3BhdGgpOgogICAgICAgICAgICAgICAgICAgICAgICBvcy5yZW1vdmUoemlwX3BhdGgpCiAgICAgICAgICAgICAgICAgICAgb3MuY2htb2QoIi4vcGxhdGZvcm0tdG9vbHMvYWRiIiwgMG83NTUpCiAgICAgICAgICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICAgICAgICAgICAgICBsb2coIkFEQiBkb3dubG9hZGVkIGFuZCBleHRyYWN0ZWQgc3VjY2Vzc2Z1bGx5IHRvIC4vcGxhdGZvcm0tdG9vbHMvIikKICAgICAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgICAgICAgICBsb2coZiJFcnJvciBkb3dubG9hZGluZyBwbGF0Zm9ybS10b29sczoge2V9IikKICAgICAgICAgICAgICAgICAgICBsb2coIlBsZWFzZSBpbnN0YWxsIEFEQiBtYW51YWxseSAoZS5nLiBicmV3IGluc3RhbGwgYW5kcm9pZC1wbGF0Zm9ybS10b29scykiKQogICAgICAgICAgICAgICAgICAgIHJldHVybgoKICAgICMgQXV0b21hdGljYWxseSBpbnN0YWxsIHB5bnB1dCBpZiBtaXNzaW5nIG9uIG1hY09TIGZvciBrZXkgc2ltdWxhdGlvbgogICAgdHJ5OgogICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgIGV4Y2VwdCBJbXBvcnRFcnJvcjoKICAgICAgICBsb2coIkluc3RhbGxpbmcgcHlucHV0IGxpYnJhcnkgZm9yIGtleWJvYXJkIHNpbXVsYXRpb24uLi4iKQogICAgICAgIHN1YnByb2Nlc3MuY2hlY2tfY2FsbChbc3lzLmV4ZWN1dGFibGUsICItbSIsICJwaXAiLCAiaW5zdGFsbCIsICJweW5wdXQiXSkKICAgICAgICBmcm9tIHB5bnB1dC5rZXlib2FyZCBpbXBvcnQgS2V5LCBDb250cm9sbGVyCgogICAga2V5Ym9hcmQgPSBDb250cm9sbGVyKCkKCiAgICBkZWYgc2VuZF9kb3duKGtleV9jaGFyKToKICAgICAgICB0cnk6CiAgICAgICAgICAgIGtleWJvYXJkLnByZXNzKGtleV9jaGFyKQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiS2V5IHByZXNzIGVycm9yOiB7ZX0iKQoKICAgIGRlZiBzZW5kX3VwKGtleV9jaGFyKToKICAgICAgICB0cnk6CiAgICAgICAgICAgIGtleWJvYXJkLnJlbGVhc2Uoa2V5X2NoYXIpCiAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgICAgICBsb2coZiJLZXkgcmVsZWFzZSBlcnJvcjoge2V9IikKCiAgICBsb2coZiJTZXR0aW5nIHVwIEFEQiBwb3J0IGZvcndhcmRpbmcgKHRjcDp7UE9SVH0pLi4uIikKICAgIHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAiZm9yd2FyZCIsICItLXJlbW92ZSIsIGYidGNwOntQT1JUfSJdLCBzdGRvdXQ9c3VicHJvY2Vzcy5ERVZOVUxMLCBzdGRlcnI9c3VicHJvY2Vzcy5ERVZOVUxMKQogICAgZndkX3Byb2MgPSBzdWJwcm9jZXNzLnJ1bihbYWRiX2NtZCwgImZvcndhcmQiLCBmInRjcDp7UE9SVH0iLCBmInRjcDp7UE9SVH0iXSwgY2FwdHVyZV9vdXRwdXQ9VHJ1ZSwgdGV4dD1UcnVlKQogICAgaWYgZndkX3Byb2MucmV0dXJuY29kZSAhPSAwOgogICAgICAgIGxvZyhmIkFEQiBmb3J3YXJkIG1lc3NhZ2U6IHtmd2RfcHJvYy5zdGRlcnIuc3RyaXAoKSBvciBmd2RfcHJvYy5zdGRvdXQuc3RyaXAoKX0iKQogICAgICAgIGxvZygiRW5zdXJlIHlvdXIgQW5kcm9pZCBkZXZpY2UgaXMgVVNCIGNvbm5lY3RlZCB3aXRoIFVTQiBkZWJ1Z2dpbmcgZW5hYmxlZC4iKQoKICAgIGxvZyhmIkNvbm5lY3RpbmcgdG8gQW5kcm9pZCBUYWlrbyBjb250cm9sbGVyIG9uIGxvY2FsaG9zdDp7UE9SVH0uLi4iKQogICAgbG9nKCJOb3RlOiBFbnN1cmUgVGVybWluYWwvQXBwIGhhcyBBY2Nlc3NpYmlsaXR5IHBlcm1pc3Npb24gaW4gU3lzdGVtIFNldHRpbmdzIC0+IFByaXZhY3kgJiBTZWN1cml0eSAtPiBBY2Nlc3NpYmlsaXR5LiIpCgogICAgd2hpbGUgVHJ1ZToKICAgICAgICB0cnk6CiAgICAgICAgICAgIHMgPSBzb2NrZXQuc29ja2V0KHNvY2tldC5BRl9JTkVULCBzb2NrZXQuU09DS19TVFJFQU0pCiAgICAgICAgICAgIHMuc2V0dGltZW91dCgzLjApCiAgICAgICAgICAgIHMuY29ubmVjdCgoIjEyNy4wLjAuMSIsIFBPUlQpKQogICAgICAgICAgICAKICAgICAgICAgICAgZiA9IHMubWFrZWZpbGUoInIiLCBlbmNvZGluZz0idXRmLTgiKQogICAgICAgICAgICBiYW5uZXIgPSBmLnJlYWRsaW5lKCkKICAgICAgICAgICAgaWYgbm90IGJhbm5lcjoKICAgICAgICAgICAgICAgIHMuY2xvc2UoKQogICAgICAgICAgICAgICAgbG9nKCJXYWl0aW5nIGZvciBBbmRyb2lkIGFwcCByZXNwb25zZS4uLiAocmV0cnlpbmcgaW4gMnMpIikKICAgICAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKICAgICAgICAgICAgICAgIGNvbnRpbnVlCgogICAgICAgICAgICBzLnNldHRpbWVvdXQoTm9uZSkKICAgICAgICAgICAgbG9nKCJDb25uZWN0ZWQgc3VjY2Vzc2Z1bGx5IHRvIFRhaWtvIEFwcCEgU3RhcnQgeW91ciBnYW1lIG5vdyEiKQogICAgICAgICAgICAKICAgICAgICAgICAgZm9yIGxpbmUgaW4gZjoKICAgICAgICAgICAgICAgIGxpbmUgPSBsaW5lLnN0cmlwKCkKICAgICAgICAgICAgICAgIGlmIG5vdCBsaW5lOgogICAgICAgICAgICAgICAgICAgIGNvbnRpbnVlCiAgICAgICAgICAgICAgICBwYXJ0cyA9IGxpbmUuc3BsaXQoIiAiKQogICAgICAgICAgICAgICAgaWYgbGVuKHBhcnRzKSA9PSAyOgogICAgICAgICAgICAgICAgICAgIGFjdGlvbiwga2V5X2NoYXIgPSBwYXJ0c1swXSwgcGFydHNbMV0ubG93ZXIoKQogICAgICAgICAgICAgICAgICAgIGxvZyhmIltLRVldIHthY3Rpb259IC0+IHtrZXlfY2hhcn0iKQogICAgICAgICAgICAgICAgICAgIGlmIGFjdGlvbiA9PSAiRE9XTiI6CiAgICAgICAgICAgICAgICAgICAgICAgIHNlbmRfZG93bihrZXlfY2hhcikKICAgICAgICAgICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAiVVAiOgogICAgICAgICAgICAgICAgICAgICAgICBzZW5kX3VwKGtleV9jaGFyKQoKICAgICAgICAgICAgbG9nKCJEaXNjb25uZWN0ZWQgYnkgQW5kcm9pZCBhcHAuIFJlY29ubmVjdGluZyBpbiAyIHNlY29uZHMuLi4iKQogICAgICAgICAgICBzLmNsb3NlKCkKICAgICAgICAgICAgdGltZS5zbGVlcCgyKQogICAgICAgIGV4Y2VwdCAoc29ja2V0LnRpbWVvdXQsIENvbm5lY3Rpb25SZWZ1c2VkRXJyb3IsIE9TRXJyb3IpOgogICAgICAgICAgICBsb2coIldhaXRpbmcgZm9yIEFuZHJvaWQgYXBwIGNvbm5lY3Rpb24uLi4gKHJldHJ5aW5nIGluIDIgc2Vjb25kcykiKQogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgZXhjZXB0IEtleWJvYXJkSW50ZXJydXB0OgogICAgICAgICAgICBsb2coIkV4aXRpbmcuLi4iKQogICAgICAgICAgICBicmVhawogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiRXJyb3I6IHtlfS4gUmVjb25uZWN0aW5nIGluIDIgc2Vjb25kcy4uLiIpCiAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKCmlmIF9fbmFtZV9fID09ICJfX21haW5fXyI6CiAgICB0cnk6CiAgICAgICAgbWFpbigpCiAgICBleGNlcHQgRXhjZXB0aW9uIGFzIGVycjoKICAgICAgICBwcmludChmIkZhdGFsIEVycm9yOiB7ZXJyfSIsIGZsdXNoPVRydWUpCiAgICAgICAgdHJhY2ViYWNrLnByaW50X2V4YygpCiAgICAgICAgaW5wdXQoIlByZXNzIEVudGVyIHRvIGV4aXQuLi4iKQo=", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)
            val powerShellScript = String(android.util.Base64.decode("IyBXaW5kb3dzIFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKIyBGaWxlIGV4dGVuc2lvbiB0byBzYXZlIGFzOiBjb250cm9sbGVyLnBzMQojIFVzYWdlOiBSaWdodC1jbGljayB0aGUgc2F2ZWQgZmlsZSBhbmQgc2VsZWN0ICJSdW4gd2l0aCBQb3dlclNoZWxsIgoKJHBvcnQgPSA2MDAwMQokYWRiQ21kID0gImFkYiIKCiMgQ2hlY2sgaWYgYWRiIGlzIGluIFBBVEgKaWYgKCEoR2V0LUNvbW1hbmQgYWRiIC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSkgewogICAgV3JpdGUtSG9zdCAiQURCIGlzIG5vdCBpbiBQQVRILiBDaGVja2luZyBsb2NhbCBwbGF0Zm9ybS10b29scy4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgIAogICAgaWYgKFRlc3QtUGF0aCAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIikgewogICAgICAgICRhZGJDbWQgPSAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIgogICAgICAgIFdyaXRlLUhvc3QgIkZvdW5kIGxvY2FsIEFEQiBpbiBwbGF0Zm9ybS10b29scyBmb2xkZXIuIiAtRm9yZWdyb3VuZENvbG9yIEdyZWVuCiAgICB9IGVsc2UgewogICAgICAgIFdyaXRlLUhvc3QgIlRyeWluZyB0byBpbnN0YWxsIEFEQiB2aWEgd2luZ2V0Li4uIiAtRm9yZWdyb3VuZENvbG9yIEN5YW4KICAgICAgICBpZiAoR2V0LUNvbW1hbmQgd2luZ2V0IC1FcnJvckFjdGlvbiBTaWxlbnRseUNvbnRpbnVlKSB7CiAgICAgICAgICAgIHRyeSB7CiAgICAgICAgICAgICAgICB3aW5nZXQgaW5zdGFsbCBHb29nbGUuQWRiIC0tc2lsZW50IC0tYWNjZXB0LXNvdXJjZS1hZ3JlZW1lbnRzIC0tYWNjZXB0LXBhY2thZ2UtYWdyZWVtZW50cyB8IE91dC1OdWxsCiAgICAgICAgICAgICAgICAkZW52OlBhdGggPSBbU3lzdGVtLkVudmlyb25tZW50XTo6R2V0RW52aXJvbm1lbnRWYXJpYWJsZSgiUGF0aCIsIk1hY2hpbmUiKSArICI7IiArIFtTeXN0ZW0uRW52aXJvbm1lbnRdOjpHZXRFbnZpcm9ubWVudFZhcmlhYmxlKCJQYXRoIiwiVXNlciIpCiAgICAgICAgICAgIH0gY2F0Y2gge30KICAgICAgICB9CgogICAgICAgIGlmICghKEdldC1Db21tYW5kIGFkYiAtRXJyb3JBY3Rpb24gU2lsZW50bHlDb250aW51ZSkpIHsKICAgICAgICAgICAgV3JpdGUtSG9zdCAid2luZ2V0IHVuYXZhaWxhYmxlLiBEb3dubG9hZGluZyBvZmZpY2lhbCBBbmRyb2lkIFNESyBQbGF0Zm9ybSBUb29scy4uLiIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgJHVybCA9ICJodHRwczovL2RsLmdvb2dsZS5jb20vYW5kcm9pZC9yZXBvc2l0b3J5L3BsYXRmb3JtLXRvb2xzLWxhdGVzdC13aW5kb3dzLnppcCIKICAgICAgICAgICAgJG91dHB1dCA9ICIuXHBsYXRmb3JtLXRvb2xzLnppcCIKICAgICAgICAgICAgdHJ5IHsKICAgICAgICAgICAgICAgIEludm9rZS1XZWJSZXF1ZXN0IC1VcmkgJHVybCAtT3V0RmlsZSAkb3V0cHV0CiAgICAgICAgICAgICAgICBFeHBhbmQtQXJjaGl2ZSAtUGF0aCAkb3V0cHV0IC1EZXN0aW5hdGlvblBhdGggIi4iIC1Gb3JjZQogICAgICAgICAgICAgICAgUmVtb3ZlLUl0ZW0gJG91dHB1dAogICAgICAgICAgICAgICAgaWYgKFRlc3QtUGF0aCAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIikgewogICAgICAgICAgICAgICAgICAgICRhZGJDbWQgPSAiLlxwbGF0Zm9ybS10b29sc1xhZGIuZXhlIgogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkFEQiBkb3dubG9hZGVkIGFuZCBleHRyYWN0ZWQgc3VjY2Vzc2Z1bGx5ISIgLUZvcmVncm91bmRDb2xvciBHcmVlbgogICAgICAgICAgICAgICAgfSBlbHNlIHsKICAgICAgICAgICAgICAgICAgICBXcml0ZS1Ib3N0ICJFcnJvcjogRmFpbGVkIHRvIGV4dHJhY3QgcGxhdGZvcm0tdG9vbHMuIiAtRm9yZWdyb3VuZENvbG9yIFJlZAogICAgICAgICAgICAgICAgICAgIFBhdXNlCiAgICAgICAgICAgICAgICAgICAgRXhpdAogICAgICAgICAgICAgICAgfQogICAgICAgICAgICB9IGNhdGNoIHsKICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkVycm9yOiBDb3VsZCBub3QgZG93bmxvYWQgQURCLiBQbGVhc2UgaW5zdGFsbCBBREIgb3IgcGxhdGZvcm0tdG9vbHMgbWFudWFsbHkuIiAtRm9yZWdyb3VuZENvbG9yIFJlZAogICAgICAgICAgICAgICAgUGF1c2UKICAgICAgICAgICAgICAgIEV4aXQKICAgICAgICAgICAgfQogICAgICAgIH0KICAgIH0KfQoKIyBBZGQgQyMgaGVscGVyIGZvciBXaW4zMiBsb3ctbGF0ZW5jeSBrZXkgZXZlbnRzIHdpdGggRGlyZWN0WC9EaXJlY3RJbnB1dCBIYXJkd2FyZSBTY2FuQ29kZSBzdXBwb3J0CmlmICghKCJUYWlrb0tleWJvYXJkIiAtYXMgW3R5cGVdKSkgewogICAgJFNpZ25hdHVyZSA9IEAiCnVzaW5nIFN5c3RlbTsKdXNpbmcgU3lzdGVtLlJ1bnRpbWUuSW50ZXJvcFNlcnZpY2VzOwoKcHVibGljIGNsYXNzIFRhaWtvS2V5Ym9hcmQgewogICAgW0RsbEltcG9ydCgidXNlcjMyLmRsbCIpXQogICAgcHVibGljIHN0YXRpYyBleHRlcm4gdm9pZCBrZXliZF9ldmVudChieXRlIGJWaywgYnl0ZSBiU2NhbiwgdWludCBkd0ZsYWdzLCBVSW50UHRyIGR3RXh0cmFJbmZvKTsKCiAgICBbRGxsSW1wb3J0KCJ1c2VyMzIuZGxsIildCiAgICBwdWJsaWMgc3RhdGljIGV4dGVybiB1aW50IE1hcFZpcnR1YWxLZXkodWludCB1Q29kZSwgdWludCB1TWFwVHlwZSk7CgogICAgW0RsbEltcG9ydCgidXNlcjMyLmRsbCIsIFNldExhc3RFcnJvciA9IHRydWUpXQogICAgcHVibGljIHN0YXRpYyBleHRlcm4gdWludCBTZW5kSW5wdXQodWludCBuSW5wdXRzLCBJbnRQdHIgcElucHV0cywgaW50IGNiU2l6ZSk7CgogICAgcHJpdmF0ZSBjb25zdCB1aW50IEtFWUVWRU5URl9LRVlVUCA9IDB4MDAwMjsKCiAgICBwcml2YXRlIHN0YXRpYyB2b2lkIFNlbmREb3duUmF3KGJ5dGUgdmtleSwgdXNob3J0IHNjYW5Db2RlKSB7CiAgICAgICAga2V5YmRfZXZlbnQodmtleSwgKGJ5dGUpc2NhbkNvZGUsIDAsIFVJbnRQdHIuWmVybyk7CgogICAgICAgIHRyeSB7CiAgICAgICAgICAgIGludCBjYlNpemUgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA0MCA6IDI4OwogICAgICAgICAgICBieXRlW10gaW5wdXRCeXRlcyA9IG5ldyBieXRlW2NiU2l6ZV07CiAgICAgICAgICAgIEJpdENvbnZlcnRlci5HZXRCeXRlcygodWludCkxKS5Db3B5VG8oaW5wdXRCeXRlcywgMCk7IC8vIElOUFVUX0tFWUJPQVJECiAgICAgICAgICAgIGludCBraU9mZnNldCA9IChJbnRQdHIuU2l6ZSA9PSA4KSA/IDggOiA0OwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoc2NhbkNvZGUpLkNvcHlUbyhpbnB1dEJ5dGVzLCBraU9mZnNldCArIDIpOyAvLyB3U2NhbgogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMHgwMDA4KS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyA0KTsgLy8gS0VZRVZFTlRGX1NDQU5DT0RFCgogICAgICAgICAgICBHQ0hhbmRsZSBoYW5kbGUgPSBHQ0hhbmRsZS5BbGxvYyhpbnB1dEJ5dGVzLCBHQ0hhbmRsZVR5cGUuUGlubmVkKTsKICAgICAgICAgICAgU2VuZElucHV0KDEsIGhhbmRsZS5BZGRyT2ZQaW5uZWRPYmplY3QoKSwgY2JTaXplKTsKICAgICAgICAgICAgaGFuZGxlLkZyZWUoKTsKICAgICAgICB9IGNhdGNoIHt9CiAgICB9CgogICAgcHJpdmF0ZSBzdGF0aWMgdm9pZCBTZW5kVXBSYXcoYnl0ZSB2a2V5LCB1c2hvcnQgc2NhbkNvZGUpIHsKICAgICAgICBrZXliZF9ldmVudCh2a2V5LCAoYnl0ZSlzY2FuQ29kZSwgS0VZRVZFTlRGX0tFWVVQLCBVSW50UHRyLlplcm8pOwoKICAgICAgICB0cnkgewogICAgICAgICAgICBpbnQgY2JTaXplID0gKEludFB0ci5TaXplID09IDgpID8gNDAgOiAyODsKICAgICAgICAgICAgYnl0ZVtdIGlucHV0Qnl0ZXMgPSBuZXcgYnl0ZVtjYlNpemVdOwogICAgICAgICAgICBCaXRDb252ZXJ0ZXIuR2V0Qnl0ZXMoKHVpbnQpMSkuQ29weVRvKGlucHV0Qnl0ZXMsIDApOyAvLyBJTlBVVF9LRVlCT0FSRAogICAgICAgICAgICBpbnQga2lPZmZzZXQgPSAoSW50UHRyLlNpemUgPT0gOCkgPyA4IDogNDsKICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKHNjYW5Db2RlKS5Db3B5VG8oaW5wdXRCeXRlcywga2lPZmZzZXQgKyAyKTsgLy8gd1NjYW4KICAgICAgICAgICAgQml0Q29udmVydGVyLkdldEJ5dGVzKCh1aW50KTB4MDAwYSkuQ29weVRvKGlucHV0Qnl0ZXMsIGtpT2Zmc2V0ICsgNCk7IC8vIEtFWUVWRU5URl9TQ0FOQ09ERSB8IEtFWUVWRU5URl9LRVlVUAoKICAgICAgICAgICAgR0NIYW5kbGUgaGFuZGxlID0gR0NIYW5kbGUuQWxsb2MoaW5wdXRCeXRlcywgR0NIYW5kbGVUeXBlLlBpbm5lZCk7CiAgICAgICAgICAgIFNlbmRJbnB1dCgxLCBoYW5kbGUuQWRkck9mUGlubmVkT2JqZWN0KCksIGNiU2l6ZSk7CiAgICAgICAgICAgIGhhbmRsZS5GcmVlKCk7CiAgICAgICAgfSBjYXRjaCB7fQogICAgfQoKICAgIHB1YmxpYyBzdGF0aWMgdm9pZCBEb3duKGJ5dGUgdmtleSkgewogICAgICAgIHVzaG9ydCBzY2FuQ29kZSA9ICh1c2hvcnQpTWFwVmlydHVhbEtleSh2a2V5LCAwKTsKICAgICAgICBTZW5kRG93blJhdyh2a2V5LCBzY2FuQ29kZSk7CiAgICB9CgogICAgcHVibGljIHN0YXRpYyB2b2lkIFVwKGJ5dGUgdmtleSkgewogICAgICAgIHVzaG9ydCBzY2FuQ29kZSA9ICh1c2hvcnQpTWFwVmlydHVhbEtleSh2a2V5LCAwKTsKICAgICAgICBTZW5kVXBSYXcodmtleSwgc2NhbkNvZGUpOwogICAgfQp9CiJACiAgICB0cnkgewogICAgICAgIEFkZC1UeXBlIC1UeXBlRGVmaW5pdGlvbiAkU2lnbmF0dXJlIC1FcnJvckFjdGlvbiBTdG9wCiAgICB9IGNhdGNoIHsKICAgICAgICBXcml0ZS1Ib3N0ICJGYWlsZWQgdG8gY29tcGlsZSBrZXlib2FyZCBoZWxwZXI6ICRfIiAtRm9yZWdyb3VuZENvbG9yIFJlZAogICAgfQp9CgpXcml0ZS1Ib3N0ICI9PT0gVGFpa28gQ29udHJvbGxlciBSZWNlaXZlciBmb3IgV2luZG93cyA9PT0iIC1Gb3JlZ3JvdW5kQ29sb3IgR3JlZW4KV3JpdGUtSG9zdCAiU2V0dGluZyB1cCBBREIgcG9ydCBmb3J3YXJkaW5nICh0Y3A6JHBvcnQpLi4uIiAtRm9yZWdyb3VuZENvbG9yIEN5YW4KdHJ5IHsgJiAkYWRiQ21kIGZvcndhcmQgLS1yZW1vdmUgdGNwOiRwb3J0IDI+JG51bGwgfSBjYXRjaCB7fQoKJGZ3ZE91dCA9ICYgJGFkYkNtZCBmb3J3YXJkIHRjcDokcG9ydCB0Y3A6JHBvcnQgMj4mMQppZiAoJExBU1RFWElUQ09ERSAtbmUgMCkgewogICAgV3JpdGUtSG9zdCAiTm90aWNlIGZyb20gQURCOiAkZndkT3V0IiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgV3JpdGUtSG9zdCAiRW5zdXJlIEFuZHJvaWQgZGV2aWNlIGlzIGNvbm5lY3RlZCB2aWEgVVNCIGFuZCBVU0IgZGVidWdnaW5nIGlzIGVuYWJsZWQhIiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwp9CgpXcml0ZS1Ib3N0ICJDb25uZWN0aW5nIHRvIEFuZHJvaWQgVGFpa28gY29udHJvbGxlciBvbiBsb2NhbGhvc3Q6JHBvcnQuLi4iIC1Gb3JlZ3JvdW5kQ29sb3IgQ3lhbgoKd2hpbGUgKCR0cnVlKSB7CiAgICAkY2xpZW50ID0gJG51bGwKICAgIHRyeSB7CiAgICAgICAgJGNsaWVudCA9IE5ldy1PYmplY3QgU3lzdGVtLk5ldC5Tb2NrZXRzLlRjcENsaWVudAogICAgICAgICRjb25uZWN0UmVzdWx0ID0gJGNsaWVudC5CZWdpbkNvbm5lY3QoIjEyNy4wLjAuMSIsICRwb3J0LCAkbnVsbCwgJG51bGwpCiAgICAgICAgJHN1Y2Nlc3MgPSAkY29ubmVjdFJlc3VsdC5Bc3luY1dhaXRIYW5kbGUuV2FpdE9uZSgzMDAwLCAkZmFsc2UpCiAgICAgICAgaWYgKCEkc3VjY2VzcykgewogICAgICAgICAgICAkY2xpZW50LkNsb3NlKCkKICAgICAgICAgICAgdGhyb3cgIkNvbm5lY3Rpb24gdGltZW91dCIKICAgICAgICB9CiAgICAgICAgJGNsaWVudC5FbmRDb25uZWN0KCRjb25uZWN0UmVzdWx0KQoKICAgICAgICAkc3RyZWFtID0gJGNsaWVudC5HZXRTdHJlYW0oKQogICAgICAgICRzdHJlYW0uUmVhZFRpbWVvdXQgPSAzMDAwCiAgICAgICAgJHJlYWRlciA9IE5ldy1PYmplY3QgU3lzdGVtLklPLlN0cmVhbVJlYWRlcigkc3RyZWFtKQoKICAgICAgICAjIFJlYWQgYmFubmVyIHRvIHZlcmlmeSByZWFsIGFwcCBjb25uZWN0aW9uCiAgICAgICAgJGJhbm5lciA9ICRyZWFkZXIuUmVhZExpbmUoKQogICAgICAgIGlmICgkbnVsbCAtZXEgJGJhbm5lcikgewogICAgICAgICAgICAkY2xpZW50LkNsb3NlKCkKICAgICAgICAgICAgV3JpdGUtSG9zdCAiV2FpdGluZyBmb3IgQW5kcm9pZCBhcHAgY29ubmVjdGlvbi4uLiAocmV0cnlpbmcgaW4gMiBzZWNvbmRzKSIgLUZvcmVncm91bmRDb2xvciBZZWxsb3cKICAgICAgICAgICAgU3RhcnQtU2xlZXAgLVNlY29uZHMgMgogICAgICAgICAgICBjb250aW51ZQogICAgICAgIH0KCiAgICAgICAgJHN0cmVhbS5SZWFkVGltZW91dCA9IC0xCiAgICAgICAgV3JpdGUtSG9zdCAiQ29ubmVjdGVkIHN1Y2Nlc3NmdWxseSB0byBUYWlrbyBBcHAhIFN0YXJ0IHlvdXIgZ2FtZSBub3chIiAtRm9yZWdyb3VuZENvbG9yIEdyZWVuCiAgICAgICAgCiAgICAgICAgd2hpbGUgKCRjbGllbnQuQ29ubmVjdGVkKSB7CiAgICAgICAgICAgICRsaW5lID0gJHJlYWRlci5SZWFkTGluZSgpCiAgICAgICAgICAgIGlmICgkbnVsbCAtZXEgJGxpbmUpIHsKICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIkRpc2Nvbm5lY3RlZCBieSBBbmRyb2lkIGFwcC4iIC1Gb3JlZ3JvdW5kQ29sb3IgWWVsbG93CiAgICAgICAgICAgICAgICBicmVhawogICAgICAgICAgICB9CiAgICAgICAgICAgIAogICAgICAgICAgICAkbGluZSA9ICRsaW5lLlRyaW0oKQogICAgICAgICAgICBpZiAoJGxpbmUuTGVuZ3RoIC1lcSAwKSB7IGNvbnRpbnVlIH0KCiAgICAgICAgICAgICRwYXJ0cyA9ICRsaW5lLlNwbGl0KCcgJykKICAgICAgICAgICAgaWYgKCRwYXJ0cy5MZW5ndGggLWVxIDIpIHsKICAgICAgICAgICAgICAgICRhY3Rpb24gPSAkcGFydHNbMF0KICAgICAgICAgICAgICAgICRrZXkgPSAkcGFydHNbMV0uVG9VcHBlcigpCiAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgIGlmICgka2V5Lkxlbmd0aCAtZ3QgMCkgewogICAgICAgICAgICAgICAgICAgIFdyaXRlLUhvc3QgIltLRVldICRhY3Rpb24gLT4gJGtleSIgLUZvcmVncm91bmRDb2xvciBDeWFuCiAgICAgICAgICAgICAgICAgICAgJHZrZXkgPSBbYnl0ZV1bY2hhcl0ka2V5WzBdCiAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgaWYgKCRhY3Rpb24gLWVxICJET1dOIikgewogICAgICAgICAgICAgICAgICAgICAgICBbVGFpa29LZXlib2FyZF06OkRvd24oJHZrZXkpCiAgICAgICAgICAgICAgICAgICAgfSBlbHNlaWYgKCRhY3Rpb24gLWVxICJVUCIpIHsKICAgICAgICAgICAgICAgICAgICAgICAgW1RhaWtvS2V5Ym9hcmRdOjpVcCgkdmtleSkKICAgICAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgICAgICB9CiAgICAgICAgICAgIH0KICAgICAgICB9CiAgICB9IGNhdGNoIHsKICAgICAgICBXcml0ZS1Ib3N0ICJXYWl0aW5nIGZvciBBbmRyb2lkIGFwcCBjb25uZWN0aW9uLi4uIChyZXRyeWluZyBpbiAyIHNlY29uZHMpIiAtRm9yZWdyb3VuZENvbG9yIFllbGxvdwogICAgfSBmaW5hbGx5IHsKICAgICAgICBpZiAoJG51bGwgLW5lICRjbGllbnQpIHsKICAgICAgICAgICAgdHJ5IHsgJGNsaWVudC5DbG9zZSgpIH0gY2F0Y2gge30KICAgICAgICB9CiAgICB9CiAgICBTdGFydC1TbGVlcCAtU2Vjb25kcyAyCn0K", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)

            val bashScript = String(android.util.Base64.decode("IyEvdXNyL2Jpbi9lbnYgcHl0aG9uMwojIExpbnV4IFBDLXNpZGUgUmVjZWl2ZXIgZm9yIFRhaWtvIENvbnRyb2xsZXIKaW1wb3J0IHNvY2tldCwgc3VicHJvY2VzcywgdGltZSwgc3lzLCBvcywgdXJsbGliLnJlcXVlc3QsIHppcGZpbGUsIHRyYWNlYmFjawoKUE9SVCA9IDYwMDAxCgpkZWYgbG9nKG1zZyk6CiAgICBwcmludChtc2csIGZsdXNoPVRydWUpCgpkZWYgbWFpbigpOgogICAgbG9nKCI9PT0gVGFpa28gQ29udHJvbGxlciBSZWNlaXZlciBmb3IgTGludXggPT09IikKICAgIGFkYl9jbWQgPSAiYWRiIgogICAgdHJ5OgogICAgICAgIHN1YnByb2Nlc3MucnVuKFsiYWRiIiwgInZlcnNpb24iXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgIGV4Y2VwdCBGaWxlTm90Rm91bmRFcnJvcjoKICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cygiLi9wbGF0Zm9ybS10b29scy9hZGIiKToKICAgICAgICAgICAgYWRiX2NtZCA9ICIuL3BsYXRmb3JtLXRvb2xzL2FkYiIKICAgICAgICBlbHNlOgogICAgICAgICAgICBsb2coImFkYiBub3QgZm91bmQgaW4gUEFUSC4gQ2hlY2tpbmcgbG9jYWwgcGxhdGZvcm0tdG9vbHMuLi4iKQogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vYXB0LWdldCIpOgogICAgICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsic3VkbyIsICJhcHQtZ2V0IiwgInVwZGF0ZSJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbInN1ZG8iLCAiYXB0LWdldCIsICJpbnN0YWxsIiwgIi15IiwgImFkYiJdLCBjaGVjaz1UcnVlKQogICAgICAgICAgICAgICAgZWxpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vcGFjbWFuIik6CiAgICAgICAgICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oWyJzdWRvIiwgInBhY21hbiIsICItU3kiLCAiLS1ub2NvbmZpcm0iLCAiYW5kcm9pZC10b29scyJdLCBjaGVjaz1UcnVlKQogICAgICAgICAgICAgICAgZWxpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vZG5mIik6CiAgICAgICAgICAgICAgICAgICAgc3VicHJvY2Vzcy5ydW4oWyJzdWRvIiwgImRuZiIsICJpbnN0YWxsIiwgIi15IiwgImFuZHJvaWQtdG9vbHMiXSwgY2hlY2s9VHJ1ZSkKICAgICAgICAgICAgICAgIGVsc2U6CiAgICAgICAgICAgICAgICAgICAgcmFpc2UgRmlsZU5vdEZvdW5kRXJyb3IoKQogICAgICAgICAgICAgICAgYWRiX2NtZCA9ICJhZGIiCiAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgICAgICBsb2coIkRvd25sb2FkaW5nIG9mZmljaWFsIEFuZHJvaWQgU0RLIFBsYXRmb3JtIFRvb2xzIGZvciBMaW51eC4uLiIpCiAgICAgICAgICAgICAgICB1cmwgPSAiaHR0cHM6Ly9kbC5nb29nbGUuY29tL2FuZHJvaWQvcmVwb3NpdG9yeS9wbGF0Zm9ybS10b29scy1sYXRlc3QtbGludXguemlwIgogICAgICAgICAgICAgICAgemlwX3BhdGggPSAiLi9wbGF0Zm9ybS10b29scy56aXAiCiAgICAgICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICAgICAgdXJsbGliLnJlcXVlc3QudXJscmV0cmlldmUodXJsLCB6aXBfcGF0aCkKICAgICAgICAgICAgICAgICAgICB3aXRoIHppcGZpbGUuWmlwRmlsZSh6aXBfcGF0aCwgInIiKSBhcyB6aXBfcmVmOgogICAgICAgICAgICAgICAgICAgICAgICB6aXBfcmVmLmV4dHJhY3RhbGwoIi4iKQogICAgICAgICAgICAgICAgICAgIGlmIG9zLnBhdGguZXhpc3RzKHppcF9wYXRoKToKICAgICAgICAgICAgICAgICAgICAgICAgb3MucmVtb3ZlKHppcF9wYXRoKQogICAgICAgICAgICAgICAgICAgIG9zLmNobW9kKCIuL3BsYXRmb3JtLXRvb2xzL2FkYiIsIDBvNzU1KQogICAgICAgICAgICAgICAgICAgIGFkYl9jbWQgPSAiLi9wbGF0Zm9ybS10b29scy9hZGIiCiAgICAgICAgICAgICAgICAgICAgbG9nKCJBREIgZG93bmxvYWRpbmcgY29tcGxldGVkLiIpCiAgICAgICAgICAgICAgICBleGNlcHQgRXhjZXB0aW9uIGFzIGU6CiAgICAgICAgICAgICAgICAgICAgbG9nKGYiRXJyb3IgZG93bmxvYWRpbmcgcGxhdGZvcm0tdG9vbHM6IHtlfSIpCiAgICAgICAgICAgICAgICAgICAgcmV0dXJuCgogICAgaGFzX3hkb3Rvb2wgPSBGYWxzZQogICAgdHJ5OgogICAgICAgIHN1YnByb2Nlc3MucnVuKFsieGRvdG9vbCIsICItLXZlcnNpb24iXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgICAgICBoYXNfeGRvdG9vbCA9IFRydWUKICAgIGV4Y2VwdCBGaWxlTm90Rm91bmRFcnJvcjoKICAgICAgICBsb2coIkNoZWNraW5nIHhkb3Rvb2wgLyBweW5wdXQgZm9yIGxpbnV4IGtleSBpbmplY3Rpb24uLi4iKQogICAgICAgIHRyeToKICAgICAgICAgICAgaWYgb3MucGF0aC5leGlzdHMoIi91c3IvYmluL2FwdC1nZXQiKToKICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsic3VkbyIsICJhcHQtZ2V0IiwgImluc3RhbGwiLCAiLXkiLCAieGRvdG9vbCJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgIGhhc194ZG90b29sID0gVHJ1ZQogICAgICAgICAgICBlbGlmIG9zLnBhdGguZXhpc3RzKCIvdXNyL2Jpbi9wYWNtYW4iKToKICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsic3VkbyIsICJwYWNtYW4iLCAiLVN5IiwgIi0tbm9jb25maXJtIiwgInhkb3Rvb2wiXSwgY2hlY2s9RmFsc2UpCiAgICAgICAgICAgICAgICBoYXNfeGRvdG9vbCA9IFRydWUKICAgICAgICAgICAgZWxpZiBvcy5wYXRoLmV4aXN0cygiL3Vzci9iaW4vZG5mIik6CiAgICAgICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbInN1ZG8iLCAiZG5mIiwgImluc3RhbGwiLCAiLXkiLCAieGRvdG9vbCJdLCBjaGVjaz1GYWxzZSkKICAgICAgICAgICAgICAgIGhhc194ZG90b29sID0gVHJ1ZQogICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgIHBhc3MKCiAgICB1c2VfcHlucHV0ID0gRmFsc2UKICAgIGlmIG5vdCBoYXNfeGRvdG9vbDoKICAgICAgICB0cnk6CiAgICAgICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgICAgICAgICAgdXNlX3B5bnB1dCA9IFRydWUKICAgICAgICBleGNlcHQgSW1wb3J0RXJyb3I6CiAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgIHN1YnByb2Nlc3MuY2hlY2tfY2FsbChbc3lzLmV4ZWN1dGFibGUsICItbSIsICJwaXAiLCAiaW5zdGFsbCIsICJweW5wdXQiXSkKICAgICAgICAgICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgICAgICAgICAgICAgIHVzZV9weW5wdXQgPSBUcnVlCiAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgICAgICBsb2coIldhcm5pbmc6IE5laXRoZXIgeGRvdG9vbCBub3IgcHlucHV0IGNvdWxkIGJlIGluaXRpYWxpemVkLiIpCgogICAga2V5Ym9hcmQgPSBOb25lCiAgICBpZiB1c2VfcHlucHV0OgogICAgICAgIGZyb20gcHlucHV0LmtleWJvYXJkIGltcG9ydCBLZXksIENvbnRyb2xsZXIKICAgICAgICBrZXlib2FyZCA9IENvbnRyb2xsZXIoKQoKICAgIGRlZiBzZW5kX2Rvd24oa2V5X2NoYXIpOgogICAgICAgIGlmIGhhc194ZG90b29sOgogICAgICAgICAgICBzdWJwcm9jZXNzLnJ1bihbInhkb3Rvb2wiLCAia2V5ZG93biIsIGtleV9jaGFyXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgICAgICBlbGlmIHVzZV9weW5wdXQgYW5kIGtleWJvYXJkOgogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBrZXlib2FyZC5wcmVzcyhrZXlfY2hhcikKICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgIHBhc3MKCiAgICBkZWYgc2VuZF91cChrZXlfY2hhcik6CiAgICAgICAgaWYgaGFzX3hkb3Rvb2w6CiAgICAgICAgICAgIHN1YnByb2Nlc3MucnVuKFsieGRvdG9vbCIsICJrZXl1cCIsIGtleV9jaGFyXSwgc3Rkb3V0PXN1YnByb2Nlc3MuREVWTlVMTCwgc3RkZXJyPXN1YnByb2Nlc3MuREVWTlVMTCkKICAgICAgICBlbGlmIHVzZV9weW5wdXQgYW5kIGtleWJvYXJkOgogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBrZXlib2FyZC5yZWxlYXNlKGtleV9jaGFyKQogICAgICAgICAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgICAgICAgICAgcGFzcwoKICAgIGxvZyhmIlNldHRpbmcgdXAgQURCIHBvcnQgZm9yd2FyZGluZyAodGNwOntQT1JUfSkuLi4iKQogICAgc3VicHJvY2Vzcy5ydW4oW2FkYl9jbWQsICJmb3J3YXJkIiwgIi0tcmVtb3ZlIiwgZiJ0Y3A6e1BPUlR9Il0sIHN0ZG91dD1zdWJwcm9jZXNzLkRFVk5VTEwsIHN0ZGVycj1zdWJwcm9jZXNzLkRFVk5VTEwpCiAgICBmd2RfcHJvYyA9IHN1YnByb2Nlc3MucnVuKFthZGJfY21kLCAiZm9yd2FyZCIsIGYidGNwOntQT1JUfSIsIGYidGNwOntQT1JUfSJdLCBjYXB0dXJlX291dHB1dD1UcnVlLCB0ZXh0PVRydWUpCiAgICBpZiBmd2RfcHJvYy5yZXR1cm5jb2RlICE9IDA6CiAgICAgICAgbG9nKGYiQURCIGZvcndhcmQgbWVzc2FnZToge2Z3ZF9wcm9jLnN0ZGVyci5zdHJpcCgpIG9yIGZ3ZF9wcm9jLnN0ZG91dC5zdHJpcCgpfSIpCiAgICAgICAgbG9nKCJFbnN1cmUgeW91ciBBbmRyb2lkIGRldmljZSBpcyBVU0IgY29ubmVjdGVkIHdpdGggVVNCIGRlYnVnZ2luZyBlbmFibGVkLiIpCgogICAgbG9nKGYiQ29ubmVjdGluZyB0byBBbmRyb2lkIFRhaWtvIGNvbnRyb2xsZXIgb24gbG9jYWxob3N0OntQT1JUfS4uLiIpCiAgICB3aGlsZSBUcnVlOgogICAgICAgIHRyeToKICAgICAgICAgICAgcyA9IHNvY2tldC5zb2NrZXQoc29ja2V0LkFGX0lORVQsIHNvY2tldC5TT0NLX1NUUkVBTSkKICAgICAgICAgICAgcy5zZXR0aW1lb3V0KDMuMCkKICAgICAgICAgICAgcy5jb25uZWN0KCgiMTI3LjAuMC4xIiwgUE9SVCkpCiAgICAgICAgICAgIAogICAgICAgICAgICBmID0gcy5tYWtlZmlsZSgiciIsIGVuY29kaW5nPSJ1dGYtOCIpCiAgICAgICAgICAgIGJhbm5lciA9IGYucmVhZGxpbmUoKQogICAgICAgICAgICBpZiBub3QgYmFubmVyOgogICAgICAgICAgICAgICAgcy5jbG9zZSgpCiAgICAgICAgICAgICAgICBsb2coIldhaXRpbmcgZm9yIEFuZHJvaWQgYXBwIHJlc3BvbnNlLi4uIChyZXRyeWluZyBpbiAycykiKQogICAgICAgICAgICAgICAgdGltZS5zbGVlcCgyKQogICAgICAgICAgICAgICAgY29udGludWUKCiAgICAgICAgICAgIHMuc2V0dGltZW91dChOb25lKQogICAgICAgICAgICBsb2coIkNvbm5lY3RlZCBzdWNjZXNzZnVsbHkgdG8gVGFpa28gQXBwISBTdGFydCB5b3VyIGdhbWUgbm93ISIpCiAgICAgICAgICAgIAogICAgICAgICAgICBmb3IgbGluZSBpbiBmOgogICAgICAgICAgICAgICAgbGluZSA9IGxpbmUuc3RyaXAoKQogICAgICAgICAgICAgICAgaWYgbm90IGxpbmU6CiAgICAgICAgICAgICAgICAgICAgY29udGludWUKICAgICAgICAgICAgICAgIHBhcnRzID0gbGluZS5zcGxpdCgiICIpCiAgICAgICAgICAgICAgICBpZiBsZW4ocGFydHMpID09IDI6CiAgICAgICAgICAgICAgICAgICAgYWN0aW9uLCBrZXlfY2hhciA9IHBhcnRzWzBdLCBwYXJ0c1sxXS5sb3dlcigpCiAgICAgICAgICAgICAgICAgICAgbG9nKGYiW0tFWV0ge2FjdGlvbn0gLT4ge2tleV9jaGFyfSIpCiAgICAgICAgICAgICAgICAgICAgaWYgYWN0aW9uID09ICJET1dOIjoKICAgICAgICAgICAgICAgICAgICAgICAgc2VuZF9kb3duKGtleV9jaGFyKQogICAgICAgICAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJVUCI6CiAgICAgICAgICAgICAgICAgICAgICAgIHNlbmRfdXAoa2V5X2NoYXIpCgogICAgICAgICAgICBsb2coIkRpc2Nvbm5lY3RlZCBieSBBbmRyb2lkIGFwcC4gUmVjb25uZWN0aW5nIGluIDIgc2Vjb25kcy4uLiIpCiAgICAgICAgICAgIHMuY2xvc2UoKQogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgZXhjZXB0IChzb2NrZXQudGltZW91dCwgQ29ubmVjdGlvblJlZnVzZWRFcnJvciwgT1NFcnJvcik6CiAgICAgICAgICAgIGxvZygiV2FpdGluZyBmb3IgQW5kcm9pZCBhcHAgY29ubmVjdGVkLi4uIChyZXRyeWluZyBpbiAycykiKQogICAgICAgICAgICB0aW1lLnNsZWVwKDIpCiAgICAgICAgZXhjZXB0IEtleWJvYXJkSW50ZXJydXB0OgogICAgICAgICAgICBsb2coIkV4aXRpbmcuLi4iKQogICAgICAgICAgICBicmVhawogICAgICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICAgICAgbG9nKGYiRXJyb3I6IHtlfS4gUmVjb25uZWN0aW5nIGluIDIgc2Vjb25kcy4uLiIpCiAgICAgICAgICAgIHRpbWUuc2xlZXAoMikKCmlmIF9fbmFtZV9fID09ICJfX21haW5fXyI6CiAgICB0cnk6CiAgICAgICAgbWFpbigpCiAgICBleGNlcHQgRXhjZXB0aW9uIGFzIGVycjoKICAgICAgICBsb2coZiJGYXRhbCBFcnJvcjoge2Vycn0iKQogICAgICAgIHRyYWNlYmFjay5wcmludF9leGMoKQogICAgICAgIGlucHV0KCJQcmVzcyBFbnRlciB0byBleGl0Li4uIikK", android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8)

            var activeScriptTab by remember { mutableStateOf(0) } // 0 = Python, 1 = PowerShell, 2 = Bash
            val clipboardManager = LocalClipboardManager.current
            val scriptText = when (activeScriptTab) {
                0 -> powerShellScript
                1 -> pythonScript
                else -> bashScript
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
                            text = "📖 接続手順:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).invertIfDark(isDark)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. 端末の「USBデバッグ」を有効にしてPCに接続します。\n" +
                                   "2. お使いのPCのOSに合わせたタブを選択し、下の「スクリプトをコピー」ボタンを押します。\n" +
                                   "3. コピーした内容をPC側で任意のファイル名・指定された拡張子で保存します。\n" +
                                   "   ※ macOS / Linux版は初回実行時にファイルの実行権限（実行許可）の設定が必要です。\n" +
                                   "      (ファイルプロパティ/情報を見る画面、またはターミナルの chmod +x コマンド等から設定可能です)\n" +
                                   "4. 保存したファイルを実行すると、ADB環境の自動構築・ポート転送・キー入力ツールの準備・接続まで全自動で行われます！",
                            fontSize = 10.sp,
                            color = if (isDark) Color.White else Color.DarkGray,
                            lineHeight = 14.sp
                        )
                    }

                    Divider(color = Color(0xFF78350F).copy(alpha = 0.10f).invertIfDark(isDark))

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
                                    .heightIn(min = 34.dp)
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
                        Text(
                            text = "スクリプトをコピー",
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }


        // --- 3. Drum Settings Options ---
        CollapsibleSettingCard(
            title = "🥁 太鼓の設定 (振動・大音符・ログ)",
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

        // --- Taiko Size & Position Adjustment Card ---
        TaikoSizeSettingCard(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            expandSizeCard = expandSizeCard,
            onExpandSizeCardChange = { expandSizeCard = it },
            isDark = isDark
        )

        // --- 4. Custom Key Configuration Card ---
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
                    .clickable { onExpandedChange(!isExpanded) }
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

    CollapsibleSettingCard(
        title = "🥁 太鼓のサイズ・位置調整",
        subtitle = if (isLandscape) "横画面 (${currentSize}% / 位置${currentPos}%)" else "縦画面 (${currentSize}% / 位置${currentPos}%)",
        isExpanded = expandSizeCard,
        onExpandedChange = onExpandSizeCardChange,
        isDarkTheme = isDark,
        headerTrailingContent = {
            OutlinedButton(
                onClick = {
                    if (isLandscape) {
                        onSettingsChanged(settings.copy(landscapeSizePercent = 100, landscapeVerticalPosPercent = 55))
                    } else {
                        onSettingsChanged(settings.copy(portraitSizePercent = 100, portraitVerticalPosPercent = 50))
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
                                val newSize = (currentSize - 5).coerceIn(20, 200)
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
                                val newSize = (currentSize + 5).coerceIn(20, 200)
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
                                    val clamped = parsed.coerceIn(20, 200)
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
                    text = "上下位置: ${currentPos}% (0%=上, 50%=中央, 100%=下)",
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
                                val newPos = (currentPos - 5).coerceIn(0, 100)
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
                                val newPos = (currentPos + 5).coerceIn(0, 100)
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
                                    val clamped = parsed.coerceIn(0, 100)
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

            Divider(color = Color(0xFF78350F).copy(alpha = 0.1f).invertIfDark(isDark))

            // Orientation-specific Presets Section
            Text(
                text = if (isLandscape) "📍 横画面用 プリセット設定 (2枠)" else "📍 縦画面用 プリセット設定 (2枠)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78350F).invertIfDark(isDark)
            )

            val preset1 = if (isLandscape) settings.landscapePreset1 else settings.portraitPreset1
            val preset2 = if (isLandscape) settings.landscapePreset2 else settings.portraitPreset2
            val prefix = if (isLandscape) "横画面" else "縦画面"

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
                                        landscapeVerticalPosPercent = preset1.verticalPositionPercent
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitSizePercent = preset1.sizePercent,
                                        portraitVerticalPosPercent = preset1.verticalPositionPercent
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
                                        landscapePreset1 = DrumPreset(settings.landscapeSizePercent, settings.landscapeVerticalPosPercent)
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitPreset1 = DrumPreset(settings.landscapeSizePercent, settings.landscapeVerticalPosPercent)
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
                    text = "サイズ: ${preset1.sizePercent}% / 位置: ${preset1.verticalPositionPercent}%",
                    fontSize = 10.sp,
                    color = if (isDark) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

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
                                        landscapeVerticalPosPercent = preset2.verticalPositionPercent
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitSizePercent = preset2.sizePercent,
                                        portraitVerticalPosPercent = preset2.verticalPositionPercent
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
                                        landscapePreset2 = DrumPreset(settings.landscapeSizePercent, settings.landscapeVerticalPosPercent)
                                    ))
                                } else {
                                    onSettingsChanged(settings.copy(
                                        portraitPreset2 = DrumPreset(settings.landscapeSizePercent, settings.landscapeVerticalPosPercent)
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
                    text = "サイズ: ${preset2.sizePercent}% / 位置: ${preset2.verticalPositionPercent}%",
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
            text = "Shizuku（ADB権限実行環境）を使用して、端末ローカルでダイレクトにキーボード/ゲームパッド信号を注入します。Root化不要で超低遅延で動作します。",
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
        android.content.Intent("android.settings.USB_PREFERENCES_SETTINGS"),
        android.content.Intent("android.settings.USB_DETAILS_SETTINGS"),
        android.content.Intent().apply { setClassName("com.android.settings", "com.android.settings.Settings\$UsbDetailsActivity") },
        android.content.Intent().apply { setClassName("com.android.settings", "com.android.settings.usb.UsbDetailsActivity") },
        android.content.Intent("android.settings.CONNECTED_DEVICE_SETTINGS"),
        android.content.Intent("android.settings.TETHER_SETTINGS"),
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
