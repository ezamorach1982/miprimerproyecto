package com.funtv.player.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.model.VodExtraInfo
import com.funtv.player.data.prefs.FavoriteType
import com.funtv.player.databinding.ActivityVodDetailsBinding
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.launch

/**
 * Ficha de una película antes de reproducirla: póster, sinopsis/reparto (si el
 * panel los expone vía get_vod_info) y un botón que dice "Reproducir" o
 * "Continuar viendo" según si ya había una posición guardada.
 */
class VodDetailsActivity : FragmentActivity() {

    private lateinit var binding: ActivityVodDetailsBinding

    private val streamId: Int by lazy { intent.getIntExtra(EXTRA_STREAM_ID, 0) }
    private val streamName: String by lazy { intent.getStringExtra(EXTRA_NAME).orEmpty() }
    private val streamIcon: String? by lazy { intent.getStringExtra(EXTRA_ICON) }
    private val containerExtension: String? by lazy { intent.getStringExtra(EXTRA_CONTAINER) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVodDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textVodTitle.text = streamName
        if (!streamIcon.isNullOrBlank()) {
            binding.imageCover.load(streamIcon)
        }

        binding.buttonPlay.setOnClickListener { playMovie() }
        updatePlayButtonLabel()
        loadExtraInfo()
    }

    override fun onResume() {
        super.onResume()
        updatePlayButtonLabel()
    }

    private fun app() = funTvApp()

    private fun playUrl(): String? {
        val session = app().sessionManager.getSession() ?: return null
        return StreamUrlBuilder.vodUrl(session, streamId, containerExtension)
    }

    private fun updatePlayButtonLabel() {
        val url = playUrl() ?: return
        val hasProgress = app().playbackPositionManager.getPosition(url) > 0
        binding.buttonPlay.text = getString(if (hasProgress) R.string.vod_continue else R.string.vod_play)
    }

    private fun playMovie() {
        val url = playUrl() ?: return
        startActivity(PlaybackActivity.newIntent(this, url, streamName, posterUrl = streamIcon, contentType = FavoriteType.VOD))
    }

    private fun loadExtraInfo() {
        val session = app().sessionManager.getSession() ?: return
        lifecycleScope.launch {
            try {
                val response = app().xtreamClient.getVodInfo(session, streamId)
                bindInfo(response.info)
            } catch (e: Exception) {
                // La ficha extendida es un plus; si falla, se queda con lo ya mostrado.
            }
        }
    }

    private fun bindInfo(info: VodExtraInfo?) {
        if (info == null) return
        binding.textVodPlot.text = info.plot.orEmpty()
        binding.textVodCast.text = info.cast?.takeIf { it.isNotBlank() }?.let { "Reparto: $it" }.orEmpty()

        val metaParts = listOfNotNull(
            info.releaseDate?.takeIf { it.isNotBlank() },
            info.genre?.takeIf { it.isNotBlank() },
            info.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
            info.duration?.takeIf { it.isNotBlank() }
        )
        binding.textVodMeta.text = metaParts.joinToString(" · ")

        val cover = info.movieImage
        if (!cover.isNullOrBlank()) {
            binding.imageCover.load(cover)
        }
    }

    companion object {
        private const val EXTRA_STREAM_ID = "extra_stream_id"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_ICON = "extra_icon"
        private const val EXTRA_CONTAINER = "extra_container"

        fun newIntent(
            context: Context,
            streamId: Int,
            name: String,
            iconUrl: String?,
            containerExtension: String?
        ): Intent =
            Intent(context, VodDetailsActivity::class.java)
                .putExtra(EXTRA_STREAM_ID, streamId)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_ICON, iconUrl)
                .putExtra(EXTRA_CONTAINER, containerExtension)
    }
}
