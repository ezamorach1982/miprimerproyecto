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

    /**
     * Todos los episodios que siguen al dado, en orden: el resto de la misma
     * temporada y luego las temporadas siguientes. Se pasa completa (no solo el
     * siguiente) para que el autoplay de PlaybackActivity pueda encadenar varios
     * episodios seguidos, no solo uno.
     */
    private fun remainingEpisodes(from: Episode): List<Episode> {
        val info = seriesInfo ?: return emptyList()
        val season = from.season ?: 0
        val sameSeasonEpisodes = info.episodesBySeason[season]?.sortedBy { it.episodeNum ?: 0 } ?: return emptyList()
        val currentIndex = sameSeasonEpisodes.indexOfFirst { it.id == from.id }
        if (currentIndex == -1) return emptyList()

        val result = mutableListOf<Episode>()
        result += sameSeasonEpisodes.drop(currentIndex + 1)
        info.episodesBySeason.keys.filter { it > season }.sorted().forEach { laterSeason ->
            result += info.episodesBySeason[laterSeason]?.sortedBy { it.episodeNum ?: 0 }.orEmpty()
        }
        return result
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
            val queue = remainingEpisodes(episode).map { ep ->
                PlaybackActivity.UpNextItem(episodeUrl(session, ep), ep.title.orEmpty(), ep.info?.movieImage)
            }
            startActivity(
                PlaybackActivity.newIntent(
                    requireContext(),
                    episodeUrl(session, episode),
                    episode.title.orEmpty(),
                    posterUrl = episode.info?.movieImage,
                    upNextQueue = queue
                )
            )
        }
    }
}
