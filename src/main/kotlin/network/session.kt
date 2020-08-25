package tomon.bot.network

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import tomon.bot.model.Packet
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.zip.Inflater
import com.github.jafarlihi.eemit.EventEmitter

class SessionOptions(val zlib: Boolean?, val ws: String?)

object GatewayOp{
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val HELLO = 3
    const val HEARTBEAT_ACK = 4
    const val VOICE_STATE_UPDATE = 5
}

class Session(options: SessionOptions?,emitter: EventEmitter<Any>?) {
    private lateinit var url : URI
    private var zlib = false
    private var _emitter :EventEmitter<Any>? = emitter

    private var _heartbeatTimer : Timer? = null
    private var _heartbeatInterval : Int = 40000
    private var ws : WS
    private var ready : Boolean = false
    private var connected : Boolean = false
    private var sessionId : String? = null
    public var token : String = ""
    private var buffer : ByteArray? = null

    init {
        if (options?.zlib is Boolean){
            this.zlib = options.zlib
        }
        var url = if (options?.ws is String){
            options.ws
        }else{
            Config.ws
        }
        url += if (this.zlib ){
            "?compress=zlib-stream"
        }else {
            ""
        }
        this.url = URI(url)
        this.ws = WS()
        this.ws.onOpen = fun (){
            this.handleOpen()
        }
        this.ws.onClose = fun (code:Int,reason:String?){
            this.handleClose(code,reason)
        }
        this.ws.onMessage = fun (data:Any){
            this.handleMessage(data)
        }
        this.ws.onReconnect = fun (count:Int){
            this._emitter?.emit("NETWORK_RECONNECTING",count)
        }

    }
    public fun open(){
        this.ws.open(this.url)
    }

    public fun close(code:Int,reason:String?){
        this.ws.close(code,reason)
    }
    public fun send(op:Int,data:Any?=null){
        this.ws.send(mapOf("op" to op, "d" to data))
    }
    public fun state():WS.WebsocketState{
        return this.ws.state()
    }
    public fun connected():Boolean{
        return this.connected
    }
    public fun ready():Boolean{
        return this.ready
    }


    private fun handleOpen(){
        this.connected = true
        this._emitter?.emit("NETWORK_CONNECTED", null)
    }
    private fun handleClose(code:Int,reason:String?){
        this.stopHeartbeat()
        this.sessionId = null
        this.connected = false
        this.ready = false
        if (reason != null) {
            this._emitter?.emit("NETWORK_DISCONNECTED", arrayOf<Any>(code,reason))
        }else{
            this._emitter?.emit("NETWORK_DISCONNECTED",code)
        }
    }
    private fun handleMessage(data:Any){
        var raw :String = ""
        if(this.zlib){
            if (data is ByteBuffer){
                if (this.buffer == null){
                    this.buffer = ByteArray(65535)
                }
                val array = data.array()
                val l = array.size
                val flush = l >= 4 && array[l-4] == 0x00.toByte() && data[l-3] == 0xff.toByte() && data[l-1] == 0xff.toByte()
                this.buffer!!.plus(array)
                if (!flush){
                    return
                }
                raw = decompress(this.buffer!!)
            }
        }else if (data is String){
            raw = data
        }else{
            return
        }
        val packet : Packet
        try {
            packet = Gson().fromJson(raw, Packet::class.java)
        } catch (err:JsonSyntaxException){
            return
        }
        this.handlePacket(packet)
    }

    private fun decompress(input: ByteArray) : String {
        val inflater = Inflater()
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(65535)

        inflater.setInput(input)

        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }

        outputStream.close()

        return outputStream.toString(UTF_8)
    }
    private fun handlePacket(data:Packet){
        when (data.Op){
            GatewayOp.DISPATCH -> {
                this._emitter?.emit(data.Event,data)
                this._emitter?.emit("DISPATCH",data)
            }
            GatewayOp.IDENTIFY -> {
                this.ready = true
                this._emitter?.emit("READY",data)
            }
            GatewayOp.HELLO -> {
                this._heartbeatInterval = data.Data.get("heartbeat_interval").asInt
                this.sessionId = data.Data.get("session_id").asString
                this._heartbeatInterval = data.Data.get("heartbeat_interval").asInt
                this.heartbeat()
                this._emitter?.emit("HELLO",data)
                this.send(GatewayOp.IDENTIFY, mapOf("token" to this.token))
            }
            GatewayOp.HEARTBEAT_ACK -> {
                this._emitter?.emit("HEARTBEAT_ACK",null)
            }
        }
    }
    private fun heartbeat(){
        val that = this
        this._heartbeatTimer = Timer(false)
        this._heartbeatTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                that._emitter?.emit("HEARTBEAT", null)
                that.send(GatewayOp.HEARTBEAT)
            }
       }, 0,this._heartbeatInterval.toLong())
    }
    private fun stopHeartbeat(){
        this._heartbeatTimer?.cancel()
        this._heartbeatTimer = null
    }
}