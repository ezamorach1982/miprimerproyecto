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
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import com.funtv.player.data.model.XtreamSession
import com.funtv.player.ui.browse.ContentType
import com.funtv.player.ui.browse.SectionBrowseActivity
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla de inicio: solo 4 tarjetas grandes (TV en Vivo, Películas, Series,
 * Cuenta). Cada sección abre su propia pantalla con sus categorías —así no se
 * mezclan canales, películas y series en una sola lista.
 */
class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTitle(getString(R.string.home_title))
        setHeadersState(HEADERS_ENABLED)
        setHeadersTransitionOnBackEnabled(true)
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.funtv_background))

        setAdapter(rowsAdapter)
        onItemViewClickedListener = ItemViewClickedListener()

        buildRows()
    }

    override fun onResume() {
        super.onResume()
        if (session() == null) goToLogin()
    }

    private fun session(): XtreamSession? = requireContext().funTvApp().sessionManager.getSession()

    private fun buildRows() {
        val header = HeaderItem(getString(R.string.landing_header))
        val itemsAdapter = ArrayObjectAdapter(LandingPresenter())
        itemsAdapter.add(LandingItem.LiveTv)
        itemsAdapter.add(LandingItem.Movies)
        itemsAdapter.add(LandingItem.Series)
        itemsAdapter.add(LandingItem.Account)
        rowsAdapter.clear()
        rowsAdapter.add(ListRow(header, itemsAdapter))
    }

    private fun openSection(type: ContentType) {
        startActivity(SectionBrowseActivity.newIntent(requireContext(), type))
    }

    private fun logout() {
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { requireContext().funTvApp().sessionManager.clearSession() }
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
            when (item) {
                is LandingItem.LiveTv -> openSection(ContentType.LIVE)
                is LandingItem.Movies -> openSection(ContentType.VOD)
                is LandingItem.Series -> openSection(ContentType.SERIES)
                is LandingItem.Account -> logout()
            }
        }
    }
}
