package com.iptvtv.player

import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class PlaybackActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private var sleepTimer: CountDownTimer? = null
    private var resizeModeIndex = 0

    private val resizeModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playback)

        val playerView = findViewById<PlayerView>(R.id.player_view)
        val streamUrl = intent.getStringExtra("stream_url")

        if (streamUrl.isNullOrBlank()) {
            finish()
            return
        }

        trackSelector = DefaultTrackSelector(this)

        val dataSourceFactory = if (streamUrl.startsWith("rtmp://")) {
            RtmpDataSource.Factory()
        } else {
            DefaultDataSource.Factory(this)
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer
                exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_M -> {
                showPlaybackMenu()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showPlaybackMenu() {
        val options = arrayOf(
            getString(R.string.subtitles_audio),
            getString(R.string.aspect_ratio),
            getString(R.string.sleep_timer)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.playback_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTrackSelectionDialog()
                    1 -> cycleAspectRatio()
                    2 -> showSleepTimerDialog()
                }
            }
            .show()
    }

    private fun showTrackSelectionDialog() {
        val exoPlayer = player ?: return
        val tracks = exoPlayer.currentTracks
        val labels = mutableListOf<String>()
        val trackRefs = mutableListOf<Pair<androidx.media3.common.Tracks.Group, Int>>()

        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO && group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val prefix = if (group.type == C.TRACK_TYPE_AUDIO) "🔊" else "💬"
                labels.add("$prefix ${format.label ?: format.language ?: "Track ${i + 1}"}")
                trackRefs.add(group to i)
            }
        }

        if (labels.isEmpty()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.no_tracks_available)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.subtitles_audio)
            .setItems(labels.toTypedArray()) { _, which ->
                val (group, index) = trackRefs[which]
                val override = TrackSelectionOverride(group.mediaTrackGroup, index)
                trackSelector.parameters = trackSelector.parameters.buildUpon()
                    .setOverrideForType(override)
                    .build()
            }
            .show()
    }

    private fun cycleAspectRatio() {
        val playerView = findViewById<PlayerView>(R.id.player_view)
        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
        playerView.resizeMode = resizeModes[resizeModeIndex]
    }

    private fun showSleepTimerDialog() {
        val options = arrayOf(
            getString(R.string.off),
            "15 " + getString(R.string.minutes),
            "30 " + getString(R.string.minutes),
            "60 " + getString(R.string.minutes)
        )
        val minutesValues = intArrayOf(0, 15, 30, 60)

        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_timer)
            .setItems(options) { _, which ->
                sleepTimer?.cancel()
                val minutes = minutesValues[which]
                if (minutes > 0) {
                    sleepTimer = object : CountDownTimer(minutes * 60_000L, 60_000L) {
                        override fun onTick(millisUntilFinished: Long) {}
                        override fun onFinish() {
                            player?.pause()
                        }
                    }.start()
                }
            }
            .show()
    }

    override fun onStop() {
        super.onStop()
        sleepTimer?.cancel()
        player?.release()
        player = null
    }
}
