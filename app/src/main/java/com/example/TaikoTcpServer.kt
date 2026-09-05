package com.example

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class TaikoTcpServer(
    private val onConnectionStatusChanged: (Int) -> Unit // returns the number of active PC clients
) {
    private var serverSocket: ServerSocket? = null
    private val clients = ConcurrentHashMap<Socket, OutputStream>()
    private var executor = Executors.newCachedThreadPool()
    private var sendExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var isRunning = false
    @Volatile private var activePort = 60001

    fun start(port: Int = 60001) {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) return
        isRunning = true
        activePort = port
        
        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newCachedThreadPool()
        }
        if (sendExecutor.isShutdown || sendExecutor.isTerminated) {
            sendExecutor = Executors.newSingleThreadExecutor()
        }

        executor.execute {
            try {
                var sSocket: ServerSocket? = null
                for (i in 1..10) {
                    if (!isRunning) return@execute
                    try {
                        sSocket = ServerSocket().apply {
                            reuseAddress = true
                            bind(InetSocketAddress("0.0.0.0", port))
                        }
                        break
                    } catch (e: Exception) {
                        if (i < 10) {
                            Log.w("TaikoTcpServer", "Port $port waiting for socket release (attempt $i/10)...")
                            try { Thread.sleep(150) } catch (_: InterruptedException) { return@execute }
                        } else {
                            throw e
                        }
                    }
                }
                serverSocket = sSocket
                Log.d("TaikoTcpServer", "Server bound successfully to 0.0.0.0:$port")
                TaikoLogManager.log("TCP Server active on port $port. Ready for PC connection!")
                
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d("TaikoTcpServer", "PC Client accepted: ${socket.remoteSocketAddress}")
                    
                    executor.execute {
                        handleClient(socket)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("TaikoTcpServer", "Error in server socket accept", e)
                    TaikoLogManager.log("TCP Server Error: ${e.message}")
                    isRunning = false
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            try { socket.trafficClass = 0x10 } catch (_: Exception) {} // IPTOS_LOWDELAY
            try { socket.sendBufferSize = 4096 } catch (_: Exception) {}

            // In USB PC mode, cleanup any prior stale/abandoned connections immediately
            val priorSockets = clients.keys.toList()
            for (oldSocket in priorSockets) {
                if (oldSocket != socket) {
                    clients.remove(oldSocket)
                    try { oldSocket.close() } catch (_: Exception) {}
                }
            }

            val outStream = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            
            // Send initial OK handshake banner
            outStream.write("OK\n".toByteArray(Charsets.UTF_8))
            outStream.flush()

            clients[socket] = outStream
            onConnectionStatusChanged(clients.size)

            Log.d("TaikoTcpServer", "PC Client connected successfully, active clients: ${clients.size}")
            TaikoLogManager.log("PC Client connected to port $activePort (Remote port: ${socket.port}) | Active clients: ${clients.size}")

            // Reader loop: blocks waiting for client messages (e.g. PING) or EOF on disconnect
            while (isRunning && !socket.isClosed) {
                val line = reader.readLine() ?: break
                if (line == "PING") {
                    try {
                        outStream.write("PONG\n".toByteArray(Charsets.UTF_8))
                        outStream.flush()
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("TaikoTcpServer", "Client handler ended: ${e.message}")
        } finally {
            clients.remove(socket)
            onConnectionStatusChanged(clients.size)
            try {
                socket.close()
            } catch (e: Exception) {}
            Log.d("TaikoTcpServer", "PC Client disconnected. Active clients: ${clients.size}")
            TaikoLogManager.log("PC Client disconnected (Remote port: ${socket.port}). Active clients: ${clients.size}")
        }
    }

    fun sendKeyEvent(key: String, isPressed: Boolean) {
        sendMultiKeyEvents(listOf(key), isPressed)
    }

    fun sendMultiKeyEvents(keys: List<String>, isPressed: Boolean) {
        if (clients.isEmpty() || keys.isEmpty() || sendExecutor.isShutdown) return
        val action = if (isPressed) "DOWN" else "UP"
        val message = buildString {
            keys.forEach { key ->
                append(action).append(" ").append(key).append("\n")
            }
        }
        val bytes = message.toByteArray(Charsets.UTF_8)

        sendExecutor.execute {
            val iterator = clients.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val socket = entry.key
                val out = entry.value
                try {
                    out.write(bytes)
                    out.flush()
                } catch (e: Exception) {
                    Log.w("TaikoTcpServer", "Failed to write to socket, dropping client", e)
                    iterator.remove()
                    try {
                        socket.close()
                    } catch (ex: Exception) {}
                    onConnectionStatusChanged(clients.size)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        
        val currentClients = clients.keys.toList()
        clients.clear()
        for (socket in currentClients) {
            try {
                socket.setSoLinger(true, 0)
                socket.close()
            } catch (e: Exception) {}
        }
        onConnectionStatusChanged(0)

        try {
            executor.shutdownNow()
            sendExecutor.shutdownNow()
        } catch (e: Exception) {}

        Log.d("TaikoTcpServer", "Server stopped")
        TaikoLogManager.log("TCP Server stopped")
    }
}
