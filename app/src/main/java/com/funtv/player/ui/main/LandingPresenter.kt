package com.funtv.player.ui.main

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.funtv.player.R
import com.funtv.player.util.formatLastUpdated
import com.funtv.player.util.funTvApp

/**
 * Tarjetas grandes de sección del inicio: foto a página completa con un degradado y el
 * título encima (en vez del ícono + barra de info plana de antes), con un pequeño efecto
 * de "elevación" al enfocar con D-pad — el patrón que usan las apps de streaming actuales.
 */
class LandingPresenter : Presenter() {

    private class LandingViewHolder(
        root: FrameLayout,
        val imageView: ImageView,
        val titleView: TextView,
        val subtitleView: TextView,
        val badgeView: TextView
    ) : ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val scrim = android.view.View(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SCRIM_HEIGHT).apply {
                gravity = Gravity.BOTTOM
            }
            background = ContextCompat.getDrawable(context, R.drawable.bg_hero_card_scrim)
        }

        val titleView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = TEXT_MARGIN
                rightMargin = TEXT_MARGIN
                bottomMargin = TEXT_MARGIN + SUBTITLE_RESERVED
            }
            setTextColor(ContextCompat.getColor(context, R.color.funtv_text_primary))
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }

        val subtitleView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = TEXT_MARGIN
                bottomMargin = TEXT_MARGIN
            }
            setTextColor(ContextCompat.getColor(context, R.color.funtv_text_secondary))
            textSize = 12f
        }

        // Insignia de color por tipo (EN VIVO/PELÍCULAS/SERIES): mismo lenguaje visual que
        // las tarjetas de contenido, aquí sí visible de entrada en el propio Inicio.
        val badgeView = TextView(context).apply {
            setBackgroundResource(R.drawable.bg_badge_pill)
            setTextColor(ContextCompat.getColor(context, R.color.funtv_text_primary))
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(BADGE_PADDING_H_PX, BADGE_PADDING_V_PX, BADGE_PADDING_H_PX, BADGE_PADDING_V_PX)
        }
        val badgeParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = BADGE_MARGIN_PX
            leftMargin = BADGE_MARGIN_PX
        }

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(CARD_WIDTH, CARD_HEIGHT)
            isFocusable = true
            isFocusableInTouchMode = true
            addView(imageView)
            addView(scrim)
            addView(titleView)
            addView(subtitleView)
            addView(badgeView, badgeParams)
            // El anillo de foco va como foreground (encima de todo), no background: la
            // foto a página completa lo taparía por debajo si fuera el fondo.
            foreground = ContextCompat.getDrawable(context, R.drawable.bg_hero_card_focus)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, CORNER_RADIUS)
                }
            }
            setOnFocusChangeListener { view, hasFocus ->
                val scale = if (hasFocus) FOCUS_SCALE else 1f
                view.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_ANIM_MS).start()
            }
        }

        return LandingViewHolder(root, imageView, titleView, subtitleView, badgeView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val holder = viewHolder as LandingViewHolder
        val landingItem = item as? LandingItem ?: return
        val context = holder.view.context
        holder.titleView.text = context.getString(landingItem.titleRes)
        holder.subtitleView.text = formatLastUpdated(context, lastUpdatedFor(context, landingItem))
        holder.imageView.setImageResource(landingItem.drawableRes)

        val (badgeTextRes, badgeColorRes) = when (landingItem) {
            LandingItem.LiveTv -> R.string.badge_live to R.color.funtv_blue
            LandingItem.Movies -> R.string.badge_movie_plural to R.color.funtv_red
            LandingItem.Series -> R.string.badge_series_plural to R.color.funtv_green
        }
        holder.badgeView.text = context.getString(badgeTextRes)
        holder.badgeView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, badgeColorRes))
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as LandingViewHolder
        holder.imageView.setImageDrawable(null)
    }

    private fun lastUpdatedFor(context: Context, item: LandingItem): Long {
        val cache = context.funTvApp().catalogCache
        return when (item) {
            is LandingItem.LiveTv -> cache.readLive()?.lastUpdatedAt ?: 0L
            is LandingItem.Movies -> cache.readVod()?.lastUpdatedAt ?: 0L
            is LandingItem.Series -> cache.readSeries()?.lastUpdatedAt ?: 0L
        }
    }

    companion object {
        private const val CARD_WIDTH = 480
        private const val CARD_HEIGHT = 270
        private const val SCRIM_HEIGHT = 150
        private const val TEXT_MARGIN = 16
        private const val SUBTITLE_RESERVED = 20
        private const val FOCUS_SCALE = 1.06f
        private const val FOCUS_ANIM_MS = 150L
        private const val CORNER_RADIUS = 30f
        private const val BADGE_PADDING_H_PX = 10
        private const val BADGE_PADDING_V_PX = 4
        private const val BADGE_MARGIN_PX = 10
    }
}
