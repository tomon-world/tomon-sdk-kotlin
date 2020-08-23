package tomon.bot.network

import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.util.*

class WS() {
    enum class WebsocketState {
        NOT_YET_CONNECTED, OPEN, CLOSING, CLOSED
    }

    fun retryDelay(times: Int): Int {
        times.let {
            when {
                times <= 0 -> return 500
                times <= 1 -> return 1000
                times <= 5 -> return 5000
                else -> 10000
            }
        }
        return 10000
    }

    private var ws: WebSocketClient?
    private var retryCount: Int
    private var reconnecting: Boolean
    private var reconnectTimer: Timer?
    private val gson = Gson()
    private var forceClose : Boolean

    var onOpen: (() -> Unit)?
    var onClose: ((code: Int, reason: String?) -> Unit)?
    var onReconnect: ((count: Int) -> Unit)?
    var onError: ((error: Exception) -> Unit)?
    var onMessage: ((data: Any) -> Unit)?

    init {
        this.ws = null
        this.retryCount = 0
        this.reconnecting = false
        this.reconnectTimer = null
        this.onOpen = null
        this.onClose = null
        this.onReconnect = null
        this.onError = null
        this.onMessage = null
        this.forceClose = false
    }

    public fun open(url: URI) {
        if (this.state() != WebsocketState.CLOSED) {
            return
        }
        this.forceClose = false
        this._connect(url)
    }

    public fun close(code: Int, reason: String?) {
        if (this.state() != WebsocketState.CLOSED) {
            this.reconnecting = false
            this.forceClose = true
            this._close(code, reason)
        }
    }

    public fun send(data: Any) {
        if (this.state() != WebsocketState.OPEN) {
            return
        }
        val json = gson.toJson(data)
        this.ws?.send(json)
    }

    public fun url(): URI {
        if (this.ws == null) {
            return URI.create("")
        }
        return this.ws!!.uri
    }

    public fun reconnecting(): Boolean {
        return this.reconnecting
    }

    public fun state(): WebsocketState {
        return when (this.ws?.readyState) {
            ReadyState.CLOSED -> WebsocketState.CLOSED
            ReadyState.CLOSING -> WebsocketState.CLOSING
            ReadyState.NOT_YET_CONNECTED -> WebsocketState.NOT_YET_CONNECTED
            ReadyState.OPEN -> WebsocketState.OPEN
            null -> WebsocketState.CLOSED
        }
    }

    private fun _connect(url: URI) {
        val that = this

        class Client(url: URI) : WebSocketClient(url) {

            override fun onOpen(handshake: ServerHandshake?) {
                that._onOpen()
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                that._onClose(code, reason)
            }

            override fun onMessage(data: String?) {
                if (data == null) {
                    return
                }
                that._onMessage(data)
            }

            override fun onMessage(data: ByteBuffer?) {
                if (data == null) {
                    return
                }
                that._onMessage(data)
            }

            override fun onError(error: Exception?) {
                that._onError(error)
            }

        }
        this.ws = Client(url)
        this.ws!!.connect()
    }

    private fun _reconnect(url: URI) {
        if (this.reconnectTimer != null) {
            this.reconnectTimer?.cancel()
        }
        this.reconnecting = true
        this.reconnectTimer = Timer(false)
        val that = this
        this.reconnectTimer?.schedule(object : TimerTask() {
            override fun run() {
                that.retryCount += 1
                that._connect(that.url())
                if (that.onReconnect != null) {
                    that.onReconnect!!(that.retryCount)
                }
            }
        }, this.retryDelay(this.retryCount).toLong())
    }

    private fun _close(code: Int, reason: String?) {
        this.ws?.close(code, reason)
    }

    private fun _stopReconnect() {
        if (this.reconnectTimer != null) {
            this.reconnectTimer!!.cancel()
            this.reconnectTimer = null
        }
    }

    private fun _onOpen() {
        this.retryCount = 0
        this.reconnecting = false
        this._stopReconnect()
        if (this.onOpen != null) {
            this.onOpen!!()
        }
    }

    private fun _onClose(code: Int, reason: String?) {
        var resultReason: String = ""
        if (reason != null) {
            resultReason = try {
                val data = gson.fromJson(reason, MutableMap::class.java)
                data["reason"].toString()
            } catch (e: Exception) {
                reason
            }
        }
        val needReconnect = if (code == 1006) {
            true
        } else code >= 4000
        if (needReconnect && !this.forceClose) {
            this._reconnect(this.url())
        }
        if (this.onClose != null) {
            this.onClose!!(code, reason)
        }
    }

    private fun _onMessage(data: Any?) {
        if (this.onMessage != null && data != null) {
            this.onMessage!!(data)
        }
    }

    private fun _onError(exception: Exception?) {
        if (this.onError != null && exception != null) {
            this.onError!!(exception)
        }
    }
}

