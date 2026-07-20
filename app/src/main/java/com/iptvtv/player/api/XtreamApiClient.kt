package com.iptvtv.player.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvtv.player.model.Channel
import java.net.HttpURLConnection
import java.net.URL

private data class XtCategory(val category_id: String?, val category_name: String?)

private data class XtLiveStream(
    val name: String?,
    val stream_id: Int?,
    val stream_icon: String?,
    val category_id: String?
)

/**
 * عميل بسيط للتواصل مع أي بروفايدر يدعم Xtream Codes API القياسي
 * (player_api.php). يجلب تصنيفات وقنوات البث المباشر فقط في هذه النسخة.
 */
object XtreamApiClient {
    private val gson = Gson()

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        conn.requestMethod = "GET"
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return text
    }

    fun fetchLiveChannels(host: String, port: String, user: String, pass: String): List<Channel> {
        val base = "http://$host:$port/player_api.php?username=$user&password=$pass"

        val categories: List<XtCategory> = try {
            val json = get("$base&action=get_live_categories")
            gson.fromJson(json, object : TypeToken<List<XtCategory>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val catMap = categories.associate { (it.category_id ?: "") to (it.category_name ?: "General") }

        val streams: List<XtLiveStream> = try {
            val json = get("$base&action=get_live_streams")
            gson.fromJson(json, object : TypeToken<List<XtLiveStream>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return streams.mapNotNull { s ->
            val id = s.stream_id ?: return@mapNotNull null
            Channel(
                id = id.toString(),
                name = s.name ?: "Channel $id",
                streamUrl = "http://$host:$port/live/$user/$pass/$id.m3u8",
                logoUrl = s.stream_icon,
                category = catMap[s.category_id] ?: "General"
            )
        }
    }
}
