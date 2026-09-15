package com.funtv.player.data.api

import com.funtv.player.data.model.XtreamSession

/** Construye las URLs reproducibles a partir de la convención de rutas de Xtream Codes. */
object StreamUrlBuilder {

    fun liveUrl(session: XtreamSession, streamId: Int): String =
        "${session.baseUrl}/live/${session.username}/${session.password}/$streamId.m3u8"

    fun vodUrl(session: XtreamSession, streamId: Int, containerExtension: String?): String {
        val ext = containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"
        return "${session.baseUrl}/movie/${session.username}/${session.password}/$streamId.$ext"
    }

    fun seriesEpisodeUrl(session: XtreamSession, episodeId: String, containerExtension: String?): String {
        val ext = containerExtension?.takeIf { it.isNotBlank() } ?: "mp4"
        return "${session.baseUrl}/series/${session.username}/${session.password}/$episodeId.$ext"
    }
}
