package com.funtv.player

import android.app.Application
import com.funtv.player.data.api.XtreamClient
import com.funtv.player.data.prefs.PlaybackPositionManager
import com.funtv.player.data.prefs.SessionManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FunTvApplication : Application() {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val xtreamClient: XtreamClient by lazy { XtreamClient(okHttpClient) }

    val sessionManager: SessionManager by lazy { SessionManager(this) }

    val playbackPositionManager: PlaybackPositionManager by lazy { PlaybackPositionManager(this) }
}
