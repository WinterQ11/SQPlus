package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

sealed class RealtimeChangeEvent {
    data class ChannelChanged(val eventType: String, val channelId: String) : RealtimeChangeEvent()
    data class DocumentChanged(val eventType: String, val documentId: String, val channelId: String?) : RealtimeChangeEvent()
    object RefreshAll : RealtimeChangeEvent()
}

class RealtimeSyncService(
    private val config: SupabaseConfig,
    private val scope: CoroutineScope,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "RealtimeSyncService"
    }

    private val refCounter = AtomicInteger(1)
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _changeEvents = MutableSharedFlow<RealtimeChangeEvent>(extraBufferCapacity = 64)
    val changeEvents: SharedFlow<RealtimeChangeEvent> = _changeEvents.asSharedFlow()

    fun start() {
        connect()
    }

    fun stop() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "App closed")
        webSocket = null
        _isConnected.value = false
    }

    private fun connect() {
        val baseUrl = config.getSupabaseUrl()
        val anonKey = config.getAnonKey()

        val wsUrl = baseUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/realtime/v1/websocket?apikey=$anonKey&vsn=1.0.0"

        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Supabase Realtime")
                _isConnected.value = true
                joinChannels(webSocket)
                startHeartbeat(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime connection failure: ${t.message}")
                _isConnected.value = false
                scheduleReconnect()
            }
        })
    }

    private fun joinChannels(ws: WebSocket) {
        val ref1 = refCounter.getAndIncrement().toString()
        val joinChannelsMsg = JSONObject().apply {
            put("topic", "realtime:public:channels")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "channels")
                        })
                    })
                })
            })
            put("ref", ref1)
        }.toString()
        ws.send(joinChannelsMsg)

        val ref2 = refCounter.getAndIncrement().toString()
        val joinDocsMsg = JSONObject().apply {
            put("topic", "realtime:public:documents")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "documents")
                        })
                    })
                })
            })
            put("ref", ref2)
        }.toString()
        ws.send(joinDocsMsg)
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && _isConnected.value) {
                delay(25_000L)
                val ref = refCounter.getAndIncrement().toString()
                val heartbeatMsg = JSONObject().apply {
                    put("topic", "phoenix")
                    put("event", "heartbeat")
                    put("payload", JSONObject())
                    put("ref", ref)
                }.toString()
                ws.send(heartbeatMsg)
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val topic = json.optString("topic")
            val payload = json.optJSONObject("payload")

            if (event == "postgres_changes" && payload != null) {
                val data = payload.optJSONObject("data")
                val recordType = data?.optString("type") ?: ""
                val record = data?.optJSONObject("record") ?: data?.optJSONObject("old_record")
                val table = data?.optString("table") ?: ""

                if (table == "channels" || topic.contains("channels")) {
                    val channelId = record?.optString("id") ?: ""
                    _changeEvents.tryEmit(RealtimeChangeEvent.ChannelChanged(recordType, channelId))
                } else if (table == "documents" || topic.contains("documents")) {
                    val docId = record?.optString("id") ?: ""
                    val channelId = record?.optString("channel_id")
                    _changeEvents.tryEmit(RealtimeChangeEvent.DocumentChanged(recordType, docId, channelId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing realtime message: $text", e)
        }
    }

    private fun scheduleReconnect() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(5_000L)
            if (!_isConnected.value) {
                connect()
            }
        }
    }
}
