package com.example

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.util.concurrent.TimeUnit

@Serializable
data class InputMessageEvent(
    val part: String,
    val isPressed: Boolean,
    val key: String? = null
)

@Serializable
data class WebSocketPayload(
    val type: String,
    val roomId: String? = null,
    val clientsCount: Int? = null,
    val inputs: List<InputMessageEvent>? = null,
    val connectionMode: String? = null,
    val adbHost: String? = null,
    val adbPort: String? = null
)

class TaikoWebSocketClient(
    private val onConnectionChanged: (Boolean, String?) -> Unit,
    private val onPeerCountChanged: (Int) -> Unit,
    private val onRemoteInputReceived: (List<InputMessageEvent>) -> Unit
) {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }
    private var isConnected = false

    fun connect(url: String, roomId: String) {
        disconnect()

        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnectionChanged(true, null)
                Log.d("TaikoWS", "WebSocket connection opened!")
                TaikoLogManager.log("WS Opened. Joining room: $roomId")
                
                // Join room immediately
                joinRoom(roomId)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val payload = json.decodeFromString<WebSocketPayload>(text)
                    when (payload.type) {
                        "joined" -> {
                            val count = payload.clientsCount ?: 1
                            onPeerCountChanged(count - 1)
                            TaikoLogManager.log("WS Joined Room. Active players: $count")
                        }
                        "peer_joined" -> {
                            val count = payload.clientsCount ?: 1
                            onPeerCountChanged(count - 1)
                            TaikoLogManager.log("WS Peer Joined. Active players: $count")
                        }
                        "peer_left" -> {
                            val count = payload.clientsCount ?: 1
                            onPeerCountChanged(count - 1)
                            TaikoLogManager.log("WS Peer Left. Active players: $count")
                        }
                        "input" -> {
                            payload.inputs?.let {
                                onRemoteInputReceived(it)
                                val parts = it.joinToString(", ") { ev -> "${ev.part}=${if (ev.isPressed) "Down" else "Up"}" }
                                TaikoLogManager.log("WS Recv Input: $parts")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("TaikoWS", "Failed to parse message: $text", e)
                    TaikoLogManager.log("WS Parse Err: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                onConnectionChanged(false, null)
                onPeerCountChanged(0)
                TaikoLogManager.log("WS Closed (code: $code, reason: $reason)")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onConnectionChanged(false, t.localizedMessage)
                onPeerCountChanged(0)
                Log.e("TaikoWS", "WebSocket failure", t)
                TaikoLogManager.log("WS Fail: ${t.localizedMessage}")
            }
        })
    }

    fun joinRoom(roomId: String) {
        if (!isConnected) return
        val payload = WebSocketPayload(type = "join", roomId = roomId)
        webSocket?.send(json.encodeToString(payload))
    }

    fun sendInput(inputs: List<InputMessageEvent>, roomId: String, mode: String, adbHost: String, adbPort: String) {
        if (!isConnected) return
        val payload = WebSocketPayload(
            type = "input",
            inputs = inputs,
            roomId = roomId,
            connectionMode = mode,
            adbHost = adbHost,
            adbPort = adbPort
        )
        webSocket?.send(json.encodeToString(payload))
        val parts = inputs.joinToString(", ") { ev -> "${ev.part}=${if (ev.isPressed) "Down" else "Up"}" }
        TaikoLogManager.log("WS Sent Input: $parts")
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Disconnect requested")
            webSocket = null
            client?.dispatcher?.executorService?.shutdown()
            client = null
        } catch (e: Exception) {
            Log.e("TaikoWS", "Error disconnecting", e)
        }
        isConnected = false
    }
}
