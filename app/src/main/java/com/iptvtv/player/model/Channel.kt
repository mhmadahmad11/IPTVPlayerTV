package com.iptvtv.player.model

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "General",
    var isFavorite: Boolean = false,
    val hasArchive: Boolean = false,
    val archiveDurationMinutes: Int = 0
)
