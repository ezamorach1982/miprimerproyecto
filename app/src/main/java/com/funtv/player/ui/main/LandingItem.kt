package com.funtv.player.ui.main

import com.funtv.player.R

/** Las tarjetas grandes de la pantalla de inicio: cada una abre su propia sección. */
sealed class LandingItem(val titleRes: Int, val drawableRes: Int) {
    object LiveTv : LandingItem(R.string.header_live, R.drawable.landing_live)
    object Movies : LandingItem(R.string.header_movies, R.drawable.landing_movies)
    object Series : LandingItem(R.string.header_series, R.drawable.landing_series)
    object Search : LandingItem(R.string.header_search, R.drawable.landing_search)
    object Account : LandingItem(R.string.header_account, R.drawable.landing_account)
    object Refresh : LandingItem(R.string.header_refresh, R.drawable.landing_refresh)
}
