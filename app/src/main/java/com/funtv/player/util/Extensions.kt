package com.funtv.player.util

import android.content.Context
import com.funtv.player.FunTvApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.funTvApp(): FunTvApplication = applicationContext as FunTvApplication

/** Convierte el exp_date (timestamp Unix en segundos, o null/0 para "sin vencimiento") que envía Xtream Codes a dd/MM/yyyy. */
fun formatExpirationDate(expDate: String?): String? {
    val seconds = expDate?.toLongOrNull() ?: return null
    if (seconds <= 0L) return null
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(seconds * 1000))
}
