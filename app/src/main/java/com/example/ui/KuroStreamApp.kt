package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.player.VideoPlayerOverlay
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaDetailOverlay
import com.example.ui.screens.StreamHubScreen
import com.example.ui.screens.WatchlistScreen

private data class NavItemSpec(
    val tab: PrimaryTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun KuroStreamApp(
    viewModel: StreamingViewModel
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val watchlistEntities by viewModel.watchlistEntities.collectAsStateWithLifecycle()
    val watchProgressList by viewModel.watchProgressList.collectAsStateWithLifecycle()
    val customStreams by viewModel.customStreamEntities.collectAsStateWithLifecycle()
    val watchlistIds by viewModel.watchlistIds.collectAsStateWithLifecycle()
    val streamOverrides by viewModel.streamOverrides.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedMediaDetail.collectAsStateWithLifecycle()
    val isLoadingEpisodes by viewModel.isLoadingEpisodes.collectAsStateWithLifecycle()
    val activePlayback by viewModel.activePlayback.collectAsStateWithLifecycle()

    // Back press on non-home tabs navigates back to Home
    if (activePlayback == null && selectedDetail == null && currentTab != PrimaryTab.HOME) {
        BackHandler {
            viewModel.selectTab(PrimaryTab.HOME)
        }
    }

    val navItems = listOf(
        NavItemSpec(
            tab = PrimaryTab.HOME,
            labelRes = R.string.nav_home,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            tag = "nav_tab_home"
        ),
        NavItemSpec(
            tab = PrimaryTab.EXPLORE,
            labelRes = R.string.nav_explore,
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore,
            tag = "nav_tab_explore"
        ),
        NavItemSpec(
            tab = PrimaryTab.WATCHLIST,
            labelRes = R.string.nav_watchlist,
            selectedIcon = Icons.Filled.Bookmarks,
            unselectedIcon = Icons.Outlined.Bookmarks,
            tag = "nav_tab_watchlist"
        ),
        NavItemSpec(
            tab = PrimaryTab.STREAM_HUB,
            labelRes = R.string.nav_studio,
            selectedIcon = Icons.Filled.LiveTv,
            unselectedIcon = Icons.Outlined.LiveTv,
            tag = "nav_tab_stream_hub"
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("main_navigation_rail")
                    ) {
                        navItems.forEach { spec ->
                            val selected = currentTab == spec.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(spec.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                                        contentDescription = stringResource(spec.labelRes)
                                    )
                                },
                                label = { Text(stringResource(spec.labelRes)) },
                                modifier = Modifier.testTag(spec.tag)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        PrimaryTabContent(
                            currentTab = currentTab,
                            viewModel = viewModel,
                            catalogState = catalogState,
                            watchlistEntities = watchlistEntities,
                            watchProgressList = watchProgressList,
                            customStreams = customStreams,
                            watchlistIds = watchlistIds
                        )
                    }
                }
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            modifier = Modifier.testTag("main_bottom_navigation")
                        ) {
                            navItems.forEach { spec ->
                                val selected = currentTab == spec.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(spec.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                                            contentDescription = stringResource(spec.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(spec.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(spec.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        PrimaryTabContent(
                            currentTab = currentTab,
                            viewModel = viewModel,
                            catalogState = catalogState,
                            watchlistEntities = watchlistEntities,
                            watchProgressList = watchProgressList,
                            customStreams = customStreams,
                            watchlistIds = watchlistIds
                        )
                    }
                }
            }
        }

        // Detail Screen Overlay
        val detailItem = selectedDetail
        if (detailItem != null) {
            val savedWatchlistEntry = watchlistEntities.firstOrNull { it.mediaId == detailItem.id }
            val itemProgress = watchProgressList.firstOrNull { it.mediaId == detailItem.id }
            MediaDetailOverlay(
                media = detailItem,
                isInWatchlist = watchlistIds.contains(detailItem.id),
                isFavorite = savedWatchlistEntry?.isFavorite == true,
                isLoadingEpisodes = isLoadingEpisodes,
                customStreamOverride = streamOverrides[detailItem.id],
                savedProgress = itemProgress,
                onBack = { viewModel.closeMediaDetail() },
                onPlayMedia = { episode ->
                    viewModel.startPlayback(detailItem, episode)
                },
                onResumeProgress = { progress ->
                    viewModel.resumeFromProgress(progress)
                },
                onToggleWatchlist = { viewModel.toggleWatchlist(detailItem) },
                onToggleFavorite = { viewModel.toggleWatchlistFavorite(detailItem.id) },
                onSaveStreamOverride = { customUrl ->
                    viewModel.attachCustomStreamOverride(detailItem.id, customUrl)
                }
            )
        }

        // Active Fullscreen Media3 ExoPlayer Overlay
        val playbackSession = activePlayback
        if (playbackSession != null) {
            VideoPlayerOverlay(
                session = playbackSession,
                onBack = { posMs, durMs ->
                    viewModel.recordPlaybackPosition(posMs, durMs)
                    viewModel.closePlayer()
                },
                onProgressTick = { posMs, durMs ->
                    viewModel.recordPlaybackPosition(posMs, durMs)
                },
                onPlayNextEpisode = {
                    viewModel.playNextEpisode()
                }
            )
        }
    }
}

@Composable
private fun PrimaryTabContent(
    currentTab: PrimaryTab,
    viewModel: StreamingViewModel,
    catalogState: CatalogUiState,
    watchlistEntities: List<com.example.data.local.WatchlistEntity>,
    watchProgressList: List<com.example.data.local.WatchProgressEntity>,
    customStreams: List<com.example.data.local.CustomStreamEntity>,
    watchlistIds: Set<String>
) {
    when (currentTab) {
        PrimaryTab.HOME -> {
            HomeScreen(
                catalogState = catalogState,
                watchProgress = watchProgressList,
                customStreams = customStreams,
                watchlistIds = watchlistIds,
                onSelectCategory = viewModel::selectHomeCategory,
                onWatchMedia = { media -> viewModel.startPlayback(media) },
                onOpenDetails = viewModel::openMediaDetail,
                onToggleWatchlist = viewModel::toggleWatchlist,
                onResumeProgress = viewModel::resumeFromProgress,
                onRemoveProgress = viewModel::removeFromHistory,
                onRefresh = viewModel::refreshCatalogs
            )
        }

        PrimaryTab.EXPLORE -> {
            ExploreScreen(
                catalogState = catalogState,
                watchlistIds = watchlistIds,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSelectCategory = viewModel::selectExploreCategory,
                onOpenDetails = viewModel::openMediaDetail,
                onToggleWatchlist = viewModel::toggleWatchlist
            )
        }

        PrimaryTab.WATCHLIST -> {
            WatchlistScreen(
                watchlist = watchlistEntities,
                watchHistory = watchProgressList,
                onOpenSavedItem = { model ->
                    val richModel = viewModel.findMediaById(model.id) ?: model
                    viewModel.openMediaDetail(richModel)
                },
                onWatchSavedItem = { model ->
                    val richModel = viewModel.findMediaById(model.id) ?: model
                    viewModel.startPlayback(richModel)
                },
                onToggleFavorite = viewModel::toggleWatchlistFavorite,
                onRemoveWatchlist = viewModel::removeFromWatchlist,
                onResumeHistory = viewModel::resumeFromProgress,
                onRemoveHistory = viewModel::removeFromHistory,
                onExploreCatalog = { viewModel.selectTab(PrimaryTab.EXPLORE) }
            )
        }

        PrimaryTab.STREAM_HUB -> {
            StreamHubScreen(
                customStreams = customStreams,
                onPlayTemporaryStream = viewModel::playDirectTemporaryStream,
                onSaveStream = viewModel::saveAndOptionallyPlayCustomStream,
                onPlaySavedCustom = { model -> viewModel.startPlayback(model) },
                onDeleteCustom = viewModel::deleteCustomStream
            )
        }
    }
}
