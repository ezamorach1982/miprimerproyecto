package com.funtv.player.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.funtv.player.R
import com.funtv.player.data.model.SeriesInfoResponse
import com.funtv.player.databinding.ActivitySeriesDetailsBinding
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.launch

class SeriesDetailsActivity : FragmentActivity() {

    private lateinit var binding: ActivitySeriesDetailsBinding
    private val seriesId: Int by lazy { intent.getIntExtra(EXTRA_SERIES_ID, 0) }
    private val seriesName: String by lazy { intent.getStringExtra(EXTRA_SERIES_NAME).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeriesDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textSeriesTitle.text = seriesName
        loadSeriesInfo()
    }

    private fun loadSeriesInfo() {
        val session = funTvApp().sessionManager.getSession() ?: return
        lifecycleScope.launch {
            try {
                val info = funTvApp().xtreamClient.getSeriesInfo(session, seriesId)
                bindInfo(info)
            } catch (e: Exception) {
                binding.textSeriesPlot.text = getString(R.string.details_loading_error)
            }
        }
    }

    private fun bindInfo(info: SeriesInfoResponse) {
        val extra = info.info
        if (!extra?.name.isNullOrBlank()) {
            binding.textSeriesTitle.text = extra?.name
        }
        binding.textSeriesPlot.text = extra?.plot.orEmpty()
        binding.textSeriesCast.text = extra?.cast?.takeIf { it.isNotBlank() }?.let { "Reparto: $it" }.orEmpty()

        val metaParts = listOfNotNull(
            extra?.releaseDate?.takeIf { it.isNotBlank() },
            extra?.genre?.takeIf { it.isNotBlank() },
            extra?.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" }
        )
        binding.textSeriesMeta.text = metaParts.joinToString(" · ")

        val cover = extra?.cover
        if (!cover.isNullOrBlank()) {
            binding.imageCover.load(cover)
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.seasonsFragmentContainer) as? SeasonsRowsFragment
        fragment?.submitSeriesInfo(info)
    }

    companion object {
        private const val EXTRA_SERIES_ID = "extra_series_id"
        private const val EXTRA_SERIES_NAME = "extra_series_name"

        fun newIntent(context: Context, seriesId: Int, seriesName: String): Intent =
            Intent(context, SeriesDetailsActivity::class.java)
                .putExtra(EXTRA_SERIES_ID, seriesId)
                .putExtra(EXTRA_SERIES_NAME, seriesName)
    }
}
