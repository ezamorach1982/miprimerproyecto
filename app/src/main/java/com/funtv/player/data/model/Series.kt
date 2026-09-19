package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

data class Series(
    @SerializedName("series_id") val seriesId: Int = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("cover") val cover: String? = "",
    @SerializedName("plot") val plot: String? = "",
    @SerializedName("cast") val cast: String? = "",
    @SerializedName("director") val director: String? = "",
    @SerializedName("genre") val genre: String? = "",
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("release_date") val releaseDateAlt: String? = null,
    @SerializedName("rating") val rating: String? = "",
    @SerializedName("category_id") val categoryId: String? = "",
    /** Timestamp Unix (segundos) de cuándo se agregó al panel, para la categoría "Recién agregado". */
    @SerializedName("last_modified") val added: String? = null
) {
    val effectiveReleaseDate: String?
        get() = releaseDate ?: releaseDateAlt
}
