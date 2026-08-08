package com.example

import android.util.Log
import java.io.BufferedWriter
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
    @Volatile private var isRunning = false

    fun start(port: Int = 60001) {
        if (isRunning) return
        isRunning = true
        
        if (executor.isShutdown || executor.isTerminated) {
            executor = Executors.newCachedThreadPool()
        }

        executor.execute {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
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
            socket.keepAlive = true
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
            
            // Send initial OK handshake banner
            synchronized(writer) {
                writer.write("OK\n")
                writer.flush()
            }

            clients[socket] = writer
            onConnectionStatusChanged(clients.size)

            Log.d("TaikoTcpServer", "PC Client connected successfully, active clients: ${clients.size}")
            TaikoLogManager.log("PC Client connected to port 60001 (Client remote port: ${socket.port}) | Active clients: ${clients.size}")

            // Keep socket active with a periodic heartbeat (PING every 2 seconds).
            // PC receiver scripts are receive-only (Android -> PC).
            // Sending a heartbeat reliably monitors socket liveness without relying on incoming socket reads,
            // preventing premature TCP EOF disconnects on half-duplex socket reads.
            while (isRunning && !socket.isClosed) {
                Thread.sleep(2000)
                synchronized(writer) {
                    writer.write("PING\n")
                    writer.flush()
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
        if (clients.isEmpty() || executor.isShutdown) return
        val action = if (isPressed) "DOWN" else "UP"
        val message = "$action $key\n"
        
        executor.execute {
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
        } catch (e: Exception) {}

        Log.d("TaikoTcpServer", "Server stopped")
        TaikoLogManager.log("TCP Server stopped")
    }
}
