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
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp

/** Muestra una fila por temporada, con los episodios como tarjetas navegables por D-pad. */
class SeasonsRowsFragment : RowsSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val episodePresenter = EpisodePresenter()
    private var seriesInfo: SeriesInfoResponse? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter(rowsAdapter)
        setOnItemViewClickedListener(ItemClickedListener())
    }

    fun submitSeriesInfo(info: SeriesInfoResponse) {
        seriesInfo = info
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

    /** Busca el episodio que sigue al dado: el siguiente en la misma temporada o, si es el último, el primero de la siguiente temporada con episodios. */
    private fun findNextEpisode(current: Episode): Episode? {
        val info = seriesInfo ?: return null
        val season = current.season ?: 0
        val sameSeasonEpisodes = info.episodesBySeason[season]?.sortedBy { it.episodeNum ?: 0 } ?: return null
        val currentIndex = sameSeasonEpisodes.indexOfFirst { it.id == current.id }
        if (currentIndex == -1) return null
        if (currentIndex + 1 < sameSeasonEpisodes.size) return sameSeasonEpisodes[currentIndex + 1]

        val nextSeasonNumber = info.episodesBySeason.keys.filter { it > season }.minOrNull() ?: return null
        return info.episodesBySeason[nextSeasonNumber]?.sortedBy { it.episodeNum ?: 0 }?.firstOrNull()
    }

    private fun episodeUrl(session: XtreamSession, episode: Episode): String =
        StreamUrlBuilder.seriesEpisodeUrl(session, episode.id, episode.containerExtension)

    private inner class ItemClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            val episode = item as? Episode ?: return
            val session = requireContext().funTvApp().sessionManager.getSession() ?: return
            val next = findNextEpisode(episode)
            startActivity(
                PlaybackActivity.newIntent(
                    requireContext(),
                    episodeUrl(session, episode),
                    episode.title.orEmpty(),
                    posterUrl = episode.info?.movieImage,
                    nextUrl = next?.let { episodeUrl(session, it) },
                    nextTitle = next?.title,
                    nextPosterUrl = next?.info?.movieImage
                )
            )
        }
    }
}
