package com.indhg.aiforcoyote.relay

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 内嵌 v4 中继服务端（127.0.0.1）。
 * 原样移植 dglab-websocket-server v4-server.ts 的房间/转发语义：
 * - 无 tid 的连接 = 控制方；带 ?tid= 的连接 = 被控方（郊狼 App）
 * - 控制方下发 {"type":"message","clientId":被控方,"data":...}，转发给被控方为 {"type":"message","data":...}
 * - 被控方上行转发给控制方为 {"type":"message","clientId":被控方,"data":...}
 * - 业务心跳 30s 广播 + 原生 ping 10s（3 次未响应踢除）+ 控制方空闲 5 分钟关闭
 */
class V4RelayServer(private val port: Int = 9998) : WebSocketServer(InetSocketAddress(port)) {

    private val wsToClientId = ConcurrentHashMap<WebSocket, String>()
    private val controllersById = ConcurrentHashMap<String, WebSocket>()
    private val controlledClients = ConcurrentHashMap<WebSocket, MutableMap<String, WebSocket>>()
    private val clientToController = ConcurrentHashMap<WebSocket, WebSocket>()
    private val idleTimers = ConcurrentHashMap<WebSocket, ScheduledFuture<*>>()
    private val missedPongs = ConcurrentHashMap<WebSocket, Int>()

    private val scheduler = Executors.newScheduledThreadPool(2)
    private var heartbeatTask: ScheduledFuture<*>? = null
    private var pingTask: ScheduledFuture<*>? = null

    private val json = Json { ignoreUnknownKeys = true }

    fun startRelay() {
        start()
        Log.i(TAG, "中继启动 端口=$port")
        heartbeatTask = scheduler.scheduleAtFixedRate({ broadcastHeartbeat() }, 30, 30, TimeUnit.SECONDS)
        pingTask = scheduler.scheduleAtFixedRate({ pingConnections() }, 10, 10, TimeUnit.SECONDS)
    }

    fun stopRelay() {
        heartbeatTask?.cancel(false)
        pingTask?.cancel(false)
        try {
            stop(1000)
        } catch (_: Exception) {
        }
        Log.i(TAG, "中继停止")
    }

    private fun sendFrame(ws: WebSocket, payload: JsonObject) {
        if (!ws.isOpen) return
        ws.send(payload.toString())
    }

    private fun obj(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
        for ((k, v) in pairs) {
            when (v) {
                null -> put(k, JsonNull)
                is JsonElement -> put(k, v)
                is String -> put(k, JsonPrimitive(v))
                is Boolean -> put(k, JsonPrimitive(v))
                is Int -> put(k, JsonPrimitive(v))
                is Long -> put(k, JsonPrimitive(v))
                else -> put(k, JsonPrimitive(v.toString()))
            }
        }
    }

    private fun newClientId(): String {
        var id: String
        do {
            val bytes = ByteArray(4).also { Random.nextBytes(it) }
            id = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        } while (wsToClientId.values.contains(id))
        return id
    }

    private fun broadcastHeartbeat() {
        val payload = """{"type":"heartbeat"}"""
        wsToClientId.keys.forEach { ws -> if (ws.isOpen) ws.send(payload) }
    }

    private fun pingConnections() {
        wsToClientId.keys.forEach { ws ->
            if (!ws.isOpen) return@forEach
            val missed = (missedPongs[ws] ?: 0) + 1
            if (missed > MAX_MISSED_PONGS) {
                Log.w(TAG, "WS探活超时 连接=${clientIdOf(ws)}")
                missedPongs.remove(ws)
                ws.closeConnection(4000, "ws timeout")
                return@forEach
            }
            missedPongs[ws] = missed
            ws.sendPing()
        }
    }

    private fun clientIdOf(ws: WebSocket): String = wsToClientId[ws] ?: "-"

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val clientId = newClientId()
        sendFrame(conn, obj("type" to "hello", "clientId" to clientId))
        wsToClientId[conn] = clientId
        missedPongs[conn] = 0

        val tid = parseTid(handshake.resourceDescriptor)
        if (tid != null) {
            attachClient(conn, clientId, tid)
        } else {
            attachController(conn, clientId)
        }
    }

    private fun parseTid(resource: String): String? {
        return try {
            val uri = URI.create(resource)
            val query = uri.query ?: return null
            query.split("&")
                .mapNotNull { it.split("=", limit = 2).takeIf { kv -> kv.size == 2 } }
                .firstOrNull { it[0] == "tid" || it[0] == "targetId" }
                ?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun attachController(ws: WebSocket, clientId: String) {
        controllersById[clientId] = ws
        controlledClients[ws] = ConcurrentHashMap()
        startIdleTimer(ws)
        Log.i(TAG, "控制方连接 控制方=$clientId")
    }

    private fun attachClient(ws: WebSocket, clientId: String, tid: String) {
        val controllerWs = controllersById[tid]
        if (controllerWs == null || !controllerWs.isOpen) {
            sendFrame(ws, obj("type" to "error", "code" to "controller_not_found"))
            ws.closeConnection(4001, "controller_not_found")
            Log.w(TAG, "被控方拒绝 目标=$tid 原因=控制方不存在")
            return
        }
        val clients = controlledClients[controllerWs]
        if (clients == null) {
            sendFrame(ws, obj("type" to "error", "code" to "controller_not_found"))
            ws.closeConnection(4001, "controller_not_found")
            return
        }
        clients[clientId] = ws
        clientToController[ws] = controllerWs
        cancelIdleTimer(controllerWs)
        sendFrame(ws, obj("type" to "controller_attached", "clientId" to tid))
        sendFrame(controllerWs, obj("type" to "client_attached", "clientId" to clientId))
        Log.i(TAG, "被控方接入 被控方=$clientId 控制方=$tid")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        val parsed = try {
            json.parseToJsonElement(message).jsonObject
        } catch (_: Exception) {
            Log.w(TAG, "WS JSON无效 连接=${clientIdOf(conn)}")
            return
        }
        val type = parsed["type"]?.jsonPrimitive?.contentOrNull
        if (type == "pong") return
        if (type == "ping") {
            sendFrame(conn, obj("type" to "pong", "ts" to System.currentTimeMillis()))
            return
        }
        if (type != "message") return
        val clientId = wsToClientId[conn] ?: return
        val targetId = parsed["clientId"]?.jsonPrimitive?.contentOrNull

        if (controllersById.containsKey(clientId)) {
            // 控制方 → 指定被控方
            if (targetId == null) {
                sendFrame(conn, obj("type" to "error", "code" to "bad_request", "message" to "message.clientId is required"))
                return
            }
            val clientWs = controlledClients[conn]?.get(targetId)
            if (clientWs != null && clientWs.isOpen) {
                sendFrame(clientWs, obj("type" to "message", "data" to parsed["data"]))
                Log.i(TAG, "WS转发 控制方=$clientId 被控方=$targetId")
            } else {
                Log.w(TAG, "WS被控方不存在 控制方=$clientId 被控方=$targetId")
                sendFrame(conn, obj("type" to "error", "code" to "client_not_found", "clientId" to targetId))
            }
            return
        }

        // 被控方 → 所属控制方
        val controllerWs = clientToController[conn]
        if (controllerWs != null && controllerWs.isOpen) {
            sendFrame(controllerWs, obj("type" to "message", "clientId" to clientId, "data" to parsed["data"]))
            Log.i(TAG, "WS上报 被控方=$clientId")
        } else {
            Log.w(TAG, "WS控制方缺失 被控方=$clientId")
        }
    }

    override fun onWebsocketPong(conn: WebSocket, f: org.java_websocket.framing.Framedata) {
        missedPongs[conn] = 0
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        val clientId = wsToClientId.remove(conn)
        missedPongs.remove(conn)
        if (clientId == null) return

        if (controllersById.containsKey(clientId)) {
            controllersById.remove(clientId)
            val clients = controlledClients.remove(conn)
            cancelIdleTimer(conn)
            clients?.forEach { (cId, cWs) ->
                clientToController.remove(cWs)
                sendFrame(cWs, obj("type" to "controller_disconnected", "clientId" to clientId))
                cWs.closeConnection(4000, "controller_disconnected")
                Log.i(TAG, "踢出被控方 被控方=$cId 控制方=$clientId")
            }
            Log.i(TAG, "控制方断开 控制方=$clientId 码=$code")
            return
        }

        val controllerWs = clientToController.remove(conn)
        if (controllerWs != null) {
            val clients = controlledClients[controllerWs]
            clients?.remove(clientId)
            if (controllerWs.isOpen) {
                sendFrame(controllerWs, obj("type" to "client_disconnected", "clientId" to clientId))
                if (clients.isNullOrEmpty()) startIdleTimer(controllerWs)
            }
            Log.i(TAG, "被控方断开 被控方=$clientId 码=$code")
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.w(TAG, "中继错误 ${conn?.let { clientIdOf(it) } ?: "-"}: ${ex.message}")
    }

    override fun onStart() = Unit

    private fun startIdleTimer(controllerWs: WebSocket) {
        cancelIdleTimer(controllerWs)
        val task = scheduler.schedule(
            {
                idleTimers.remove(controllerWs)
                if (controllerWs.isOpen) {
                    sendFrame(controllerWs, obj("type" to "idle_timeout"))
                    controllerWs.closeConnection(4002, "idle_timeout")
                    Log.w(TAG, "控制方空闲超时 控制方=${clientIdOf(controllerWs)}")
                }
            },
            IDLE_TIMEOUT_MIN,
            TimeUnit.MINUTES,
        )
        idleTimers[controllerWs] = task
    }

    private fun cancelIdleTimer(ws: WebSocket) {
        idleTimers.remove(ws)?.cancel(false)
    }

    companion object {
        private const val TAG = "V4Relay"
        private const val MAX_MISSED_PONGS = 3
        private const val IDLE_TIMEOUT_MIN = 5L
    }
}
