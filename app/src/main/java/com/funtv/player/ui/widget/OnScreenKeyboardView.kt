package com.funtv.player.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.funtv.player.R

/**
 * Teclado en pantalla propio, hecho con botones normales navegables por D-pad.
 *
 * En algunos televisores/cajitas Android TV el teclado flotante del sistema no
 * captura bien el control remoto: al presionar una flecha con el teclado abierto,
 * el foco se sale del teclado y salta a otro campo de la pantalla en vez de moverse
 * entre letras, haciendo imposible escribir. Como eso ocurre dentro del propio
 * teclado del sistema (fuera del control de esta app), la solución es no usarlo:
 * se desactiva con [attachTo] (`showSoftInputOnFocus = false`) y en su lugar este
 * teclado —construido con Views comunes de la app— escribe directamente en el
 * EditText que tenga el foco.
 */
class OnScreenKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var target: EditText? = null
    private var shifted = false
    var onDone: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_onscreen_keyboard, this, true)
        wireKeys(this)
    }

    /** El teclado escribirá en [editText] mientras tenga el foco; se desactiva su teclado del sistema. */
    fun attachTo(editText: EditText) {
        editText.showSoftInputOnFocus = false
        editText.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) target = editText }
    }

    private fun wireKeys(root: ViewGroup) {
        for (i in 0 until root.childCount) {
            when (val child = root.getChildAt(i)) {
                is ViewGroup -> wireKeys(child)
                is Button -> bindKey(child)
                else -> Unit
            }
        }
    }

    private fun bindKey(key: Button) {
        when (key.tag) {
            "backspace" -> key.setOnClickListener { backspace() }
            "space" -> key.setOnClickListener { insert(" ") }
            "shift" -> key.setOnClickListener { toggleShift() }
            "done" -> key.setOnClickListener { onDone?.invoke() }
            else -> key.setOnClickListener { insert(key.text.toString()) }
        }
    }

    private fun insert(value: String) {
        target?.append(value)
    }

    private fun backspace() {
        val text = target?.text ?: return
        if (text.isNotEmpty()) text.delete(text.length - 1, text.length)
    }

    private fun toggleShift() {
        shifted = !shifted
        applyShiftToLetters(this)
    }

    private fun applyShiftToLetters(root: ViewGroup) {
        for (i in 0 until root.childCount) {
            when (val child = root.getChildAt(i)) {
                is ViewGroup -> applyShiftToLetters(child)
                is Button -> if (child.tag == "letter") {
                    child.text = if (shifted) child.text.toString().uppercase() else child.text.toString().lowercase()
                }
                else -> Unit
            }
        }
    }
}
