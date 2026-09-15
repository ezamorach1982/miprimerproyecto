package com.funtv.player.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.data.prefs.ContinueWatchingEntry
import com.funtv.player.data.prefs.FavoriteEntry
import com.funtv.player.data.prefs.FavoriteType
import com.funtv.player.ui.browse.ContentType
import com.funtv.player.ui.browse.SectionBrowseActivity
import com.funtv.player.ui.details.SeriesDetailsActivity
import com.funtv.player.ui.details.VodDetailsActivity
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.ui.search.SearchActivity
import com.funtv.player.util.funTvApp

/**
 * Pantalla de inicio: filas "Continuar viendo" y "Favoritos" (si hay algo) + las
 * tarjetas grandes de sección (TV en Vivo, Películas, Series, Buscar, Cuenta).
 * Cada sección abre su propia pantalla con sus categorías —así no se mezclan
 * canales, películas y series en una sola lista.
 */
class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val cardPresenter = CardPresenter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTitle(getString(R.string.home_title))
        setHeadersState(HEADERS_ENABLED)
        setHeadersTransitionOnBackEnabled(true)
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.funtv_background))

        setAdapter(rowsAdapter)
        onItemViewClickedListener = ItemViewClickedListener()
    }

    override fun onResume() {
        super.onResume()
        if (session() == null) {
            goToLogin()
        } else {
            buildRows()
        }
    }

    private fun app() = requireContext().funTvApp()

    private fun session(): XtreamSession? = app().sessionManager.getSession()

    private fun buildRows() {
        rowsAdapter.clear()
        addContinueWatchingRowIfAny()
        addFavoritesRowIfAny()

        val header = HeaderItem(getString(R.string.landing_header))
        val itemsAdapter = ArrayObjectAdapter(LandingPresenter())
        itemsAdapter.add(LandingItem.LiveTv)
        itemsAdapter.add(LandingItem.Movies)
        itemsAdapter.add(LandingItem.Series)
        itemsAdapter.add(LandingItem.Search)
        itemsAdapter.add(LandingItem.Account)
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun addContinueWatchingRowIfAny() {
        val entries = app().playbackPositionManager.getContinueWatching()
        if (entries.isEmpty()) return
        val header = HeaderItem(getString(R.string.continue_watching_header))
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        entries.forEach { itemsAdapter.add(HomeCardItem.ContinueWatchingCard(it)) }
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun addFavoritesRowIfAny() {
        val favorites = app().favoritesManager.getFavorites()
        if (favorites.isEmpty()) return
        val header = HeaderItem(getString(R.string.favorites_header))
        val itemsAdapter = ArrayObjectAdapter(cardPresenter)
        favorites.forEach { itemsAdapter.add(HomeCardItem.FavoriteCard(it)) }
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun openSection(type: ContentType) {
        startActivity(SectionBrowseActivity.newIntent(requireContext(), type))
    }

    private fun resumeWatching(entry: ContinueWatchingEntry) {
        startActivity(
            PlaybackActivity.newIntent(requireContext(), entry.url, entry.title, posterUrl = entry.posterUrl)
        )
    }

    private fun openFavorite(entry: FavoriteEntry) {
        when (entry.type) {
            FavoriteType.LIVE -> {
                val session = session() ?: return
                val url = StreamUrlBuilder.liveUrl(session, entry.id)
                startActivity(PlaybackActivity.newIntent(requireContext(), url, entry.title, isLive = true))
            }
            FavoriteType.VOD -> {
                startActivity(
                    VodDetailsActivity.newIntent(requireContext(), entry.id, entry.title, entry.posterUrl, entry.containerExtension)
                )
            }
            FavoriteType.SERIES -> {
                startActivity(SeriesDetailsActivity.newIntent(requireContext(), entry.id, entry.title))
            }
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
            when (item) {
                is LandingItem.LiveTv -> openSection(ContentType.LIVE)
                is LandingItem.Movies -> openSection(ContentType.VOD)
                is LandingItem.Series -> openSection(ContentType.SERIES)
                is LandingItem.Search -> startActivity(SearchActivity.newIntent(requireContext()))
                is LandingItem.Account -> startActivity(AccountActivity.newIntent(requireContext()))
                is HomeCardItem.ContinueWatchingCard -> resumeWatching(item.entry)
                is HomeCardItem.FavoriteCard -> openFavorite(item.entry)
            }
        }
    }
}
