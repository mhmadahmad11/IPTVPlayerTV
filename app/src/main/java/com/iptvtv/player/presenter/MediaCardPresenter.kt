package com.iptvtv.player.presenter

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.iptvtv.player.R
import com.iptvtv.player.model.CardData

/** يعرض أي عنصر (قناة/فيلم/مسلسل/حلقة) كبطاقة موحدة داخل صفوف Leanback. */
class MediaCardPresenter : Presenter() {

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
        val data = item as CardData
        val cardView = viewHolder.view as ImageCardView
        cardView.titleText = data.title
        cardView.contentText = data.subtitle
        cardView.setMainImageDimensions(cardWidthPx, cardHeightPx)

        if (!data.imageUrl.isNullOrBlank()) {
            Glide.with(cardView.context)
                .load(data.imageUrl)
                .placeholder(R.drawable.default_channel)
                .error(R.drawable.default_channel)
                .into(cardView.mainImageView)
        } else {
            cardView.mainImageView.setImageResource(R.drawable.default_channel)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        (viewHolder.view as ImageCardView).mainImage = null
    }
}
