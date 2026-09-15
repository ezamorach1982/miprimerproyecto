package com.funtv.player.ui.main

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.funtv.player.R

/**
 * Extiende FragmentActivity (no AppCompatActivity): Theme.FunTV.Browse desciende de
 * Theme.Leanback, y AppCompatActivity exige un tema descendiente de Theme.AppCompat.
 */
class MainActivity : FragmentActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, MainFragment())
                .commitNow()
        }
    }
}
