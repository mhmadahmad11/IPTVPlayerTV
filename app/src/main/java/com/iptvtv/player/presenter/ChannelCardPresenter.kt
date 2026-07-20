package com.iptvtv.player.presenter

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.iptvtv.player.R
import com.iptvtv.player.model.Channel

/** يعرض كل قناة كبطاقة (شعار + اسم + تصنيف) داخل صفوف BrowseSupportFragment. */
class ChannelCardPresenter : Presenter() {

    private val cardWidthPx = 320
    private val cardHeightPx = 180

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setMainImageDimensions(cardWidthPx, cardHeightPx)
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val channel = item as Channel
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = if (channel.isFavorite) "★ ${channel.name}" else channel.name
        cardView.contentText = channel.category
        cardView.setMainImageDimensions(cardWidthPx, cardHeightPx)

        if (!channel.logoUrl.isNullOrBlank()) {
            Glide.with(cardView.context)
                .load(channel.logoUrl)
                .placeholder(R.drawable.default_channel)
                .error(R.drawable.default_channel)
                .into(cardView.mainImageView)
        } else {
            cardView.mainImageView.setImageResource(R.drawable.default_channel)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }
}
