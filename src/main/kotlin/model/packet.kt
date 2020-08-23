package tomon.bot.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class Packet(
        @SerializedName("op") val Op: Int,
        @SerializedName("d") val Data: JsonObject,
        @SerializedName("e") val Event: String
)