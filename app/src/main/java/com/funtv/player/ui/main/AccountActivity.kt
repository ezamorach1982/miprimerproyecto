package com.funtv.player.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.Coil
import com.funtv.player.BuildConfig
import com.funtv.player.R
import com.funtv.player.data.ContentRefresher
import com.funtv.player.databinding.ActivityAccountBinding
import com.funtv.player.ui.login.LoginActivity
import com.funtv.player.util.CrashHandler
import com.funtv.player.util.formatExpirationDate
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.launch

/**
 * Cuenta conectada (servidor, usuario, vencimiento) y ajustes generales: actualizar el
 * catálogo, borrar la caché de imágenes, versión de la app y cierre de sesión.
 */
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private var refreshInProgress = false

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

        binding.buttonRefreshContent.setOnClickListener { refreshContent() }
        binding.buttonClearImageCache.setOnClickListener { clearImageCache() }
        binding.buttonClearFavorites.setOnClickListener { clearFavorites() }
        binding.buttonViewLastCrash.setOnClickListener { showLastCrash() }
        binding.buttonLogout.setOnClickListener { logout() }
    }

    private fun refreshContent() {
        if (refreshInProgress) return
        val session = funTvApp().sessionManager.getSession() ?: return
        refreshInProgress = true
        binding.buttonRefreshContent.isEnabled = false
        binding.buttonRefreshContent.text = getString(R.string.action_refresh_in_progress)
        lifecycleScope.launch {
            val success = ContentRefresher.refreshAll(funTvApp(), session)
            refreshInProgress = false
            binding.buttonRefreshContent.isEnabled = true
            binding.buttonRefreshContent.text = getString(R.string.action_refresh_now)
            Toast.makeText(
                this@AccountActivity,
                if (success) R.string.refresh_success else R.string.refresh_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearImageCache() {
        val loader = Coil.imageLoader(this)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        Toast.makeText(this, R.string.image_cache_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun clearFavorites() {
        funTvApp().favoritesManager.clearAll()
        Toast.makeText(this, R.string.favorites_cleared, Toast.LENGTH_SHORT).show()
    }

    /** Muestra el último fallo no controlado guardado en disco, para poder reportarlo sin necesitar Logcat. */
    private fun showLastCrash() {
        val report = CrashHandler.readLastCrash(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.last_crash_dialog_title)
            .setMessage(report ?: getString(R.string.last_crash_none))
            .setPositiveButton(R.string.action_close, null)
        if (report != null) {
            dialog.setNegativeButton(R.string.action_clear) { _, _ -> CrashHandler.clearLastCrash(this) }
        }
        dialog.show()
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
