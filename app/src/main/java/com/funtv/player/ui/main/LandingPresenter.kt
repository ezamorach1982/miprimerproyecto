package com.funtv.player.ui.main

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.funtv.player.util.formatLastUpdated
import com.funtv.player.util.funTvApp

/** Tarjetas grandes de sección del inicio, con "actualizado hace X" como subtítulo (mismo dato que ve el botón Actualizar). */
class LandingPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        // Las imágenes de fondo son fotos (no íconos vectoriales a medida): que
        // rellenen la tarjeta recortando en vez de deformarse o dejar bordes vacíos.
        cardView.mainImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        val landingItem = item as? LandingItem ?: return
        val context = cardView.context
        cardView.titleText = context.getString(landingItem.titleRes)
        cardView.contentText = formatLastUpdated(context, lastUpdatedFor(context, landingItem))
        cardView.mainImageView.setImageResource(landingItem.drawableRes)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
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
    }
}
