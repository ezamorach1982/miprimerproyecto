package com.funtv.player

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import com.funtv.player.data.api.XtreamClient
import com.funtv.player.data.cache.CatalogCache
import com.funtv.player.data.prefs.PlaybackPositionManager
import com.funtv.player.data.prefs.SessionManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FunTvApplication : Application() {

    // Timeouts para llamadas al catálogo (player_api.php), no para video: el
    // reproductor usa su propio DataSource con sus propios tiempos. Más cortos
    // que antes para que un panel lento falle rápido y pueda reintentarse, en
    // vez de dejar al usuario mirando un spinner mudo por 15-20 segundos.
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val xtreamClient: XtreamClient by lazy { XtreamClient(okHttpClient) }

    val sessionManager: SessionManager by lazy { SessionManager(this) }

    val playbackPositionManager: PlaybackPositionManager by lazy { PlaybackPositionManager(this) }

    val catalogCache: CatalogCache by lazy { CatalogCache(this) }

    override fun onCreate() {
        super.onCreate()
        warmUpPreferencesInBackground()
        configureImageLoader()
    }

    /** Primer acceso a SharedPreferences (I/O de disco) fuera del hilo principal, para evitar un micro-tirón la primera vez que se usan. */
    private fun warmUpPreferencesInBackground() {
        Thread {
            sessionManager
            playbackPositionManager
        }.start()
    }

    /** Caché de imágenes en disco explícita (logos/pósters), para no re-descargar lo mismo cada vez que se recompone una fila. */
    private fun configureImageLoader() {
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
