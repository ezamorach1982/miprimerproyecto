package com.funtv.player.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import com.funtv.player.R

/**
 * Extiende FragmentActivity (no AppCompatActivity): Theme.FunTV.Browse desciende de
 * Theme.Leanback, y AppCompatActivity exige un tema descendiente de Theme.AppCompat.
 */
class MainActivity : FragmentActivity(R.layout.activity_main) {

    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mainFrame, MainFragment())
                .commitNow()
        }

        // Evita que un "Atrás" accidental en la pantalla de inicio cierre la app de golpe.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    finish()
                    return
                }
                backPressedOnce = true
                Toast.makeText(this@MainActivity, R.string.exit_confirm, Toast.LENGTH_SHORT).show()
                window.decorView.postDelayed({ backPressedOnce = false }, 2000)
            }
        })
    }
}
