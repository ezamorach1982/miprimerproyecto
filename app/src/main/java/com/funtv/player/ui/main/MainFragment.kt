package com.funtv.player.ui.main

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
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val cardPresenter = CardPresenter()

    // Evita golpear el panel con demasiadas peticiones simultáneas en cuentas con muchas categorías.
    private val concurrencyLimiter = Semaphore(5)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTitle(getString(R.string.home_title))
        setHeadersState(HEADERS_ENABLED)
        setHeadersTransitionOnBackEnabled(true)
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.funtv_background))

        setAdapter(rowsAdapter)
        onItemViewClickedListener = ItemViewClickedListener()

        loadContent()
    }

    override fun onResume() {
        super.onResume()
        // Si no hay sesión (por ejemplo, se cerró sesión desde otra pantalla), vuelve al login.
        if (session() == null) {
            goToLogin()
        }
    }

    private fun app() = requireContext().funTvApp()

    private fun session(): XtreamSession? = app().sessionManager.getSession()

    private fun loadContent() {
        val session = session() ?: return
        progressBarManager.show()
        rowsAdapter.clear()

        viewLifecycleOwner.lifecycleScope.launch {
            var anySucceeded = false
            var anyFailed = false

            try {
                val categories = app().xtreamClient.getLiveCategories(session)
                val rows = buildRowsForCategories(session, categories) { cat ->
                    app().xtreamClient.getLiveStreams(session, cat.categoryId).map { HomeCardItem.Live(it) }
                }
                rows.forEach { rowsAdapter.add(it) }
                anySucceeded = anySucceeded || rows.isNotEmpty()
            } catch (e: Exception) {
                anyFailed = true
            }

            try {
                val categories = app().xtreamClient.getVodCategories(session)
                val rows = buildRowsForCategories(session, categories) { cat ->
                    app().xtreamClient.getVodStreams(session, cat.categoryId).map { HomeCardItem.Vod(it) }
                }
                rows.forEach { rowsAdapter.add(it) }
                anySucceeded = anySucceeded || rows.isNotEmpty()
            } catch (e: Exception) {
                anyFailed = true
            }

            try {
                val categories = app().xtreamClient.getSeriesCategories(session)
                val rows = buildRowsForCategories(session, categories) { cat ->
                    app().xtreamClient.getSeries(session, cat.categoryId).map { HomeCardItem.SeriesItem(it) }
                }
                rows.forEach { rowsAdapter.add(it) }
                anySucceeded = anySucceeded || rows.isNotEmpty()
            } catch (e: Exception) {
                anyFailed = true
            }

            addAccountRow()
            progressBarManager.hide()

            if (!anySucceeded) {
                showLoadErrorRow()
            } else if (anyFailed && isAdded) {
                Toast.makeText(requireContext(), getString(R.string.home_error_loading), Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun buildRowsForCategories(
        session: XtreamSession,
        categories: List<Category>,
        fetchItems: suspend (Category) -> List<HomeCardItem>
    ): List<ListRow> = coroutineScope {
        categories
            .map { category ->
                async(Dispatchers.IO) {
                    val items = concurrencyLimiter.withPermit {
                        try {
                            fetchItems(category)
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

    private fun addAccountRow() {
        val header = HeaderItem(getString(R.string.header_account))
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        itemsAdapter.add(HomeCardItem.Logout)
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun showLoadErrorRow() {
        rowsAdapter.clear()
        val header = HeaderItem(getString(R.string.header_account))
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        itemsAdapter.add(HomeCardItem.Retry(getString(R.string.home_error_loading)))
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { app().sessionManager.clearSession() }
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
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
                    startActivity(PlaybackActivity.newIntent(requireContext(), url, item.stream.name.orEmpty()))
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
                is HomeCardItem.Logout -> logout()
            }
        }
    }
}
