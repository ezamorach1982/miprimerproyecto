package com.funtv.player.ui.browse

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.funtv.player.R
import com.funtv.player.data.ContentRefresher
import com.funtv.player.data.api.XtreamSessionExpiredException
import com.funtv.player.data.cache.LiveCacheEntry
import com.funtv.player.data.cache.SeriesCacheEntry
import com.funtv.player.data.cache.VodCacheEntry
import com.funtv.player.data.model.Category
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.ui.search.SearchActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Muestra las categorías de UNA sola sección (TV en Vivo, Películas o Series) en una
 * lista lateral, y el contenido de la categoría seleccionada en una cuadrícula vertical
 * con scroll a la derecha (en vez de filas horizontales con una tarjeta "Ver todo" al
 * final, que en categorías con miles de elementos nunca se alcanza).
 *
 * Carga en dos fases: si hay categorías en caché de una visita anterior se muestran de
 * inmediato mientras se refrescan en segundo plano; si no hay caché, se espera la
 * respuesta del panel.
 */
class SectionBrowseFragment : Fragment(R.layout.fragment_section_browse) {

    private lateinit var recyclerSidebar: RecyclerView
    private lateinit var textSectionTitle: TextView

    private val sidebarAdapter = CategorySidebarAdapter { category -> selectCategory(category) }
    private var selectedCategoryId: String? = null
    private var backgroundRefreshJob: Job? = null

    private val contentType: ContentType by lazy {
        ContentType.valueOf(requireArguments().getString(ARG_CONTENT_TYPE)!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textSectionTitle = view.findViewById(R.id.textSectionTitle)
        textSectionTitle.text = getString(sectionTitleRes())

        recyclerSidebar = view.findViewById(R.id.recyclerSidebar)
        recyclerSidebar.layoutManager = LinearLayoutManager(requireContext())
        recyclerSidebar.adapter = sidebarAdapter

        view.findViewById<View>(R.id.buttonSectionSearch).setOnClickListener {
            startActivity(SearchActivity.newIntent(requireContext(), contentType))
        }

        loadCategories()
        startBackgroundFullRefresh()
    }

    /**
     * Además de la lista (liviana) de categorías para el panel lateral, descarga en
     * segundo plano el contenido de TODAS las categorías de esta sección y lo guarda en
     * caché —igual que antes del rediseño a lista lateral + cuadrícula—, para que el
     * buscador (que solo mira el caché local, sin buscar en el servidor) encuentre
     * resultados de toda la sección y no solo de la categoría que el usuario ya abrió.
     */
    private fun startBackgroundFullRefresh() {
        if (backgroundRefreshJob?.isActive == true) return
        val session = session() ?: return
        backgroundRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            when (contentType) {
                ContentType.LIVE -> ContentRefresher.refreshLive(app(), session)
                ContentType.VOD -> ContentRefresher.refreshVod(app(), session)
                ContentType.SERIES -> ContentRefresher.refreshSeries(app(), session)
            }
        }
    }

    private fun app() = requireContext().funTvApp()

    private fun session(): XtreamSession? = app().sessionManager.getSession()

    private fun sectionTitleRes() = when (contentType) {
        ContentType.LIVE -> R.string.header_live
        ContentType.VOD -> R.string.header_movies
        ContentType.SERIES -> R.string.header_series
    }

    private fun loadCategories() {
        val session = session() ?: return

        val cachedCategories = readCachedCategories()
        if (!cachedCategories.isNullOrEmpty()) {
            showCategories(cachedCategories)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val fresh = try {
                withContext(Dispatchers.IO) { fetchCategories(session) }
            } catch (e: XtreamSessionExpiredException) {
                goToLoginDueToExpiredSession()
                return@launch
            } catch (e: Exception) {
                null
            }

            if (fresh.isNullOrEmpty()) {
                if (cachedCategories.isNullOrEmpty() && isAdded) {
                    Toast.makeText(requireContext(), R.string.home_error_loading, Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            showCategories(fresh)
            withContext(Dispatchers.IO) { mergeCategoriesIntoCache(fresh) }
        }
    }

    private fun showCategories(categories: List<Category>) {
        if (!isAdded) return
        sidebarAdapter.submitList(categories)
        if (selectedCategoryId == null || categories.none { it.categoryId == selectedCategoryId }) {
            categories.firstOrNull()?.let { selectCategory(it) }
        }
    }

    private fun selectCategory(category: Category) {
        if (!isAdded) return
        selectedCategoryId = category.categoryId
        sidebarAdapter.setSelected(category.categoryId)
        childFragmentManager.beginTransaction()
            .replace(R.id.gridContainer, CategoryGridFragment.newInstance(contentType, category.categoryId, category.categoryName))
            .commitNow()
    }

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

    private suspend fun fetchCategories(session: XtreamSession): List<Category> = when (contentType) {
        ContentType.LIVE -> app().xtreamClient.getLiveCategories(session)
        ContentType.VOD -> app().xtreamClient.getVodCategories(session)
        ContentType.SERIES -> app().xtreamClient.getSeriesCategories(session)
    }

    private fun readCachedCategories(): List<Category>? = when (contentType) {
        ContentType.LIVE -> app().catalogCache.readLive()?.categories
        ContentType.VOD -> app().catalogCache.readVod()?.categories
        ContentType.SERIES -> app().catalogCache.readSeries()?.categories
    }

    /** Actualiza solo la lista de categorías del caché, sin tocar el contenido ya guardado por categoría. */
    private fun mergeCategoriesIntoCache(categories: List<Category>) {
        val cache = app().catalogCache
        when (contentType) {
            ContentType.LIVE -> cache.writeLive(LiveCacheEntry(categories, cache.readLive()?.streamsByCategory.orEmpty()))
            ContentType.VOD -> cache.writeVod(VodCacheEntry(categories, cache.readVod()?.streamsByCategory.orEmpty()))
            ContentType.SERIES -> cache.writeSeries(SeriesCacheEntry(categories, cache.readSeries()?.seriesByCategory.orEmpty()))
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
