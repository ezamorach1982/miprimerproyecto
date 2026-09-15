package com.funtv.player.ui.main

import com.funtv.player.data.model.Category
import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.VodStream
import com.funtv.player.data.prefs.ContinueWatchingEntry
import com.funtv.player.data.prefs.FavoriteEntry

/** Envuelve cada tipo de contenido que puede aparecer como tarjeta dentro de una sección. */
sealed class HomeCardItem {
    data class Live(val stream: LiveStream) : HomeCardItem()
    data class Vod(val stream: VodStream) : HomeCardItem()
    data class SeriesItem(val series: Series) : HomeCardItem()
    data class Retry(val sectionTitle: String) : HomeCardItem()

    /** Tarjeta al final de cada fila: abre la categoría completa en una cuadrícula vertical. */
    data class SeeAll(val category: Category) : HomeCardItem()

    /** Tarjeta de la fila "Continuar viendo" del inicio: retoma la reproducción directo, sin pasar por la ficha. */
    data class ContinueWatchingCard(val entry: ContinueWatchingEntry) : HomeCardItem()

    /** Tarjeta de la fila "Favoritos" del inicio. */
    data class FavoriteCard(val entry: FavoriteEntry) : HomeCardItem()
}
