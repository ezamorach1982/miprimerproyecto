package com.funtv.player.data.model

/** Credenciales + host ya normalizado (con esquema, sin slash final). */
data class XtreamSession(
    val baseUrl: String,
    val username: String,
    val password: String
)
