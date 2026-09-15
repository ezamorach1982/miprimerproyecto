package com.funtv.player.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.funtv.player.R
import com.funtv.player.databinding.ActivityPlaybackBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reproduce streams en vivo (HLS), VOD y episodios de serie (HLS o MP4 progresivo)
 * con ExoPlayer (Media3). Ante un error de conexión, reintenta automáticamente con
 * espera creciente antes de mostrar el error definitivo con opción de reintento manual.
 */
class PlaybackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaybackBinding
    private var player: ExoPlayer? = null
    private var retryJob: Job? = null
    private var retryCount = 0

    private val streamUrl: String by lazy { intent.getStringExtra(EXTRA_URL).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonManualRetry.setOnClickListener {
            retryCount = 0
            hideError()
            preparePlayer()
        }
        binding.buttonBack.setOnClickListener { finish() }

        title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        preparePlayer()
    }

    private fun preparePlayer() {
        if (streamUrl.isBlank()) {
            showError(getString(R.string.player_error_generic))
            return
        }
        releasePlayer()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("FunTV/1.0 (Linux;Android)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                binding.progressBuffering.visibility =
                    if (playbackState == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                if (playbackState == Player.STATE_READY) {
                    retryCount = 0
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError()
            }
        })

        binding.playerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        player = exoPlayer
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
            preparePlayer()
        }
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
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        player?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 2000L

        fun newIntent(context: Context, url: String, title: String): Intent =
            Intent(context, PlaybackActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }
}
