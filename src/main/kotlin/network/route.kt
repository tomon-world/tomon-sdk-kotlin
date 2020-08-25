package tomon.bot.network

import com.google.gson.Gson
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.lang.Exception
import java.util.concurrent.TimeUnit




class Route {
    private val base = "https://beta.tomon.co/api/v1"
    var path: String = ""
    var token: String = ""
    var url : String = ""
    constructor(path: String, token: String) {
        this.path = path
        this.token = token
        this.url = this.base + path
    }

    fun auth(): String {
        if(this.token == "") {
            return this.token
        } else {
            return ("Bearer " + this.token)
        }

    }

    private fun request(method: String, url: String, auth:Boolean?, data: Any?, files: List<File>?): String? {
        var client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build()
        var requestBuilder = Request.Builder().url(url)
        var formBuilder = FormBody.Builder()
        var multiPartBuilder = MultipartBody.Builder()
        var request : Request
        if (!(auth != null && auth == false)) {
            requestBuilder.addHeader("authorization", this.auth())
        }
        if (files == null) {
            if (data != null) {
                if (data is Map<*, *>) {
                    for ((k, v) in data) {
                        formBuilder.add(k as String, v as String)
                    }
                }
            }
            when(method) {
                "POST" -> {request = requestBuilder.post(formBuilder.build()).build()}
                "GET" -> {request = requestBuilder.get().build()}
                "PATCH" -> {request = requestBuilder.patch(formBuilder.build()).build()}
                "PUT" -> {request = requestBuilder.put(formBuilder.build()).build()}
                "DELETE" -> {request = requestBuilder.delete(formBuilder.build()).build()}
                else -> {throw IllegalArgumentException("Wrong method")}
            }
        } else {
            files.forEachIndexed { i, file -> multiPartBuilder.setType(MultipartBody.FORM).addFormDataPart("file$i", file.name, file.asRequestBody()) }
            if (data != null) {
                multiPartBuilder.addFormDataPart("payload_json", Gson().toJson(data))
            }
            when(method) {
                "POST" -> {request = requestBuilder.post(multiPartBuilder.build()).build()}
                "GET" -> {request = requestBuilder.get().build()}
                "PATCH" -> {request = requestBuilder.patch(multiPartBuilder.build()).build()}
                "PUT" -> {request = requestBuilder.put(multiPartBuilder.build()).build()}
                "DELETE" -> {request = requestBuilder.delete(multiPartBuilder.build()).build()}
                else -> {throw IllegalArgumentException("Wrong method")}
            }
        }
        var response : Response
        try {
            response = client.newCall(request).execute()
        }catch (e: Exception) {
            return null
        }
        if (response.code in 200..299) {
            return response.body?.string()
        } else if (response.code == 404) {
            return null
        } else if (response.code == 403) {
            return null
        }
        return null
    }

    fun post(auth: Boolean? = null, data: Any? = null, files: List<File>? = null): String? {
        return request("POST", this.url, auth, data, files)
    }

    fun get(auth: Boolean? = null, data: Any? = null, files: List<File>? = null): String? {
        return request("GET", this.url, auth, data, files)
    }

    fun patch(auth: Boolean? = null, data: Any? = null, files: List<File>? = null): String? {
        return request("PATCH", this.url, auth, data, files)
    }

    fun put(auth: Boolean? = null, data: Any? = null, files: List<File>? = null): String? {
        return request("PUT", this.url, auth, data, files)
    }

    fun delete(auth: Boolean? = null, data: Any? = null, files: List<File>? = null): String? {
        return request("DELETE", this.url, auth, data, files)
    }
}