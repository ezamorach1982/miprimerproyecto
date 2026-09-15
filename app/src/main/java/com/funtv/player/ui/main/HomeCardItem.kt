package com.funtv.player.ui.main

import com.funtv.player.data.model.LiveStream
import com.funtv.player.data.model.Series
import com.funtv.player.data.model.VodStream

/** Envuelve cada tipo de contenido que puede aparecer como tarjeta en el Home. */
sealed class HomeCardItem {
    data class Live(val stream: LiveStream) : HomeCardItem()
    data class Vod(val stream: VodStream) : HomeCardItem()
    data class SeriesItem(val series: Series) : HomeCardItem()
    data class Retry(val sectionTitle: String) : HomeCardItem()
    object Logout : HomeCardItem()
}
