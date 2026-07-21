package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import com.iptvtv.player.api.XtreamApiClient
import com.iptvtv.player.model.CardData
import com.iptvtv.player.model.Episode
import com.iptvtv.player.presenter.MediaCardPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeriesDetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_details)

        val seriesId = intent.getStringExtra("series_id")
        val seriesName = intent.getStringExtra("series_name") ?: ""
        val host = intent.getStringExtra("host")
        val port = intent.getStringExtra("port")
        val user = intent.getStringExtra("user")
        val pass = intent.getStringExtra("pass")

        title = seriesName

        val rowsFragment = RowsSupportFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.series_rows_fragment, rowsFragment)
            .commit()

        if (seriesId == null || host == null || port == null || user == null || pass == null) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val episodes = withContext(Dispatchers.IO) {
                try {
                    XtreamApiClient.fetchSeriesEpisodes(host, port, user, pass, seriesId)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            showEpisodes(rowsFragment, episodes)
        }
    }

    private fun showEpisodes(rowsFragment: RowsSupportFragment, episodes: List<Episode>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = MediaCardPresenter()

        episodes.groupBy { it.season }.toSortedMap().forEach { (season, list) ->
            val adapter = ArrayObjectAdapter(cardPresenter)
            list.sortedBy { it.episodeNumber }.forEach { ep ->
                adapter.add(CardData("E${ep.episodeNumber} - ${ep.title}", "Season $season", null, ep))
            }
            rowsAdapter.add(ListRow(HeaderItem(season.toLong(), "Season $season"), adapter))
        }

        if (rowsAdapter.size() == 0) {
            val emptyAdapter = ArrayObjectAdapter(cardPresenter)
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.no_channels_found)), emptyAdapter))
        }

        rowsFragment.adapter = rowsAdapter
        rowsFragment.onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val data = item as? CardData ?: return@OnItemViewClickedListener
            val episode = data.payload as? Episode ?: return@OnItemViewClickedListener
            val intent = Intent(this, PlaybackActivity::class.java)
            intent.putExtra("stream_url", episode.streamUrl)
            intent.putExtra("channel_name", episode.title)
            startActivity(intent)
        }
    }
}
