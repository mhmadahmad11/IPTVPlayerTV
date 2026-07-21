package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.iptvtv.player.api.XtreamApiClient
import com.iptvtv.player.data.PlaylistRepository
import com.iptvtv.player.databinding.ActivitySetupBinding
import com.iptvtv.player.model.Channel
import com.iptvtv.player.model.SeriesItem
import com.iptvtv.player.model.VodItem
import com.iptvtv.player.parser.M3UParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var mode = "xtream"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateModeUI()

        binding.toggleMode.setOnClickListener {
            mode = if (mode == "xtream") "m3u" else "xtream"
            updateModeUI()
        }

        binding.loadButton.setOnClickListener { load() }
    }

    private fun updateModeUI() {
        val isXtream = mode == "xtream"
        binding.xtreamFields.visibility = if (isXtream) View.VISIBLE else View.GONE
        binding.m3uFields.visibility = if (isXtream) View.GONE else View.VISIBLE
        binding.toggleMode.text = if (isXtream)
            getString(R.string.switch_to_m3u) else getString(R.string.switch_to_xtream)
    }

    private fun load() {
        binding.loadButton.isEnabled = false
        binding.statusText.text = getString(R.string.loading)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (mode == "xtream") {
                        val host = binding.hostInput.text.toString().trim()
                        val port = binding.portInput.text.toString().trim().ifBlank { "80" }
                        val user = binding.userInput.text.toString().trim()
                        val pass = binding.passInput.text.toString().trim()
                        PlaylistRepository.saveXtreamSource(this@SetupActivity, host, port, user, pass)

                        val liveChannels = XtreamApiClient.fetchLiveChannels(host, port, user, pass)
                        val vodItems = try {
                            XtreamApiClient.fetchVodItems(host, port, user, pass)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        val seriesItems = try {
                            XtreamApiClient.fetchSeriesList(host, port, user, pass)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        Triple(liveChannels, vodItems, seriesItems)
                    } else {
                        val url = binding.m3uUrlInput.text.toString().trim()
                        PlaylistRepository.saveM3uSource(this@SetupActivity, url)
                        val channels = M3UParser.parseFromUrl(url)
                        Triple(channels, emptyList<VodItem>(), emptyList<SeriesItem>())
                    }
                }

                val (channels, vodItems, seriesItems) = result

                if (channels.isEmpty() && vodItems.isEmpty() && seriesItems.isEmpty()) {
                    binding.statusText.text = getString(R.string.no_channels_found)
                    binding.loadButton.isEnabled = true
                    return@launch
                }

                PlaylistRepository.saveChannels(this@SetupActivity, channels as List<Channel>)
                PlaylistRepository.saveVodItems(this@SetupActivity, vodItems as List<VodItem>)
                PlaylistRepository.saveSeriesItems(this@SetupActivity, seriesItems as List<SeriesItem>)

                val pin = binding.parentalPinInput.text.toString().trim()
                PlaylistRepository.setParentalPin(this@SetupActivity, pin.ifBlank { null })

                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                binding.statusText.text = getString(R.string.error_loading, e.message ?: "")
                binding.loadButton.isEnabled = true
            }
        }
    }
}
