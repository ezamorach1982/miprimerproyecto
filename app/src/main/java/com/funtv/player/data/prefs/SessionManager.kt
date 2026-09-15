package com.funtv.player.data.prefs

import android.content.Context
import com.funtv.player.data.model.XtreamSession

/**
 * Persiste las credenciales del panel Xtream Codes en disco para no pedirlas en
 * cada arranque de la app.
 *
 * Se usa SharedPreferences simple (no EncryptedSharedPreferences): esa librería
 * exige minSdk 23, y este proyecto mantiene minSdk 21 para cubrir también equipos
 * Fire TV/Android TV más antiguos. Si tu flota de dispositivos es toda API 23+,
 * puedes migrar a androidx.security:security-crypto para cifrar este archivo.
 */
class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun saveSession(session: XtreamSession) {
        prefs.edit()
            .putString(KEY_SERVER, session.baseUrl)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_PASSWORD, session.password)
            .apply()
    }

    fun getSession(): XtreamSession? {
        val server = prefs.getString(KEY_SERVER, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val password = prefs.getString(KEY_PASSWORD, null) ?: return null
        return XtreamSession(server, username, password)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE = "funtv_prefs"
        private const val KEY_SERVER = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
