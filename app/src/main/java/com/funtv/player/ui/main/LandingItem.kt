package com.funtv.player.ui.main

import com.funtv.player.R

/** Las tarjetas grandes de la pantalla de inicio: cada una abre su propia sección. */
sealed class LandingItem(val titleRes: Int, val colorRes: Int) {
    object LiveTv : LandingItem(R.string.header_live, R.color.funtv_red)
    object Movies : LandingItem(R.string.header_movies, R.color.funtv_blue)
    object Series : LandingItem(R.string.header_series, R.color.funtv_green)
    object Account : LandingItem(R.string.header_account, R.color.funtv_yellow)
}
