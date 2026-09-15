package com.funtv.player.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.funtv.player.BuildConfig
import com.funtv.player.R
import com.funtv.player.databinding.ActivityAccountBinding
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.util.formatExpirationDate
import com.funtv.player.util.funTvApp

/** Datos de la cuenta conectada (servidor, usuario, vencimiento), versión de la app y cierre de sesión. */
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = funTvApp().sessionManager.getSession()
        binding.textAccountServer.text = session?.baseUrl.orEmpty()
        binding.textAccountUsername.text = getString(R.string.account_username_format, session?.username.orEmpty())

        val expiration = formatExpirationDate(funTvApp().sessionManager.getExpirationDate())
        binding.textAccountExpiration.text = if (expiration != null) {
            getString(R.string.login_expiration, expiration)
        } else {
            getString(R.string.login_no_expiration)
        }

        binding.textAppVersion.text = getString(R.string.app_version_format, BuildConfig.VERSION_NAME)

        binding.buttonLogout.setOnClickListener { logout() }
    }

    private fun logout() {
        val app = funTvApp()
        app.sessionManager.clearSession()
        app.catalogCache.clear()
        app.favoritesManager.clearAll()
        app.playbackPositionManager.clearAll()
        app.liveZapList = emptyList()
        app.liveZapIndex = -1
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, AccountActivity::class.java)
    }
}
