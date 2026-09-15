package com.funtv.player.ui.main

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.load
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.prefs.FavoriteEntry
import com.funtv.player.data.prefs.FavoriteType
import com.funtv.player.data.prefs.FavoritesManager
import com.funtv.player.util.funTvApp

class CardPresenter : Presenter() {

    private class CardViewHolder(cardView: ImageCardView, val progressView: View) : ViewHolder(cardView)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.setBackgroundColor(ContextCompat.getColor(parent.context, R.color.funtv_surface))

        // Barra de progreso ("continuar viendo"): una vista aparte sobre la imagen,
        // no un drawable compuesto, para no depender de callbacks de carga de imagen.
        val progressView = View(parent.context).apply {
            setBackgroundColor(ContextCompat.getColor(parent.context, R.color.funtv_accent))
            visibility = View.GONE
        }
        val params = FrameLayout.LayoutParams(0, PROGRESS_BAR_HEIGHT_PX).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = CARD_HEIGHT - PROGRESS_BAR_HEIGHT_PX
        }
        cardView.addView(progressView, params)

        return CardViewHolder(cardView, progressView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val holder = viewHolder as CardViewHolder
        val cardView = holder.view as ImageCardView
        val context = cardView.context
        when (item) {
            is HomeCardItem.Live -> {
                cardView.titleText = item.stream.name
                cardView.contentText = context.getString(R.string.header_live)
                loadImage(cardView, item.stream.streamIcon)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.Vod -> {
                cardView.titleText = item.stream.name
                cardView.contentText = context.getString(R.string.header_movies)
                loadImage(cardView, item.stream.streamIcon)
                applyProgress(holder.progressView, vodProgress(context, item))
            }
            is HomeCardItem.SeriesItem -> {
                cardView.titleText = item.series.name
                cardView.contentText = context.getString(R.string.header_series)
                loadImage(cardView, item.series.cover)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.Retry -> {
                cardView.titleText = context.getString(R.string.action_retry)
                cardView.contentText = item.sectionTitle
                cardView.mainImageView.setImageResource(R.drawable.ic_retry)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.SeeAll -> {
                cardView.titleText = context.getString(R.string.action_see_all)
                cardView.contentText = ""
                cardView.mainImageView.setImageResource(R.drawable.ic_see_all)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.ContinueWatchingCard -> {
                val entry = item.entry
                cardView.titleText = entry.title
                val percent = if (entry.durationMs > 0) (entry.positionMs * 100 / entry.durationMs).toInt() else 0
                cardView.contentText = context.getString(R.string.continue_watching_percent, percent)
                loadImage(cardView, entry.posterUrl)
                val progress = if (entry.durationMs > 0) entry.positionMs.toFloat() / entry.durationMs else 0f
                applyProgress(holder.progressView, progress)
            }
            is HomeCardItem.FavoriteCard -> {
                val entry = item.entry
                cardView.titleText = entry.title
                cardView.contentText = typeLabel(context, entry.type)
                loadImage(cardView, entry.posterUrl)
                applyProgress(holder.progressView, 0f)
            }
        }

        // Mantener presionada una tarjeta de contenido la marca/desmarca como favorita.
        val favoriteEntry = favoriteEntryFor(item)
        cardView.setOnLongClickListener {
            val nowFavorite = context.funTvApp().favoritesManager.toggle(favoriteEntry ?: return@setOnLongClickListener false)
            Toast.makeText(
                context,
                if (nowFavorite) R.string.favorite_added else R.string.favorite_removed,
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as CardViewHolder
        val cardView = holder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
        applyProgress(holder.progressView, 0f)
    }

    private fun typeLabel(context: Context, type: FavoriteType): String = when (type) {
        FavoriteType.LIVE -> context.getString(R.string.header_live)
        FavoriteType.VOD -> context.getString(R.string.header_movies)
        FavoriteType.SERIES -> context.getString(R.string.header_series)
    }

    private fun favoriteEntryFor(item: Any): FavoriteEntry? = when (item) {
        is HomeCardItem.Live -> FavoriteEntry(
            key = FavoritesManager.keyFor(FavoriteType.LIVE, item.stream.streamId),
            type = FavoriteType.LIVE,
            id = item.stream.streamId,
            title = item.stream.name.orEmpty(),
            posterUrl = item.stream.streamIcon
        )
        is HomeCardItem.Vod -> FavoriteEntry(
            key = FavoritesManager.keyFor(FavoriteType.VOD, item.stream.streamId),
            type = FavoriteType.VOD,
            id = item.stream.streamId,
            title = item.stream.name.orEmpty(),
            posterUrl = item.stream.streamIcon,
            containerExtension = item.stream.containerExtension
        )
        is HomeCardItem.SeriesItem -> FavoriteEntry(
            key = FavoritesManager.keyFor(FavoriteType.SERIES, item.series.seriesId),
            type = FavoriteType.SERIES,
            id = item.series.seriesId,
            title = item.series.name.orEmpty(),
            posterUrl = item.series.cover
        )
        is HomeCardItem.FavoriteCard -> item.entry
        else -> null
    }

    private fun vodProgress(context: Context, item: HomeCardItem.Vod): Float {
        val app = context.funTvApp()
        val session = app.sessionManager.getSession() ?: return 0f
        val url = StreamUrlBuilder.vodUrl(session, item.stream.streamId, item.stream.containerExtension)
        return app.playbackPositionManager.getProgress(url)
    }

    private fun applyProgress(progressView: View, progress: Float) {
        if (progress <= 0f) {
            progressView.visibility = View.GONE
            return
        }
        progressView.visibility = View.VISIBLE
        val params = progressView.layoutParams as FrameLayout.LayoutParams
        params.width = (CARD_WIDTH * progress).toInt().coerceAtLeast(4)
        progressView.layoutParams = params
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
        private const val PROGRESS_BAR_HEIGHT_PX = 8
    }
}
