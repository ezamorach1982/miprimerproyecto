package com.funtv.player.data.api

import com.funtv.player.data.model.Category
import com.funtv.player.data.model.Episode
import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.SeasonInfo
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.SeriesExtraInfo
import com.funtv.player.data.model.SeriesInfoResponse
import com.funtv.player.data.model.VodInfoResponse
import com.funtv.player.data.model.VodStream
import com.funtv.player.data.model.XtreamAuthResponse
import com.funtv.player.data.model.XtreamSession
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Error de negocio o de red al hablar con el panel Xtream Codes; el mensaje ya está listo para mostrarse al usuario. */
open class XtreamException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** La sesión guardada ya no es válida (cuenta vencida, contraseña cambiada, etc.): hay que volver a Login. */
class XtreamSessionExpiredException : XtreamException("La sesión ya no es válida, inicia sesión de nuevo")

/**
 * Cliente ligero del API Xtream Codes (player_api.php) sobre OkHttp + Gson.
 *
 * No se usa Retrofit porque las respuestas de distintos paneles Xtream son
 * inconsistentes (una categoría vacía puede llegar como `{}` en vez de `[]`,
 * ids numéricos a veces como String); aquí se controla ese parseo a mano.
 */
class XtreamClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson = Gson()
) {

    suspend fun login(rawServerUrl: String, username: String, password: String): Pair<XtreamSession, XtreamAuthResponse> {
        val baseUrl = normalizeBaseUrl(rawServerUrl)
        val url = "$baseUrl/player_api.php?username=${encode(username)}&password=${encode(password)}"
        val body = executeGet(url)

        val response = try {
            gson.fromJson(body, XtreamAuthResponse::class.java)
        } catch (e: Exception) {
            throw XtreamException("El servidor respondió con un formato inesperado", e)
        } ?: throw XtreamException("Respuesta vacía del servidor")

        if (response.userInfo?.auth != 1) {
            throw XtreamException("Usuario o contraseña incorrectos")
        }

        return XtreamSession(baseUrl, username, password) to response
    }

    suspend fun getLiveCategories(session: XtreamSession): List<Category> =
        parseArray(executeGet(actionUrl(session, "get_live_categories")))

    suspend fun getLiveStreams(session: XtreamSession, categoryId: String): List<LiveStream> =
        parseArray(executeGet(actionUrl(session, "get_live_streams") + "&category_id=${encode(categoryId)}"))

    suspend fun getVodCategories(session: XtreamSession): List<Category> =
        parseArray(executeGet(actionUrl(session, "get_vod_categories")))

    suspend fun getVodStreams(session: XtreamSession, categoryId: String): List<VodStream> =
        parseArray(executeGet(actionUrl(session, "get_vod_streams") + "&category_id=${encode(categoryId)}"))

    /** Ficha extendida (sinopsis, reparto, etc.) de una película. No todos los paneles la completan; en ese caso llega con "info" vacío. */
    suspend fun getVodInfo(session: XtreamSession, vodId: Int): VodInfoResponse {
        val body = executeGet(actionUrl(session, "get_vod_info") + "&vod_id=$vodId")
        return try {
            gson.fromJson(body, VodInfoResponse::class.java) ?: VodInfoResponse()
        } catch (e: Exception) {
            VodInfoResponse()
        }
    }

    suspend fun getSeriesCategories(session: XtreamSession): List<Category> =
        parseArray(executeGet(actionUrl(session, "get_series_categories")))

    suspend fun getSeries(session: XtreamSession, categoryId: String): List<Series> =
        parseArray(executeGet(actionUrl(session, "get_series") + "&category_id=${encode(categoryId)}"))

    suspend fun getSeriesInfo(session: XtreamSession, seriesId: Int): SeriesInfoResponse {
        val body = executeGet(actionUrl(session, "get_series_info") + "&series_id=$seriesId")
        return parseSeriesInfo(body)
    }

    private inline fun <reified T> parseArray(json: String): List<T> {
        val trimmed = json.trim()
        // Algunos paneles Xtream devuelven `{}` en vez de `[]` para una lista vacía.
        if (!trimmed.startsWith("[")) {
            if (looksLikeExpiredSession(trimmed)) throw XtreamSessionExpiredException()
            return emptyList()
        }
        val type = TypeToken.getParameterized(List::class.java, T::class.java).type
        return try {
            gson.fromJson<List<T>>(trimmed, type) ?: emptyList()
        } catch (e: Exception) {
            throw XtreamException("El servidor respondió con un formato inesperado", e)
        }
    }

    /** Cuando la sesión ya no es válida, el panel suele devolver el mismo objeto de login con auth=0 en vez de la lista pedida. */
    private fun looksLikeExpiredSession(json: String): Boolean = try {
        val obj = JsonParser.parseString(json).asJsonObject
        obj.getAsJsonObject("user_info")?.get("auth")?.asInt == 0
    } catch (e: Exception) {
        false
    }

    private fun parseSeriesInfo(json: String): SeriesInfoResponse {
        val root = try {
            JsonParser.parseString(json).asJsonObject
        } catch (e: Exception) {
            throw XtreamException("No se pudo leer la información de la serie", e)
        }

        val info = root.getAsJsonObject("info")?.let {
            gson.fromJson(it, SeriesExtraInfo::class.java)
        }

        val seasonListType = TypeToken.getParameterized(List::class.java, SeasonInfo::class.java).type
        val seasons: List<SeasonInfo> = root.getAsJsonArray("seasons")
            ?.let { gson.fromJson<List<SeasonInfo>>(it, seasonListType) }
            ?: emptyList()

        val episodesBySeason = mutableMapOf<Int, List<Episode>>()
        val episodesElement = root.get("episodes")
        if (episodesElement != null && episodesElement.isJsonObject) {
            val episodeListType = TypeToken.getParameterized(List::class.java, Episode::class.java).type
            val episodesObject = episodesElement.asJsonObject
            for (seasonKey in episodesObject.keySet()) {
                val seasonNumber = seasonKey.toIntOrNull() ?: continue
                val episodesArray = episodesObject.getAsJsonArray(seasonKey) ?: continue
                val episodes: List<Episode>? = gson.fromJson(episodesArray, episodeListType)
                episodesBySeason[seasonNumber] = episodes ?: emptyList()
            }
        }

        // Si el panel no envía "seasons" explícitas, se derivan de las claves de "episodes".
        val effectiveSeasons = if (seasons.isNotEmpty()) {
            seasons
        } else {
            episodesBySeason.keys.sorted().map { SeasonInfo(seasonNumber = it, name = "Temporada $it") }
        }

        return SeriesInfoResponse(info = info, seasons = effectiveSeasons, episodesBySeason = episodesBySeason)
    }

    private fun actionUrl(session: XtreamSession, action: String): String =
        "${session.baseUrl}/player_api.php?username=${encode(session.username)}&password=${encode(session.password)}&action=$action"

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun executeGet(url: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).get().build()
        val call = httpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        XtreamException("No se pudo conectar con el servidor. Verifica la URL y tu conexión", e)
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                XtreamException("El servidor respondió con un error (código ${resp.code})")
                            )
                        }
                        return
                    }
                    val bodyString = resp.body?.string()
                    when {
                        bodyString == null && continuation.isActive ->
                            continuation.resumeWithException(XtreamException("El servidor no devolvió datos"))
                        bodyString != null && continuation.isActive ->
                            continuation.resume(bodyString)
                    }
                }
            }
        })
    }

    companion object {
        fun normalizeBaseUrl(input: String): String {
            var url = input.trim().trimEnd('/')
            if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                url = "http://$url"
            }
            return url
        }
    }
}
