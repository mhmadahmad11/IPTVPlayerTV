package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import com.iptvtv.player.data.PlaylistRepository
import com.iptvtv.player.databinding.ActivitySearchBinding
import com.iptvtv.player.model.CardData
import com.iptvtv.player.model.Channel
import com.iptvtv.player.model.SeriesItem
import com.iptvtv.player.model.VodItem
import com.iptvtv.player.presenter.MediaCardPresenter

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        binding.resultsContainer.removeAllViews()
        if (query.length < 2) return

        val lower = query.lowercase()
        val channels = PlaylistRepository.loadChannels(this).filter { it.name.lowercase().contains(lower) }
        val vodItems = PlaylistRepository.loadVodItems(this).filter { it.name.lowercase().contains(lower) }
        val seriesItems = PlaylistRepository.loadSeriesItems(this).filter { it.name.lowercase().contains(lower) }

        val allResults = mutableListOf<CardData>()
        channels.forEach { allResults.add(CardData(it.name, getString(R.string.live_tv_label), it.logoUrl, it)) }
        vodItems.forEach { allResults.add(CardData(it.name, getString(R.string.movie), it.logoUrl, it)) }
        seriesItems.forEach { allResults.add(CardData(it.name, getString(R.string.series), it.coverUrl, it)) }

        binding.resultsCount.text = getString(R.string.results_count, allResults.size)

        // نعرض النتائج كقائمة بسيطة بأسماء نصية (متوافقة مع الريموت بدون تعقيد)
        allResults.take(50).forEach { result ->
            val itemView = layoutInflater.inflate(R.layout.item_search_result, binding.resultsContainer, false)
            val titleView = itemView.findViewById<android.widget.TextView>(R.id.result_title)
            val subtitleView = itemView.findViewById<android.widget.TextView>(R.id.result_subtitle)
            titleView.text = result.title
            subtitleView.text = result.subtitle
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
            itemView.setOnClickListener { handleResultClick(result) }
            binding.resultsContainer.addView(itemView)
        }
    }

    private fun handleResultClick(data: CardData) {
        when (val payload = data.payload) {
            is Channel -> {
                val intent = Intent(this, PlaybackActivity::class.java)
                intent.putExtra("stream_url", payload.streamUrl)
                intent.putExtra("channel_name", payload.name)
                startActivity(intent)
            }
            is VodItem -> {
                val intent = Intent(this, PlaybackActivity::class.java)
                intent.putExtra("stream_url", payload.streamUrl)
                intent.putExtra("channel_name", payload.name)
                startActivity(intent)
            }
            is SeriesItem -> {
                val intent = Intent(this, SeriesDetailsActivity::class.java)
                intent.putExtra("series_id", payload.id)
                intent.putExtra("series_name", payload.name)
                intent.putExtra("host", PlaylistRepository.getXtreamHost(this))
                intent.putExtra("port", PlaylistRepository.getXtreamPort(this))
                intent.putExtra("user", PlaylistRepository.getXtreamUser(this))
                intent.putExtra("pass", PlaylistRepository.getXtreamPass(this))
                startActivity(intent)
            }
        }
    }
}
