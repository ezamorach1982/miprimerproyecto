package com.funtv.player.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.prefs.ContinueWatchingEntry
import com.funtv.player.data.prefs.PlaybackPositionManager
import com.funtv.player.databinding.ActivityPlaybackBinding
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reproduce streams en vivo (HLS), VOD y episodios de serie (HLS o MP4 progresivo)
 * con ExoPlayer (Media3).
 *
 * Resiliencia en dos capas:
 *  1. ExoPlayer reintenta internamente la carga de cada segmento varias veces
 *     (DefaultLoadErrorHandlingPolicy) antes de considerar el error fatal.
 *  2. Si el error sí es fatal, esta Activity reintenta reutilizando la MISMA
 *     instancia de ExoPlayer en vez de destruirla y crear una nueva.
 *
 * Para películas y episodios recuerda la posición de reproducción ("continuar
 * viendo"). Al terminar un episodio, si se conoce el siguiente, lo reproduce
 * automáticamente tras un aviso breve. En TV en vivo, DPAD arriba/abajo cambia
 * de canal dentro de la misma categoría sin salir del reproductor.
 */
class PlaybackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaybackBinding
    private var player: ExoPlayer? = null
    private var retryJob: Job? = null
    private var positionSaveJob: Job? = null
    private var nextEpisodeJob: Job? = null
    private var retryCount = 0

    /** Punto desde el que debe arrancar la próxima vez que se prepare: al abrir, la posición guardada; tras un corte, donde quedó. */
    private var startPositionMs: Long = 0L

    // Lo que se está reproduciendo AHORA: cambia al hacer zapping de canal, a
    // diferencia de los extras originales del Intent (que quedan fijos).
    private var currentUrl: String = ""
    private var currentTitle: String = ""
    private var currentPoster: String? = null

    private val isLive: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_LIVE, false) }
    private val nextUrl: String? by lazy { intent.getStringExtra(EXTRA_NEXT_URL) }
    private val nextTitle: String by lazy { intent.getStringExtra(EXTRA_NEXT_TITLE).orEmpty() }
    private val nextPoster: String? by lazy { intent.getStringExtra(EXTRA_NEXT_POSTER) }
    private val positionManager: PlaybackPositionManager by lazy { funTvApp().playbackPositionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        currentTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        currentPoster = intent.getStringExtra(EXTRA_POSTER)

        binding.buttonManualRetry.setOnClickListener {
            retryCount = 0
            hideError()
            retryOrStart()
        }
        binding.buttonBack.setOnClickListener { finish() }

        title = currentTitle

        if (!isLive) {
            val saved = positionManager.getPosition(currentUrl)
            if (saved >= PlaybackPositionManager.MIN_RESUME_POSITION_MS) {
                startPositionMs = saved
            }
        }

        if (currentUrl.isBlank()) {
            showError(getString(R.string.player_error_generic))
        } else {
            buildPlayer()
        }
    }

    /** Solo se llama para la primera reproducción o tras un zapping: crea la instancia de ExoPlayer. */
    private fun buildPlayer() {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("FunTV/1.0 (Linux;Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val loadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy(SEGMENT_RETRY_COUNT)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.addListener(playerListener)

        binding.playerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(currentUrl))
        if (startPositionMs > 0) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        player = exoPlayer
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            binding.progressBuffering.visibility =
                if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            when (playbackState) {
                Player.STATE_READY -> {
                    retryCount = 0
                    startPositionSaveLoop()
                }
                Player.STATE_ENDED -> {
                    if (!isLive) {
                        positionManager.clearPosition(currentUrl)
                        maybeAutoPlayNext()
                    }
                }
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            startPositionMs = player?.currentPosition ?: startPositionMs
            handlePlaybackError()
        }
    }

    /** Reintento (automático o manual): reusa la instancia existente si la hay, en vez de recrearla. */
    private fun retryOrStart() {
        val existingPlayer = player
        if (existingPlayer == null) {
            buildPlayer()
            return
        }
        if (startPositionMs > 0) {
            existingPlayer.seekTo(startPositionMs)
        }
        existingPlayer.playWhenReady = true
        existingPlayer.prepare()
    }

    private fun handlePlaybackError() {
        if (retryCount >= MAX_RETRIES) {
            showError(getString(R.string.player_retry_exhausted))
            return
        }
        retryCount++
        showError(getString(R.string.player_retrying, retryCount, MAX_RETRIES))
        retryJob?.cancel()
        retryJob = lifecycleScope.launch {
            delay(RETRY_DELAY_MS * retryCount)
            hideError()
            retryOrStart()
        }
    }

    private fun startPositionSaveLoop() {
        if (isLive) return
        positionSaveJob?.cancel()
        positionSaveJob = lifecycleScope.launch {
            while (true) {
                delay(POSITION_SAVE_INTERVAL_MS)
                persistPositionIfNeeded()
            }
        }
    }

    private fun persistPositionIfNeeded() {
        if (isLive) return
        val current = player ?: return
        val position = current.currentPosition
        val duration = current.duration
        if (duration > 0 && position >= duration - PlaybackPositionManager.END_THRESHOLD_MS) {
            positionManager.clearPosition(currentUrl)
        } else if (position >= PlaybackPositionManager.MIN_RESUME_POSITION_MS && duration > 0) {
            positionManager.savePosition(
                ContinueWatchingEntry(
                    url = currentUrl,
                    title = currentTitle,
                    posterUrl = currentPoster,
                    positionMs = position,
                    durationMs = duration,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Al terminar un episodio, si se conoce el siguiente, avisa y lo reproduce solo tras una pausa breve. */
    private fun maybeAutoPlayNext() {
        val url = nextUrl ?: return
        Toast.makeText(this, getString(R.string.player_next_episode, nextTitle), Toast.LENGTH_LONG).show()
        nextEpisodeJob?.cancel()
        nextEpisodeJob = lifecycleScope.launch {
            delay(AUTO_NEXT_DELAY_MS)
            startActivity(newIntent(this@PlaybackActivity, url, nextTitle, posterUrl = nextPoster))
            finish()
        }
    }

    /** Cambia de canal dentro de la misma categoría sin salir de esta pantalla (DPAD arriba/abajo). */
    private fun zapTo(newIndex: Int) {
        val app = funTvApp()
        val list = app.liveZapList
        if (list.isEmpty()) return
        val safeIndex = ((newIndex % list.size) + list.size) % list.size
        val stream = list[safeIndex]
        val session = app.sessionManager.getSession() ?: return

        app.liveZapIndex = safeIndex
        currentUrl = StreamUrlBuilder.liveUrl(session, stream.streamId)
        currentTitle = stream.name.orEmpty()
        currentPoster = stream.streamIcon
        title = currentTitle
        Toast.makeText(this, currentTitle, Toast.LENGTH_SHORT).show()

        retryCount = 0
        retryJob?.cancel()
        hideError()

        val existingPlayer = player
        if (existingPlayer == null) {
            buildPlayer()
            return
        }
        existingPlayer.setMediaItem(MediaItem.fromUri(currentUrl))
        existingPlayer.playWhenReady = true
        existingPlayer.prepare()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isLive && funTvApp().liveZapList.isNotEmpty()) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    zapTo(funTvApp().liveZapIndex - 1)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    zapTo(funTvApp().liveZapIndex + 1)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showError(message: String) {
        binding.textPlaybackError.text = message
        binding.layoutError.visibility = View.VISIBLE
        binding.progressBuffering.visibility = View.GONE
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun releasePlayer() {
        retryJob?.cancel()
        positionSaveJob?.cancel()
        nextEpisodeJob?.cancel()
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        persistPositionIfNeeded()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_POSTER = "extra_poster"
        private const val EXTRA_IS_LIVE = "extra_is_live"
        private const val EXTRA_NEXT_URL = "extra_next_url"
        private const val EXTRA_NEXT_TITLE = "extra_next_title"
        private const val EXTRA_NEXT_POSTER = "extra_next_poster"
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 2000L
        private const val POSITION_SAVE_INTERVAL_MS = 5000L
        private const val SEGMENT_RETRY_COUNT = 6
        private const val AUTO_NEXT_DELAY_MS = 4000L

        fun newIntent(
            context: Context,
            url: String,
            title: String,
            isLive: Boolean = false,
            posterUrl: String? = null,
            nextUrl: String? = null,
            nextTitle: String? = null,
            nextPosterUrl: String? = null
        ): Intent =
            Intent(context, PlaybackActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_IS_LIVE, isLive)
                .putExtra(EXTRA_POSTER, posterUrl)
                .putExtra(EXTRA_NEXT_URL, nextUrl)
                .putExtra(EXTRA_NEXT_TITLE, nextTitle)
                .putExtra(EXTRA_NEXT_POSTER, nextPosterUrl)
    }
}
