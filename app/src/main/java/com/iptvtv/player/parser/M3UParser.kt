package com.iptvtv.player.parser

import com.iptvtv.player.model.Channel
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * يقوم بتحميل وتحليل ملفات M3U / M3U8 القياسية (بصيغة #EXTINF) واستخراج
 * اسم القناة، شعارها (tvg-logo)، تصنيفها (group-title)، ورابط البث.
 */
object M3UParser {

    fun parseFromUrl(url: String): List<Channel> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true
        connection.connect()
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        return parse(text)
    }

    fun parse(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var name = ""
        var logo: String? = null
        var group = "General"

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    name = line.substringAfterLast(",").trim()
                    logo = Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.get(1)
                    val g = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1)
                    group = if (g.isNullOrBlank()) "General" else g
                }
                line.isNotBlank() && !line.startsWith("#") -> {
                    channels.add(
                        Channel(
                            id = UUID.randomUUID().toString(),
                            name = name.ifBlank { "Channel" },
                            streamUrl = line,
                            logoUrl = logo,
                            category = group
                        )
                    )
                    name = ""
                    logo = null
                    group = "General"
                }
            }
        }
        return channels
    }
}
