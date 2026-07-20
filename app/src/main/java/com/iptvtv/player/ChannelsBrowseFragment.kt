package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.iptvtv.player.data.PlaylistRepository
import com.iptvtv.player.model.Channel
import com.iptvtv.player.presenter.ChannelCardPresenter

class ChannelsBrowseFragment : BrowseSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = resources.getColor(R.color.brand_color, null)

        buildRows()

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Channel) showChannelActions(item)
        }
    }

    private fun buildRows() {
        val channels = PlaylistRepository.loadChannels(requireContext())
        val favorites = PlaylistRepository.getFavorites(requireContext())
        val withFavFlag = channels.map { it.copy(isFavorite = favorites.contains(it.id)) }

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = ChannelCardPresenter()

        val favChannels = withFavFlag.filter { it.isFavorite }
        if (favChannels.isNotEmpty()) {
            val favAdapter = ArrayObjectAdapter(cardPresenter)
            favChannels.forEach { favAdapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.favorites)), favAdapter))
        }

        val categories = withFavFlag.map { it.category }.distinct().sorted()
        categories.forEachIndexed { index, category ->
            val adapter = ArrayObjectAdapter(cardPresenter)
            withFavFlag.filter { it.category == category }.forEach { adapter.add(it) }
            rowsAdapter.add(ListRow(HeaderItem((index + 1).toLong(), category), adapter))
        }

        if (rowsAdapter.size() == 0) {
            val emptyAdapter = ArrayObjectAdapter(cardPresenter)
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.no_channels_found)), emptyAdapter))
        }

        adapter = rowsAdapter
    }

    private fun showChannelActions(channel: Channel) {
        val favorites = PlaylistRepository.getFavorites(requireContext())
        val isFav = favorites.contains(channel.id)
        val options = arrayOf(
            getString(R.string.play),
            if (isFav) getString(R.string.remove_favorite) else getString(R.string.add_favorite)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(channel.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(requireContext(), PlaybackActivity::class.java)
                        intent.putExtra("stream_url", channel.streamUrl)
                        intent.putExtra("channel_name", channel.name)
                        startActivity(intent)
                    }
                    1 -> {
                        PlaylistRepository.toggleFavorite(requireContext(), channel.id)
                        buildRows()
                    }
                }
            }
            .show()
    }
}
