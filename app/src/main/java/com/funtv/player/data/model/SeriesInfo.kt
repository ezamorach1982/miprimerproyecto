package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

/**
 * Resultado de get_series_info. Se parsea manualmente en XtreamClient porque el
 * campo "episodes" es un objeto {"1": [...], "2": [...]} indexado por temporada,
 * pero algunos paneles devuelven un array [] vacío en lugar de {} cuando la serie
 * no tiene episodios cargados; Gson no puede mapear ambos casos al mismo tipo.
 */
data class SeriesInfoResponse(
    val info: SeriesExtraInfo?,
    val seasons: List<SeasonInfo>,
    val episodesBySeason: Map<Int, List<Episode>>
)

data class SeriesExtraInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("cast") val cast: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("rating") val rating: String? = null
)

data class SeasonInfo(
    @SerializedName("season_number") val seasonNumber: Int = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("cover") val cover: String? = ""
)

data class Episode(
    @SerializedName("id") val id: String = "",
    @SerializedName("episode_num") val episodeNum: Int? = 0,
    @SerializedName("title") val title: String? = "",
    @SerializedName("container_extension") val containerExtension: String? = "mp4",
    @SerializedName("season") val season: Int? = 0
)
