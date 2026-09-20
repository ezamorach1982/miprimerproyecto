package com.funtv.player.ui.main

import android.content.Intent
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
import com.funtv.player.R
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.data.prefs.ContinueWatchingEntry
import com.funtv.player.ui.browse.ContentType
import com.funtv.player.ui.browse.SectionBrowseActivity
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.ui.player.PlaybackActivity
import com.funtv.player.util.funTvApp

/**
 * Filas del inicio: "Continuar viendo" (si hay algo) + las tarjetas grandes de sección
 * (TV en Vivo, Películas, Series). Sin cabecera propia ni barra de título —eso lo pone
 * MainFragment, que aloja esta fragment dentro de su propio diseño (logo, reloj,
 * buscar/actualizar/cuenta arriba; vencimiento/usuario abajo). Los favoritos viven
 * ahora como una categoría más dentro de cada sección, no aparte en el inicio.
 */
class HomeRowsFragment : RowsSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
    private val cardPresenter = CardPresenter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter(rowsAdapter)
        setOnItemViewClickedListener(ItemViewClickedListener())
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

    fun buildRows() {
        if (!isAdded) return
        rowsAdapter.clear()
        addContinueWatchingRowIfAny()

        val header = HeaderItem(getString(R.string.landing_header))
        val itemsAdapter = ArrayObjectAdapter(LandingPresenter())
        itemsAdapter.add(LandingItem.LiveTv)
        itemsAdapter.add(LandingItem.Movies)
        itemsAdapter.add(LandingItem.Series)
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

    private fun openSection(type: ContentType) {
        startActivity(SectionBrowseActivity.newIntent(requireContext(), type))
    }

    private fun resumeWatching(entry: ContinueWatchingEntry) {
        startActivity(
            PlaybackActivity.newIntent(requireContext(), entry.url, entry.title, posterUrl = entry.posterUrl, contentType = entry.type)
        )
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
                is HomeCardItem.ContinueWatchingCard -> resumeWatching(item.entry)
            }
        }
    }
}
