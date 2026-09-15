package com.funtv.player.ui.details

import android.os.Bundle
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.model.Episode
import com.funtv.player.data.model.SeriesInfoResponse
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp

/** Muestra una fila por temporada, con los episodios como tarjetas navegables por D-pad. */
class SeasonsRowsFragment : RowsSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val episodePresenter = EpisodePresenter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter(rowsAdapter)
        setOnItemViewClickedListener(ItemClickedListener())
    }

    fun submitSeriesInfo(info: SeriesInfoResponse) {
        rowsAdapter.clear()
        info.seasons.sortedBy { it.seasonNumber }.forEach { season ->
            val episodes = info.episodesBySeason[season.seasonNumber].orEmpty()
            if (episodes.isEmpty()) return@forEach
            val header = HeaderItem(season.name?.takeIf { it.isNotBlank() } ?: "Temporada ${season.seasonNumber}")
            val itemsAdapter = ArrayObjectAdapter(episodePresenter)
            itemsAdapter.addAll(0, episodes.sortedBy { it.episodeNum ?: 0 })
            rowsAdapter.add(ListRow(header, itemsAdapter))
        }
    }

    private inner class ItemClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            val episode = item as? Episode ?: return
            val session = requireContext().funTvApp().sessionManager.getSession() ?: return
            val url = StreamUrlBuilder.seriesEpisodeUrl(session, episode.id, episode.containerExtension)
            startActivity(PlaybackActivity.newIntent(requireContext(), url, episode.title.orEmpty()))
        }
    }
}
