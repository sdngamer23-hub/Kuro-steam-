package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomStreamEntity
import com.example.data.local.WatchProgressEntity
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.data.repository.toMediaItemModel
import com.example.ui.CatalogUiState
import com.example.ui.components.CategoryFilterBar
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.HeroSpotlightBanner
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.SectionHeader

@Composable
fun HomeScreen(
    catalogState: CatalogUiState,
    watchProgress: List<WatchProgressEntity>,
    customStreams: List<CustomStreamEntity>,
    watchlistIds: Set<String>,
    onSelectCategory: (MediaCategory) -> Unit,
    onWatchMedia: (MediaItemModel) -> Unit,
    onOpenDetails: (MediaItemModel) -> Unit,
    onToggleWatchlist: (MediaItemModel) -> Unit,
    onResumeProgress: (WatchProgressEntity) -> Unit,
    onRemoveProgress: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCategory = catalogState.selectedHomeCategory

    val filteredPremieres = catalogState.featuredPremieres.filter {
        activeCategory == MediaCategory.ALL || it.category == activeCategory
    }
    val heroItem = filteredPremieres.firstOrNull()
        ?: catalogState.featuredPremieres.first()

    val customModels = customStreams.map { it.toMediaItemModel() }.filter {
        activeCategory == MediaCategory.ALL || it.category == activeCategory
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroSpotlightBanner(
                featuredItem = heroItem,
                isInWatchlist = watchlistIds.contains(heroItem.id),
                onWatchNow = { onWatchMedia(heroItem) },
                onToggleWatchlist = { onToggleWatchlist(heroItem) },
                onOpenDetails = { onOpenDetails(heroItem) }
            )
        }

        item {
            CategoryFilterBar(
                selectedCategory = activeCategory,
                onSelectCategory = onSelectCategory
            )
        }

        if (catalogState.errorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = catalogState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh catalog",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Continue Watching Carousel
        if (watchProgress.isNotEmpty()) {
            item {
                Column {
                    SectionHeader(
                        title = stringResource(R.string.section_continue_watching),
                        accentBadge = "${watchProgress.size} Active"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(watchProgress, key = { it.mediaId }) { progress ->
                            ContinueWatchingCard(
                                progress = progress,
                                onResume = { onResumeProgress(progress) },
                                onRemove = { onRemoveProgress(progress.mediaId) }
                            )
                        }
                    }
                }
            }
        }

        // User Custom Streams (if added)
        if (customModels.isNotEmpty()) {
            item {
                MediaRailSection(
                    title = stringResource(R.string.section_custom_streams),
                    badge = "LOCAL",
                    items = customModels,
                    watchlistIds = watchlistIds,
                    onItemClick = onOpenDetails,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Open Cinema & Full Playable Films/Series
        if (filteredPremieres.isNotEmpty()) {
            item {
                MediaRailSection(
                    title = stringResource(R.string.section_cinema_premieres),
                    badge = "INSTANT HD",
                    items = filteredPremieres,
                    watchlistIds = watchlistIds,
                    onItemClick = onOpenDetails,
                    onToggleWatchlist = onToggleWatchlist
                )
            }
        }

        // Trending Anime Rail
        if (activeCategory == MediaCategory.ALL || activeCategory == MediaCategory.ANIME) {
            if (catalogState.topAnime.isNotEmpty()) {
                item {
                    MediaRailSection(
                        title = stringResource(R.string.section_top_anime),
                        badge = "JIKAN LIVE",
                        items = catalogState.topAnime,
                        watchlistIds = watchlistIds,
                        onItemClick = onOpenDetails,
                        onToggleWatchlist = onToggleWatchlist
                    )
                }
            }
        }

        // Popular Web Series Rail
        if (activeCategory == MediaCategory.ALL || activeCategory == MediaCategory.WEB_SERIES) {
            if (catalogState.popularSeries.isNotEmpty()) {
                item {
                    MediaRailSection(
                        title = stringResource(R.string.section_popular_series),
                        badge = "SERIES",
                        items = catalogState.popularSeries,
                        watchlistIds = watchlistIds,
                        onItemClick = onOpenDetails,
                        onToggleWatchlist = onToggleWatchlist
                    )
                }
            }
        }

        // Movies Rail
        if (activeCategory == MediaCategory.ALL || activeCategory == MediaCategory.MOVIES) {
            val movieItems = (catalogState.featuredPremieres.filter { it.category == MediaCategory.MOVIES } +
                catalogState.animeMovies).distinctBy { it.id }
            if (movieItems.isNotEmpty()) {
                item {
                    MediaRailSection(
                        title = "Box Office & Anime Films",
                        badge = "MOVIES",
                        items = movieItems,
                        watchlistIds = watchlistIds,
                        onItemClick = onOpenDetails,
                        onToggleWatchlist = onToggleWatchlist
                    )
                }
            }
        }

        if (catalogState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun MediaRailSection(
    title: String,
    badge: String,
    items: List<MediaItemModel>,
    watchlistIds: Set<String>,
    onItemClick: (MediaItemModel) -> Unit,
    onToggleWatchlist: (MediaItemModel) -> Unit
) {
    Column {
        SectionHeader(title = title, accentBadge = badge)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { media ->
                MediaPosterCard(
                    item = media,
                    isInWatchlist = watchlistIds.contains(media.id),
                    onClick = { onItemClick(media) },
                    onToggleWatchlist = { onToggleWatchlist(media) }
                )
            }
        }
    }
}
