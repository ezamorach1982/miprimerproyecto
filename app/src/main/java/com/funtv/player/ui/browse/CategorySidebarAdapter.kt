package com.funtv.player.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funtv.player.R

/**
 * Lista lateral de categorías (TV en vivo / Películas / Series), más "Favoritos" y
 * "Recién agregado" como categorías virtuales fijas al principio. Al seleccionar una
 * se reemplaza la cuadrícula de la derecha con su contenido, en vez de depender de
 * una tarjeta "Ver todo" al final de una fila horizontal que nunca se alcanza en
 * categorías con miles de elementos.
 */
class CategorySidebarAdapter(
    private val onEntrySelected: (SidebarEntry) -> Unit
) : RecyclerView.Adapter<CategorySidebarAdapter.ViewHolder>() {

    private val entries = mutableListOf<SidebarEntry>()
    private var selectedKey: String? = null

    fun submitList(newEntries: List<SidebarEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    fun setSelected(selectionKey: String) {
        if (selectedKey == selectionKey) return
        selectedKey = selectionKey
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_sidebar, parent, false) as TextView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.textView.context
        holder.textView.text = when (entry) {
            is SidebarEntry.RealCategory -> entry.category.categoryName
            SidebarEntry.Favorites -> context.getString(R.string.favorites_header)
            SidebarEntry.RecentlyAdded -> context.getString(R.string.recently_added_header)
        }
        holder.textView.isSelected = entry.selectionKey == selectedKey
        holder.textView.setOnClickListener { onEntrySelected(entry) }
    }

    override fun getItemCount(): Int = entries.size

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
