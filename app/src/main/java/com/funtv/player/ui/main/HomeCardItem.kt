package com.funtv.player.ui.main

import com.funtv.player.data.model.Category
import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.VodStream

/** Envuelve cada tipo de contenido que puede aparecer como tarjeta dentro de una sección. */
sealed class HomeCardItem {
    data class Live(val stream: LiveStream) : HomeCardItem()
    data class Vod(val stream: VodStream) : HomeCardItem()
    data class SeriesItem(val series: Series) : HomeCardItem()
    data class Retry(val sectionTitle: String) : HomeCardItem()

    /** Tarjeta al final de cada fila: abre la categoría completa en una cuadrícula vertical. */
    data class SeeAll(val category: Category) : HomeCardItem()
}
