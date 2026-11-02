package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomStreamEntity
import com.example.data.local.StreamOverrideEntity
import com.example.data.local.WatchProgressEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.data.repository.StreamingRepository
import com.example.data.repository.toMediaItemModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PrimaryTab {
    HOME,
    EXPLORE,
    WATCHLIST,
    STREAM_HUB
}

data class ActivePlaybackSession(
    val media: MediaItemModel,
    val episode: EpisodeItem?,
    val streamUrl: String,
    val startPositionMs: Long = 0L
)

data class CatalogUiState(
    val isLoading: Boolean = true,
    val selectedHomeCategory: MediaCategory = MediaCategory.ALL,
    val selectedExploreCategory: MediaCategory = MediaCategory.ALL,
    val featuredPremieres: List<MediaItemModel> = emptyList(),
    val topAnime: List<MediaItemModel> = emptyList(),
    val animeMovies: List<MediaItemModel> = emptyList(),
    val popularSeries: List<MediaItemModel> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<MediaItemModel> = emptyList(),
    val errorMessage: String? = null
)

class StreamingViewModel(
    private val repository: StreamingRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(PrimaryTab.HOME)
    val currentTab: StateFlow<PrimaryTab> = _currentTab.asStateFlow()

    private val _selectedMediaDetail = MutableStateFlow<MediaItemModel?>(null)
    val selectedMediaDetail: StateFlow<MediaItemModel?> = _selectedMediaDetail.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    private val _activePlayback = MutableStateFlow<ActivePlaybackSession?>(null)
    val activePlayback: StateFlow<ActivePlaybackSession?> = _activePlayback.asStateFlow()

    private val _catalogState = MutableStateFlow(
        CatalogUiState(
            isLoading = true,
            featuredPremieres = repository.getFeaturedOpenCinema()
        )
    )
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    val watchlistEntities: StateFlow<List<WatchlistEntity>> = repository.watchlistFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchProgressList: StateFlow<List<WatchProgressEntity>> = repository.watchProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customStreamEntities: StateFlow<List<CustomStreamEntity>> = repository.customStreamsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streamOverrides: StateFlow<Map<String, String>> = repository.streamOverridesFlow
        .combine(MutableStateFlow(Unit)) { list: List<StreamOverrideEntity>, _ ->
            list.associate { it.mediaId to it.customStreamUrl }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val watchlistIds: StateFlow<Set<String>> = repository.watchlistFlow
        .combine(MutableStateFlow(Unit)) { list, _ ->
            list.map { it.mediaId }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var searchJob: Job? = null

    init {
        refreshCatalogs()
    }

    fun refreshCatalogs() {
        viewModelScope.launch {
            _catalogState.update { it.copy(isLoading = true, errorMessage = null) }

            val animeRes = repository.fetchTopAnimeCatalog()
            val seriesRes = repository.fetchPopularWebSeriesCatalog()
            val moviesRes = repository.fetchAnimeMoviesCatalog()

            val animeList = animeRes.getOrDefault(emptyList())
            val seriesList = seriesRes.getOrDefault(emptyList())
            val moviesList = moviesRes.getOrDefault(emptyList())

            val anyRemoteLoaded = animeList.isNotEmpty() || seriesList.isNotEmpty() || moviesList.isNotEmpty()

            _catalogState.update { state ->
                state.copy(
                    isLoading = false,
                    topAnime = animeList,
                    popularSeries = seriesList,
                    animeMovies = moviesList,
                    errorMessage = if (!anyRemoteLoaded) {
                        "Live catalog offline — Open Cinema films & custom streams remain ready to watch."
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun selectTab(tab: PrimaryTab) {
        _currentTab.value = tab
    }

    fun selectHomeCategory(category: MediaCategory) {
        _catalogState.update { it.copy(selectedHomeCategory = category) }
    }

    fun selectExploreCategory(category: MediaCategory) {
        _catalogState.update { it.copy(selectedExploreCategory = category) }
    }

    fun onSearchQueryChange(newQuery: String) {
        _catalogState.update { it.copy(searchQuery = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _catalogState.update { it.copy(isSearching = false, searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            _catalogState.update { it.copy(isSearching = true) }
            delay(350)
            val result = repository.searchAllCatalogs(newQuery)
            _catalogState.update {
                it.copy(
                    isSearching = false,
                    searchResults = result.getOrDefault(emptyList())
                )
            }
        }
    }

    fun openMediaDetail(media: MediaItemModel) {
        _selectedMediaDetail.value = media
        if (media.id.startsWith("tvmaze_") && media.episodes.size <= 1) {
            viewModelScope.launch {
                _isLoadingEpisodes.value = true
                val fullEpisodes = repository.fetchEpisodesForMedia(media)
                if (_selectedMediaDetail.value?.id == media.id && fullEpisodes.isNotEmpty()) {
                    _selectedMediaDetail.value = media.copy(episodes = fullEpisodes)
                }
                _isLoadingEpisodes.value = false
            }
        }
    }

    fun closeMediaDetail() {
        _selectedMediaDetail.value = null
    }

    fun startPlayback(
        media: MediaItemModel,
        episode: EpisodeItem? = media.episodes.firstOrNull(),
        resumePositionMs: Long? = null
    ) {
        val overrideUrl = streamOverrides.value[media.id]
        val resolvedUrl = overrideUrl?.takeIf { it.isNotBlank() }
            ?: episode?.streamUrl?.takeIf { it.isNotBlank() }
            ?: media.streamUrl

        val savedProgress = watchProgressList.value.firstOrNull {
            it.mediaId == media.id && (episode == null || it.episodeId == episode.id)
        }
        val startPos = resumePositionMs ?: savedProgress?.positionMs ?: 0L

        _activePlayback.value = ActivePlaybackSession(
            media = media,
            episode = episode,
            streamUrl = resolvedUrl,
            startPositionMs = startPos
        )
    }

    fun resumeFromProgress(progress: WatchProgressEntity) {
        val allKnown = buildAllKnownMediaList()
        val matched = allKnown.firstOrNull { it.id == progress.mediaId }
        val media = matched ?: MediaItemModel(
            id = progress.mediaId,
            title = progress.title,
            subtitle = progress.episodeTitle,
            category = runCatching { MediaCategory.valueOf(progress.category) }
                .getOrDefault(MediaCategory.MOVIES),
            rating = 8.5,
            releaseYear = "Saved",
            genres = listOf(progress.category),
            synopsis = "Resuming saved stream session.",
            posterUrl = progress.posterUrl,
            backdropUrl = progress.backdropUrl,
            streamUrl = progress.streamUrl,
            isLiveStreamReady = true
        )
        val matchedEp = media.episodes.firstOrNull { it.id == progress.episodeId }
            ?: EpisodeItem(
                id = progress.episodeId,
                number = progress.episodeNumber,
                title = progress.episodeTitle,
                durationMinutes = (progress.durationMs / 60000L).toInt().coerceAtLeast(1),
                summary = "",
                thumbnailUrl = progress.backdropUrl,
                streamUrl = progress.streamUrl
            )

        _activePlayback.value = ActivePlaybackSession(
            media = media,
            episode = matchedEp,
            streamUrl = progress.streamUrl,
            startPositionMs = progress.positionMs
        )
    }

    fun playNextEpisode() {
        val session = _activePlayback.value ?: return
        val episodes = session.media.episodes
        if (episodes.size <= 1) return
        val currentIdx = episodes.indexOfFirst { it.id == session.episode?.id }
        if (currentIdx in 0 until episodes.lastIndex) {
            val nextEp = episodes[currentIdx + 1]
            startPlayback(media = session.media, episode = nextEp, resumePositionMs = 0L)
        }
    }

    fun recordPlaybackPosition(positionMs: Long, durationMs: Long) {
        val session = _activePlayback.value ?: return
        viewModelScope.launch {
            repository.savePlaybackProgress(
                media = session.media,
                episode = session.episode,
                streamUrl = session.streamUrl,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun closePlayer() {
        _activePlayback.value = null
    }

    fun toggleWatchlist(media: MediaItemModel) {
        viewModelScope.launch {
            repository.toggleWatchlist(media)
        }
    }

    fun toggleWatchlistFavorite(mediaId: String) {
        viewModelScope.launch {
            repository.toggleWatchlistFavorite(mediaId)
        }
    }

    fun removeFromWatchlist(mediaId: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(mediaId)
        }
    }

    fun clearWatchlist() {
        viewModelScope.launch {
            repository.clearWatchlist()
        }
    }

    fun removeFromHistory(mediaId: String) {
        viewModelScope.launch {
            repository.removeWatchProgress(mediaId)
        }
    }

    fun saveAndOptionallyPlayCustomStream(
        title: String,
        category: MediaCategory,
        streamUrl: String,
        posterUrl: String,
        synopsis: String,
        playImmediately: Boolean
    ) {
        if (streamUrl.isBlank()) return
        viewModelScope.launch {
            val created = repository.addCustomStream(
                title = title,
                category = category,
                streamUrl = streamUrl,
                posterUrl = posterUrl,
                synopsis = synopsis
            )
            if (playImmediately) {
                startPlayback(created, created.episodes.firstOrNull(), 0L)
            }
        }
    }

    fun playDirectTemporaryStream(title: String, category: MediaCategory, streamUrl: String) {
        val cleanUrl = streamUrl.trim()
        if (cleanUrl.isBlank()) return
        val tempMedia = MediaItemModel(
            id = "direct_${cleanUrl.hashCode()}",
            title = title.trim().ifBlank { "Direct Stream Session" },
            subtitle = "Instant Direct Stream",
            category = category,
            rating = 9.0,
            releaseYear = "Live",
            genres = listOf(category.displayName, "Direct Stream"),
            synopsis = "Direct MP4 / HLS stream playback.",
            posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            streamUrl = cleanUrl,
            isLiveStreamReady = true,
            isCustomSource = true
        )
        startPlayback(tempMedia, null, 0L)
    }

    fun deleteCustomStream(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomStream(id)
        }
    }

    fun attachCustomStreamOverride(mediaId: String, customStreamUrl: String) {
        val clean = customStreamUrl.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            repository.setCustomStreamOverride(mediaId, clean)
            val currentDetail = _selectedMediaDetail.value
            if (currentDetail != null && currentDetail.id == mediaId) {
                _selectedMediaDetail.value = currentDetail.copy(
                    streamUrl = clean,
                    isLiveStreamReady = true
                )
            }
        }
    }

    fun findMediaById(mediaId: String): MediaItemModel? {
        return buildAllKnownMediaList().firstOrNull { it.id == mediaId }
    }

    private fun buildAllKnownMediaList(): List<MediaItemModel> {
        val state = _catalogState.value
        val customModels = customStreamEntities.value.map { it.toMediaItemModel() }
        val savedModels = watchlistEntities.value.map { it.toMediaItemModel() }
        return (customModels + state.featuredPremieres + state.topAnime + state.popularSeries + state.animeMovies + savedModels)
            .distinctBy { it.id }
    }

    class Factory(private val repository: StreamingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StreamingViewModel(repository) as T
        }
    }
}
