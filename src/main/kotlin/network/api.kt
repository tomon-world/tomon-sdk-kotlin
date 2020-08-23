package tomon.bot.network

class Api {
    private var token: String = ""

    fun token(): String {
        return this.token
    }

    fun setToken(token:String) {
        this.token = token
    }

    fun route(path: String): Route {
        return Route(path, this.token)
    }
}
