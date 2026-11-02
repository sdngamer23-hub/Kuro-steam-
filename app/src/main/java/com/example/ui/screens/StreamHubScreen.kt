package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomStreamEntity
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.data.remote.OpenCinemaCatalog
import com.example.data.repository.toMediaItemModel
import com.example.ui.player.KuroExoPlayerComponent
import com.example.ui.player.rememberKuroPlayerController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamHubScreen(
    customStreams: List<CustomStreamEntity>,
    onPlayTemporaryStream: (title: String, category: MediaCategory, streamUrl: String) -> Unit,
    onSaveStream: (
        title: String,
        category: MediaCategory,
        streamUrl: String,
        posterUrl: String,
        synopsis: String,
        playNow: Boolean
    ) -> Unit,
    onPlaySavedCustom: (MediaItemModel) -> Unit,
    onDeleteCustom: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable { mutableStateOf("") }
    var streamUrl by rememberSaveable { mutableStateOf("") }
    var posterUrl by rememberSaveable { mutableStateOf("") }
    var synopsis by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf(MediaCategory.ANIME.name) }

    val selectedCategory = runCatching { MediaCategory.valueOf(selectedCategoryName) }
        .getOrDefault(MediaCategory.ANIME)

    val activePreviewUrl = streamUrl.trim().ifBlank { OpenCinemaCatalog.SINTEL_MP4 }
    val activePreviewTitle = title.trim().ifBlank { "Sintel: Blade of the Dragon (Preview)" }
    val inlinePlayerController = rememberKuroPlayerController(
        streamUrl = activePreviewUrl,
        autoPlay = false
    )

    val selectableCategories = listOf(
        MediaCategory.ANIME,
        MediaCategory.WEB_SERIES,
        MediaCategory.MOVIES
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("stream_hub_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.widthIn(max = 640.dp)) {
                Text(
                    text = stringResource(R.string.studio_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.studio_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Inline 16:9 Custom Media3 ExoPlayer Component Deck
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .testTag("inline_player_preview_card")
            ) {
                KuroExoPlayerComponent(
                    controller = inlinePlayerController,
                    title = activePreviewTitle,
                    subtitle = stringResource(R.string.player_live_preview),
                    isFullscreen = false,
                    showBackButton = false,
                    onToggleFullscreen = {
                        inlinePlayerController.pause()
                        onPlayTemporaryStream(
                            activePreviewTitle,
                            selectedCategory,
                            activePreviewUrl
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                )
            }
        }

        // Quick-load preset HLS / MP4 test links
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        title = "Adaptive HLS Master Stream"
                        streamUrl = OpenCinemaCatalog.MUX_TEST_HLS
                        selectedCategoryName = MediaCategory.ANIME.name
                        synopsis = "Live adaptive multi-bitrate HLS (.m3u8) test stream."
                    },
                    label = { Text(stringResource(R.string.sample_hls_chip)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preset_hls_chip")
                )

                AssistChip(
                    onClick = {
                        title = "Tears of Steel 1080p Sci-Fi"
                        streamUrl = OpenCinemaCatalog.TEARS_OF_STEEL_MP4
                        selectedCategoryName = MediaCategory.MOVIES.name
                        synopsis = "Cyberpunk live-action & VFX open movie stream."
                    },
                    label = { Text(stringResource(R.string.sample_mp4_chip)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preset_mp4_chip")
                )
            }
        }

        // Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selectableCategories.forEach { cat ->
                            val isSelected = cat == selectedCategory
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryName = cat.name },
                                label = { Text(cat.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("hub_cat_${cat.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.field_stream_title)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_title_input")
                    )

                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text(stringResource(R.string.field_stream_url)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_url_input")
                    )

                    OutlinedTextField(
                        value = posterUrl,
                        onValueChange = { posterUrl = it },
                        label = { Text(stringResource(R.string.field_poster_url)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_poster_input")
                    )

                    OutlinedTextField(
                        value = synopsis,
                        onValueChange = { synopsis = it },
                        label = { Text(stringResource(R.string.field_synopsis)) },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_synopsis_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onPlayTemporaryStream(title, selectedCategory, streamUrl)
                            },
                            enabled = streamUrl.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("play_direct_stream_button")
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play Now")
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveStream(
                                    title,
                                    selectedCategory,
                                    streamUrl,
                                    posterUrl,
                                    synopsis,
                                    false
                                )
                                title = ""
                                streamUrl = ""
                                posterUrl = ""
                                synopsis = ""
                            },
                            enabled = streamUrl.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_custom_stream_button")
                        ) {
                            Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.action_save_stream))
                        }
                    }
                }
            }
        }

        if (customStreams.isNotEmpty()) {
            item {
                Text(
                    text = "${stringResource(R.string.section_custom_streams)} (${customStreams.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(customStreams, key = { it.id }) { entity ->
                val model = entity.toMediaItemModel()
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .testTag("custom_stream_card_${entity.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entity.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entity.streamUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onPlaySavedCustom(model) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("play_custom_${entity.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.action_watch_now),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { onDeleteCustom(entity.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_custom_${entity.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_item),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
