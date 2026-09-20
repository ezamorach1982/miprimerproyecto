package com.funtv.player.util

import android.content.Context
import android.os.Build
import com.funtv.player.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guarda el último fallo no controlado en un archivo de texto legible (sin depender de
 * ninguna cuenta/servicio externo como Firebase) para poder revisarlo después desde
 * Cuenta y ajustes, en vez de depender de que el usuario capture el Logcat a mano.
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            crashLogFile(context).writeText(formatReport(throwable))
        } catch (e: Exception) {
            // Si ni siquiera se puede escribir el reporte, que el fallo original siga su curso igual.
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun formatReport(throwable: Throwable): String {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("FunTV — último error: $timestamp")
            appendLine("Versión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
            appendLine()
            append(stackTrace)
        }
    }

    companion object {
        private const val FILE_NAME = "last_crash.txt"

        fun install(context: Context) {
            val appContext = context.applicationContext
            Thread.setDefaultUncaughtExceptionHandler(
                CrashHandler(appContext, Thread.getDefaultUncaughtExceptionHandler())
            )
        }

        fun crashLogFile(context: Context): File = File(context.filesDir, FILE_NAME)

        fun readLastCrash(context: Context): String? {
            val file = crashLogFile(context)
            return if (file.exists()) file.readText() else null
        }

        fun clearLastCrash(context: Context) {
            crashLogFile(context).delete()
        }
    }
}
