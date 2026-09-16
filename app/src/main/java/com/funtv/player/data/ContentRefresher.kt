package com.funtv.player.data

import com.funtv.player.FunTvApplication
import com.funtv.player.data.cache.LiveCacheEntry
import com.funtv.player.data.cache.SeriesCacheEntry
import com.funtv.player.data.cache.VodCacheEntry
import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.VodStream
import com.funtv.player.data.model.XtreamSession
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Vuelve a descargar categorías + contenido de TV en vivo, películas y series desde el
 * panel y sobreescribe el caché local. Lo usan el botón "Actualizar" del inicio y el de
 * la pantalla de cuenta, para refrescar todo el catálogo de una vez sin tener que
 * entrar sección por sección ni categoría por categoría.
 */
object ContentRefresher {

    private const val MAX_CONCURRENT_REQUESTS = 5

    suspend fun refreshAll(app: FunTvApplication, session: XtreamSession): Boolean = withContext(Dispatchers.IO) {
        var liveOk = false
        var vodOk = false
        var seriesOk = false
        coroutineScope {
            launch { liveOk = refreshLive(app, session) }
            launch { vodOk = refreshVod(app, session) }
            launch { seriesOk = refreshSeries(app, session) }
        }
        liveOk || vodOk || seriesOk
    }

    /** Descarga categorías + contenido de TV en vivo y sobreescribe su caché. */
    suspend fun refreshLive(app: FunTvApplication, session: XtreamSession): Boolean = withContext(Dispatchers.IO) {
        refreshLiveInternal(app, session, Semaphore(MAX_CONCURRENT_REQUESTS))
    }

    /** Descarga categorías + contenido de películas y sobreescribe su caché. */
    suspend fun refreshVod(app: FunTvApplication, session: XtreamSession): Boolean = withContext(Dispatchers.IO) {
        refreshVodInternal(app, session, Semaphore(MAX_CONCURRENT_REQUESTS))
    }

    /** Descarga categorías + contenido de series y sobreescribe su caché. */
    suspend fun refreshSeries(app: FunTvApplication, session: XtreamSession): Boolean = withContext(Dispatchers.IO) {
        refreshSeriesInternal(app, session, Semaphore(MAX_CONCURRENT_REQUESTS))
    }

    private suspend fun refreshLiveInternal(app: FunTvApplication, session: XtreamSession, limiter: Semaphore): Boolean {
        val categories = runCatching { app.xtreamClient.getLiveCategories(session) }.getOrNull()
        if (categories.isNullOrEmpty()) return false
        val map = ConcurrentHashMap<String, List<LiveStream>>()
        coroutineScope {
            categories.forEach { category ->
                launch {
                    val items = limiter.withPermit {
                        runCatching { app.xtreamClient.getLiveStreams(session, category.categoryId) }.getOrNull()
                    }
                    if (!items.isNullOrEmpty()) map[category.categoryId] = items
                }
            }
        }
        if (map.isEmpty()) return false
        app.catalogCache.writeLive(LiveCacheEntry(categories, map, System.currentTimeMillis()))
        return true
    }

    private suspend fun refreshVodInternal(app: FunTvApplication, session: XtreamSession, limiter: Semaphore): Boolean {
        val categories = runCatching { app.xtreamClient.getVodCategories(session) }.getOrNull()
        if (categories.isNullOrEmpty()) return false
        val map = ConcurrentHashMap<String, List<VodStream>>()
        coroutineScope {
            categories.forEach { category ->
                launch {
                    val items = limiter.withPermit {
                        runCatching { app.xtreamClient.getVodStreams(session, category.categoryId) }.getOrNull()
                    }
                    if (!items.isNullOrEmpty()) map[category.categoryId] = items
                }
            }
        }
        if (map.isEmpty()) return false
        app.catalogCache.writeVod(VodCacheEntry(categories, map, System.currentTimeMillis()))
        return true
    }

    private suspend fun refreshSeriesInternal(app: FunTvApplication, session: XtreamSession, limiter: Semaphore): Boolean {
        val categories = runCatching { app.xtreamClient.getSeriesCategories(session) }.getOrNull()
        if (categories.isNullOrEmpty()) return false
        val map = ConcurrentHashMap<String, List<Series>>()
        coroutineScope {
            categories.forEach { category ->
                launch {
                    val items = limiter.withPermit {
                        runCatching { app.xtreamClient.getSeries(session, category.categoryId) }.getOrNull()
                    }
                    if (!items.isNullOrEmpty()) map[category.categoryId] = items
                }
            }
        }
        if (map.isEmpty()) return false
        app.catalogCache.writeSeries(SeriesCacheEntry(categories, map, System.currentTimeMillis()))
        return true
    }
}
