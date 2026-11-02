package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.WatchProgressEntity
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.ui.components.RatingBadge
import com.example.ui.components.formatMillis
import java.util.Locale

private const val SYNOPSIS_COLLAPSE_THRESHOLD = 180

/**
 * Adaptive detail screen displaying the poster artwork, atmospheric backdrop,
 * structured metadata (rating, release year, studio/network, format/runtime, genres),
 * expandable synopsis/description, watchlist controls, and episode guide for a
 * selected movie, anime, or web series entry.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDetailScreen(
    media: MediaItemModel,
    isInWatchlist: Boolean,
    isFavorite: Boolean = false,
    isLoadingEpisodes: Boolean = false,
    customStreamOverride: String? = null,
    savedProgress: WatchProgressEntity? = null,
    onBack: () -> Unit,
    onPlayMedia: (EpisodeItem?) -> Unit,
    onResumeProgress: ((WatchProgressEntity) -> Unit)? = null,
    onToggleWatchlist: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onSaveStreamOverride: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showAttachDialog by rememberSaveable { mutableStateOf(false) }
    var overrideUrlInput by rememberSaveable(customStreamOverride) {
        mutableStateOf(customStreamOverride.orEmpty())
    }
    var isSynopsisExpanded by rememberSaveable(media.id) { mutableStateOf(false) }

    if (showAttachDialog) {
        AttachCustomStreamDialog(
            mediaTitle = media.title,
            urlInput = overrideUrlInput,
            onUrlInputChange = { overrideUrlInput = it },
            onConfirm = {
                onSaveStreamOverride(overrideUrlInput)
                showAttachDialog = false
            },
            onDismiss = { showAttachDialog = false }
        )
    }

    val hasCustomOverride = !customStreamOverride.isNullOrBlank()
    val studioLabel = media.studioOrNetwork.ifBlank {
        stringResource(R.string.detail_studio_fallback)
    }
    val runtimeOrEpisodesLabel = formatRuntimeOrEpisodes(
        media = media,
        featureFallback = stringResource(R.string.detail_feature_film),
        episodesWord = stringResource(R.string.detail_episodes)
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("media_detail_screen")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = maxWidth >= 600.dp
            val contentMaxWidth = if (isWideLayout) 920.dp else 700.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .testTag("media_detail_scroll_list"),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // 1. Atmospheric Backdrop + Asymmetric Foreground Poster Header
                item(key = "hero_poster_header") {
                    DetailHeroPosterHeader(
                        media = media,
                        isWideLayout = isWideLayout,
                        contentMaxWidth = contentMaxWidth,
                        isInWatchlist = isInWatchlist,
                        isFavorite = isFavorite,
                        hasCustomOverride = hasCustomOverride,
                        onBack = onBack,
                        onToggleWatchlist = onToggleWatchlist,
                        onToggleFavorite = onToggleFavorite
                    )
                }

                // 2. Primary Action Bar & Optional Saved Progress
                item(key = "actions_and_progress") {
                    DetailSectionContainer(maxWidth = contentMaxWidth) {
                        Spacer(modifier = Modifier.height(16.dp))

                        DetailActionRow(
                            isInWatchlist = isInWatchlist,
                            hasSavedProgress = savedProgress != null,
                            onPrimaryPlay = {
                                if (savedProgress != null && onResumeProgress != null) {
                                    onResumeProgress(savedProgress)
                                } else {
                                    onPlayMedia(media.episodes.firstOrNull())
                                }
                            },
                            onToggleWatchlist = onToggleWatchlist,
                            onOpenStreamDialog = { showAttachDialog = true }
                        )

                        if (savedProgress != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SavedProgressResumeBanner(
                                progress = savedProgress,
                                onResume = {
                                    if (onResumeProgress != null) {
                                        onResumeProgress(savedProgress)
                                    } else {
                                        onPlayMedia(media.episodes.firstOrNull())
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Structured Metadata Tiles (Rating, Release Year, Studio, Format/Runtime)
                item(key = "metadata_tiles") {
                    DetailSectionContainer(maxWidth = contentMaxWidth) {
                        Spacer(modifier = Modifier.height(20.dp))
                        MetadataSummarySection(
                            rating = media.rating,
                            releaseYear = media.releaseYear,
                            studioOrNetwork = studioLabel,
                            formatOrRuntime = runtimeOrEpisodesLabel,
                            isWideLayout = isWideLayout
                        )
                    }
                }

                // 4. Genre Chips
                if (media.genres.isNotEmpty()) {
                    item(key = "genres_row") {
                        DetailSectionContainer(maxWidth = contentMaxWidth) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_genres_row")
                            ) {
                                media.genres.forEachIndexed { index, genre ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.testTag("detail_genre_chip_$index")
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Synopsis / Description Card
                item(key = "synopsis_description") {
                    DetailSectionContainer(maxWidth = contentMaxWidth) {
                        Spacer(modifier = Modifier.height(22.dp))
                        SynopsisDescriptionCard(
                            synopsis = media.synopsis,
                            isExpanded = isSynopsisExpanded,
                            onToggleExpand = { isSynopsisExpanded = !isSynopsisExpanded }
                        )
                    }
                }

                // 6. Episodes / Feature Stream Header
                item(key = "episodes_header") {
                    DetailSectionContainer(maxWidth = contentMaxWidth) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("detail_episodes_header")
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "${stringResource(R.string.detail_episodes)} (${media.episodes.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isLoadingEpisodes) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 7. Episode / Stream List
                items(media.episodes, key = { it.id }) { ep ->
                    DetailEpisodeCard(
                        episode = ep,
                        onClick = { onPlayMedia(ep) },
                        maxContentWidth = contentMaxWidth,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionContainer(
    maxWidth: Dp,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        content()
    }
}

@Composable
private fun DetailHeroPosterHeader(
    media: MediaItemModel,
    isWideLayout: Boolean,
    contentMaxWidth: Dp,
    isInWatchlist: Boolean,
    isFavorite: Boolean,
    hasCustomOverride: Boolean,
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onToggleFavorite: (() -> Unit)?
) {
    val headerMinHeight = if (isWideLayout) 360.dp else 340.dp
    val posterWidth = if (isWideLayout) 156.dp else 118.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric Backdrop Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerMinHeight)
        ) {
            AsyncImage(
                model = media.backdropUrl.ifBlank { media.posterUrl },
                contentDescription = stringResource(R.string.cd_detail_backdrop, media.title),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.img_hero_cinema),
                placeholder = painterResource(id = R.drawable.img_hero_cinema),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("detail_backdrop_image")
            )

            // Multi-stop Obsidian Cinema Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.62f),
                            0.38f to Color.Black.copy(alpha = 0.28f),
                            0.78f to MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
            )
        }

        // Foreground Safe-Area Top Bar + Overlapping Poster & Title Block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Quick Actions Row
            Row(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = Color.White
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInWatchlist && onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                                .testTag("detail_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.cd_favorite_toggle),
                                tint = if (isFavorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleWatchlist,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                            .testTag("detail_watchlist_button")
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) {
                                Icons.Filled.Bookmark
                            } else {
                                Icons.Filled.BookmarkBorder
                            },
                            contentDescription = stringResource(R.string.cd_watchlist_toggle),
                            tint = if (isInWatchlist) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.White
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isWideLayout) 56.dp else 72.dp))

            // Asymmetric Poster + Title & Badges Block anchored over the lower backdrop fade
            Row(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Framed 2:3 Foreground Poster Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .width(posterWidth)
                        .aspectRatio(2f / 3f)
                        .testTag("detail_poster_card")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = media.posterUrl.ifBlank { media.backdropUrl },
                            contentDescription = stringResource(
                                R.string.cd_detail_poster,
                                media.title
                            ),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.img_hero_cinema),
                            placeholder = painterResource(id = R.drawable.img_hero_cinema),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("detail_poster_image")
                        )

                        RatingBadge(
                            rating = media.rating,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                }

                // Title, Category Badge, Stream Readiness & Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("detail_category_badge")
                        ) {
                            Text(
                                text = media.category.displayName.uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = if (hasCustomOverride || media.isLiveStreamReady) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("detail_stream_status_badge")
                        ) {
                            Text(
                                text = when {
                                    hasCustomOverride -> stringResource(R.string.detail_custom_stream_bound)
                                    media.isLiveStreamReady -> stringResource(R.string.detail_stream_ready)
                                    else -> stringResource(R.string.detail_trailer_stream)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasCustomOverride || media.isLiveStreamReady) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = media.title,
                        style = if (isWideLayout) {
                            MaterialTheme.typography.displayMedium
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("detail_title_text")
                    )

                    if (media.subtitle.isNotBlank()) {
                        Text(
                            text = media.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("detail_subtitle_text")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailActionRow(
    isInWatchlist: Boolean,
    hasSavedProgress: Boolean,
    onPrimaryPlay: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenStreamDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPrimaryPlay,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .testTag("detail_play_primary_button")
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (hasSavedProgress) R.string.action_resume_episode else R.string.action_watch_now
                ),
                style = MaterialTheme.typography.labelLarge
            )
        }

        OutlinedButton(
            onClick = onToggleWatchlist,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                if (isInWatchlist) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .height(50.dp)
                .testTag("detail_watchlist_action_button")
        ) {
            Icon(
                imageVector = if (isInWatchlist) {
                    Icons.Filled.Bookmark
                } else {
                    Icons.Filled.BookmarkBorder
                },
                contentDescription = null,
                tint = if (isInWatchlist) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (isInWatchlist) R.string.action_in_watchlist else R.string.action_add_watchlist
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        OutlinedButton(
            onClick = onOpenStreamDialog,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = Modifier
                .height(50.dp)
                .testTag("detail_attach_stream_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = stringResource(R.string.action_attach_custom_stream),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.detail_stream_url_short))
        }
    }
}

@Composable
private fun SavedProgressResumeBanner(
    progress: WatchProgressEntity,
    onResume: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResume)
            .testTag("detail_resume_progress_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.section_continue_watching)} • Ep ${progress.episodeNumber}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatMillis(progress.positionMs)} / ${formatMillis(progress.durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.progressFraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun MetadataSummarySection(
    rating: Double,
    releaseYear: String,
    studioOrNetwork: String,
    formatOrRuntime: String,
    isWideLayout: Boolean
) {
    val formattedScore = String.format(Locale.US, "%.1f / 10", rating)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detail_metadata_section")
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_metadata_grid"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetadataStatTile(
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.detail_label_rating),
                    value = formattedScore,
                    tag = "detail_meta_rating",
                    modifier = Modifier.weight(1f)
                )
                MetadataStatTile(
                    icon = Icons.Filled.CalendarToday,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.detail_label_release),
                    value = releaseYear,
                    tag = "detail_meta_year",
                    modifier = Modifier.weight(1f)
                )
                MetadataStatTile(
                    icon = Icons.Filled.MovieFilter,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.detail_label_studio),
                    value = studioOrNetwork,
                    tag = "detail_meta_studio",
                    modifier = Modifier.weight(1f)
                )
                MetadataStatTile(
                    icon = Icons.Filled.Schedule,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    label = stringResource(R.string.detail_label_format),
                    value = formatOrRuntime,
                    tag = "detail_meta_runtime",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_metadata_grid")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetadataStatTile(
                        icon = Icons.Filled.Star,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        label = stringResource(R.string.detail_label_rating),
                        value = formattedScore,
                        tag = "detail_meta_rating",
                        modifier = Modifier.weight(1f)
                    )
                    MetadataStatTile(
                        icon = Icons.Filled.CalendarToday,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = stringResource(R.string.detail_label_release),
                        value = releaseYear,
                        tag = "detail_meta_year",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetadataStatTile(
                        icon = Icons.Filled.MovieFilter,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = stringResource(R.string.detail_label_studio),
                        value = studioOrNetwork,
                        tag = "detail_meta_studio",
                        modifier = Modifier.weight(1f)
                    )
                    MetadataStatTile(
                        icon = Icons.Filled.Schedule,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        label = stringResource(R.string.detail_label_format),
                        value = formatOrRuntime,
                        tag = "detail_meta_runtime",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataStatTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = iconTint.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SynopsisDescriptionCard(
    synopsis: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val canCollapse = synopsis.length > SYNOPSIS_COLLAPSE_THRESHOLD

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("detail_description_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.secondary)
                )
                Text(
                    text = stringResource(R.string.detail_synopsis),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (!canCollapse || isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_synopsis_text")
            )

            if (canCollapse) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_synopsis_toggle_button")
                ) {
                    Text(
                        text = stringResource(
                            if (isExpanded) R.string.detail_show_less else R.string.detail_read_more
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachCustomStreamDialog(
    mediaTitle: String,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_attach_custom_stream)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.detail_bind_stream_prompt, mediaTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlInputChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.detail_bind_stream_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_override_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = urlInput.isNotBlank(),
                modifier = Modifier.testTag("confirm_override_button")
            ) {
                Text(stringResource(R.string.detail_save_and_bind))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DetailEpisodeCard(
    episode: EpisodeItem,
    onClick: () -> Unit,
    maxContentWidth: Dp,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(max = maxContentWidth)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("episode_card_${episode.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(114.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.img_hero_cinema),
                    placeholder = painterResource(id = R.drawable.img_hero_cinema),
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.34f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "S${episode.season} • E${episode.number}  ${episode.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.durationMinutes > 0) {
                    Text(
                        text = "${episode.durationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (episode.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = episode.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatRuntimeOrEpisodes(
    media: MediaItemModel,
    featureFallback: String,
    episodesWord: String
): String {
    if (media.category == MediaCategory.MOVIES) {
        val runtimeMin = media.episodes.firstOrNull()?.durationMinutes ?: 0
        return if (runtimeMin > 0) "$runtimeMin min" else featureFallback
    }
    val epCount = media.episodes.size
    return if (epCount > 0) {
        "$epCount $episodesWord"
    } else {
        media.category.displayName
    }
}
