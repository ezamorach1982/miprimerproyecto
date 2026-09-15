package com.funtv.player.data.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

/** Un favorito: lo suficiente para dibujar su tarjeta y navegar a su ficha/reproducción sin volver a consultar el panel. */
data class FavoriteEntry(
    @SerializedName("key") val key: String,
    @SerializedName("type") val type: FavoriteType,
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("posterUrl") val posterUrl: String? = null,
    @SerializedName("containerExtension") val containerExtension: String? = null,
    @SerializedName("addedAt") val addedAt: Long = System.currentTimeMillis()
)

enum class FavoriteType { LIVE, VOD, SERIES }

/** Marcar canales/películas/series como favoritos (mantener presionado sobre una tarjeta). */
class FavoritesManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isFavorite(key: String): Boolean = readAll().containsKey(key)

    /** Alterna el estado y devuelve si quedó marcado como favorito. */
    fun toggle(entry: FavoriteEntry): Boolean {
        val all = readAll()
        return if (all.containsKey(entry.key)) {
            all.remove(entry.key)
            writeAll(all)
            false
        } else {
            all[entry.key] = entry
            writeAll(all)
            true
        }
    }

    fun getFavorites(): List<FavoriteEntry> =
        readAll().values.sortedByDescending { it.addedAt }

    /** Se llama al cerrar sesión: los IDs son específicos de cada proveedor Xtream, no deben sobrevivir a un cambio de cuenta. */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun readAll(): MutableMap<String, FavoriteEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, FavoriteEntry>>() {}.type
            gson.fromJson<MutableMap<String, FavoriteEntry>>(json, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun writeAll(map: Map<String, FavoriteEntry>) {
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(map)).apply()
    }

    companion object {
        private const val PREFS_FILE = "funtv_favorites"
        private const val KEY_ENTRIES = "entries"

        fun keyFor(type: FavoriteType, id: Int): String = "${type.name}:$id"
    }
}
