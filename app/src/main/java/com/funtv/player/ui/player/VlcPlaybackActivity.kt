package com.funtv.player.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.funtv.player.R
import com.funtv.player.databinding.ActivityVlcPlaybackBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * Reproductor alternativo con el motor de VLC (decodifica por software casi
 * cualquier formato/códec de audio), para el contenido IPTV que ExoPlayer muestra
 * sin sonido por no tener un decodificador de hardware compatible en el
 * dispositivo. Es una pantalla aparte e independiente del reproductor principal
 * (PlaybackActivity): si algo falla acá, no afecta la reproducción normal.
 *
 * No reimplementa zapping, "continuar viendo" ni siguiente episodio automático:
 * es solo un plan B puntual para cuando el reproductor principal se queda mudo.
 */
class VlcPlaybackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVlcPlaybackBinding
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVlcPlaybackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.buttonBack.setOnClickListener { finish() }

        if (url.isBlank()) {
            showError()
            return
        }

        try {
            startPlayback(url)
        } catch (e: Exception) {
            showError()
        }
    }

    private fun startPlayback(url: String) {
        val vlc = LibVLC(this, arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--network-caching=3000"))
        val player = MediaPlayer(vlc)
        player.attachViews(binding.vlcVideoLayout, null, false, false)

        val media = Media(vlc, Uri.parse(url))
        player.media = media
        media.release()

        player.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.EncounteredError -> runOnUiThread { showError() }
                MediaPlayer.Event.Playing -> runOnUiThread { binding.progressBuffering.visibility = View.GONE }
                MediaPlayer.Event.Buffering -> runOnUiThread {
                    binding.progressBuffering.visibility = if (event.buffering < 100f) View.VISIBLE else View.GONE
                }
                else -> Unit
            }
        }

        player.play()
        libVLC = vlc
        mediaPlayer = player
    }

    private fun showError() {
        if (isFinishing || isDestroyed) return
        Toast.makeText(this, R.string.player_error_generic, Toast.LENGTH_LONG).show()
        binding.layoutVlcError.visibility = View.VISIBLE
        binding.progressBuffering.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
        mediaPlayer = null
        libVLC = null
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        fun newIntent(context: Context, url: String, title: String): Intent =
            Intent(context, VlcPlaybackActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }
}
