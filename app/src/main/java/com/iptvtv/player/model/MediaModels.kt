package com.iptvtv.player.model

data class VodItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "Movies"
)

data class SeriesItem(
    val id: String,
    val name: String,
    val coverUrl: String? = null,
    val category: String = "Series"
)

data class Episode(
    val id: String,
    val title: String,
    val streamUrl: String,
    val season: Int,
    val episodeNumber: Int
)

data class EpgProgram(
    val title: String,
    val start: String,
    val end: String
)

/** غلاف عام لأي عنصر قابل للعرض كبطاقة (قناة/فيلم/مسلسل/حلقة). */
data class CardData(
    val title: String,
    val subtitle: String,
    val imageUrl: String?,
    val payload: Any
)
