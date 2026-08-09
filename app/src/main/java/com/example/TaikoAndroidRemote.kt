package com.example

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object NetworkUtils {
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4
                            return sAddr
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("NetworkUtils", "Error getting IP address", ex)
        }
        return "127.0.0.1"
    }

    fun getAllLocalIpAddresses(): List<String> {
        val list = mutableListOf<String>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            list.add(sAddr)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkUtils", "Error getting all IP addresses", e)
        }
        return list
    }
}

/**
 * 送信側（太鼓側）から受信側（ゲーム側）Androidへキー入力を送るクライアント
 */
class TaikoAndroidRemoteSender {
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var executor = Executors.newSingleThreadExecutor()
    @Volatile private var isConnected = false

    private val _statusState = MutableStateFlow("disconnected") // "disconnected", "connecting", "connected", "error"
    val statusState: StateFlow<String> = _statusState

    private val _errorMessageState = MutableStateFlow<String?>(null)
    val errorMessageState: StateFlow<String?> = _errorMessageState

    private var listener: ConnectionListener? = null

    companion object {
        fun scanAndFindReceiverIp(
            targetPort: Int = 60001,
            udpDiscoveryPort: Int = 60002,
            onFound: (ip: String) -> Unit,
            onNotFound: () -> Unit
        ) {
            Executors.newSingleThreadExecutor().execute {
                var foundIp: String? = null

                // 1. Priority Fast Scan for Wired/USB/Tethering known direct IPs
                val priorityIps = listOf(
                    "127.0.0.1",
                    "192.168.42.129",
                    "192.168.42.1",
                    "192.168.43.1",
                    "192.168.49.1",
                    "10.0.2.2"
                )

                val priorityPool = Executors.newFixedThreadPool(priorityIps.size)
                val priorityFutures = priorityIps.map { ip ->
                    priorityPool.submit<String?> {
                        try {
                            val s = Socket()
                            s.connect(java.net.InetSocketAddress(ip, targetPort), 150)
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

                // 2. Try UDP Broadcast Discovery if not found yet
                if (foundIp == null) {
                    try {
                        val udpSocket = java.net.DatagramSocket()
                        udpSocket.soTimeout = 400
                        udpSocket.broadcast = true
                        val reqMsg = "DISCOVER_TAIKO_RECEIVER".toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                        val packet = java.net.DatagramPacket(
                            reqMsg,
                            reqMsg.size,
                            java.net.InetAddress.getByName("255.255.255.255"),
                            udpDiscoveryPort
                        )
                        udpSocket.send(packet)

                        val buf = ByteArray(256)
                        val respPacket = java.net.DatagramPacket(buf, buf.size)
                        udpSocket.receive(respPacket)
                        val respStr = String(respPacket.data, 0, respPacket.length, java.nio.charset.StandardCharsets.UTF_8)
                        if (respStr.startsWith("TAIKO_RECEIVER_ACK")) {
                            foundIp = respPacket.address.hostAddress
                        }
                        udpSocket.close()
                    } catch (e: Exception) {
                        Log.d("TaikoRemoteSender", "UDP broadcast scan timeout or skipped: ${e.message}")
                    }
                }

                // 3. Fast TCP Subnet Scan across all network interfaces
                if (foundIp == null) {
                    val localIps = NetworkUtils.getAllLocalIpAddresses()
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
                                    s.connect(java.net.InetSocketAddress(ip, targetPort), 250)
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

    private var outputStream: java.io.OutputStream? = null
    private val socketLock = Any()

    fun connect(host: String, port: Int, listener: ConnectionListener? = null) {
        this.listener = listener
        disconnect()

        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newSingleThreadExecutor()
        }

        _statusState.value = "connecting"
        _errorMessageState.value = null

        executor.execute {
            try {
                Log.d("TaikoRemoteSender", "Connecting to $host:$port ...")
                TaikoLogManager.log("受信側 (ゲーム) へ接続試行中... $host:$port")

                val s = Socket()
                s.tcpNoDelay = true
                s.keepAlive = true
                s.sendBufferSize = 4096
                s.connect(java.net.InetSocketAddress(host, port), 5000)

                val out = s.getOutputStream()

                synchronized(socketLock) {
                    socket = s
                    outputStream = out
                    writer = BufferedWriter(OutputStreamWriter(out, "UTF-8"))
                    isConnected = true
                }

                _statusState.value = "connected"
                listener?.onConnected()
                TaikoLogManager.log("受信側 (ゲーム) に接続成功！ ($host:$port)")
            } catch (e: Exception) {
                isConnected = false
                _statusState.value = "error"
                val errMsg = e.message ?: "接続失敗"
                _errorMessageState.value = errMsg
                listener?.onError(errMsg)
                Log.e("TaikoRemoteSender", "Connection failed to $host:$port", e)
                TaikoLogManager.log("受信側 (ゲーム) への接続に失敗: $errMsg")
            }
        }
    }

    fun sendKeyEvent(partOrKey: String, isPressed: Boolean) {
        sendMultiKeyEvents(listOf(partOrKey), isPressed)
    }

    fun sendMultiKeyEvents(keys: List<String>, isPressed: Boolean) {
        if (!isConnected || keys.isEmpty() || executor.isShutdown) return
        val action = if (isPressed) "DOWN" else "UP"
        val message = "$action ${keys.joinToString(" ")}\n"
        val bytes = message.toByteArray(Charsets.UTF_8)

        executor.execute {
            try {
                synchronized(socketLock) {
                    val out = outputStream ?: return@execute
                    out.write(bytes)
                    out.flush()
                }
            } catch (e: Exception) {
                Log.w("TaikoRemoteSender", "Send key error", e)
                isConnected = false
                _statusState.value = "error"
                _errorMessageState.value = "送信エラー: ${e.message}"
                TaikoLogManager.log("送信エラー: ${e.message}")
            }
        }
    }

    fun disconnect() {
        if (isConnected) {
            listener?.onDisconnected()
        }
        isConnected = false
        synchronized(socketLock) {
            try {
                socket?.close()
            } catch (e: Exception) {}
            socket = null
            writer = null
            outputStream = null
        }
        _statusState.value = "disconnected"
    }
}

/**
 * 受信側（ゲーム側）で送信側（太鼓側）からのキー入力を待ち受け、Shizuku経由で注入するサーバー
 */
class TaikoAndroidRemoteReceiver(
    private val onKeyEventsReceived: (keys: List<String>, isPressed: Boolean) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var udpSocket: java.net.DatagramSocket? = null
    private val clients = ConcurrentHashMap<Socket, BufferedReader>()
    private var executor = Executors.newCachedThreadPool()
    @Volatile private var isRunning = false

    private val _activeClientsState = MutableStateFlow(0)
    val activeClientsState: StateFlow<Int> = _activeClientsState

    private var onClientCountChanged: ((Int) -> Unit)? = null

    private fun startUdpDiscovery(udpPort: Int = 60002) {
        executor.execute {
            try {
                val socket = java.net.DatagramSocket(udpPort)
                udpSocket = socket
                val buffer = ByteArray(256)
                while (isRunning && !socket.isClosed) {
                    val packet = java.net.DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length, java.nio.charset.StandardCharsets.UTF_8).trim()
                    if (msg.startsWith("DISCOVER_TAIKO_RECEIVER")) {
                        val responseData = "TAIKO_RECEIVER_ACK".toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                        val replyPacket = java.net.DatagramPacket(
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

    fun start(port: Int = 60001, onClientCountChanged: ((Int) -> Unit)? = null) {
        this.onClientCountChanged = onClientCountChanged
        if (isRunning) return
        isRunning = true

        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newCachedThreadPool()
        }

        startUdpDiscovery()

        executor.execute {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
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
                        onKeyEventsReceived(keys, isPressed)
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
        } catch (e: Exception) {}
        serverSocket = null

        try {
            udpSocket?.close()
        } catch (e: Exception) {}
        udpSocket = null

        val currentClients = clients.keys.toList()
        clients.clear()
        for (socket in currentClients) {
            try {
                socket.close()
            } catch (e: Exception) {}
        }
        _activeClientsState.value = 0

        try {
            executor.shutdownNow()
        } catch (e: Exception) {}

        TaikoLogManager.log("受信待機サーバー (ゲーム側) を停止しました")
    }
}
