package com.iptvtv.player.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvtv.player.model.Channel
import java.io.File

/**
 * مسؤول عن تخزين قائمة القنوات محلياً (ملف JSON) وتخزين المفضلة وبيانات
 * مصدر البث (M3U أو Xtream Codes) في SharedPreferences.
 */
object PlaylistRepository {
    private const val PLAYLIST_FILE = "playlist_cache.json"
    private const val PREFS = "iptvtv_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_SOURCE_TYPE = "source_type"
    private const val KEY_M3U_URL = "m3u_url"
    private const val KEY_XT_HOST = "xt_host"
    private const val KEY_XT_PORT = "xt_port"
    private const val KEY_XT_USER = "xt_user"
    private const val KEY_XT_PASS = "xt_pass"

    private val gson = Gson()

    fun saveChannels(context: Context, channels: List<Channel>) {
        val file = File(context.filesDir, PLAYLIST_FILE)
        file.writeText(gson.toJson(channels))
    }

    fun loadChannels(context: Context): List<Channel> {
        val file = File(context.filesDir, PLAYLIST_FILE)
        if (!file.exists()) return emptyList()
        val type = object : TypeToken<List<Channel>>() {}.type
        return try {
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun hasPlaylist(context: Context): Boolean =
        File(context.filesDir, PLAYLIST_FILE).exists()

    fun clearPlaylist(context: Context) {
        File(context.filesDir, PLAYLIST_FILE).delete()
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFavorites(context: Context): MutableSet<String> =
        prefs(context).getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun toggleFavorite(context: Context, channelId: String) {
        val favs = getFavorites(context)
        if (favs.contains(channelId)) favs.remove(channelId) else favs.add(channelId)
        prefs(context).edit().putStringSet(KEY_FAVORITES, favs).apply()
    }

    // بيانات مصدر Xtream Codes
    fun saveXtreamSource(context: Context, host: String, port: String, user: String, pass: String) {
        prefs(context).edit()
            .putString(KEY_SOURCE_TYPE, "xtream")
            .putString(KEY_XT_HOST, host)
            .putString(KEY_XT_PORT, port)
            .putString(KEY_XT_USER, user)
            .putString(KEY_XT_PASS, pass)
            .apply()
    }

    // بيانات مصدر M3U
    fun saveM3uSource(context: Context, url: String) {
        prefs(context).edit()
            .putString(KEY_SOURCE_TYPE, "m3u")
            .putString(KEY_M3U_URL, url)
            .apply()
    }

    fun getSourceType(context: Context): String? = prefs(context).getString(KEY_SOURCE_TYPE, null)
    fun getM3uUrl(context: Context): String? = prefs(context).getString(KEY_M3U_URL, null)
    fun getXtreamHost(context: Context): String? = prefs(context).getString(KEY_XT_HOST, null)
    fun getXtreamPort(context: Context): String? = prefs(context).getString(KEY_XT_PORT, null)
    fun getXtreamUser(context: Context): String? = prefs(context).getString(KEY_XT_USER, null)
    fun getXtreamPass(context: Context): String? = prefs(context).getString(KEY_XT_PASS, null)
}
