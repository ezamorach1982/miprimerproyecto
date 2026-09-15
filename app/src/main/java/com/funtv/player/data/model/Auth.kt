package com.funtv.player.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta de player_api.php sin acción: autenticación + info de cuenta/servidor.
 * Los campos numéricos de Xtream Codes a veces llegan como String y a veces como
 * número JSON según el panel; se declaran como String? porque el adaptador String
 * de Gson acepta ambos tokens sin fallar.
 */
data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?
)

data class UserInfo(
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("auth") val auth: Int? = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("exp_date") val expDate: String? = null,
    @SerializedName("is_trial") val isTrial: String? = null,
    @SerializedName("active_cons") val activeConnections: String? = null,
    @SerializedName("max_connections") val maxConnections: String? = null,
    @SerializedName("message") val message: String? = null
)

data class ServerInfo(
    @SerializedName("url") val url: String? = null,
    @SerializedName("port") val port: String? = null,
    @SerializedName("https_port") val httpsPort: String? = null,
    @SerializedName("server_protocol") val serverProtocol: String? = null,
    @SerializedName("timezone") val timezone: String? = null
)
