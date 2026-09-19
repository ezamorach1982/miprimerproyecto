package com.funtv.player.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.funtv.player.R
import com.funtv.player.data.api.XtreamException
import com.funtv.player.databinding.ActivityLoginBinding
import com.funtv.player.ui.main.MainActivity
import com.funtv.player.util.funTvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonConnect.setOnClickListener {
            performLogin(
                serverUrl = DEFAULT_SERVER_URL,
                username = binding.editUsername.text.toString(),
                password = binding.editPassword.text.toString(),
                isAutoLogin = false
            )
        }

        // El teclado del sistema no responde bien al control remoto en varios
        // televisores (el foco se sale del teclado en vez de moverse entre letras),
        // así que se usa un teclado propio en pantalla en su lugar.
        binding.onScreenKeyboard.attachTo(binding.editUsername)
        binding.onScreenKeyboard.attachTo(binding.editPassword)
        binding.onScreenKeyboard.onDone = { binding.buttonConnect.performClick() }

        tryAutoLoginOrShowForm()
    }

    /**
     * Si ya hay una sesión guardada, se reintenta la conexión en silencio: el formulario
     * (y con él, el teclado en pantalla) queda oculto mientras tanto, y solo se muestra si
     * el reintento falla o si de entrada no hay nada guardado. Antes el formulario siempre
     * se mostraba de una, así que el teclado aparecía en cada apertura de la app aunque el
     * usuario ya estuviera conectado.
     */
    private fun tryAutoLoginOrShowForm() {
        lifecycleScope.launch {
            val session = withContext(Dispatchers.IO) { funTvApp().sessionManager.getSession() }
            if (session == null) {
                showForm()
                return@launch
            }
            binding.editUsername.setText(session.username)
            binding.editPassword.setText(session.password)
            performLogin(session.baseUrl, session.username, session.password, isAutoLogin = true)
        }
    }

    private fun showForm() {
        binding.initialLoadingProgress.visibility = android.view.View.GONE
        binding.loginFormContainer.visibility = android.view.View.VISIBLE
    }

    private fun performLogin(serverUrl: String, username: String, password: String, isAutoLogin: Boolean) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            if (!isAutoLogin) showError(getString(R.string.login_error_empty_fields))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val (session, response) = funTvApp().xtreamClient.login(serverUrl, username, password)
                val expDate = response.userInfo?.expDate
                withContext(Dispatchers.IO) { funTvApp().sessionManager.saveSession(session, expDate) }
                showExpiration(expDate)
                delay(1200)
                goToHome()
            } catch (e: XtreamException) {
                setLoading(false)
                if (isAutoLogin) showForm()
                showError(e.message ?: getString(R.string.login_error_generic))
            } catch (e: Exception) {
                setLoading(false)
                if (isAutoLogin) showForm()
                showError(getString(R.string.login_error_generic))
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressLogin.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonConnect.isEnabled = !loading
        if (loading) {
            binding.textError.visibility = android.view.View.GONE
            binding.textExpiration.visibility = android.view.View.GONE
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }

    private fun showExpiration(expDate: String?) {
        binding.progressLogin.visibility = android.view.View.GONE
        binding.textExpiration.text = formatExpiration(expDate)
        binding.textExpiration.visibility = android.view.View.VISIBLE
    }

    private fun formatExpiration(expDate: String?): String {
        val seconds = expDate?.toLongOrNull()
        if (seconds == null || seconds <= 0L) {
            return getString(R.string.login_no_expiration)
        }
        val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(seconds * 1000))
        return getString(R.string.login_expiration, formatted)
    }

    private fun goToHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        /** Servidor Xtream Codes fijo: este build solo se usa con este panel, así que el usuario no tiene que escribirlo. */
        private const val DEFAULT_SERVER_URL = "http://xdplus.site:2082"
    }
}
