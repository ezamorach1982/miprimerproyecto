package com.funtv.player.data.cache

import android.content.Context
import com.funtv.player.data.model.Category
import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.VodStream
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type

data class LiveCacheEntry(
    val categories: List<Category>,
    val streamsByCategory: Map<String, List<LiveStream>>
)

data class VodCacheEntry(
    val categories: List<Category>,
    val streamsByCategory: Map<String, List<VodStream>>
)

data class SeriesCacheEntry(
    val categories: List<Category>,
    val seriesByCategory: Map<String, List<Series>>
)

/**
 * Caché en disco (JSON plano, sin base de datos) del catálogo por sección.
 *
 * Permite mostrar contenido al instante al reabrir una sección (patrón
 * "stale-while-revalidate": se muestra lo cacheado de inmediato mientras se
 * refresca en segundo plano), en vez de depender de que el panel IPTV
 * responda rápido cada vez que el usuario navega.
 */
class CatalogCache(context: Context) {

    private val dir = File(context.filesDir, "catalog_cache").apply { mkdirs() }
    private val gson = Gson()

    fun readLive(): LiveCacheEntry? = read("live", object : TypeToken<LiveCacheEntry>() {}.type)
    fun writeLive(entry: LiveCacheEntry) = write("live", entry)

    fun readVod(): VodCacheEntry? = read("vod", object : TypeToken<VodCacheEntry>() {}.type)
    fun writeVod(entry: VodCacheEntry) = write("vod", entry)

    fun readSeries(): SeriesCacheEntry? = read("series", object : TypeToken<SeriesCacheEntry>() {}.type)
    fun writeSeries(entry: SeriesCacheEntry) = write("series", entry)

    /** Se llama al cerrar sesión: el catálogo de una cuenta no debe filtrarse a la siguiente. */
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun <T> read(key: String, type: Type): T? {
        val file = File(dir, "$key.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            null
        }
    }

    private fun write(key: String, data: Any) {
        val file = File(dir, "$key.json")
        try {
            file.writeText(gson.toJson(data))
        } catch (e: Exception) {
            // El caché es una optimización de rendimiento, no algo crítico: si falla, se ignora.
        }
    }
}
