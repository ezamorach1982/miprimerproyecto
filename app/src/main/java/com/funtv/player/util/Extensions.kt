package com.funtv.player.util

import android.content.Context
import com.funtv.player.FunTvApplication
import com.funtv.player.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Context.funTvApp(): FunTvApplication = applicationContext as FunTvApplication

/** Convierte el exp_date (timestamp Unix en segundos, o null/0 para "sin vencimiento") que envía Xtream Codes a dd/MM/yyyy. */
fun formatExpirationDate(expDate: String?): String? {
    val seconds = expDate?.toLongOrNull() ?: return null
    if (seconds <= 0L) return null
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(seconds * 1000))
}

/** "Actualizado hace X" para las tarjetas de sección del inicio, a partir de CatalogCache.lastUpdatedAt (0 = nunca). */
fun formatLastUpdated(context: Context, timestampMs: Long): String {
    if (timestampMs <= 0L) return context.getString(R.string.home_updated_never)
    val elapsedMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    return when {
        elapsedMs < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.home_updated_just_now)
        elapsedMs < TimeUnit.HOURS.toMillis(1) ->
            context.getString(R.string.home_updated_minutes, TimeUnit.MILLISECONDS.toMinutes(elapsedMs).toInt())
        elapsedMs < TimeUnit.DAYS.toMillis(1) ->
            context.getString(R.string.home_updated_hours, TimeUnit.MILLISECONDS.toHours(elapsedMs).toInt())
        else -> context.getString(R.string.home_updated_days, TimeUnit.MILLISECONDS.toDays(elapsedMs).toInt())
    }
}
