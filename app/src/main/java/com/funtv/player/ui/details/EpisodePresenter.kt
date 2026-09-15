package com.funtv.player.ui.details

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.funtv.player.R
import com.funtv.player.data.model.Episode

class EpisodePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.funtv_surface))
        cardView.mainImageView.setImageResource(R.drawable.placeholder_poster)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        val episode = item as? Episode ?: return
        cardView.titleText = "Ep. ${episode.episodeNum ?: 0}"
        cardView.contentText = episode.title.orEmpty()
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        // No hay recursos externos (imágenes remotas) que liberar en esta tarjeta.
    }

    companion object {
        private const val CARD_WIDTH = 280
        private const val CARD_HEIGHT = 160
    }
}
