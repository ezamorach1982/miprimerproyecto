package com.funtv.player.ui.main

import com.funtv.player.R

/**
 * Las tres tarjetas grandes de la pantalla de inicio, cada una abre su propia sección.
 * Buscar/Actualizar/Cuenta viven como íconos en la barra superior (ver MainFragment),
 * no como tarjetas de esta fila.
 */
sealed class LandingItem(val titleRes: Int, val drawableRes: Int) {
    object LiveTv : LandingItem(R.string.header_live, R.drawable.landing_live)
    object Movies : LandingItem(R.string.header_movies, R.drawable.landing_movies)
    object Series : LandingItem(R.string.header_series, R.drawable.landing_series)
}
