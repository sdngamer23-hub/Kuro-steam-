package com.example.data.repository

import com.example.data.local.CustomStreamEntity
import com.example.data.local.StreamOverrideEntity
import com.example.data.local.StreamingDao
import com.example.data.local.WatchProgressEntity
import com.example.data.local.WatchlistDao
import com.example.data.local.WatchlistEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.data.remote.JikanAnimeDto
import com.example.data.remote.JikanApiService
import com.example.data.remote.OpenCinemaCatalog
import com.example.data.remote.TvMazeApiService
import com.example.data.remote.TvMazeShowDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow

class StreamingRepository(
    private val watchlistDao: WatchlistDao,
    private val dao: StreamingDao,
    private val jikanApi: JikanApiService,
    private val tvMazeApi: TvMazeApiService
) {

    val watchlistFlow: Flow<List<WatchlistEntity>> = watchlistDao.observeAllWatchlist()
    val watchlistCountFlow: Flow<Int> = watchlistDao.observeWatchlistCount()
    val watchProgressFlow: Flow<List<WatchProgressEntity>> = dao.observeWatchProgress()
    val customStreamsFlow: Flow<List<CustomStreamEntity>> = dao.observeCustomStreams()
    val streamOverridesFlow: Flow<List<StreamOverrideEntity>> = dao.observeStreamOverrides()

    fun observeWatchlistByCategory(category: MediaCategory): Flow<List<WatchlistEntity>> {
        return if (category == MediaCategory.ALL) {
            watchlistDao.observeAllWatchlist()
        } else {
            watchlistDao.observeWatchlistByCategory(category.name)
        }
    }

    fun searchWatchlist(query: String): Flow<List<WatchlistEntity>> =
        watchlistDao.searchWatchlist(query)

    fun observeIsInWatchlist(mediaId: String): Flow<Boolean> =
        watchlistDao.observeIsInWatchlist(mediaId)

    fun getFeaturedOpenCinema(): List<MediaItemModel> = OpenCinemaCatalog.featuredPremieres

    suspend fun fetchTopAnimeCatalog(): Result<List<MediaItemModel>> = runCatching {
        val response = jikanApi.getTopAnime(limit = 16)
        response.data.mapIndexed { idx, dto -> dto.toMediaItemModel(idx, MediaCategory.ANIME) }
    }

    suspend fun fetchAnimeMoviesCatalog(): Result<List<MediaItemModel>> = runCatching {
        val response = jikanApi.getTopAnimeMovies(limit = 10)
        response.data.mapIndexed { idx, dto -> dto.toMediaItemModel(idx + 3, MediaCategory.MOVIES) }
    }

    suspend fun fetchPopularWebSeriesCatalog(): Result<List<MediaItemModel>> = runCatching {
        val shows = tvMazeApi.getPopularShows(page = 0).take(18)
        shows.mapIndexed { idx, dto -> dto.toMediaItemModel(idx) }
    }

    suspend fun searchAllCatalogs(query: String): Result<List<MediaItemModel>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@runCatching emptyList()

        coroutineScope {
            val localMatches = OpenCinemaCatalog.featuredPremieres.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.genres.any { g -> g.contains(trimmed, ignoreCase = true) }
            }

            val animeDeferred = async {
                runCatching {
                    jikanApi.searchAnime(trimmed, limit = 10).data.mapIndexed { i, dto ->
                        val cat = if (dto.type.equals("Movie", ignoreCase = true)) {
                            MediaCategory.MOVIES
                        } else {
                            MediaCategory.ANIME
                        }
                        dto.toMediaItemModel(i, cat)
                    }
                }.getOrDefault(emptyList())
            }

            val seriesDeferred = async {
                runCatching {
                    tvMazeApi.searchShows(trimmed).take(10).mapIndexed { i, wrapper ->
                        wrapper.show.toMediaItemModel(i)
                    }
                }.getOrDefault(emptyList())
            }

            localMatches + animeDeferred.await() + seriesDeferred.await()
        }
    }

    suspend fun fetchEpisodesForMedia(media: MediaItemModel): List<EpisodeItem> {
        if (media.id.startsWith("tvmaze_")) {
            val showId = media.id.removePrefix("tvmaze_").toIntOrNull()
            if (showId != null) {
                val remoteEpisodes = runCatching {
                    tvMazeApi.getShowEpisodes(showId).take(24)
                }.getOrDefault(emptyList())
                if (remoteEpisodes.isNotEmpty()) {
                    return remoteEpisodes.mapIndexed { idx, ep ->
                        EpisodeItem(
                            id = "tvmaze_ep_${ep.id}",
                            number = ep.number ?: (idx + 1),
                            season = ep.season ?: 1,
                            title = ep.name,
                            durationMinutes = ep.runtime ?: 42,
                            summary = stripHtml(ep.summary).ifBlank {
                                "Season ${ep.season ?: 1} • Episode ${ep.number ?: (idx + 1)}"
                            },
                            thumbnailUrl = ep.image?.original ?: ep.image?.medium ?: media.backdropUrl,
                            streamUrl = OpenCinemaCatalog.fallbackStreamForIndex(idx)
                        )
                    }
                }
            }
        }
        return media.episodes
    }

    suspend fun toggleWatchlist(item: MediaItemModel) {
        val exists = watchlistDao.isInWatchlist(item.id)
        if (exists) {
            watchlistDao.deleteByMediaId(item.id)
        } else {
            watchlistDao.upsertWatchlistItem(
                WatchlistEntity(
                    mediaId = item.id,
                    title = item.title,
                    category = item.category.name,
                    posterUrl = item.posterUrl,
                    backdropUrl = item.backdropUrl,
                    rating = item.rating,
                    releaseYear = item.releaseYear,
                    genresCsv = item.genres.joinToString(", "),
                    studioOrNetwork = item.studioOrNetwork,
                    streamUrl = item.streamUrl,
                    synopsis = item.synopsis,
                    isFavorite = false
                )
            )
        }
    }

    suspend fun toggleWatchlistFavorite(mediaId: String) {
        val current = watchlistDao.getWatchlistItemById(mediaId) ?: return
        watchlistDao.updateFavoriteFlag(mediaId, !current.isFavorite)
    }

    suspend fun removeFromWatchlist(mediaId: String) {
        watchlistDao.deleteByMediaId(mediaId)
    }

    suspend fun clearWatchlist() {
        watchlistDao.clearWatchlist()
    }

    suspend fun savePlaybackProgress(
        media: MediaItemModel,
        episode: EpisodeItem?,
        streamUrl: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (positionMs <= 1000L && durationMs <= 0L) return
        dao.upsertWatchProgress(
            WatchProgressEntity(
                mediaId = media.id,
                title = media.title,
                episodeId = episode?.id ?: "${media.id}_main",
                episodeTitle = episode?.title ?: media.subtitle.ifBlank { "Feature Stream" },
                episodeNumber = episode?.number ?: 1,
                category = media.category.name,
                posterUrl = media.posterUrl,
                backdropUrl = media.backdropUrl,
                streamUrl = streamUrl,
                positionMs = positionMs,
                durationMs = durationMs.coerceAtLeast(1L),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeWatchProgress(mediaId: String) {
        dao.deleteWatchProgress(mediaId)
    }

    suspend fun addCustomStream(
        title: String,
        category: MediaCategory,
        streamUrl: String,
        posterUrl: String,
        synopsis: String
    ): MediaItemModel {
        val cleanTitle = title.trim().ifBlank { "Custom Stream" }
        val cleanUrl = streamUrl.trim()
        val cleanPoster = posterUrl.trim().ifBlank {
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg"
        }
        val cleanSynopsis = synopsis.trim().ifBlank {
            "User-added custom stream source ready for instant playback."
        }
        val id = dao.insertCustomStream(
            CustomStreamEntity(
                title = cleanTitle,
                category = category.name,
                streamUrl = cleanUrl,
                posterUrl = cleanPoster,
                synopsis = cleanSynopsis
            )
        )
        return CustomStreamEntity(
            id = id,
            title = cleanTitle,
            category = category.name,
            streamUrl = cleanUrl,
            posterUrl = cleanPoster,
            synopsis = cleanSynopsis
        ).toMediaItemModel()
    }

    suspend fun deleteCustomStream(id: Long) {
        dao.deleteCustomStream(id)
    }

    suspend fun setCustomStreamOverride(mediaId: String, customUrl: String) {
        dao.upsertStreamOverride(
            StreamOverrideEntity(
                mediaId = mediaId,
                customStreamUrl = customUrl.trim()
            )
        )
    }

    private fun JikanAnimeDto.toMediaItemModel(
        index: Int,
        defaultCategory: MediaCategory
    ): MediaItemModel {
        val displayTitle = (titleEnglish?.takeIf { it.isNotBlank() } ?: title)
        val poster = images?.jpg?.largeImageUrl ?: images?.jpg?.imageUrl.orEmpty()
        val backdrop = trailer?.images?.maximumImageUrl
            ?: trailer?.images?.largeImageUrl
            ?: poster
        val epCount = (episodes ?: 12).coerceIn(1, 12)
        val stream = OpenCinemaCatalog.fallbackStreamForIndex(index)
        val studioName = studios.firstOrNull()?.name ?: "Anime Studio"
        val cleanSynopsis = synopsis?.replace("[Written by MAL Rewrite]", "")?.trim()
            ?: "Acclaimed anime production with dynamic storytelling and vivid animation."

        val episodeList = (1..epCount.coerceAtMost(8)).map { epNum ->
            EpisodeItem(
                id = "jikan_${malId}_ep_$epNum",
                number = epNum,
                season = 1,
                title = if (defaultCategory == MediaCategory.MOVIES && epCount == 1) {
                    "$displayTitle — Feature Preview Stream"
                } else {
                    "Episode $epNum"
                },
                durationMinutes = if (defaultCategory == MediaCategory.MOVIES) 110 else 24,
                summary = "Preview & open-cinema showcase stream for $displayTitle (Ep $epNum). You can also attach your own HLS/MP4 source.",
                thumbnailUrl = backdrop,
                streamUrl = OpenCinemaCatalog.fallbackStreamForIndex(index + epNum)
            )
        }

        return MediaItemModel(
            id = "jikan_$malId",
            title = displayTitle,
            subtitle = "$studioName • ${episodes ?: "?"} eps",
            category = defaultCategory,
            rating = score ?: 8.2,
            releaseYear = year?.toString() ?: "2024",
            genres = genres.map { it.name }.take(3).ifEmpty { listOf("Anime", "Action") },
            synopsis = cleanSynopsis,
            posterUrl = poster,
            backdropUrl = backdrop,
            streamUrl = stream,
            isLiveStreamReady = false,
            studioOrNetwork = studioName,
            episodes = episodeList
        )
    }

    private fun TvMazeShowDto.toMediaItemModel(index: Int): MediaItemModel {
        val poster = image?.original ?: image?.medium.orEmpty()
        val year = premiered?.take(4) ?: "2023"
        val networkName = webChannel?.name ?: network?.name ?: "Original Series"
        val stream = OpenCinemaCatalog.fallbackStreamForIndex(index + 2)
        val cleanSynopsis = stripHtml(summary).ifBlank {
            "Critically acclaimed serialized drama and adventure series."
        }

        return MediaItemModel(
            id = "tvmaze_$id",
            title = name,
            subtitle = "$networkName • Web Series",
            category = MediaCategory.WEB_SERIES,
            rating = rating?.average ?: 8.0,
            releaseYear = year,
            genres = genres.take(3).ifEmpty { listOf("Drama", "Series") },
            synopsis = cleanSynopsis,
            posterUrl = poster,
            backdropUrl = poster,
            streamUrl = stream,
            isLiveStreamReady = false,
            studioOrNetwork = networkName,
            episodes = listOf(
                EpisodeItem(
                    id = "tvmaze_${id}_ep_1",
                    number = 1,
                    season = 1,
                    title = "Pilot Premiere",
                    durationMinutes = 45,
                    summary = cleanSynopsis,
                    thumbnailUrl = poster,
                    streamUrl = stream
                )
            )
        )
    }

    companion object {
        fun stripHtml(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return raw
                .replace(Regex("<[^>]*>"), "")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim()
        }
    }
}

fun CustomStreamEntity.toMediaItemModel(): MediaItemModel {
    val parsedCategory = runCatching { MediaCategory.valueOf(category) }
        .getOrDefault(MediaCategory.MOVIES)
    return MediaItemModel(
        id = "custom_$id",
        title = title,
        subtitle = "Custom ${parsedCategory.displayName} Stream",
        category = parsedCategory,
        rating = 9.0,
        releaseYear = "Custom",
        genres = listOf(parsedCategory.displayName, "Direct URL"),
        synopsis = synopsis,
        posterUrl = posterUrl,
        backdropUrl = posterUrl,
        streamUrl = streamUrl,
        isLiveStreamReady = true,
        studioOrNetwork = "User Stream Hub",
        episodes = listOf(
            EpisodeItem(
                id = "custom_${id}_ep1",
                number = 1,
                season = 1,
                title = title,
                durationMinutes = 0,
                summary = synopsis,
                thumbnailUrl = posterUrl,
                streamUrl = streamUrl
            )
        ),
        isCustomSource = true
    )
}

fun WatchlistEntity.toMediaItemModel(): MediaItemModel {
    val parsedCategory = runCatching { MediaCategory.valueOf(category) }
        .getOrDefault(MediaCategory.MOVIES)
    val parsedGenres = genresCsv.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { listOf(parsedCategory.displayName) }

    val subLabel = if (studioOrNetwork.isNotBlank()) {
        "$studioOrNetwork • ${parsedCategory.displayName}"
    } else {
        "${parsedCategory.displayName} • Saved"
    }

    return MediaItemModel(
        id = mediaId,
        title = title,
        subtitle = subLabel,
        category = parsedCategory,
        rating = rating,
        releaseYear = releaseYear,
        genres = parsedGenres,
        synopsis = synopsis,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        streamUrl = streamUrl,
        isLiveStreamReady = mediaId.startsWith("open_") || mediaId.startsWith("custom_"),
        studioOrNetwork = studioOrNetwork,
        episodes = listOf(
            EpisodeItem(
                id = "${mediaId}_saved_ep1",
                number = 1,
                season = 1,
                title = title,
                durationMinutes = 24,
                summary = synopsis,
                thumbnailUrl = backdropUrl,
                streamUrl = streamUrl
            )
        ),
        isCustomSource = mediaId.startsWith("custom_")
    )
}
