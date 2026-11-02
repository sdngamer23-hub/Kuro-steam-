package com.example.data.model

enum class MediaCategory(val displayName: String) {
    ALL("All"),
    ANIME("Anime"),
    WEB_SERIES("Web Series"),
    MOVIES("Movies")
}

data class EpisodeItem(
    val id: String,
    val number: Int,
    val season: Int = 1,
    val title: String,
    val durationMinutes: Int,
    val summary: String,
    val thumbnailUrl: String,
    val streamUrl: String
)

data class MediaItemModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MediaCategory,
    val rating: Double,
    val releaseYear: String,
    val genres: List<String>,
    val synopsis: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val isLiveStreamReady: Boolean = true,
    val studioOrNetwork: String = "",
    val episodes: List<EpisodeItem> = emptyList(),
    val isCustomSource: Boolean = false
)
