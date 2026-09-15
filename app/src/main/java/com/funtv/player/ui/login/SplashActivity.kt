package com.funtv.player.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pantalla de arranque: logo con una animación breve antes de pasar a Login. */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imageSplashLogo)
        logo.alpha = 0f
        logo.scaleX = 0.85f
        logo.scaleY = 0.85f
        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ANIMATION_DURATION_MS)
            .start()

        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val ANIMATION_DURATION_MS = 500L
        private const val SPLASH_DELAY_MS = 1100L
    }
}
