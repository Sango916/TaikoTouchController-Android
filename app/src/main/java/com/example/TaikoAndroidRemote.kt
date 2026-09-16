package com.example

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

object NetworkUtils {
    data class NetworkInterfaceIp(
        val name: String,
        val displayName: String,
        val ip: String,
        val isWired: Boolean,
        val isHotspot: Boolean
    )

    fun getGatewayIpAddresses(): List<String> {
        val gateways = mutableListOf<String>()
        try {
            val file = java.io.File("/proc/net/route")
            if (file.exists() && file.canRead()) {
                file.forEachLine { line ->
                    val tokens = line.trim().split("\\s+".toRegex())
                    if (tokens.size >= 3) {
                        val dest = tokens[1]
                        val gw = tokens[2]
                        if (dest == "00000000" && gw != "00000000" && gw.length == 8) {
                            try {
                                val b1 = gw.substring(0, 2).toInt(16)
                                val b2 = gw.substring(2, 4).toInt(16)
                                val b3 = gw.substring(4, 6).toInt(16)
                                val b4 = gw.substring(6, 8).toInt(16)
                                val ip = "$b1.$b2.$b3.$b4"
                                if (ip != "0.0.0.0") gateways.add(ip)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return gateways.distinct()
    }

    fun getDetailedLocalIpAddresses(): List<NetworkInterfaceIp> {
        val list = mutableListOf<NetworkInterfaceIp>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val nameLower = intf.name.lowercase()
                val isWired = nameLower.contains("rndis") || nameLower.contains("usb") ||
                              nameLower.contains("eth") || nameLower.contains("ncm") ||
                              nameLower.contains("tun")
                val isHotspot = nameLower.contains("ap") || nameLower.contains("softap") ||
                                nameLower.contains("hotspot") || nameLower.contains("tether") ||
                                nameLower.contains("p2p")

                val displayName = when {
                    isHotspot -> "テザリング / AP (${intf.name})"
                    nameLower.contains("wlan") || nameLower.contains("wifi") -> "Wi-Fi (${intf.name})"
                    nameLower.contains("rndis") || nameLower.contains("usb") -> "USB テザリング (${intf.name})"
                    nameLower.contains("eth") -> "有線 LAN (${intf.name})"
                    else -> intf.name
                }

                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4
                            list.add(NetworkInterfaceIp(intf.name, displayName, sAddr, isWired, isHotspot))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting detailed IP addresses", e)
        }

        // Sort priority: 1) Hotspot/Tethering direct IPs (192.168.43.1 / 192.168.49.1), 2) Wi-Fi private LAN IPs, 3) USB / Wired LAN IPs, 4) Others
        return list.sortedWith(compareBy<NetworkInterfaceIp> {
            when {
                it.ip == "192.168.43.1" || it.ip == "192.168.49.1" -> 0
                it.isHotspot -> 1
                it.name.lowercase().contains("wlan") -> 2
                it.isWired -> 3
                it.ip.startsWith("192.168.43.") -> 4
                it.ip.startsWith("192.168.49.") -> 5
                it.ip.startsWith("192.168.") -> 6
                it.ip.startsWith("10.") -> 7
                it.ip.startsWith("172.") -> 8
                else -> 9
            }
        })
    }

    fun getAllLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().map { it.ip }.distinct()
    }

    fun getLocalIpAddress(): String {
        return getAllLocalIpAddresses().firstOrNull() ?: "127.0.0.1"
    }

    fun getWiredLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().filter { it.isWired }.map { it.ip }.distinct()
    }

    fun getWirelessLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().filter { !it.isWired }.map { it.ip }.distinct()
    }
}

enum class RemoteAction {
    DOWN,
    UP,
    HIT,
    STATE
}

data class RemoteEvent(
    val sessionId: Long,
    val eventId: Long,
    val action: RemoteAction,
    val keys: List<String>,
    val allHeldKeys: List<String> = emptyList()
) {
    fun serialize(): String {
        val keysCsv = keys.joinToString(",")
        val heldCsv = allHeldKeys.joinToString(",")
        return "EV|$sessionId|$eventId|${action.name}|$keysCsv|$heldCsv"
    }

    companion object {
        fun parse(raw: String): RemoteEvent? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed == "PING") return null

            if (trimmed.startsWith("EV|")) {
                val parts = trimmed.split("|")
                if (parts.size >= 5) {
                    val sessionId = parts[1].toLongOrNull() ?: 0L
                    val eventId = parts[2].toLongOrNull() ?: 0L
                    val action = try { RemoteAction.valueOf(parts[3]) } catch (_: Exception) { RemoteAction.DOWN }
                    val keys = if (parts[4].isEmpty()) emptyList() else parts[4].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val held = if (parts.size >= 6 && parts[5].isNotEmpty()) {
                        parts[5].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    } else emptyList()
                    return RemoteEvent(sessionId, eventId, action, keys, held)
                }
            }

            // Fallback for legacy UDP packets: <seq>|<action>|<keysCsv>|<allHeldCsv>
            if (trimmed.contains("|")) {
                val parts = trimmed.split("|")
                if (parts.size >= 4) {
                    val seq = parts[0].toLongOrNull() ?: 0L
                    val actionStr = parts[1]
                    val action = when (actionStr) {
                        "DOWN" -> RemoteAction.DOWN
                        "UP" -> RemoteAction.UP
                        "HIT" -> RemoteAction.HIT
                        else -> RemoteAction.STATE
                    }
                    val keys = parts[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val held = parts[3].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    return RemoteEvent(0L, seq, action, keys, held)
                }
            }

            // Fallback for legacy TCP packets: "DOWN key1 key2", "UP key1", "HIT key1"
            val words = trimmed.split("\\s+".toRegex())
            if (words.isNotEmpty()) {
                val actionStr = words[0]
                val action = when (actionStr) {
                    "DOWN" -> RemoteAction.DOWN
                    "UP" -> RemoteAction.UP
                    "HIT" -> RemoteAction.HIT
                    else -> return null
                }
                val keys = words.drop(1)
                return RemoteEvent(0L, 0L, action, keys)
            }
            return null
        }
    }
}

/**
 * 送信側（太鼓側）から受信側（ゲーム側）Androidへキー入力を送るクライアント
 * UDP 超低遅延・状態同期 (State-Based Sync) + 冗長送信 (Redundant Transmission) + TCPバックアップ
 */
class TaikoAndroidRemoteSender {
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }

    private var socket: Socket? = null
    private var udpSocket: DatagramSocket? = null
    private var udpTargetAddress: InetAddress? = null
    private var udpTargetPort: Int = 60003

    private var writer: BufferedWriter? = null
    private var executor = Executors.newSingleThreadExecutor()
    private var udpSendExecutor = Executors.newSingleThreadExecutor()
    private var tcpSendExecutor = Executors.newSingleThreadExecutor()
    private val seqNumber = AtomicLong(1)
    private var sessionId: Long = kotlin.math.abs(java.security.SecureRandom().nextLong()).let { if (it == 0L) 1L else it }

    var transportMode: String = "hybrid" // "hybrid", "udp_only", "tcp_only"

    @Volatile private var isConnected = false
    private val currentlyPressedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val _statusState = MutableStateFlow("disconnected") // "disconnected", "connecting", "connected", "error"
    val statusState: StateFlow<String> = _statusState

    private val _errorMessageState = MutableStateFlow<String?>(null)
    val errorMessageState: StateFlow<String?> = _errorMessageState

    private var listener: ConnectionListener? = null
    private var heartbeatThread: Thread? = null

    companion object {
        fun scanAndFindReceiverIp(
            targetPort: Int = 60002,
            udpDiscoveryPort: Int = targetPort + 200,
            connectionType: String = "wireless", // "wired" or "wireless"
            onFound: (ip: String) -> Unit,
            onNotFound: () -> Unit
        ) {
            Executors.newSingleThreadExecutor().execute {
                var foundIp: String? = null
                val isWired = connectionType == "wired"
                val priorityIps = LinkedHashSet<String>()

                // 1. Gateways (Default route points directly to Wi-Fi Hotspot Host or Wi-Fi Router)
                val gatewayIps = NetworkUtils.getGatewayIpAddresses()
                priorityIps.addAll(gatewayIps)

                // 2. Common Android Hotspot / Tethering gateway & AP addresses
                priorityIps.add("192.168.43.1")   // Android Wi-Fi Hotspot standard AP IP
                priorityIps.add("192.168.49.1")   // Wi-Fi Direct P2P Group Owner IP
                priorityIps.add("192.168.50.1")
                priorityIps.add("192.168.44.1")
                priorityIps.add("192.168.42.129") // Android USB Tethering client IP
                priorityIps.add("192.168.42.1")   // Android USB Tethering host IP
                priorityIps.add("192.168.137.1")
                priorityIps.add("172.20.10.1")    // iOS / alternate tethering gateway
                priorityIps.add("10.0.2.2")
                priorityIps.add("127.0.0.1")

                val localIps = if (isWired) {
                    NetworkUtils.getWiredLocalIpAddresses().ifEmpty { NetworkUtils.getAllLocalIpAddresses() }
                } else {
                    NetworkUtils.getAllLocalIpAddresses()
                }

                for (localIp in localIps) {
                    val parts = localIp.split(".")
                    if (parts.size == 4) {
                        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                        priorityIps.add("$prefix.1")
                        priorityIps.add("$prefix.129")
                        priorityIps.add("$prefix.2")
                        priorityIps.add("$prefix.100")
                        priorityIps.add("$prefix.254")
                    }
                }

                // Step A: Fast probe priority IPs with TCP connection (250ms timeout)
                if (priorityIps.isNotEmpty()) {
                    val pool = Executors.newFixedThreadPool(minOf(32, priorityIps.size))
                    val completionService = java.util.concurrent.ExecutorCompletionService<String?>(pool)
                    for (ip in priorityIps) {
                        completionService.submit {
                            try {
                                val s = Socket()
                                s.connect(InetSocketAddress(ip, targetPort), 250)
                                s.close()
                                ip
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    var checked = 0
                    while (checked < priorityIps.size) {
                        val completedFuture = try {
                            completionService.poll(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                        } catch (e: Exception) { null }
                        if (completedFuture != null) {
                            checked++
                            val res = try { completedFuture.get() } catch (e: Exception) { null }
                            if (res != null) {
                                foundIp = res
                                break
                            }
                        } else {
                            break
                        }
                    }
                    pool.shutdownNow()
                }

                // Step B: Multi-target UDP Broadcast & Direct Unicast Discovery
                if (foundIp == null && !isWired) {
                    try {
                        val udpSocket = DatagramSocket(null).apply {
                            reuseAddress = true
                            bind(InetSocketAddress(0))
                        }
                        udpSocket.soTimeout = 350
                        udpSocket.broadcast = true
                        val reqMsg = "DISCOVER_TAIKO_RECEIVER".toByteArray(Charsets.UTF_8)
                        
                        val discoveryPorts = listOf(udpDiscoveryPort, targetPort + 200, 60004, 60202).distinct()

                        // Subnet broadcasts
                        val broadcastTargets = listOf(
                            "255.255.255.255",
                            "192.168.43.255",
                            "192.168.49.255",
                            "192.168.42.255"
                        ) + localIps.mapNotNull {
                            val parts = it.split(".")
                            if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.255" else null
                        } + priorityIps // Also send direct unicast discovery packets!

                        for (dPort in discoveryPorts) {
                            for (bTarget in broadcastTargets.distinct()) {
                                try {
                                    udpSocket.send(DatagramPacket(
                                        reqMsg,
                                        reqMsg.size,
                                        InetAddress.getByName(bTarget),
                                        dPort
                                    ))
                                } catch (_: Exception) {}
                            }
                        }

                        val buf = ByteArray(256)
                        val respPacket = DatagramPacket(buf, buf.size)
                        try {
                            udpSocket.receive(respPacket)
                            val respStr = String(respPacket.data, 0, respPacket.length, Charsets.UTF_8)
                            if (respStr.startsWith("TAIKO_RECEIVER_ACK")) {
                                foundIp = respPacket.address.hostAddress
                            }
                        } catch (_: Exception) {}
                        udpSocket.close()
                    } catch (e: Exception) {
                        Log.d("TaikoRemoteSender", "UDP broadcast scan note: ${e.message}")
                    }
                }

                // Step C: Full Subnet Parallel TCP Scan (Fallback)
                if (foundIp == null) {
                    val candidateIps = LinkedHashSet<String>()
                    // Ensure standard hotspot / tethering subnets are always tested
                    for (i in 1..254) {
                        candidateIps.add("192.168.43.$i")
                        candidateIps.add("192.168.49.$i")
                        candidateIps.add("192.168.42.$i")
                    }

                    for (localIp in localIps) {
                        val parts = localIp.split(".")
                        if (parts.size == 4) {
                            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                            val selfLastOctet = parts[3].toIntOrNull() ?: -1
                            for (i in 1..254) {
                                if (i != selfLastOctet) {
                                    candidateIps.add("$prefix.$i")
                                }
                            }
                        }
                    }

                    if (candidateIps.isNotEmpty()) {
                        val candidateList = candidateIps.toList()
                        val pool = Executors.newFixedThreadPool(64)
                        val completionService = java.util.concurrent.ExecutorCompletionService<String?>(pool)
                        for (ip in candidateList) {
                            completionService.submit {
                                try {
                                    val s = Socket()
                                    s.connect(InetSocketAddress(ip, targetPort), 300)
                                    s.close()
                                    ip
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        var finishedCount = 0
                        val totalTasks = candidateList.size
                        while (finishedCount < totalTasks) {
                            val completedFuture = try {
                                completionService.poll(350, java.util.concurrent.TimeUnit.MILLISECONDS)
                            } catch (e: Exception) { null }
                            if (completedFuture != null) {
                                finishedCount++
                                val res = try { completedFuture.get() } catch (e: Exception) { null }
                                if (res != null) {
                                    foundIp = res
                                    break
                                }
                            } else {
                                break // Timeout
                            }
                        }
                        pool.shutdownNow()
                    }
                }

                if (foundIp != null) {
                    onFound(foundIp)
                } else {
                    onNotFound()
                }
            }
        }
    }

    private val socketLock = Any()
    private var monitorThread: Thread? = null

    @Volatile private var shouldAutoReconnect = false
    private var lastHost: String = ""
    private var lastPort: Int = 60001

    fun connect(host: String, port: Int, listener: ConnectionListener?) {
        connect(host, port, true, listener)
    }

    private var wiredJob: java.util.concurrent.Future<*>? = null

    fun connect(host: String, port: Int, autoReconnect: Boolean = true, listener: ConnectionListener? = null) {
        this.listener = listener
        this.lastHost = host
        this.lastPort = port
        this.shouldAutoReconnect = autoReconnect

        stopConnectionInternal()

        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newSingleThreadExecutor()
        }

        _statusState.value = "connecting"
        _errorMessageState.value = null

        executor.execute {
            try {
                connectInternalSync(host, port, listener)
            } catch (e: Exception) {
                isConnected = false
                _statusState.value = "error"
                val errMsg = e.message ?: "接続失敗"
                _errorMessageState.value = errMsg
                listener?.onError(errMsg)
                Log.e("TaikoRemoteSender", "Connection failed to $host:$port", e)
                TaikoLogManager.log("受信側 (ゲーム) への接続に失敗: $errMsg")

                if (shouldAutoReconnect) {
                    scheduleAutoReconnect()
                }
            }
        }
    }

    private fun connectInternalSync(host: String, port: Int, listener: ConnectionListener?) {
        Log.d("TaikoRemoteSender", "Connecting sync to $host:$port ...")
        TaikoLogManager.log("受信側 (ゲーム) へ接続試行中... $host:$port")

        // 1. Setup TCP Socket first
        val s = Socket()
        s.tcpNoDelay = true
        try { s.trafficClass = 0x10 } catch (_: Exception) {}
        s.keepAlive = true
        s.sendBufferSize = 4096
        s.connect(InetSocketAddress(host, port), 4000)

        val out = s.getOutputStream()
        val inStream = s.getInputStream()

        // 2. Setup UDP Socket bound to the same local address interface used by TCP to guarantee correct tethering routing
        udpTargetAddress = InetAddress.getByName(host)
        udpTargetPort = port + 100
        val localAddr = s.localAddress
        udpSocket = DatagramSocket(null).apply {
            reuseAddress = true
            try { trafficClass = 0x10 } catch (_: Exception) {}
            try {
                if (localAddr != null && !localAddr.isAnyLocalAddress && !localAddr.isLoopbackAddress) {
                    bind(InetSocketAddress(localAddr, 0))
                } else {
                    bind(InetSocketAddress(0))
                }
            } catch (e: Exception) {
                Log.w("TaikoRemoteSender", "Failed to bind UDP to localAddr $localAddr, falling back to wildcard: ${e.message}")
                try {
                    bind(InetSocketAddress(0))
                } catch (_: Exception) {}
            }
        }

        synchronized(socketLock) {
            socket = s
            writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
            sessionId = kotlin.math.abs(java.security.SecureRandom().nextLong()).let { if (it == 0L) 1L else it }
            seqNumber.set(1)
            isConnected = true
        }

        _statusState.value = "connected"
        listener?.onConnected()
        TaikoLogManager.log("受信側 (ゲーム) に接続完了！ ($host:$port)")

        // Send initial handshake state event
        val initialEvent = RemoteEvent(sessionId, 0L, RemoteAction.STATE, emptyList(), currentlyPressedKeys.toList())
        dispatchRemoteEvent(initialEvent, redundantUdp = false)

        // 3. Start Heartbeat / State Sync loop
        startHeartbeatThread()

        // 4. Connection Monitor Thread
        monitorThread = Thread {
            try {
                val buffer = ByteArray(128)
                while (isConnected && socket?.isClosed == false) {
                    val readBytes = inStream.read(buffer)
                    if (readBytes == -1) break
                }
            } catch (e: Exception) {
                Log.d("TaikoRemoteSender", "Connection monitor read ended: ${e.message}")
            } finally {
                if (isConnected) {
                    Log.w("TaikoRemoteSender", "Detected server disconnect in monitor thread")
                    TaikoLogManager.log("受信側 (ゲーム) との接続が切断されました")
                    onUnexpectedDisconnect()
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun startHeartbeatThread() {
        heartbeatThread?.interrupt()
        heartbeatThread = Thread {
            var tickCount = 0
            try {
                while (isConnected) {
                    Thread.sleep(250) // Idle state sync heartbeat
                    tickCount++
                    if (tickCount % 4 == 0) { // Every 1000ms, send TCP Keep-Alive
                        synchronized(socketLock) {
                            try {
                                writer?.write("PING\n")
                                writer?.flush()
                            } catch (e: Exception) {
                                Log.w("TaikoRemoteSender", "TCP keepalive ping failed: ${e.message}")
                                onUnexpectedDisconnect()
                                return@Thread
                            }
                        }
                    }
                    val stateEvent = RemoteEvent(
                        sessionId = sessionId,
                        eventId = seqNumber.getAndIncrement(),
                        action = RemoteAction.STATE,
                        keys = emptyList(),
                        allHeldKeys = currentlyPressedKeys.toList()
                    )
                    dispatchRemoteEvent(stateEvent, redundantUdp = false)
                }
            } catch (_: InterruptedException) {
            } catch (e: Exception) {
                Log.d("TaikoRemoteSender", "Heartbeat ended: ${e.message}")
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun sendKeyEvent(partOrKey: String, isPressed: Boolean) {
        sendMultiKeyEvents(listOf(partOrKey), isPressed)
    }

    fun sendHitEvent(keys: List<String>) {
        if (keys.isEmpty()) return

        // Direct USB AOA Hardware Pipeline
        if (TaikoUsbDirectManager.sendKeys(keys, true)) {
            TaikoUsbDirectManager.sendKeys(keys, false)
            return
        }

        if (!isConnected) return

        val event = RemoteEvent(
            sessionId = sessionId,
            eventId = seqNumber.getAndIncrement(),
            action = RemoteAction.HIT,
            keys = keys,
            allHeldKeys = currentlyPressedKeys.toList()
        )
        dispatchRemoteEvent(event, redundantUdp = true)
    }

    fun sendMultiKeyEvents(keys: List<String>, isPressed: Boolean) {
        if (keys.isEmpty()) return

        // 1. Try Direct USB AOA Hardware Pipeline First (<0.5ms Latency)
        if (TaikoUsbDirectManager.sendKeys(keys, isPressed)) {
            return
        }

        // 2. Update locally pressed key set
        if (isPressed) {
            currentlyPressedKeys.addAll(keys)
        } else {
            currentlyPressedKeys.removeAll(keys.toSet())
        }

        if (!isConnected) return

        val action = if (isPressed) RemoteAction.DOWN else RemoteAction.UP

        val event = RemoteEvent(
            sessionId = sessionId,
            eventId = seqNumber.getAndIncrement(),
            action = action,
            keys = keys,
            allHeldKeys = currentlyPressedKeys.toList()
        )
        dispatchRemoteEvent(event, redundantUdp = true)

        if (!isPressed) {
            val stateEvent = RemoteEvent(
                sessionId = sessionId,
                eventId = seqNumber.getAndIncrement(),
                action = RemoteAction.STATE,
                keys = emptyList(),
                allHeldKeys = currentlyPressedKeys.toList()
            )
            dispatchRemoteEvent(stateEvent, redundantUdp = false)
        }
    }

    private fun dispatchRemoteEvent(event: RemoteEvent, redundantUdp: Boolean = false) {
        val payload = event.serialize()

        // 1. Ultra-fast Zero-Queue Direct UDP Transmission (if hybrid or udp_only)
        if (transportMode != "tcp_only") {
            val udp = udpSocket
            val addr = udpTargetAddress
            if (udp != null && addr != null) {
                val bytes = payload.toByteArray(Charsets.UTF_8)
                if (udpSendExecutor.isShutdown || udpSendExecutor.isTerminated) {
                    udpSendExecutor = Executors.newSingleThreadExecutor()
                }
                udpSendExecutor.execute {
                    try {
                        val packet = DatagramPacket(bytes, bytes.size, addr, udpTargetPort)
                        udp.send(packet)
                        if (redundantUdp) {
                            udp.send(packet)
                            udp.send(packet)
                        }
                    } catch (e: Exception) {
                        Log.d("TaikoRemoteSender", "UDP send error: ${e.message}")
                    }
                }
            }
        }

        // 2. Guaranteed Concurrent TCP Fast-Stream Pipeline (if hybrid or tcp_only)
        if (transportMode != "udp_only") {
            if (tcpSendExecutor.isShutdown || tcpSendExecutor.isTerminated) {
                tcpSendExecutor = Executors.newSingleThreadExecutor()
            }
            tcpSendExecutor.execute {
                synchronized(socketLock) {
                    try {
                        writer?.write(payload + "\n")
                        writer?.flush()
                    } catch (e: Exception) {
                        Log.e("TaikoRemoteSender", "TCP key send error: ${e.message}")
                        onUnexpectedDisconnect()
                    }
                }
            }
        }
    }

    private fun onUnexpectedDisconnect() {
        stopConnectionInternal()
        listener?.onDisconnected()
        if (shouldAutoReconnect) {
            TaikoLogManager.log("自動再接続をスケジュール中 (2秒後)...")
            scheduleAutoReconnect()
        }
    }

    private fun scheduleAutoReconnect() {
        if (!shouldAutoReconnect || executor.isShutdown) return
        executor.execute {
            try {
                Thread.sleep(2000)
            } catch (e: Exception) {}
            if (shouldAutoReconnect && !isConnected) {
                connect(lastHost, lastPort, true, listener)
            }
        }
    }

    private fun stopConnectionInternal() {
        isConnected = false
        try {
            heartbeatThread?.interrupt()
            heartbeatThread = null
        } catch (_: Exception) {}

        try {
            monitorThread?.interrupt()
            monitorThread = null
        } catch (_: Exception) {}

        try {
            udpSocket?.close()
        } catch (_: Exception) {}
        udpSocket = null

        try {
            udpSendExecutor.shutdownNow()
        } catch (_: Exception) {}

        try {
            tcpSendExecutor.shutdownNow()
        } catch (_: Exception) {}

        synchronized(socketLock) {
            try {
                socket?.close()
            } catch (_: Exception) {}
            socket = null
            writer = null
        }
        _statusState.value = "disconnected"
    }

    fun disconnect() {
        shouldAutoReconnect = false
        val wasConnected = isConnected
        stopConnectionInternal()
        if (wasConnected) {
            listener?.onDisconnected()
        }
    }
}

/**
 * 受信側（ゲーム側）で送信側（太鼓側）からのキー入力を待ち受け、Shizuku/Accessibility/KeyInjector経由で注入するサーバー
 * UDP State-based Sync Server + TCP Fallback Server
 */
class TaikoAndroidRemoteReceiver(
    private val onKeyEventsReceived: (keys: List<String>, isPressed: Boolean) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private val udpDiscoverySockets = Collections.synchronizedList(mutableListOf<DatagramSocket>())
    private var udpInputSocket: DatagramSocket? = null

    private val clients = ConcurrentHashMap<Socket, BufferedReader>()
    private var executor = Executors.newCachedThreadPool()
    private val keyDispatchExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var isRunning = false
    private val activePressedKeysOnReceiver = ConcurrentHashMap.newKeySet<String>()

    data class EventKey(val sessionId: Long, val eventId: Long)
    private val processedEvents = ConcurrentHashMap<EventKey, Boolean>()
    private val processedOrder = java.util.concurrent.ConcurrentLinkedQueue<EventKey>()
    private val maxDedupEntries = 2048

    private val _activeClientsState = MutableStateFlow(0)
    val activeClientsState: StateFlow<Int> = _activeClientsState

    private var onClientCountChanged: ((Int) -> Unit)? = null

    private fun acceptEvent(event: RemoteEvent): Boolean {
        if (event.eventId <= 0L) return true
        val key = EventKey(event.sessionId, event.eventId)
        if (processedEvents.putIfAbsent(key, true) != null) {
            return false // Duplicate already processed across UDP or TCP!
        }
        processedOrder.add(key)
        while (processedOrder.size > maxDedupEntries) {
            processedOrder.poll()?.let { processedEvents.remove(it) }
        }
        return true
    }

    private fun dispatchRemoteEvent(event: RemoteEvent) {
        if (!acceptEvent(event)) {
            return
        }
        keyDispatchExecutor.execute {
            try {
                when (event.action) {
                    RemoteAction.HIT -> {
                        if (event.keys.isNotEmpty()) {
                            onKeyEventsReceived(event.keys, true)
                            try {
                                Thread.sleep(25)
                            } catch (_: InterruptedException) {}
                            onKeyEventsReceived(event.keys, false)
                        }
                    }
                    RemoteAction.DOWN -> {
                        if (event.keys.isNotEmpty()) {
                            onKeyEventsReceived(event.keys, true)
                            activePressedKeysOnReceiver.addAll(event.keys)
                        }
                    }
                    RemoteAction.UP -> {
                        if (event.keys.isNotEmpty()) {
                            onKeyEventsReceived(event.keys, false)
                            activePressedKeysOnReceiver.removeAll(event.keys.toSet())
                        }
                    }
                    RemoteAction.STATE -> {
                        val expectedPressedKeys = event.allHeldKeys.toSet()
                        val stuckPressed = activePressedKeysOnReceiver - expectedPressedKeys
                        if (stuckPressed.isNotEmpty()) {
                            onKeyEventsReceived(stuckPressed.toList(), false)
                            activePressedKeysOnReceiver.removeAll(stuckPressed)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TaikoRemoteReceiver", "Error dispatching remote event", e)
            }
        }
    }

    private fun createAndBindDatagramSocket(udpPort: Int, maxRetries: Int = 10, delayMs: Long = 200): DatagramSocket? {
        var lastErr: Exception? = null
        for (i in 1..maxRetries) {
            if (!isRunning) return null
            try {
                val socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    try { trafficClass = 0x10 } catch (_: Exception) {}
                    bind(InetSocketAddress(udpPort))
                }
                return socket
            } catch (e: Exception) {
                lastErr = e
                if (i < maxRetries) {
                    Log.w("TaikoRemoteReceiver", "UDP bind to $udpPort waiting for socket release (attempt $i/$maxRetries)...")
                    try { Thread.sleep(delayMs) } catch (_: InterruptedException) { return null }
                }
            }
        }
        Log.e("TaikoRemoteReceiver", "Failed to bind DatagramSocket to port $udpPort", lastErr)
        return null
    }

    private fun createAndBindServerSocket(port: Int, maxRetries: Int = 10, delayMs: Long = 200): ServerSocket? {
        var lastErr: Exception? = null
        for (i in 1..maxRetries) {
            if (!isRunning) return null
            try {
                val socket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                return socket
            } catch (e: Exception) {
                lastErr = e
                if (i < maxRetries) {
                    Log.w("TaikoRemoteReceiver", "TCP Server bind to $port waiting for socket release (attempt $i/$maxRetries)...")
                    try { Thread.sleep(delayMs) } catch (_: InterruptedException) { return null }
                }
            }
        }
        Log.e("TaikoRemoteReceiver", "Failed to bind ServerSocket to port $port", lastErr)
        return null
    }

    private fun startUdpDiscovery(udpPort: Int) {
        executor.execute {
            try {
                val socket = createAndBindDatagramSocket(udpPort) ?: return@execute
                udpDiscoverySockets.add(socket)
                val buffer = ByteArray(256)
                while (isRunning && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    if (msg.startsWith("DISCOVER_TAIKO_RECEIVER")) {
                        val responseData = "TAIKO_RECEIVER_ACK".toByteArray(Charsets.UTF_8)
                        val replyPacket = DatagramPacket(
                            responseData,
                            responseData.size,
                            packet.address,
                            packet.port
                        )
                        socket.send(replyPacket)
                    }
                }
            } catch (e: Exception) {
                Log.d("TaikoRemoteReceiver", "UDP discovery port $udpPort finished or stopped: ${e.message}")
            }
        }
    }

    private fun startUdpInputServer(udpPort: Int) {
        executor.execute {
            try {
                val socket = createAndBindDatagramSocket(udpPort) ?: run {
                    TaikoLogManager.log("UDP 受信サーバーのポート $udpPort が他で利用中です")
                    return@execute
                }
                udpInputSocket = socket
                TaikoLogManager.log("UDP 超低遅延入力サーバー起動 (ポート $udpPort)")
                val buffer = ByteArray(1024)

                while (isRunning && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val payload = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    val event = RemoteEvent.parse(payload)
                    if (event != null) {
                        dispatchRemoteEvent(event)
                    }
                }
            } catch (e: Exception) {
                Log.e("TaikoRemoteReceiver", "UDP Input server error: ${e.message}", e)
                TaikoLogManager.log("UDP 受信サーバーエラー (ポート $udpPort): ${e.message}")
            }
        }
    }

    fun start(port: Int = 60001, onClientCountChanged: ((Int) -> Unit)? = null) {
        this.onClientCountChanged = onClientCountChanged
        if (isRunning) return
        isRunning = true

        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newCachedThreadPool()
        }

        val discoveryPort = port + 200
        val udpInputPort = port + 100

        startUdpDiscovery(discoveryPort)
        if (discoveryPort != 60004) {
            startUdpDiscovery(60004)
        }
        if (discoveryPort != 60202 && 60004 != 60202) {
            startUdpDiscovery(60202)
        }
        startUdpInputServer(udpInputPort)

        executor.execute {
            try {
                val sSocket = createAndBindServerSocket(port) ?: run {
                    TaikoLogManager.log("受信サーバー起動失敗: ポート $port が使用中です")
                    return@execute
                }
                serverSocket = sSocket
                Log.d("TaikoRemoteReceiver", "Receiver bound successfully to port $port")
                TaikoLogManager.log("受信待機サーバー起動 (ゲーム側): ポート $port で送信側 (太鼓) からの接続を待機中")

                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d("TaikoRemoteReceiver", "Sender Android connected: ${socket.remoteSocketAddress}")

                    executor.execute {
                        handleSenderClient(socket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("TaikoRemoteReceiver", "Receiver socket error", e)
                    TaikoLogManager.log("受信待機サーバーエラー: ${e.message}")
                }
            }
        }
    }

    private fun handleSenderClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            try { socket.trafficClass = 0x10 } catch (_: Exception) {}
            socket.keepAlive = true
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))

            clients[socket] = reader
            val count = clients.size
            _activeClientsState.value = count
            onClientCountChanged?.invoke(count)

            TaikoLogManager.log("送信側 (太鼓) Androidが接続しました (Remote: ${socket.remoteSocketAddress})")

            var line: String?
            while (isRunning && !socket.isClosed) {
                line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed == "PING") continue

                val event = RemoteEvent.parse(trimmed)
                if (event != null) {
                    dispatchRemoteEvent(event)
                }
            }
        } catch (e: Exception) {
            Log.d("TaikoRemoteReceiver", "Client handler finished: ${e.message}")
        } finally {
            clients.remove(socket)
            val count = clients.size
            _activeClientsState.value = count
            onClientCountChanged?.invoke(count)
            try {
                socket.close()
            } catch (e: Exception) {}
            TaikoLogManager.log("送信側 (太鼓) Androidが切断されました")
        }
    }

    fun stop() {
        isRunning = false
        processedEvents.clear()
        processedOrder.clear()
        if (activePressedKeysOnReceiver.isNotEmpty()) {
            val stuck = activePressedKeysOnReceiver.toList()
            onKeyEventsReceived(stuck, false)
            activePressedKeysOnReceiver.clear()
        }
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        synchronized(udpDiscoverySockets) {
            for (sock in udpDiscoverySockets) {
                try { sock.close() } catch (_: Exception) {}
            }
            udpDiscoverySockets.clear()
        }

        try {
            udpInputSocket?.close()
        } catch (_: Exception) {}
        udpInputSocket = null

        val currentClients = clients.keys.toList()
        clients.clear()
        for (socket in currentClients) {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
        _activeClientsState.value = 0

        try {
            executor.shutdownNow()
        } catch (_: Exception) {}

        TaikoLogManager.log("受信待機サーバー (ゲーム側) を停止しました")
    }
}
