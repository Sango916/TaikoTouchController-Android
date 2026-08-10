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
    fun getLocalIpAddress(): String {
        return getAllLocalIpAddresses().firstOrNull() ?: "127.0.0.1"
    }

    data class NetworkInterfaceIp(
        val name: String,
        val ip: String,
        val isWired: Boolean
    )

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
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4
                            list.add(NetworkInterfaceIp(intf.name, sAddr, isWired))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting detailed IP addresses", e)
        }
        return list
    }

    fun getAllLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().map { it.ip }
    }

    fun getWiredLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().filter { it.isWired }.map { it.ip }
    }

    fun getWirelessLocalIpAddresses(): List<String> {
        return getDetailedLocalIpAddresses().filter { !it.isWired }.map { it.ip }
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
    private val seqNumber = AtomicLong(1)

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
            targetPort: Int = 60001,
            udpDiscoveryPort: Int = 60002,
            connectionType: String = "wireless", // "wired" or "wireless"
            onFound: (ip: String) -> Unit,
            onNotFound: () -> Unit
        ) {
            Executors.newSingleThreadExecutor().execute {
                var foundIp: String? = null
                val isWired = connectionType == "wired"
                val priorityIps = LinkedHashSet<String>()

                if (isWired) {
                    val wiredIps = NetworkUtils.getWiredLocalIpAddresses()
                    for (localIp in wiredIps) {
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
                    priorityIps.add("192.168.42.129")
                    priorityIps.add("192.168.42.1")
                    priorityIps.add("192.168.43.1")
                    priorityIps.add("192.168.49.1")
                    priorityIps.add("10.0.2.2")
                    priorityIps.add("127.0.0.1")
                } else {
                    priorityIps.add("192.168.42.129")
                    priorityIps.add("192.168.42.1")
                }

                if (priorityIps.isNotEmpty()) {
                    val priorityPool = Executors.newFixedThreadPool(minOf(32, priorityIps.size))
                    val priorityFutures = priorityIps.map { ip ->
                        priorityPool.submit<String?> {
                            try {
                                val s = Socket()
                                s.connect(InetSocketAddress(ip, targetPort), 150)
                                s.close()
                                ip
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    for (future in priorityFutures) {
                        val ip = try { future.get() } catch (e: Exception) { null }
                        if (ip != null) {
                            foundIp = ip
                            break
                        }
                    }
                    priorityPool.shutdownNow()
                }

                if (foundIp == null && !isWired) {
                    try {
                        val udpSocket = DatagramSocket()
                        udpSocket.soTimeout = 400
                        udpSocket.broadcast = true
                        val reqMsg = "DISCOVER_TAIKO_RECEIVER".toByteArray(Charsets.UTF_8)
                        val packet = DatagramPacket(
                            reqMsg,
                            reqMsg.size,
                            InetAddress.getByName("255.255.255.255"),
                            udpDiscoveryPort
                        )
                        udpSocket.send(packet)

                        val buf = ByteArray(256)
                        val respPacket = DatagramPacket(buf, buf.size)
                        udpSocket.receive(respPacket)
                        val respStr = String(respPacket.data, 0, respPacket.length, Charsets.UTF_8)
                        if (respStr.startsWith("TAIKO_RECEIVER_ACK")) {
                            foundIp = respPacket.address.hostAddress
                        }
                        udpSocket.close()
                    } catch (e: Exception) {
                        Log.d("TaikoRemoteSender", "UDP broadcast scan timeout: ${e.message}")
                    }
                }

                if (foundIp == null) {
                    val localIps = if (isWired) {
                        NetworkUtils.getWiredLocalIpAddresses().ifEmpty { NetworkUtils.getAllLocalIpAddresses() }
                    } else {
                        NetworkUtils.getAllLocalIpAddresses()
                    }
                    val candidateIps = LinkedHashSet<String>()

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
                        val pool = Executors.newFixedThreadPool(64)
                        val futures = candidateIps.map { ip ->
                            pool.submit<String?> {
                                try {
                                    val s = Socket()
                                    s.connect(InetSocketAddress(ip, targetPort), 200)
                                    s.close()
                                    ip
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }

                        for (future in futures) {
                            val ip = try { future.get() } catch (e: Exception) { null }
                            if (ip != null) {
                                foundIp = ip
                                break
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
                Log.d("TaikoRemoteSender", "Connecting to $host:$port ...")
                TaikoLogManager.log("受信側 (ゲーム) へ接続試行中... $host:$port")

                // 1. Setup UDP Socket for ultra-low latency (<1ms)
                udpTargetAddress = InetAddress.getByName(host)
                udpTargetPort = port + 2
                udpSocket = DatagramSocket()

                // 2. Setup TCP Socket
                val s = Socket()
                s.tcpNoDelay = true
                try { s.trafficClass = 0x10 } catch (_: Exception) {}
                s.keepAlive = true
                s.sendBufferSize = 4096
                s.connect(InetSocketAddress(host, port), 4000)

                val out = s.getOutputStream()
                val inStream = s.getInputStream()

                synchronized(socketLock) {
                    socket = s
                    writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
                    isConnected = true
                }

                _statusState.value = "connected"
                listener?.onConnected()
                TaikoLogManager.log("受信側 (ゲーム) に無線超低遅延UDP+TCPで接続完了！ ($host:$port)")

                // 3. Start Heartbeat / State Sync loop (50ms interval)
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

    private fun startHeartbeatThread() {
        heartbeatThread?.interrupt()
        heartbeatThread = Thread {
            try {
                while (isConnected) {
                    Thread.sleep(50)
                    sendUdpStateSyncPacket("STATE", emptyList())
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

        val action = if (isPressed) "DOWN" else "UP"

        // 3. Ultra-fast UDP Transmission with Redundant Twin Packets (0ms delay)
        sendUdpStateSyncPacket(action, keys, redundantSend = true)

        // 4. TCP Fallback
        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newSingleThreadExecutor()
        }
        val tcpMessage = "$action ${keys.joinToString(" ")}\n"

        executor.execute {
            try {
                synchronized(socketLock) {
                    val w = writer ?: return@execute
                    w.write(tcpMessage)
                    w.flush()
                }
            } catch (e: Exception) {
                Log.w("TaikoRemoteSender", "TCP Send key error", e)
            }
        }
    }

    private fun sendUdpStateSyncPacket(action: String, eventKeys: List<String>, redundantSend: Boolean = false) {
        val udp = udpSocket ?: return
        val addr = udpTargetAddress ?: return

        try {
            val seq = seqNumber.getAndIncrement()
            val eventKeysCsv = eventKeys.joinToString(",")
            val allKeysCsv = currentlyPressedKeys.joinToString(",")

            // Format: SEQ|ACTION|EVENT_KEYS|ALL_PRESSED_KEYS
            val payloadStr = "$seq|$action|$eventKeysCsv|$allKeysCsv"
            val bytes = payloadStr.toByteArray(Charsets.UTF_8)

            val packet = DatagramPacket(bytes, bytes.size, addr, udpTargetPort)
            udp.send(packet)

            if (redundantSend) {
                // Immediate second packet send to eliminate Wi-Fi packet drop issues
                udp.send(packet)
            }
        } catch (e: Exception) {
            Log.d("TaikoRemoteSender", "UDP send error: ${e.message}")
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
    private var udpDiscoverySocket: DatagramSocket? = null
    private var udpInputSocket: DatagramSocket? = null

    private val clients = ConcurrentHashMap<Socket, BufferedReader>()
    private var executor = Executors.newCachedThreadPool()
    private val keyDispatchExecutor = Executors.newSingleThreadExecutor()

    @Volatile private var isRunning = false
    private val activePressedKeysOnReceiver = ConcurrentHashMap.newKeySet<String>()
    private var lastProcessedSeq = AtomicLong(0)

    private val _activeClientsState = MutableStateFlow(0)
    val activeClientsState: StateFlow<Int> = _activeClientsState

    private var onClientCountChanged: ((Int) -> Unit)? = null

    private fun startUdpDiscovery(udpPort: Int = 60002) {
        executor.execute {
            try {
                val socket = DatagramSocket(udpPort)
                udpDiscoverySocket = socket
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
                Log.d("TaikoRemoteReceiver", "UDP discovery finished or stopped: ${e.message}")
            }
        }
    }

    private fun startUdpInputServer(udpPort: Int) {
        executor.execute {
            try {
                val socket = DatagramSocket(udpPort)
                udpInputSocket = socket
                val buffer = ByteArray(1024)

                while (isRunning && !socket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)

                    val payload = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val parts = payload.split("|")
                    if (parts.size >= 4) {
                        val seq = parts[0].toLongOrNull() ?: 0L
                        val action = parts[1] // "DOWN", "UP", or "STATE"
                        val eventKeysCsv = parts[2]
                        val allKeysCsv = parts[3]

                        // Ignore outdated sequence packets
                        if (seq > 0 && seq < lastProcessedSeq.get() - 50) {
                            continue
                        }
                        if (seq > lastProcessedSeq.get()) {
                            lastProcessedSeq.set(seq)
                        }

                        val eventKeys = eventKeysCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val expectedPressedKeys = allKeysCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

                        keyDispatchExecutor.execute {
                            try {
                                // 1. Dispatch primary event
                                if (eventKeys.isNotEmpty() && action != "STATE") {
                                    val isPressed = action.equals("DOWN", ignoreCase = true)
                                    onKeyEventsReceived(eventKeys, isPressed)

                                    if (isPressed) {
                                        activePressedKeysOnReceiver.addAll(eventKeys)
                                    } else {
                                        activePressedKeysOnReceiver.removeAll(eventKeys.toSet())
                                    }
                                }

                                // 2. State Sync Check: resolve any dropped packets automatically
                                val missingPressed = expectedPressedKeys - activePressedKeysOnReceiver
                                val stuckPressed = activePressedKeysOnReceiver - expectedPressedKeys

                                if (missingPressed.isNotEmpty()) {
                                    onKeyEventsReceived(missingPressed.toList(), true)
                                    activePressedKeysOnReceiver.addAll(missingPressed)
                                }
                                if (stuckPressed.isNotEmpty()) {
                                    onKeyEventsReceived(stuckPressed.toList(), false)
                                    activePressedKeysOnReceiver.removeAll(stuckPressed)
                                }
                            } catch (e: Exception) {
                                Log.e("TaikoRemoteReceiver", "Error dispatching UDP key event", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("TaikoRemoteReceiver", "UDP Input server ended: ${e.message}")
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

        startUdpDiscovery(60002)
        startUdpInputServer(port + 2)

        executor.execute {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
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

                val parts = trimmed.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val action = parts[0] // "DOWN" or "UP"
                    val isPressed = action.equals("DOWN", ignoreCase = true)
                    val keys = parts.subList(1, parts.size)
                    if (keys.isNotEmpty()) {
                        keyDispatchExecutor.execute {
                            try {
                                onKeyEventsReceived(keys, isPressed)
                                if (isPressed) {
                                    activePressedKeysOnReceiver.addAll(keys)
                                } else {
                                    activePressedKeysOnReceiver.removeAll(keys.toSet())
                                }
                            } catch (e: Exception) {
                                Log.e("TaikoRemoteReceiver", "Error dispatching TCP key event", e)
                            }
                        }
                    }
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
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        try {
            udpDiscoverySocket?.close()
        } catch (_: Exception) {}
        udpDiscoverySocket = null

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
