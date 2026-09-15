package com.funtv.player.data.prefs

import android.content.Context

/** Recuerda en qué punto quedó cada película/episodio para poder retomarlo ("continuar viendo"). No aplica a TV en vivo. */
class PlaybackPositionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun savePosition(url: String, positionMs: Long) {
        prefs.edit().putLong(url, positionMs).apply()
    }

    fun getPosition(url: String): Long = prefs.getLong(url, 0L)

    fun clearPosition(url: String) {
        prefs.edit().remove(url).apply()
    }

    companion object {
        private const val PREFS_FILE = "funtv_playback_positions"

        /** Por debajo de esto no vale la pena ofrecer "continuar viendo". */
        const val MIN_RESUME_POSITION_MS = 10_000L

        /** A esta distancia del final, se considera "visto" y no se recuerda la posición. */
        const val END_THRESHOLD_MS = 20_000L
    }
}
