package com.example

import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Helper function to query local input devices via Android APIs
fun getDeviceList(): List<String> {
    return try {
        val ids = InputDevice.getDeviceIds()
        if (ids.isEmpty()) {
            listOf("接続されている入力デバイスはありません。")
        } else {
            ids.map { id ->
                val device = InputDevice.getDevice(id)
                if (device != null) {
                    val sourcesStr = buildString {
                        val s = device.sources
                        if ((s and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) append("KEYBOARD ")
                        if ((s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) append("GAMEPAD ")
                        if ((s and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) append("DPAD ")
                        if ((s and InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) append("TOUCHSCREEN ")
                        if ((s and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) append("MOUSE ")
                        if ((s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) append("JOYSTICK ")
                    }.trim()

                    val isVirtualStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        " | Virtual: ${device.isVirtual}"
                    } else ""

                    "ID: $id | ${device.name}\n  Sources: [$sourcesStr]$isVirtualStr"
                } else {
                    "ID: $id | (デバイス詳細取得失敗)"
                }
            }
        }
    } catch (e: Exception) {
        listOf("デバイス一覧取得エラー: ${e.message}")
    }
}

@Composable
fun LogConsoleOverlay(
    modifier: Modifier = Modifier
) {
    val logs by TaikoLogManager.logs.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    var showDeviceListMode by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf<List<String>>(emptyList()) }
    val lazyListState = rememberLazyListState()

    // Query devices dynamically when the mode is active or when refreshed
    val refreshDevices = {
        deviceList = getDeviceList()
    }

    LaunchedEffect(showDeviceListMode) {
        if (showDeviceListMode) {
            refreshDevices()
        }
    }

    // Automatically scroll to bottom when new logs arrive (only in log mode)
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && !showDeviceListMode) {
            lazyListState.scrollToItem(logs.size - 1)
        }
    }

    if (!isVisible) {
        // Floating action button to restore visibility
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            FilledTonalButton(
                onClick = { isVisible = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("📋 ログを表示", fontSize = 9.sp)
            }
        }
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showDeviceListMode) {
                        Text("🔌 接続中の入力デバイス一覧 ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                        Text("(${deviceList.size}台)", fontSize = 9.sp, color = Color.LightGray)
                    } else {
                        Text("📋 リアルタイム入力・接続ログ ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCD34D))
                        Text("(${logs.size}件)", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Toggle (Log <-> Device IDs)
                    TextButton(
                        onClick = { showDeviceListMode = !showDeviceListMode },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(22.dp)
                    ) {
                        Text(
                            text = if (showDeviceListMode) "← ログ表示" else "デバイス一覧",
                            fontSize = 9.sp,
                            color = if (showDeviceListMode) Color(0xFFFCD34D) else Color(0xFF60A5FA),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Context action: Clear (for logs) or Refresh (for devices)
                    if (showDeviceListMode) {
                        TextButton(
                            onClick = { refreshDevices() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Text("更新", fontSize = 9.sp, color = Color.White)
                        }
                    } else {
                        TextButton(
                            onClick = { TaikoLogManager.clear() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Text("クリア", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    // Size toggle button
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(22.dp)
                    ) {
                        Text(if (isExpanded) "縮小" else "拡大", fontSize = 9.sp, color = Color.White)
                    }

                    // Close button
                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hide logs",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Logs text area
            val heightDp = if (isExpanded) 160.dp else 55.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                if (showDeviceListMode) {
                    if (deviceList.isEmpty()) {
                        Text(
                            "デバイス情報を取得中...",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(deviceList) { devInfo ->
                                Text(
                                    text = devInfo,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp))
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                } else {
                    if (logs.isEmpty()) {
                        Text(
                            "ログは空です。太鼓を叩くか、接続を行うとリアルタイムでここに出力されます。",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs) { logMsg ->
                                Text(
                                    text = logMsg,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        logMsg.contains("ERR") || logMsg.contains("Fail") -> Color(0xFFF87171) // soft red
                                        logMsg.contains("Injected") || logMsg.contains("Opened") || logMsg.contains("Joined") -> Color(0xFF34D399) // soft green
                                        logMsg.contains("Send") || logMsg.contains("Sent") || logMsg.contains("Recv") -> Color(0xFF60A5FA) // soft blue
                                        else -> Color.White
                                    },
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
