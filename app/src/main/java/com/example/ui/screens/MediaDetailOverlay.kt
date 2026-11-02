package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.local.WatchProgressEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaItemModel

/**
 * Full-screen detail overlay wrapper delegating to [MediaDetailScreen].
 */
@Composable
fun MediaDetailOverlay(
    media: MediaItemModel,
    isInWatchlist: Boolean,
    isLoadingEpisodes: Boolean,
    customStreamOverride: String?,
    onBack: () -> Unit,
    onPlayMedia: (EpisodeItem?) -> Unit,
    onToggleWatchlist: () -> Unit,
    onSaveStreamOverride: (String) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    savedProgress: WatchProgressEntity? = null,
    onResumeProgress: ((WatchProgressEntity) -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    MediaDetailScreen(
        media = media,
        isInWatchlist = isInWatchlist,
        isFavorite = isFavorite,
        isLoadingEpisodes = isLoadingEpisodes,
        customStreamOverride = customStreamOverride,
        savedProgress = savedProgress,
        onBack = onBack,
        onPlayMedia = onPlayMedia,
        onResumeProgress = onResumeProgress,
        onToggleWatchlist = onToggleWatchlist,
        onToggleFavorite = onToggleFavorite,
        onSaveStreamOverride = onSaveStreamOverride,
        modifier = modifier.testTag("media_detail_overlay")
    )
}
