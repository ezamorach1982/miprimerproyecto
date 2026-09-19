package com.funtv.player.ui.browse

import com.funtv.player.data.model.Category

/** Lo que puede aparecer en la lista lateral de una sección: categorías reales del panel, más un par de categorías "virtuales" calculadas en el dispositivo. */
sealed class SidebarEntry {
    data class RealCategory(val category: Category) : SidebarEntry()
    object Favorites : SidebarEntry()
    object RecentlyAdded : SidebarEntry()

    /** Identificador estable para recordar cuál está seleccionada y no depender de la posición en la lista. */
    val selectionKey: String
        get() = when (this) {
            is RealCategory -> category.categoryId
            Favorites -> KEY_FAVORITES
            RecentlyAdded -> KEY_RECENTLY_ADDED
        }

    companion object {
        const val KEY_FAVORITES = "__favorites__"
        const val KEY_RECENTLY_ADDED = "__recently_added__"
    }
}
