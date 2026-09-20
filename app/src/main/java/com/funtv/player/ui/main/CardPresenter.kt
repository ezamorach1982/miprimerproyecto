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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import coil.load
import com.funtv.player.R
import com.funtv.player.data.api.StreamUrlBuilder
import com.funtv.player.data.prefs.FavoriteEntry
import com.funtv.player.data.prefs.FavoriteType
import com.funtv.player.data.prefs.FavoritesManager
import com.funtv.player.util.funTvApp

/**
 * Tarjeta de contenido (canal/película/serie/continuar viendo/favorito): foto a página
 * completa con degradado y título encima, igual que las tarjetas grandes del inicio
 * (LandingPresenter), para que el mismo estilo se sienta en toda la app y no solo ahí.
 *
 * @param onFavoriteToggled se llama después de marcar/desmarcar un favorito
 * (mantener presionado). La usa el Home para refrescar su fila "Favoritos" al
 * instante cuando se quita uno desde ahí mismo; el resto de las pantallas no la
 * necesita y puede omitirla.
 */
class CardPresenter(private val onFavoriteToggled: (() -> Unit)? = null) : Presenter() {

    private class CardViewHolder(
        root: FrameLayout,
        val imageView: ImageView,
        val titleView: TextView,
        val subtitleView: TextView,
        val progressView: View,
        val favoriteIcon: ImageView,
        val badgeView: TextView
    ) : ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            // Decorativa: el título ya dice lo mismo, así que un lector de pantalla no debe anunciarla aparte.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        val scrim = View(context).apply {
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
            textSize = 14f
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
            textSize = 11f
        }

        // Barra de progreso ("continuar viendo"): pegada al borde inferior, sobre la imagen.
        val progressView = View(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.funtv_accent))
            visibility = View.GONE
        }
        val progressParams = FrameLayout.LayoutParams(0, PROGRESS_BAR_HEIGHT_PX).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = CARD_HEIGHT - PROGRESS_BAR_HEIGHT_PX
        }

        // Corazón: marca visualmente las tarjetas ya guardadas como favoritas
        // (se marca/desmarca manteniendo presionada la tarjeta).
        val favoriteIcon = ImageView(context).apply {
            setImageResource(R.drawable.ic_heart_filled)
            visibility = View.GONE
        }
        val favoriteParams = FrameLayout.LayoutParams(FAVORITE_ICON_SIZE_PX, FAVORITE_ICON_SIZE_PX).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = FAVORITE_ICON_MARGIN_PX
            rightMargin = FAVORITE_ICON_MARGIN_PX
        }

        // Insignia de tipo de contenido (EN VIVO/PELÍCULA/SERIE), arriba a la izquierda para
        // no chocar con el corazón de favoritos que va arriba a la derecha.
        val badgeView = TextView(context).apply {
            setBackgroundResource(R.drawable.bg_badge_pill)
            setTextColor(ContextCompat.getColor(context, R.color.funtv_text_primary))
            textSize = 9f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(BADGE_PADDING_H_PX, BADGE_PADDING_V_PX, BADGE_PADDING_H_PX, BADGE_PADDING_V_PX)
            visibility = View.GONE
        }
        val badgeParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = FAVORITE_ICON_MARGIN_PX
            leftMargin = FAVORITE_ICON_MARGIN_PX
        }

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(CARD_WIDTH, CARD_HEIGHT)
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(ContextCompat.getColor(context, R.color.funtv_surface))
            addView(imageView)
            addView(scrim)
            addView(titleView)
            addView(subtitleView)
            addView(progressView, progressParams)
            addView(favoriteIcon, favoriteParams)
            addView(badgeView, badgeParams)
            foreground = ContextCompat.getDrawable(context, R.drawable.bg_hero_card_focus)
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, CORNER_RADIUS)
                }
            }
        }

        return CardViewHolder(root, imageView, titleView, subtitleView, progressView, favoriteIcon, badgeView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val holder = viewHolder as CardViewHolder
        val context = holder.view.context
        when (item) {
            is HomeCardItem.Live -> {
                holder.titleView.text = item.stream.name
                holder.subtitleView.text = context.getString(R.string.header_live)
                loadImage(holder.imageView, item.stream.streamIcon)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.Vod -> {
                holder.titleView.text = item.stream.name
                holder.subtitleView.text = context.getString(R.string.header_movies)
                loadImage(holder.imageView, item.stream.streamIcon)
                applyProgress(holder.progressView, vodProgress(context, item))
            }
            is HomeCardItem.SeriesItem -> {
                holder.titleView.text = item.series.name
                holder.subtitleView.text = context.getString(R.string.header_series)
                loadImage(holder.imageView, item.series.cover)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.Retry -> {
                holder.titleView.text = context.getString(R.string.action_retry)
                holder.subtitleView.text = item.sectionTitle
                holder.imageView.setImageResource(R.drawable.ic_retry)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.SeeAll -> {
                holder.titleView.text = context.getString(R.string.action_see_all)
                holder.subtitleView.text = ""
                holder.imageView.setImageResource(R.drawable.ic_see_all)
                applyProgress(holder.progressView, 0f)
            }
            is HomeCardItem.ContinueWatchingCard -> {
                val entry = item.entry
                holder.titleView.text = entry.title
                val percent = if (entry.durationMs > 0) (entry.positionMs * 100 / entry.durationMs).toInt() else 0
                holder.subtitleView.text = context.getString(R.string.continue_watching_percent, percent)
                loadImage(holder.imageView, entry.posterUrl)
                val progress = if (entry.durationMs > 0) entry.positionMs.toFloat() / entry.durationMs else 0f
                applyProgress(holder.progressView, progress)
            }
            is HomeCardItem.FavoriteCard -> {
                val entry = item.entry
                holder.titleView.text = entry.title
                holder.subtitleView.text = typeLabel(context, entry.type)
                loadImage(holder.imageView, entry.posterUrl)
                applyProgress(holder.progressView, 0f)
            }
        }

        bindBadge(context, holder.badgeView, item)

        // Mantener presionada una tarjeta de contenido la marca/desmarca como favorita.
        val favoriteEntry = favoriteEntryFor(item)
        val isFavorite = favoriteEntry != null && context.funTvApp().favoritesManager.isFavorite(favoriteEntry.key)
        holder.favoriteIcon.visibility = if (isFavorite) View.VISIBLE else View.GONE
        holder.view.setOnLongClickListener {
            val nowFavorite = context.funTvApp().favoritesManager.toggle(favoriteEntry ?: return@setOnLongClickListener false)
            holder.favoriteIcon.visibility = if (nowFavorite) View.VISIBLE else View.GONE
            Toast.makeText(
                context,
                if (nowFavorite) R.string.favorite_added else R.string.favorite_removed,
                Toast.LENGTH_SHORT
            ).show()
            onFavoriteToggled?.invoke()
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as CardViewHolder
        holder.imageView.setImageDrawable(null)
        applyProgress(holder.progressView, 0f)
        holder.favoriteIcon.visibility = View.GONE
        holder.badgeView.visibility = View.GONE
    }

    private fun typeLabel(context: Context, type: FavoriteType): String = when (type) {
        FavoriteType.LIVE -> context.getString(R.string.header_live)
        FavoriteType.VOD -> context.getString(R.string.header_movies)
        FavoriteType.SERIES -> context.getString(R.string.header_series)
    }

    /** Insignia de color por tipo de contenido (canal/película/serie), a la vista de un vistazo. */
    private fun bindBadge(context: Context, badgeView: TextView, item: Any) {
        val (textRes, colorRes) = when (item) {
            is HomeCardItem.Live -> R.string.badge_live to R.color.funtv_blue
            is HomeCardItem.Vod -> R.string.badge_movie to R.color.funtv_red
            is HomeCardItem.SeriesItem -> R.string.badge_series to R.color.funtv_green
            is HomeCardItem.FavoriteCard -> when (item.entry.type) {
                FavoriteType.LIVE -> R.string.badge_live to R.color.funtv_blue
                FavoriteType.VOD -> R.string.badge_movie to R.color.funtv_red
                FavoriteType.SERIES -> R.string.badge_series to R.color.funtv_green
            }
            is HomeCardItem.ContinueWatchingCard -> when (item.entry.type) {
                FavoriteType.VOD -> R.string.badge_movie to R.color.funtv_red
                FavoriteType.SERIES -> R.string.badge_series to R.color.funtv_green
                // Nulo en entradas guardadas antes de existir este campo: sin insignia.
                FavoriteType.LIVE, null -> {
                    badgeView.visibility = View.GONE
                    return
                }
            }
            else -> {
                badgeView.visibility = View.GONE
                return
            }
        }
        badgeView.text = context.getString(textRes)
        badgeView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        badgeView.visibility = View.VISIBLE
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

    private fun loadImage(imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(R.drawable.placeholder_poster)
            return
        }
        imageView.load(url) {
            placeholder(R.drawable.placeholder_poster)
            error(R.drawable.placeholder_poster)
        }
    }

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
        private const val SCRIM_HEIGHT = 100
        private const val TEXT_MARGIN = 10
        private const val SUBTITLE_RESERVED = 16
        private const val PROGRESS_BAR_HEIGHT_PX = 8
        private const val FAVORITE_ICON_SIZE_PX = 26
        private const val FAVORITE_ICON_MARGIN_PX = 6
        private const val BADGE_PADDING_H_PX = 8
        private const val BADGE_PADDING_V_PX = 3
        private const val CORNER_RADIUS = 24f
    }
}
