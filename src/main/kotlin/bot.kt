package tomon.bot
import com.google.gson.Gson
import tomon.bot.network.Api
import tomon.bot.network.Session
import tomon.bot.network.SessionOptions
import com.github.jafarlihi.eemit.EventEmitter

object OpCodeEvent {
    const val DISPATCH = "DISPATCH"
    const val HEARTBEAT = "HEARTBEAT"
    const val READY = "READY"
    const val HELLO = "HELLO"
}

class Bot() {
    private val emitter = EventEmitter<Any>()
    private var token : String = ""
    private var session = Session(null, this.emitter)
    private var api = Api()
    private var id : String = ""
    private var name : String = ""
    private var username : String = ""
    private var discriminator : String = ""



    fun token(): String {
        return this.token
    }

    fun id(): String{
        return this.id
    }


    fun session(): Session {
        return this.session
    }


    fun api(): Api{
        return this.api
    }

    fun name(): String {
        return this.name
    }

    fun username(): String {
        return this.username
    }

    fun discriminator(): String {
        return this.discriminator
    }

    fun on(event: String, handler: (event: String, data: Any) -> Unit) {
        emitter.on(event) { event, data -> handler(event, data) }
    }


    fun off(uuid: String) {
        emitter.off(uuid)
    }


    fun emit(event: String, params: Any) {
        emitter.emit(event, params)
    }

    fun start(token: String) {
        return this._start(token)
    }

    fun startWithPassward(fullname: String, password: String) {
        return this._start(null, fullname, password)
    }

    private fun _start(token: String? = null, fullname: String? = null, password: String? = null) {
        var credentials = mutableMapOf<String, String>()
        if (token == null && fullname == null && password == null) {
            println("no parameters")
        } else if (token != null && fullname == null && password == null) {
            credentials["token"] = token
        } else if (token == null && fullname != null && password != null) {
            credentials["full_name"] = fullname
            credentials["password"] = password
        }
        println("⏳ Start authenticating...")
        var infoJson = this.api.route("/auth/login").post(data = credentials, auth = false)
        var infoMap : Map<*, *>
        if (infoJson != null) {
            infoMap = Gson().fromJson(infoJson, Map::class.java)
            this.token = infoMap["token"] as String
            this.session.token = infoMap["token"] as String
            this.api.setToken(this.token)
            this.id = infoMap["id"] as String
            this.name = infoMap["name"] as String
            this.username = infoMap["username"] as String
            this.discriminator = infoMap["discriminator"] as String

            println("🎫 Bot ${this.name}(${this.username}#${this.discriminator}) is authenticated.")
        } else {
            println("❌ Authentication failed. Please check your identity.")
            return
        }
        println("🚢 Connecting...")
        this.on("READY") { event: String, data: Any -> readyTest() }
        this.session.open()
    }



    fun readyTest(){
        val name = this.name()
        val username = this.username()
        val discriminator = this.discriminator()
        println("🤖️ Bot ${name}(${username}#${discriminator}) is ready to work!")
    }

}