package com.funtv.player.ui.browse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import com.funtv.player.data.api.XtreamSessionExpiredException
import com.funtv.player.data.cache.LiveCacheEntry
import com.funtv.player.data.cache.SeriesCacheEntry
import com.funtv.player.data.cache.VodCacheEntry
import com.funtv.player.data.model.Category
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.details.SeriesDetailsActivity
import com.funtv.player.ui.details.VodDetailsActivity
import com.funtv.player.ui.login.LoginActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VerticalGridSupportFragment crea la vista de la cuadrícula dentro de su propio
        // onCreateView() usando el gridPresenter ya asignado; si se asigna después (p. ej.
        // en onViewCreated, que corre luego de onCreateView) el presenter todavía es null
        // ahí y la fragment revienta con NullPointerException al abrir la pantalla.
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = GRID_COLUMNS
        setGridPresenter(gridPresenter)

        adapter = itemsAdapter
        onItemViewClickedListener = ItemViewClickedListener()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = categoryName
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
            } catch (e: XtreamSessionExpiredException) {
                progressBarManager.hide()
                goToLoginDueToExpiredSession()
                return@launch
            } catch (e: Exception) {
                null
            }
            progressBarManager.hide()
            if (!fresh.isNullOrEmpty()) {
                itemsAdapter.clear()
                itemsAdapter.addAll(0, fresh)
                withContext(Dispatchers.IO) { writeCacheForThisCategory(fresh) }
            }
        }
    }

    /** Guarda lo recién descargado en el mismo caché que usan las filas horizontales, para que quede disponible la próxima vez sin depender de haber pasado antes por ahí. */
    private fun writeCacheForThisCategory(items: List<HomeCardItem>) {
        val cache = app().catalogCache
        when (contentType) {
            ContentType.LIVE -> {
                val existing = cache.readLive()
                val categories = existing?.categories.orEmpty().ifEmpty { listOf(currentCategoryPlaceholder()) }
                val map = existing?.streamsByCategory.orEmpty().toMutableMap()
                map[categoryId] = items.mapNotNull { (it as? HomeCardItem.Live)?.stream }
                cache.writeLive(LiveCacheEntry(categories, map, existing?.lastUpdatedAt ?: 0L))
            }
            ContentType.VOD -> {
                val existing = cache.readVod()
                val categories = existing?.categories.orEmpty().ifEmpty { listOf(currentCategoryPlaceholder()) }
                val map = existing?.streamsByCategory.orEmpty().toMutableMap()
                map[categoryId] = items.mapNotNull { (it as? HomeCardItem.Vod)?.stream }
                cache.writeVod(VodCacheEntry(categories, map, existing?.lastUpdatedAt ?: 0L))
            }
            ContentType.SERIES -> {
                val existing = cache.readSeries()
                val categories = existing?.categories.orEmpty().ifEmpty { listOf(currentCategoryPlaceholder()) }
                val map = existing?.seriesByCategory.orEmpty().toMutableMap()
                map[categoryId] = items.mapNotNull { (it as? HomeCardItem.SeriesItem)?.series }
                cache.writeSeries(SeriesCacheEntry(categories, map, existing?.lastUpdatedAt ?: 0L))
            }
        }
    }

    private fun currentCategoryPlaceholder() = Category(categoryId = categoryId, categoryName = categoryName)

    private fun goToLoginDueToExpiredSession() {
        if (!isAdded) return
        app().sessionManager.clearSession()
        app().catalogCache.clear()
        app().favoritesManager.clearAll()
        app().playbackPositionManager.clearAll()
        app().liveZapList = emptyList()
        app().liveZapIndex = -1
        Toast.makeText(requireContext(), R.string.session_expired, Toast.LENGTH_LONG).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
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
        // rowViewHolder y row son nulos en una cuadrícula vertical (VerticalGridPresenter no
        // tiene filas): declararlos no-nulos hacía que Kotlin generara una comprobación que
        // reventaba con NullPointerException al tocar CUALQUIER tarjeta de la cuadrícula.
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?
        ) {
            val session = session() ?: return
            when (item) {
                is HomeCardItem.Live -> {
                    val liveStreams = (0 until itemsAdapter.size()).mapNotNull { index ->
                        (itemsAdapter.get(index) as? HomeCardItem.Live)?.stream
                    }
                    app().liveZapList = liveStreams
                    app().liveZapIndex = liveStreams.indexOfFirst { it.streamId == item.stream.streamId }

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
