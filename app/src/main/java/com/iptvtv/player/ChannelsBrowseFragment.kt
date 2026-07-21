package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.iptvtv.player.api.XtreamApiClient
import com.iptvtv.player.data.PlaylistRepository
import com.iptvtv.player.model.CardData
import com.iptvtv.player.model.Channel
import com.iptvtv.player.model.SeriesItem
import com.iptvtv.player.model.VodItem
import com.iptvtv.player.presenter.MediaCardPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelsBrowseFragment : BrowseSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = resources.getColor(R.color.brand_color, null)

        setOnSearchClickedListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        buildRows()

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is CardData) handleClick(item)
        }
    }

    override fun onResume() {
        super.onResume()
        buildRows()
    }

    private fun buildRows() {
        val channels = PlaylistRepository.loadChannels(requireContext())
        val favorites = PlaylistRepository.getFavorites(requireContext())
        val withFavFlag = channels.map { it.copy(isFavorite = favorites.contains(it.id)) }
        val vodItems = PlaylistRepository.loadVodItems(requireContext())
        val seriesItems = PlaylistRepository.loadSeriesItems(requireContext())

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = MediaCardPresenter()
        var headerId = 0L

        val favChannels = withFavFlag.filter { it.isFavorite }
        if (favChannels.isNotEmpty()) {
            val adapter = ArrayObjectAdapter(cardPresenter)
            favChannels.forEach { adapter.add(CardData(it.name, it.category, it.logoUrl, it)) }
            rowsAdapter.add(ListRow(HeaderItem(headerId++, getString(R.string.favorites)), adapter))
        }

        withFavFlag.map { it.category }.distinct().sorted().forEach { category ->
            val adapter = ArrayObjectAdapter(cardPresenter)
            withFavFlag.filter { it.category == category }
                .forEach { adapter.add(CardData(it.name, it.category, it.logoUrl, it)) }
            rowsAdapter.add(ListRow(HeaderItem(headerId++, "\uD83D\uDCFA $category"), adapter))
        }

        vodItems.map { it.category }.distinct().sorted().forEach { category ->
            val adapter = ArrayObjectAdapter(cardPresenter)
            vodItems.filter { it.category == category }
                .forEach { adapter.add(CardData(it.name, getString(R.string.movie), it.logoUrl, it)) }
            rowsAdapter.add(ListRow(HeaderItem(headerId++, "\uD83C\uDFAC $category"), adapter))
        }

        seriesItems.map { it.category }.distinct().sorted().forEach { category ->
            val adapter = ArrayObjectAdapter(cardPresenter)
            seriesItems.filter { it.category == category }
                .forEach { adapter.add(CardData(it.name, getString(R.string.series), it.coverUrl, it)) }
            rowsAdapter.add(ListRow(HeaderItem(headerId++, "\uD83D\uDCFD $category"), adapter))
        }

        if (rowsAdapter.size() == 0) {
            val emptyAdapter = ArrayObjectAdapter(cardPresenter)
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.no_channels_found)), emptyAdapter))
        }

        adapter = rowsAdapter
    }

    private fun handleClick(data: CardData) {
        when (val payload = data.payload) {
            is Channel -> onChannelClicked(payload)
            is VodItem -> playStream(payload.streamUrl, payload.name)
            is SeriesItem -> onSeriesClicked(payload)
        }
    }

    private fun onChannelClicked(channel: Channel) {
        if (PlaylistRepository.isCategoryLocked(requireContext(), channel.category)) {
            askPinThen { showChannelActions(channel) }
        } else {
            showChannelActions(channel)
        }
    }

    private fun onSeriesClicked(series: SeriesItem) {
        if (PlaylistRepository.isCategoryLocked(requireContext(), series.category)) {
            askPinThen { openSeries(series) }
        } else {
            openSeries(series)
        }
    }

    private fun askPinThen(action: () -> Unit) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.parental_pin_title)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val entered = input.text.toString()
                if (entered == PlaylistRepository.getParentalPin(requireContext())) {
                    action()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setMessage(R.string.wrong_pin)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showChannelActions(channel: Channel) {
        val favorites = PlaylistRepository.getFavorites(requireContext())
        val isFav = favorites.contains(channel.id)
        val options = mutableListOf(
            getString(R.string.play),
            if (isFav) getString(R.string.remove_favorite) else getString(R.string.add_favorite),
            getString(R.string.program_info)
        )
        if (channel.hasArchive) options.add(getString(R.string.watch_archive))

        AlertDialog.Builder(requireContext())
            .setTitle(channel.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> playStream(channel.streamUrl, channel.name)
                    1 -> {
                        PlaylistRepository.toggleFavorite(requireContext(), channel.id)
                        buildRows()
                    }
                    2 -> showProgramInfo(channel)
                    3 -> if (channel.hasArchive) showArchiveOptions(channel)
                }
            }
            .show()
    }

    private fun showProgramInfo(channel: Channel) {
        val host = PlaylistRepository.getXtreamHost(requireContext())
        val port = PlaylistRepository.getXtreamPort(requireContext())
        val user = PlaylistRepository.getXtreamUser(requireContext())
        val pass = PlaylistRepository.getXtreamPass(requireContext())
        if (host == null || port == null || user == null || pass == null) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.epg_unavailable)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(channel.name)
            .setMessage(R.string.loading)
            .setPositiveButton(R.string.ok, null)
            .show()

        CoroutineScope(Dispatchers.Main).launch {
            val programs = withContext(Dispatchers.IO) {
                try {
                    XtreamApiClient.fetchShortEpg(host, port, user, pass, channel.id)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val text = if (programs.isEmpty()) {
                getString(R.string.epg_unavailable)
            } else {
                programs.joinToString("\n\n") { "${it.title}\n${it.start} - ${it.end}" }
            }
            dialog.setMessage(text)
        }
    }

    private fun showArchiveOptions(channel: Channel) {
        val options = arrayOf(
            getString(R.string.last_30_min),
            getString(R.string.last_1_hour),
            getString(R.string.last_3_hours)
        )
        val minutesOptions = arrayOf(30, 60, 180)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.watch_archive)
            .setItems(options) { _, which ->
                val host = PlaylistRepository.getXtreamHost(requireContext()) ?: return@setItems
                val port = PlaylistRepository.getXtreamPort(requireContext()) ?: return@setItems
                val user = PlaylistRepository.getXtreamUser(requireContext()) ?: return@setItems
                val pass = PlaylistRepository.getXtreamPass(requireContext()) ?: return@setItems
                val minutes = minutesOptions[which].coerceAtMost(
                    if (channel.archiveDurationMinutes > 0) channel.archiveDurationMinutes else 180
                )
                val url = XtreamApiClient.buildCatchupUrl(host, port, user, pass, channel.id, minutes)
                playStream(url, channel.name)
            }
            .show()
    }

    private fun playStream(url: String, name: String) {
        val intent = Intent(requireContext(), PlaybackActivity::class.java)
        intent.putExtra("stream_url", url)
        intent.putExtra("channel_name", name)
        startActivity(intent)
    }

    private fun openSeries(series: SeriesItem) {
        val intent = Intent(requireContext(), SeriesDetailsActivity::class.java)
        intent.putExtra("series_id", series.id)
        intent.putExtra("series_name", series.name)
        intent.putExtra("host", PlaylistRepository.getXtreamHost(requireContext()))
        intent.putExtra("port", PlaylistRepository.getXtreamPort(requireContext()))
        intent.putExtra("user", PlaylistRepository.getXtreamUser(requireContext()))
        intent.putExtra("pass", PlaylistRepository.getXtreamPass(requireContext()))
        startActivity(intent)
    }
}
