package com.funtv.player.ui.main

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import com.funtv.player.data.ContentRefresher
import com.funtv.player.ui.search.SearchActivity
import com.funtv.player.util.formatExpirationDate
import com.funtv.player.util.funTvApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Marco de la pantalla de inicio: barra superior (logo, reloj, buscar/actualizar/cuenta)
 * y pie de página (vencimiento/usuario) alrededor de HomeRowsFragment, que es quien
 * muestra las filas de contenido (continuar viendo, favoritos, TV en vivo/películas/series).
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var textClock: TextView
    private lateinit var textExpiration: TextView
    private lateinit var textUsername: TextView

    private var clockJob: Job? = null
    private var refreshJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textClock = view.findViewById(R.id.textClock)
        textExpiration = view.findViewById(R.id.textHomeExpiration)
        textUsername = view.findViewById(R.id.textHomeUsername)

        if (childFragmentManager.findFragmentById(R.id.rowsContainer) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.rowsContainer, HomeRowsFragment())
                .commitNow()
        }

        view.findViewById<View>(R.id.buttonHomeSearch).setOnClickListener {
            startActivity(SearchActivity.newIntent(requireContext()))
        }
        view.findViewById<View>(R.id.buttonHomeRefresh).setOnClickListener { refreshContent() }
        view.findViewById<View>(R.id.buttonHomeAccount).setOnClickListener {
            startActivity(AccountActivity.newIntent(requireContext()))
        }
    }

    override fun onResume() {
        super.onResume()
        updateFooter()
        startClock()
    }

    override fun onPause() {
        super.onPause()
        clockJob?.cancel()
    }

    private fun updateFooter() {
        val session = requireContext().funTvApp().sessionManager.getSession()
        val expiration = formatExpirationDate(requireContext().funTvApp().sessionManager.getExpirationDate())
        textExpiration.text = if (expiration != null) {
            getString(R.string.login_expiration, expiration)
        } else {
            getString(R.string.login_no_expiration)
        }
        textUsername.text = getString(R.string.account_username_format, session?.username.orEmpty())
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                textClock.text = CLOCK_FORMAT.format(Date())
                delay(CLOCK_TICK_MS)
            }
        }
    }

    /** Vuelve a descargar el catálogo del servidor sin salir del inicio (ícono "Actualizar" de la barra superior). */
    private fun refreshContent() {
        if (refreshJob?.isActive == true) return
        val app = requireContext().funTvApp()
        val session = app.sessionManager.getSession() ?: return
        Toast.makeText(requireContext(), R.string.refresh_in_progress, Toast.LENGTH_SHORT).show()
        refreshJob = viewLifecycleOwner.lifecycleScope.launch {
            val success = ContentRefresher.refreshAll(app, session)
            if (!isAdded) return@launch
            Toast.makeText(
                requireContext(),
                if (success) R.string.refresh_success else R.string.refresh_error,
                Toast.LENGTH_SHORT
            ).show()
            (childFragmentManager.findFragmentById(R.id.rowsContainer) as? HomeRowsFragment)?.buildRows()
        }
    }

    companion object {
        private const val CLOCK_TICK_MS = 30_000L
        private val CLOCK_FORMAT = SimpleDateFormat("HH:mm · EEE d MMM", Locale("es", "ES"))
    }
}
