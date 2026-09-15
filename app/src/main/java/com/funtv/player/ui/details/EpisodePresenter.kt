package com.funtv.player.ui.details

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.load
import com.funtv.player.R
import com.funtv.player.data.model.Episode

class EpisodePresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.funtv_surface))
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val cardView = viewHolder.view as ImageCardView
        val episode = item as? Episode ?: return
        cardView.titleText = "Ep. ${episode.episodeNum ?: 0}"
        cardView.contentText = episode.title.orEmpty()

        val poster = episode.info?.movieImage
        if (poster.isNullOrBlank()) {
            cardView.mainImageView.setImageResource(R.drawable.placeholder_poster)
        } else {
            cardView.mainImageView.load(poster) {
                placeholder(R.drawable.placeholder_poster)
                error(R.drawable.placeholder_poster)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
    }

    companion object {
        private const val CARD_WIDTH = 280
        private const val CARD_HEIGHT = 160
    }
}
