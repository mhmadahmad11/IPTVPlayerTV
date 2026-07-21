package com.iptvtv.player.api

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvtv.player.model.Channel
import com.iptvtv.player.model.Episode
import com.iptvtv.player.model.EpgProgram
import com.iptvtv.player.model.SeriesItem
import com.iptvtv.player.model.VodItem
import java.net.HttpURLConnection
import java.net.URL

private data class XtCategory(val category_id: String?, val category_name: String?)

private data class XtLiveStream(
    val name: String?,
    val stream_id: Int?,
    val stream_icon: String?,
    val category_id: String?,
    val tv_archive: Int?,
    val tv_archive_duration: Int?
)

private data class XtVodStream(
    val name: String?,
    val stream_id: Int?,
    val stream_icon: String?,
    val category_id: String?,
    val container_extension: String?
)

private data class XtSeries(
    val name: String?,
    val series_id: Int?,
    val cover: String?,
    val category_id: String?
)

private data class XtEpisode(
    val id: String?,
    val episode_num: Int?,
    val title: String?,
    val season: Int?,
    val container_extension: String?
)

private data class XtSeriesInfo(val episodes: Map<String, List<XtEpisode>>?)

private data class XtShortEpg(val start: String?, val end: String?, val title: String?)
private data class XtShortEpgResponse(val epg_listings: List<XtShortEpg>?)

/**
 * عميل بسيط للتواصل مع أي بروفايدر يدعم Xtream Codes API القياسي (player_api.php).
 * يغطي: البث المباشر، الأفلام (VOD)، المسلسلات، ودليل البرامج المصغر (Short EPG).
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

    private fun base(host: String, port: String, user: String, pass: String) =
        "http://$host:$port/player_api.php?username=$user&password=$pass"

    fun fetchLiveChannels(host: String, port: String, user: String, pass: String): List<Channel> {
        val b = base(host, port, user, pass)

        val categories: List<XtCategory> = try {
            gson.fromJson(get("$b&action=get_live_categories"), object : TypeToken<List<XtCategory>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val catMap = categories.associate { (it.category_id ?: "") to (it.category_name ?: "General") }

        val streams: List<XtLiveStream> = try {
            gson.fromJson(get("$b&action=get_live_streams"), object : TypeToken<List<XtLiveStream>>() {}.type)
                ?: emptyList()
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
                category = catMap[s.category_id] ?: "General",
                hasArchive = (s.tv_archive ?: 0) == 1,
                archiveDurationMinutes = ((s.tv_archive_duration ?: 0) * 24 * 60)
            )
        }
    }

    fun fetchVodItems(host: String, port: String, user: String, pass: String): List<VodItem> {
        val b = base(host, port, user, pass)

        val categories: List<XtCategory> = try {
            gson.fromJson(get("$b&action=get_vod_categories"), object : TypeToken<List<XtCategory>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val catMap = categories.associate { (it.category_id ?: "") to (it.category_name ?: "Movies") }

        val streams: List<XtVodStream> = try {
            gson.fromJson(get("$b&action=get_vod_streams"), object : TypeToken<List<XtVodStream>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return streams.mapNotNull { s ->
            val id = s.stream_id ?: return@mapNotNull null
            val ext = s.container_extension ?: "mp4"
            VodItem(
                id = id.toString(),
                name = s.name ?: "Movie $id",
                streamUrl = "http://$host:$port/movie/$user/$pass/$id.$ext",
                logoUrl = s.stream_icon,
                category = catMap[s.category_id] ?: "Movies"
            )
        }
    }

    fun fetchSeriesList(host: String, port: String, user: String, pass: String): List<SeriesItem> {
        val b = base(host, port, user, pass)

        val categories: List<XtCategory> = try {
            gson.fromJson(get("$b&action=get_series_categories"), object : TypeToken<List<XtCategory>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val catMap = categories.associate { (it.category_id ?: "") to (it.category_name ?: "Series") }

        val series: List<XtSeries> = try {
            gson.fromJson(get("$b&action=get_series"), object : TypeToken<List<XtSeries>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return series.mapNotNull { s ->
            val id = s.series_id ?: return@mapNotNull null
            SeriesItem(
                id = id.toString(),
                name = s.name ?: "Series $id",
                coverUrl = s.cover,
                category = catMap[s.category_id] ?: "Series"
            )
        }
    }

    fun fetchSeriesEpisodes(host: String, port: String, user: String, pass: String, seriesId: String): List<Episode> {
        val json = get("${base(host, port, user, pass)}&action=get_series_info&series_id=$seriesId")
        val info = try {
            gson.fromJson(json, XtSeriesInfo::class.java)
        } catch (e: Exception) {
            null
        }

        val episodes = mutableListOf<Episode>()
        info?.episodes?.forEach { (_, list) ->
            list.forEach { e ->
                val id = e.id ?: return@forEach
                val ext = e.container_extension ?: "mp4"
                episodes.add(
                    Episode(
                        id = id,
                        title = e.title ?: "Episode",
                        streamUrl = "http://$host:$port/series/$user/$pass/$id.$ext",
                        season = e.season ?: 1,
                        episodeNumber = e.episode_num ?: 0
                    )
                )
            }
        }
        return episodes.sortedWith(compareBy({ it.season }, { it.episodeNumber }))
    }

    /** يجلب البرنامج الحالي والقادم لقناة معينة (Short EPG). */
    fun fetchShortEpg(host: String, port: String, user: String, pass: String, streamId: String): List<EpgProgram> {
        val json = get("${base(host, port, user, pass)}&action=get_short_epg&stream_id=$streamId&limit=2")
        val response = try {
            gson.fromJson(json, XtShortEpgResponse::class.java)
        } catch (e: Exception) {
            null
        }
        return response?.epg_listings.orEmpty().mapNotNull { listing ->
            val title = listing.title?.let { decodeBase64IfNeeded(it) } ?: return@mapNotNull null
            EpgProgram(title = title, start = listing.start ?: "", end = listing.end ?: "")
        }
    }

    /** رابط تشغيل الأرشيف (Catch-up) لعدد دقائق للخلف من الآن. */
    fun buildCatchupUrl(host: String, port: String, user: String, pass: String, streamId: String, minutesAgo: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MINUTE, -minutesAgo)
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd:HH-mm", java.util.Locale.US)
        val start = fmt.format(cal.time)
        val durationMinutes = minutesAgo
        return "http://$host:$port/timeshift/$user/$pass/$durationMinutes/$start/$streamId.m3u8"
    }

    private fun decodeBase64IfNeeded(text: String): String {
        return try {
            String(Base64.decode(text, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            text
        }
    }
}
