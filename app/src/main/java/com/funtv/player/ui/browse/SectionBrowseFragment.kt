package com.funtv.player.ui.browse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.model.Category
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.details.SeriesDetailsActivity
import com.funtv.player.ui.main.CardPresenter
import com.funtv.player.ui.main.HomeCardItem
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Muestra las categorías y el contenido de UNA sola sección (TV en Vivo, Películas
 * o Series), como filas de Leanback. Evita mezclar los tres tipos en una sola
 * pantalla y, de paso, solo pide al panel los datos de la sección que el usuario
 * realmente abrió (antes se cargaba todo de una vez al entrar al Home).
 */
class SectionBrowseFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val cardPresenter = CardPresenter()
    private val concurrencyLimiter = Semaphore(5)

    private val contentType: ContentType by lazy {
        ContentType.valueOf(requireArguments().getString(ARG_CONTENT_TYPE)!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTitle(getString(sectionTitleRes()))
        setHeadersState(HEADERS_ENABLED)
        setHeadersTransitionOnBackEnabled(true)
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.funtv_background))

        setAdapter(rowsAdapter)
        onItemViewClickedListener = ItemViewClickedListener()

        loadContent()
    }

    private fun app() = requireContext().funTvApp()

    private fun session(): XtreamSession? = app().sessionManager.getSession()

    private fun sectionTitleRes() = when (contentType) {
        ContentType.LIVE -> R.string.header_live
        ContentType.VOD -> R.string.header_movies
        ContentType.SERIES -> R.string.header_series
    }

    private fun loadContent() {
        val session = session() ?: return
        progressBarManager.show()
        rowsAdapter.clear()

        viewLifecycleOwner.lifecycleScope.launch {
            val rows = try {
                val categories = fetchCategories(session)
                buildRowsForCategories(session, categories)
            } catch (e: Exception) {
                null
            }

            progressBarManager.hide()

            if (rows.isNullOrEmpty()) {
                showLoadErrorRow()
            } else {
                rows.forEach { rowsAdapter.add(it) }
            }
        }
    }

    private suspend fun fetchCategories(session: XtreamSession): List<Category> = when (contentType) {
        ContentType.LIVE -> app().xtreamClient.getLiveCategories(session)
        ContentType.VOD -> app().xtreamClient.getVodCategories(session)
        ContentType.SERIES -> app().xtreamClient.getSeriesCategories(session)
    }

    private suspend fun fetchItemsForCategory(session: XtreamSession, category: Category): List<HomeCardItem> =
        when (contentType) {
            ContentType.LIVE -> app().xtreamClient.getLiveStreams(session, category.categoryId)
                .map { HomeCardItem.Live(it) }
            ContentType.VOD -> app().xtreamClient.getVodStreams(session, category.categoryId)
                .map { HomeCardItem.Vod(it) }
            ContentType.SERIES -> app().xtreamClient.getSeries(session, category.categoryId)
                .map { HomeCardItem.SeriesItem(it) }
        }

    private suspend fun buildRowsForCategories(
        session: XtreamSession,
        categories: List<Category>
    ): List<ListRow> = coroutineScope {
        categories
            .map { category ->
                async(Dispatchers.IO) {
                    val items = concurrencyLimiter.withPermit {
                        try {
                            fetchItemsForCategory(session, category)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                    category to items
                }
            }
            .awaitAll()
            .filter { it.second.isNotEmpty() }
            .map { (category, items) ->
                val header = HeaderItem(category.categoryName)
                val itemsAdapter = ArrayObjectAdapter(cardPresenter)
                itemsAdapter.addAll(0, items)
                ListRow(header, itemsAdapter)
            }
    }

    private fun showLoadErrorRow() {
        rowsAdapter.clear()
        val header = HeaderItem(getString(sectionTitleRes()))
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        itemsAdapter.add(HomeCardItem.Retry(getString(R.string.home_error_loading)))
        rowsAdapter.add(ListRow(header, itemsAdapter))
        if (isAdded) {
            Toast.makeText(requireContext(), getString(R.string.home_error_loading), Toast.LENGTH_LONG).show()
        }
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            val session = session() ?: return
            when (item) {
                is HomeCardItem.Live -> {
                    val url = StreamUrlBuilder.liveUrl(session, item.stream.streamId)
                    startActivity(
                        PlaybackActivity.newIntent(requireContext(), url, item.stream.name.orEmpty(), isLive = true)
                    )
                }
                is HomeCardItem.Vod -> {
                    val url = StreamUrlBuilder.vodUrl(session, item.stream.streamId, item.stream.containerExtension)
                    startActivity(PlaybackActivity.newIntent(requireContext(), url, item.stream.name.orEmpty()))
                }
                is HomeCardItem.SeriesItem -> {
                    startActivity(
                        SeriesDetailsActivity.newIntent(requireContext(), item.series.seriesId, item.series.name.orEmpty())
                    )
                }
                is HomeCardItem.Retry -> loadContent()
            }
        }
    }

    companion object {
        private const val ARG_CONTENT_TYPE = "arg_content_type"

        fun newInstance(contentType: ContentType): SectionBrowseFragment {
            val fragment = SectionBrowseFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CONTENT_TYPE, contentType.name)
            }
            return fragment
        }
    }
}
