package com.iptvtv.player

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.iptvtv.player.data.PlaylistRepository

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!PlaylistRepository.hasPlaylist(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        if (savedInstanceState == null) {
            showBrowseFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        // إعادة بناء الصفوف كل مرة يرجع فيها المستخدم (لتحديث المفضلة مثلاً)
        if (PlaylistRepository.hasPlaylist(this)) {
            showBrowseFragment()
        }
    }

    private fun showBrowseFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_browse_fragment, ChannelsBrowseFragment())
            .commitAllowingStateLoss()
    }
}
