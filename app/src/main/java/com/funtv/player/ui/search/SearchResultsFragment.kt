package com.funtv.player.ui.search

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.ui.browse.ContentType
import com.funtv.player.ui.details.SeriesDetailsActivity
import com.funtv.player.ui.details.VodDetailsActivity
import com.funtv.player.ui.main.CardPresenter
import com.funtv.player.ui.main.HomeCardItem
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp

/**
 * Resultados de búsqueda: filtra por nombre sobre el catálogo ya guardado en
 * caché localmente (TV en vivo, películas y series juntos). No hay búsqueda del
 * lado del servidor en Xtream Codes, así que solo encuentra lo que ya se cargó
 * al menos una vez visitando cada sección.
 */
class SearchResultsFragment : VerticalGridSupportFragment() {

    private val itemsAdapter = ArrayObjectAdapter(CardPresenter())
    private var lastQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Igual que en CategoryGridFragment: el gridPresenter debe existir ANTES de
        // onCreateView (que corre antes de onViewCreated), o la fragment revienta con
        // NullPointerException al construir su propia vista.
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = GRID_COLUMNS
        setGridPresenter(gridPresenter)

        adapter = itemsAdapter
        onItemViewClickedListener = ItemViewClickedListener()
    }

    fun submitQuery(query: String) {
        lastQuery = query
        val results = searchCatalog(query)
        itemsAdapter.clear()
        itemsAdapter.addAll(0, results)
        if (results.isEmpty() && query.isNotBlank() && isAdded) {
            Toast.makeText(requireContext(), R.string.search_no_results, Toast.LENGTH_LONG).show()
        }
    }

    /** Si la búsqueda se abrió desde una sección (TV/Películas/Series), limita los resultados a esa sección. */
    private fun contentTypeFilter(): ContentType? =
        requireActivity().intent.getStringExtra(SearchActivity.EXTRA_CONTENT_TYPE_FILTER)?.let { ContentType.valueOf(it) }

    private fun searchCatalog(query: String): List<HomeCardItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val cache = requireContext().funTvApp().catalogCache
        val filter = contentTypeFilter()

        val liveResults = if (filter == null || filter == ContentType.LIVE) {
            cache.readLive()?.streamsByCategory?.values.orEmpty()
                .flatten()
                .filter { it.name?.lowercase()?.contains(q) == true }
                .map { HomeCardItem.Live(it) }
        } else emptyList()

        val vodResults = if (filter == null || filter == ContentType.VOD) {
            cache.readVod()?.streamsByCategory?.values.orEmpty()
                .flatten()
                .filter { it.name?.lowercase()?.contains(q) == true }
                .map { HomeCardItem.Vod(it) }
        } else emptyList()

        val seriesResults = if (filter == null || filter == ContentType.SERIES) {
            cache.readSeries()?.seriesByCategory?.values.orEmpty()
                .flatten()
                .filter { it.name?.lowercase()?.contains(q) == true }
                .map { HomeCardItem.SeriesItem(it) }
        } else emptyList()

        return liveResults + vodResults + seriesResults
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row?
        ) {
            val session = requireContext().funTvApp().sessionManager.getSession() ?: return
            when (item) {
                is HomeCardItem.Live -> {
                    val liveResults = itemsAdapterLiveStreams()
                    requireContext().funTvApp().liveZapList = liveResults
                    requireContext().funTvApp().liveZapIndex = liveResults.indexOfFirst { it.streamId == item.stream.streamId }
                    val url = StreamUrlBuilder.liveUrl(session, item.stream.streamId)
                    startActivity(
                        PlaybackActivity.newIntent(requireContext(), url, item.stream.name.orEmpty(), isLive = true)
                    )
                }
                is HomeCardItem.Vod -> {
                    startActivity(
                        VodDetailsActivity.newIntent(
                            requireContext(),
                            item.stream.streamId,
                            item.stream.name.orEmpty(),
                            item.stream.streamIcon,
                            item.stream.containerExtension
                        )
                    )
                }
                is HomeCardItem.SeriesItem -> {
                    startActivity(
                        SeriesDetailsActivity.newIntent(requireContext(), item.series.seriesId, item.series.name.orEmpty())
                    )
                }
                else -> Unit
            }
        }
    }

    private fun itemsAdapterLiveStreams() = (0 until itemsAdapter.size()).mapNotNull { index ->
        (itemsAdapter.get(index) as? HomeCardItem.Live)?.stream
    }

    companion object {
        private const val GRID_COLUMNS = 5
    }
}
