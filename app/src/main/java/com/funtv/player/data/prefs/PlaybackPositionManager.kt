package com.funtv.player.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/** Una entrada de "continuar viendo": suficiente para retomar la reproducción y para dibujar su tarjeta sin volver a consultar el panel. */
data class ContinueWatchingEntry(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("positionMs") val positionMs: Long,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("updatedAt") val updatedAt: Long,
    /** Null en entradas guardadas antes de que existiera este campo; la insignia de tipo simplemente no se muestra en ese caso. */
    @SerializedName("type") val type: FavoriteType? = null
)

/**
 * Recuerda en qué punto quedó cada película/episodio para poder retomarlo
 * ("continuar viendo"). No aplica a TV en vivo.
 */
class PlaybackPositionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePosition(entry: ContinueWatchingEntry) {
        val all = readAll()
        all[entry.url] = entry
        writeAll(all)
    }

    fun getPosition(url: String): Long = readAll()[url]?.positionMs ?: 0L

    fun getProgress(url: String): Float {
        val entry = readAll()[url] ?: return 0f
        if (entry.durationMs <= 0) return 0f
        return (entry.positionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun clearPosition(url: String) {
        val all = readAll()
        all.remove(url)
        writeAll(all)
    }

    fun getContinueWatching(limit: Int = 20): List<ContinueWatchingEntry> =
        readAll().values.sortedByDescending { it.updatedAt }.take(limit)

    /** Se llama al cerrar sesión: las URLs guardadas apuntan al servidor/cuenta anterior. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun readAll(): MutableMap<String, ContinueWatchingEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, ContinueWatchingEntry>>() {}.type
            gson.fromJson<MutableMap<String, ContinueWatchingEntry>>(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun writeAll(map: Map<String, ContinueWatchingEntry>) {
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(map)).apply()
    }

    companion object {
        private const val PREFS_FILE = "funtv_playback_positions"
        private const val KEY_ENTRIES = "entries"

        /** Por debajo de esto no vale la pena ofrecer "continuar viendo". */
        const val MIN_RESUME_POSITION_MS = 10_000L

        /** A esta distancia del final, se considera "visto" y no se recuerda la posición. */
        const val END_THRESHOLD_MS = 20_000L
    }
}
