package com.funtv.player.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.funtv.player.R
import com.funtv.player.data.model.Category

/**
 * Lista lateral de categorías (TV en vivo / Películas / Series). Al seleccionar una
 * se reemplaza la cuadrícula de la derecha con su contenido, en vez de depender de
 * una tarjeta "Ver todo" al final de una fila horizontal que nunca se alcanza en
 * categorías con miles de elementos.
 */
class CategorySidebarAdapter(
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategorySidebarAdapter.ViewHolder>() {

    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: String? = null

    fun submitList(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }

    fun setSelected(categoryId: String) {
        if (selectedCategoryId == categoryId) return
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_sidebar, parent, false) as TextView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.textView.text = category.categoryName
        holder.textView.isSelected = category.categoryId == selectedCategoryId
        holder.textView.setOnClickListener { onCategorySelected(category) }
    }

    override fun getItemCount(): Int = categories.size

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
