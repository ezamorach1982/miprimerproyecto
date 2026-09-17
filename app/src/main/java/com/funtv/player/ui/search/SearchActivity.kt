package com.funtv.player.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.funtv.player.R
import com.funtv.player.ui.browse.ContentType
import com.funtv.player.ui.widget.OnScreenKeyboardView

/**
 * Extiende FragmentActivity (no AppCompatActivity): Theme.FunTV.Browse desciende de
 * Theme.Leanback, y AppCompatActivity exige un tema descendiente de Theme.AppCompat.
 */
class SearchActivity : FragmentActivity(R.layout.activity_search) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editQuery = findViewById<android.widget.EditText>(R.id.editSearchQuery)
        val buttonGo = findViewById<android.widget.Button>(R.id.buttonSearchGo)
        val keyboard = findViewById<OnScreenKeyboardView>(R.id.onScreenKeyboard)

        buttonGo.setOnClickListener {
            performSearch(editQuery.text.toString())
        }

        // Mismo motivo que en el login: el teclado del sistema no siempre responde
        // bien al control remoto en estos televisores.
        keyboard.attachTo(editQuery)
        keyboard.onDone = { performSearch(editQuery.text.toString()) }
    }

    private fun performSearch(query: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.searchResultsContainer) as? SearchResultsFragment
        fragment?.submitQuery(query)
    }

    companion object {
        const val EXTRA_CONTENT_TYPE_FILTER = "extra_content_type_filter"

        /** [contentTypeFilter] limita la búsqueda a una sola sección (TV/Películas/Series); null busca en todas. */
        fun newIntent(context: Context, contentTypeFilter: ContentType? = null): Intent =
            Intent(context, SearchActivity::class.java).apply {
                if (contentTypeFilter != null) putExtra(EXTRA_CONTENT_TYPE_FILTER, contentTypeFilter.name)
            }
    }
}
