package com.iptvtv.player.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvtv.player.model.Channel
import com.iptvtv.player.model.SeriesItem
import com.iptvtv.player.model.VodItem
import java.io.File

/**
 * مسؤول عن تخزين قوائم القنوات/الأفلام/المسلسلات محلياً (JSON) وتخزين المفضلة
 * والرقابة الأبوية وبيانات مصدر البث (M3U أو Xtream Codes) في SharedPreferences.
 */
object PlaylistRepository {
    private const val PLAYLIST_FILE = "playlist_cache.json"
    private const val VOD_FILE = "vod_cache.json"
    private const val SERIES_FILE = "series_cache.json"

    private const val PREFS = "iptvtv_prefs"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_SOURCE_TYPE = "source_type"
    private const val KEY_M3U_URL = "m3u_url"
    private const val KEY_XT_HOST = "xt_host"
    private const val KEY_XT_PORT = "xt_port"
    private const val KEY_XT_USER = "xt_user"
    private const val KEY_XT_PASS = "xt_pass"
    private const val KEY_PARENTAL_PIN = "parental_pin"
    private const val KEY_LOCKED_KEYWORDS = "locked_keywords"

    private val gson = Gson()

    fun saveChannels(context: Context, channels: List<Channel>) {
        File(context.filesDir, PLAYLIST_FILE).writeText(gson.toJson(channels))
    }

    fun loadChannels(context: Context): List<Channel> {
        val file = File(context.filesDir, PLAYLIST_FILE)
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<Channel>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveVodItems(context: Context, items: List<VodItem>) {
        File(context.filesDir, VOD_FILE).writeText(gson.toJson(items))
    }

    fun loadVodItems(context: Context): List<VodItem> {
        val file = File(context.filesDir, VOD_FILE)
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<VodItem>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSeriesItems(context: Context, items: List<SeriesItem>) {
        File(context.filesDir, SERIES_FILE).writeText(gson.toJson(items))
    }

    fun loadSeriesItems(context: Context): List<SeriesItem> {
        val file = File(context.filesDir, SERIES_FILE)
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson(file.readText(), object : TypeToken<List<SeriesItem>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun hasPlaylist(context: Context): Boolean =
        File(context.filesDir, PLAYLIST_FILE).exists() ||
            File(context.filesDir, VOD_FILE).exists() ||
            File(context.filesDir, SERIES_FILE).exists()

    fun clearPlaylist(context: Context) {
        File(context.filesDir, PLAYLIST_FILE).delete()
        File(context.filesDir, VOD_FILE).delete()
        File(context.filesDir, SERIES_FILE).delete()
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

    // الرقابة الأبوية
    fun setParentalPin(context: Context, pin: String?) {
        prefs(context).edit().putString(KEY_PARENTAL_PIN, pin).apply()
    }

    fun getParentalPin(context: Context): String? = prefs(context).getString(KEY_PARENTAL_PIN, null)

    fun isParentalEnabled(context: Context): Boolean = !getParentalPin(context).isNullOrBlank()

    /** كلمات مفتاحية تُستخدم لتحديد التصنيفات المقفولة (تنطبق على اسم التصنيف). */
    fun getLockedKeywords(context: Context): Set<String> =
        prefs(context).getStringSet(
            KEY_LOCKED_KEYWORDS,
            setOf("adult", "adults", "xxx", "18+", "للكبار")
        ) ?: emptySet()

    fun isCategoryLocked(context: Context, category: String): Boolean {
        if (!isParentalEnabled(context)) return false
        val lower = category.lowercase()
        return getLockedKeywords(context).any { lower.contains(it.lowercase()) }
    }
}
