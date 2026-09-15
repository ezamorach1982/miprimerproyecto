package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

/** Resultado de get_vod_info: metadata adicional (sinopsis, reparto, etc.) que no viene en get_vod_streams. */
data class VodInfoResponse(
    @SerializedName("info") val info: VodExtraInfo? = null
)

data class VodExtraInfo(
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("cast") val cast: String? = null,
    @SerializedName("director") val director: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("releasedate") val releaseDate: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("movie_image") val movieImage: String? = null
)
