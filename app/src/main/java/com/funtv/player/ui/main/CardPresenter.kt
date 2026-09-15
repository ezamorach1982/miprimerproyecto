package com.funtv.player.ui.main

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.load
import com.funtv.player.R

class CardPresenter : Presenter() {

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
        val context = cardView.context
        when (item) {
            is HomeCardItem.Live -> {
                cardView.titleText = item.stream.name
                cardView.contentText = context.getString(R.string.header_live)
                loadImage(cardView, item.stream.streamIcon)
            }
            is HomeCardItem.Vod -> {
                cardView.titleText = item.stream.name
                cardView.contentText = context.getString(R.string.header_movies)
                loadImage(cardView, item.stream.streamIcon)
            }
            is HomeCardItem.SeriesItem -> {
                cardView.titleText = item.series.name
                cardView.contentText = context.getString(R.string.header_series)
                loadImage(cardView, item.series.cover)
            }
            is HomeCardItem.Retry -> {
                cardView.titleText = context.getString(R.string.action_retry)
                cardView.contentText = item.sectionTitle
                cardView.mainImageView.setImageResource(R.drawable.ic_retry)
            }
            is HomeCardItem.Logout -> {
                cardView.titleText = context.getString(R.string.action_logout)
                cardView.contentText = ""
                cardView.mainImageView.setImageResource(R.drawable.ic_retry)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
    }

    private fun loadImage(cardView: ImageCardView, url: String?) {
        if (url.isNullOrBlank()) {
            cardView.mainImageView.setImageResource(R.drawable.placeholder_poster)
            return
        }
        cardView.mainImageView.load(url) {
            placeholder(R.drawable.placeholder_poster)
            error(R.drawable.placeholder_poster)
        }
    }

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}
