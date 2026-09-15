package com.funtv.player.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.funtv.player.R

/**
 * Extiende FragmentActivity (no AppCompatActivity): Theme.FunTV.Browse desciende de
 * Theme.Leanback, y AppCompatActivity exige un tema descendiente de Theme.AppCompat.
 */
class SearchActivity : FragmentActivity(R.layout.activity_search) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val editQuery = findViewById<android.widget.EditText>(R.id.editSearchQuery)
        val buttonGo = findViewById<android.widget.Button>(R.id.buttonSearchGo)

        buttonGo.setOnClickListener {
            performSearch(editQuery.text.toString())
        }
    }

    private fun performSearch(query: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.searchResultsContainer) as? SearchResultsFragment
        fragment?.submitQuery(query)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SearchActivity::class.java)
    }
}
