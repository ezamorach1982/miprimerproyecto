package com.funtv.player.ui.browse

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
import android.content.Intent
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Muestra las categorías y el contenido de UNA sola sección (TV en Vivo, Películas
 * o Series), como filas de Leanback. Evita mezclar los tres tipos en una sola
 * pantalla y solo pide al panel los datos de la sección que el usuario abrió.
 *
 * Carga en dos fases para que la pantalla se sienta rápida sin depender de que
 * el panel IPTV responda rápido:
 *  - Si hay caché en disco de una visita anterior, se muestra de inmediato (sin
 *    spinner) mientras se refresca en segundo plano y se reemplaza al terminar.
 *  - Si no hay caché (primera vez), cada fila aparece apenas su categoría
 *    responde, en vez de esperar a que respondan todas antes de mostrar algo.
 *
 * Cada fila muestra solo una vista previa horizontal (unas pocas tarjetas), con
 * una tarjeta "Ver todo" al final que abre CategoryGridActivity con TODO el
 * contenido de esa categoría en una cuadrícula vertical con scroll.
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

    /** Permite cambiar de canal con DPAD arriba/abajo dentro del reproductor, sin salir de él. */
    private fun setUpZapList(row: Row, clicked: HomeCardItem.Live) {
        val listRow = row as? ListRow ?: return
        val itemsAdapter = listRow.adapter as? ArrayObjectAdapter ?: return
        val liveStreams = (0 until itemsAdapter.size()).mapNotNull { index ->
            (itemsAdapter.get(index) as? HomeCardItem.Live)?.stream
        }
        val clickedIndex = liveStreams.indexOfFirst { it.streamId == clicked.stream.streamId }
        app().liveZapList = liveStreams
        app().liveZapIndex = clickedIndex
    }

    private fun goToLoginDueToExpiredSession() {
        if (!isAdded) return
        app().sessionManager.clearSession()
        app().catalogCache.clear()
        Toast.makeText(requireContext(), R.string.session_expired, Toast.LENGTH_LONG).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun openCategoryGrid(category: Category) {
        startActivity(
            CategoryGridActivity.newIntent(requireContext(), contentType, category.categoryId, category.categoryName)
        )
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
        rowsAdapter.clear()

        viewLifecycleOwner.lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) { readCache() }
            val shownFromCache = cached != null && cached.isNotEmpty()

            if (shownFromCache) {
                cached!!.forEach { (category, items) -> addCategoryRow(category, items) }
            } else {
                progressBarManager.show()
            }

            val categories = try {
                fetchCategories(session)
            } catch (e: XtreamSessionExpiredException) {
                progressBarManager.hide()
                goToLoginDueToExpiredSession()
                return@launch
            } catch (e: Exception) {
                null
            }

            if (categories.isNullOrEmpty()) {
                progressBarManager.hide()
                if (!shownFromCache) showLoadErrorRow()
                return@launch
            }

            val freshItemsByCategory = mutableMapOf<String, List<HomeCardItem>>()
            var addedAny = false
            var spinnerHidden = shownFromCache

            coroutineScope {
                categories.forEach { category ->
                    launch(Dispatchers.IO) {
                        val items = concurrencyLimiter.withPermit {
                            try {
                                fetchItemsForCategory(session, category)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                        if (items.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                freshItemsByCategory[category.categoryId] = items
                                addedAny = true
                                if (!shownFromCache) {
                                    if (!spinnerHidden) {
                                        spinnerHidden = true
                                        progressBarManager.hide()
                                    }
                                    addCategoryRow(category, items)
                                }
                            }
                        }
                    }
                }
            }

            if (!spinnerHidden) progressBarManager.hide()

            when {
                shownFromCache && addedAny -> {
                    // Refresco silencioso terminado: reemplaza lo cacheado por datos frescos.
                    rowsAdapter.clear()
                    categories.forEach { category ->
                        freshItemsByCategory[category.categoryId]?.let { addCategoryRow(category, it) }
                    }
                }
                !addedAny && !shownFromCache -> showLoadErrorRow()
            }

            if (addedAny) {
                withContext(Dispatchers.IO) { writeCache(categories, freshItemsByCategory) }
            }
        }
    }

    private fun addCategoryRow(category: Category, items: List<HomeCardItem>) {
        val header = HeaderItem(category.categoryName)
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        itemsAdapter.addAll(0, items)
        itemsAdapter.add(HomeCardItem.SeeAll(category))
        rowsAdapter.add(ListRow(header, itemsAdapter))
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

    private fun readCache(): List<Pair<Category, List<HomeCardItem>>>? {
        val cache = app().catalogCache
        return when (contentType) {
            ContentType.LIVE -> cache.readLive()?.let { entry ->
                entry.categories.mapNotNull { cat ->
                    entry.streamsByCategory[cat.categoryId]?.takeIf { it.isNotEmpty() }
                        ?.let { cat to it.map { s -> HomeCardItem.Live(s) } }
                }
            }
            ContentType.VOD -> cache.readVod()?.let { entry ->
                entry.categories.mapNotNull { cat ->
                    entry.streamsByCategory[cat.categoryId]?.takeIf { it.isNotEmpty() }
                        ?.let { cat to it.map { s -> HomeCardItem.Vod(s) } }
                }
            }
            ContentType.SERIES -> cache.readSeries()?.let { entry ->
                entry.categories.mapNotNull { cat ->
                    entry.seriesByCategory[cat.categoryId]?.takeIf { it.isNotEmpty() }
                        ?.let { cat to it.map { s -> HomeCardItem.SeriesItem(s) } }
                }
            }
        }
    }

    private fun writeCache(categories: List<Category>, itemsByCategory: Map<String, List<HomeCardItem>>) {
        val cache = app().catalogCache
        when (contentType) {
            ContentType.LIVE -> {
                val map = itemsByCategory.mapValues { (_, items) -> items.mapNotNull { (it as? HomeCardItem.Live)?.stream } }
                cache.writeLive(LiveCacheEntry(categories, map))
            }
            ContentType.VOD -> {
                val map = itemsByCategory.mapValues { (_, items) -> items.mapNotNull { (it as? HomeCardItem.Vod)?.stream } }
                cache.writeVod(VodCacheEntry(categories, map))
            }
            ContentType.SERIES -> {
                val map = itemsByCategory.mapValues { (_, items) -> items.mapNotNull { (it as? HomeCardItem.SeriesItem)?.series } }
                cache.writeSeries(SeriesCacheEntry(categories, map))
            }
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
                    setUpZapList(row, item)
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
                is HomeCardItem.Retry -> loadContent()
                is HomeCardItem.SeeAll -> openCategoryGrid(item.category)
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
