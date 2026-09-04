package com.example

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class TaikoTcpServer(
    private val onConnectionStatusChanged: (Int) -> Unit // returns the number of active PC clients
) {
    private var serverSocket: ServerSocket? = null
    private val clients = ConcurrentHashMap<Socket, BufferedWriter>()
    private var executor = Executors.newCachedThreadPool()
    private var sendExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var isRunning = false
    @Volatile private var activePort = 60001

    fun start(port: Int = 60001) {
        if (isRunning) return
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
                            bind(java.net.InetSocketAddress(port))
                        }
                        break
                    } catch (e: Exception) {
                        if (i < 10) {
                            Log.w("TaikoTcpServer", "Port $port waiting for socket release (attempt $i/10)...")
                            try { Thread.sleep(200) } catch (_: InterruptedException) { return@execute }
                        } else {
                            throw e
                        }
                    }
                }
                serverSocket = sSocket
                Log.d("TaikoTcpServer", "Server bound successfully to port $port (dual-stack)")
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
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            try { socket.trafficClass = 0x10 } catch (_: Exception) {}
            socket.keepAlive = true

            // In USB PC mode, cleanup any prior stale/abandoned connections
            val priorSockets = clients.keys.toList()
            for (oldSocket in priorSockets) {
                if (oldSocket != socket) {
                    clients.remove(oldSocket)
                    try { oldSocket.close() } catch (_: Exception) {}
                }
            }

            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            
            // Send initial OK handshake banner
            synchronized(writer) {
                writer.write("OK\n")
                writer.flush()
            }

            clients[socket] = writer
            onConnectionStatusChanged(clients.size)

            Log.d("TaikoTcpServer", "PC Client connected successfully, active clients: ${clients.size}")
            TaikoLogManager.log("PC Client connected to port $activePort (Client remote port: ${socket.port}) | Active clients: ${clients.size}")

            // Start daemon heartbeat thread: sends PING every 2 seconds to keep connection alive
            val heartbeatThread = Thread {
                try {
                    while (isRunning && !socket.isClosed && clients.containsKey(socket)) {
                        Thread.sleep(2000)
                        synchronized(writer) {
                            writer.write("PING\n")
                            writer.flush()
                        }
                    }
                } catch (_: Exception) {}
            }.apply {
                isDaemon = true
                name = "TaikoTcp-Heartbeat-${socket.port}"
                start()
            }

            // Reader loop blocks on input stream. When client closes connection, readLine() immediately returns null (EOF)
            while (isRunning && !socket.isClosed) {
                val line = reader.readLine() ?: break
                if (line == "PING") {
                    synchronized(writer) {
                        writer.write("PONG\n")
                        writer.flush()
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

        sendExecutor.execute {
            val iterator = clients.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val socket = entry.key
                val writer = entry.value
                try {
                    synchronized(writer) {
                        writer.write(message)
                        writer.flush()
                    }
                } catch (e: Exception) {
                    Log.w("TaikoTcpServer", "Failed to write to socket, removing client", e)
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
