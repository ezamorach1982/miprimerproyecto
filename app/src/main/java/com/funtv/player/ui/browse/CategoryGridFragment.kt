package com.funtv.player.ui.browse

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.details.SeriesDetailsActivity
import com.funtv.player.ui.main.CardPresenter
import com.funtv.player.ui.main.HomeCardItem
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Muestra TODO el contenido de una categoría en una cuadrícula vertical (varias
 * columnas x muchas filas, con scroll hacia abajo), en vez de una única fila
 * horizontal donde solo caben 4-5 tarjetas visibles a la vez.
 */
class CategoryGridFragment : VerticalGridSupportFragment() {

    private val itemsAdapter = ArrayObjectAdapter(CardPresenter())

    private val contentType: ContentType by lazy {
        ContentType.valueOf(requireArguments().getString(ARG_CONTENT_TYPE)!!)
    }
    private val categoryId: String by lazy { requireArguments().getString(ARG_CATEGORY_ID)!! }
    private val categoryName: String by lazy { requireArguments().getString(ARG_CATEGORY_NAME).orEmpty() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = categoryName
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.funtv_background))

        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = GRID_COLUMNS
        setGridPresenter(gridPresenter)

        adapter = itemsAdapter
        onItemViewClickedListener = ItemViewClickedListener()

        loadItems()
    }

    private fun app() = requireContext().funTvApp()
    private fun session(): XtreamSession? = app().sessionManager.getSession()

    private fun loadItems() {
        val session = session() ?: return

        val cached = readCachedItems()
        if (!cached.isNullOrEmpty()) {
            itemsAdapter.addAll(0, cached)
        } else {
            progressBarManager.show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val fresh = try {
                withContext(Dispatchers.IO) { fetchItems(session) }
            } catch (e: Exception) {
                null
            }
            progressBarManager.hide()
            if (!fresh.isNullOrEmpty()) {
                itemsAdapter.clear()
                itemsAdapter.addAll(0, fresh)
            }
        }
    }

    private suspend fun fetchItems(session: XtreamSession): List<HomeCardItem> = when (contentType) {
        ContentType.LIVE -> app().xtreamClient.getLiveStreams(session, categoryId).map { HomeCardItem.Live(it) }
        ContentType.VOD -> app().xtreamClient.getVodStreams(session, categoryId).map { HomeCardItem.Vod(it) }
        ContentType.SERIES -> app().xtreamClient.getSeries(session, categoryId).map { HomeCardItem.SeriesItem(it) }
    }

    private fun readCachedItems(): List<HomeCardItem>? {
        val cache = app().catalogCache
        return when (contentType) {
            ContentType.LIVE -> cache.readLive()?.streamsByCategory?.get(categoryId)?.map { HomeCardItem.Live(it) }
            ContentType.VOD -> cache.readVod()?.streamsByCategory?.get(categoryId)?.map { HomeCardItem.Vod(it) }
            ContentType.SERIES -> cache.readSeries()?.seriesByCategory?.get(categoryId)?.map { HomeCardItem.SeriesItem(it) }
        }
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row?
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
                is HomeCardItem.Retry -> Unit
            }
        }
    }

    companion object {
        private const val GRID_COLUMNS = 5
        private const val ARG_CONTENT_TYPE = "arg_content_type"
        private const val ARG_CATEGORY_ID = "arg_category_id"
        private const val ARG_CATEGORY_NAME = "arg_category_name"

        fun newInstance(contentType: ContentType, categoryId: String, categoryName: String): CategoryGridFragment {
            val fragment = CategoryGridFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_CONTENT_TYPE, contentType.name)
                putString(ARG_CATEGORY_ID, categoryId)
                putString(ARG_CATEGORY_NAME, categoryName)
            }
            return fragment
        }
    }
}
