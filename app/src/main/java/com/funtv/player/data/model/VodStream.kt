package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

data class VodStream(
    @SerializedName("num") val num: Int? = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("stream_type") val streamType: String? = "movie",
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("stream_icon") val streamIcon: String? = "",
    @SerializedName("category_id") val categoryId: String? = "",
    @SerializedName("container_extension") val containerExtension: String? = "mp4",
    @SerializedName("rating") val rating: String? = null,
    /** Timestamp Unix (segundos) de cuándo se agregó al panel, para la categoría "Recién agregado". */
    @SerializedName("added") val added: String? = null
)
