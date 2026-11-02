package com.example.ui.player

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.ui.components.formatMillis
import kotlinx.coroutines.delay

private const val SKIP_INCREMENT_MS = 10_000L

enum class SkipFeedbackDirection {
    BACKWARD_10S,
    FORWARD_10S
}

/**
 * Lifecycle-aware state holder wrapping Media3 [ExoPlayer] with explicit Play, Pause,
 * Skip Backward 10s, Skip Forward 10s, Mute, Speed, Resize Mode, and Buffered Timeline tracking.
 */
@Stable
@OptIn(UnstableApi::class)
class KuroPlayerController(
    context: Context,
    autoPlay: Boolean = true
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(SKIP_INCREMENT_MS)
        .setSeekForwardIncrementMs(SKIP_INCREMENT_MS)
        .build()
        .apply {
            playWhenReady = autoPlay
        }

    var isPlaying by mutableStateOf(autoPlay)
        private set

    var isBuffering by mutableStateOf(true)
        private set

    var isEnded by mutableStateOf(false)
        private set

    var isMuted by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var bufferedPositionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    var playbackSpeed by mutableFloatStateOf(1.0f)
        private set

    var resizeModeIndex by mutableIntStateOf(0)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var skipPulseTrigger by mutableIntStateOf(0)
        private set

    var lastSkipDirection by mutableStateOf<SkipFeedbackDirection?>(null)
        private set

    private var savedVolumeBeforeMute: Float = 1.0f

    val availableSpeeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
    val availableResizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
    )

    val currentResizeMode: Int
        get() = availableResizeModes[resizeModeIndex].first

    val currentResizeLabel: String
        get() = availableResizeModes[resizeModeIndex].second

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing || exoPlayer.playWhenReady
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            isPlaying = playWhenReady
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            isBuffering = playbackState == Player.STATE_BUFFERING
            isEnded = playbackState == Player.STATE_ENDED
            if (playbackState == Player.STATE_ENDED) {
                isPlaying = false
            }
            if (playbackState == Player.STATE_READY) {
                val rawDur = exoPlayer.duration
                if (rawDur != C.TIME_UNSET && rawDur > 0L) {
                    durationMs = rawDur
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            isBuffering = false
            isPlaying = false
            errorMessage = error.localizedMessage ?: "Unable to play video stream."
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    fun loadStream(streamUrl: String, startPositionMs: Long = 0L, playImmediately: Boolean = true) {
        if (streamUrl.isBlank()) return
        errorMessage = null
        isEnded = false
        isBuffering = true
        val mediaItem = MediaItem.fromUri(streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPositionMs > 0L) {
            exoPlayer.seekTo(startPositionMs)
            currentPositionMs = startPositionMs
        } else {
            currentPositionMs = 0L
        }
        exoPlayer.playWhenReady = playImmediately
        isPlaying = playImmediately
    }

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0) {
            exoPlayer.prepare()
        }
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            currentPositionMs = 0L
            isEnded = false
        }
        exoPlayer.play()
        isPlaying = true
    }

    fun pause() {
        exoPlayer.pause()
        isPlaying = false
    }

    fun togglePlayPause() {
        if (isPlaying || exoPlayer.playWhenReady) {
            pause()
        } else {
            play()
        }
    }

    fun skipBackward10s() {
        val basePos = maxOf(currentPositionMs, exoPlayer.currentPosition.coerceAtLeast(0L))
        val target = (basePos - SKIP_INCREMENT_MS).coerceAtLeast(0L)
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(target)
        }
        currentPositionMs = target
        lastSkipDirection = SkipFeedbackDirection.BACKWARD_10S
        skipPulseTrigger++
    }

    fun skipForward10s() {
        val basePos = maxOf(currentPositionMs, exoPlayer.currentPosition.coerceAtLeast(0L))
        val rawDuration = exoPlayer.duration
        val maxPos = if (rawDuration != C.TIME_UNSET && rawDuration > 0L) rawDuration else Long.MAX_VALUE
        val target = (basePos + SKIP_INCREMENT_MS).coerceAtMost(maxPos)
        if (exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(target)
        }
        currentPositionMs = target
        lastSkipDirection = SkipFeedbackDirection.FORWARD_10S
        skipPulseTrigger++
    }

    fun seekToPosition(positionMs: Long) {
        val clamped = if (durationMs > 0L) {
            positionMs.coerceIn(0L, durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        exoPlayer.seekTo(clamped)
        currentPositionMs = clamped
    }

    fun toggleMute() {
        if (isMuted) {
            exoPlayer.volume = savedVolumeBeforeMute.coerceIn(0.1f, 1.0f)
            isMuted = false
        } else {
            savedVolumeBeforeMute = exoPlayer.volume.takeIf { it > 0f } ?: 1.0f
            exoPlayer.volume = 0f
            isMuted = true
        }
    }

    fun cyclePlaybackSpeed() {
        val currentIdx = availableSpeeds.indexOf(playbackSpeed).takeIf { it >= 0 } ?: 0
        val nextSpeed = availableSpeeds[(currentIdx + 1) % availableSpeeds.size]
        playbackSpeed = nextSpeed
        exoPlayer.setPlaybackSpeed(nextSpeed)
    }

    fun cycleResizeMode() {
        resizeModeIndex = (resizeModeIndex + 1) % availableResizeModes.size
    }

    fun retry() {
        errorMessage = null
        isBuffering = true
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun syncProgressFromPlayer() {
        currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
        bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(currentPositionMs)
        val rawDur = exoPlayer.duration
        if (rawDur != C.TIME_UNSET && rawDur > 0L) {
            durationMs = rawDur
        }
    }

    fun release() {
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }
}

@Composable
fun rememberKuroPlayerController(
    streamUrl: String,
    startPositionMs: Long = 0L,
    autoPlay: Boolean = true,
    onProgressReport: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> }
): KuroPlayerController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember(context) {
        KuroPlayerController(context.applicationContext, autoPlay = autoPlay)
    }

    LaunchedEffect(streamUrl) {
        controller.loadStream(
            streamUrl = streamUrl,
            startPositionMs = startPositionMs,
            playImmediately = autoPlay
        )
    }

    LaunchedEffect(controller) {
        var tick = 0
        while (true) {
            controller.syncProgressFromPlayer()
            tick++
            if (tick % 8 == 0 && controller.durationMs > 0L) {
                onProgressReport(controller.currentPositionMs, controller.durationMs)
            }
            delay(250L)
        }
    }

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    controller.syncProgressFromPlayer()
                    onProgressReport(controller.currentPositionMs, controller.durationMs)
                    controller.pause()
                }
                Lifecycle.Event.ON_STOP -> controller.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            controller.syncProgressFromPlayer()
            onProgressReport(controller.currentPositionMs, controller.durationMs)
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }

    return controller
}

/**
 * Reusable custom Jetpack Compose Video Player UI component powered by Media3 ExoPlayer.
 * Works both inline (e.g., inside cards or detail headers) and in fullscreen cinema mode,
 * providing custom Play, Pause, Skip Backward 10s, Skip Forward 10s, double-tap edge gestures,
 * buffered timeline scrubbing, mute, speed, resize mode, and lock controls.
 */
@OptIn(UnstableApi::class)
@Composable
fun KuroExoPlayerComponent(
    controller: KuroPlayerController,
    title: String,
    subtitle: String = "",
    isFullscreen: Boolean = false,
    showBackButton: Boolean = false,
    hasNextEpisode: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onToggleFullscreen: (() -> Unit)? = null,
    onNextEpisodeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var activeSkipBadge by remember { mutableStateOf<SkipFeedbackDirection?>(null) }
    var boxWidthPx by remember { mutableIntStateOf(1) }

    // Show transient -10s / +10s ripple badge whenever skip pulse increments
    LaunchedEffect(controller.skipPulseTrigger) {
        if (controller.skipPulseTrigger > 0) {
            activeSkipBadge = controller.lastSkipDirection
            delay(650L)
            activeSkipBadge = null
        }
    }

    // Auto-hide controls after 3.8s of active playback
    LaunchedEffect(controlsVisible, controller.isPlaying, isControlsLocked) {
        if (controlsVisible && controller.isPlaying && controller.errorMessage == null) {
            delay(3800L)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged { boxWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(isControlsLocked) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (!isControlsLocked) {
                            val leftZone = boxWidthPx * 0.36f
                            val rightZone = boxWidthPx * 0.64f
                            when {
                                offset.x < leftZone -> controller.skipBackward10s()
                                offset.x > rightZone -> controller.skipForward10s()
                                else -> controller.togglePlayPause()
                            }
                        }
                    }
                )
            }
            .testTag("custom_exoplayer_component")
    ) {
        // Media3 Video Surface via controllerless PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = controller.currentResizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = controller.exoPlayer
                }
            },
            update = { playerView ->
                if (playerView.player !== controller.exoPlayer) {
                    playerView.player = controller.exoPlayer
                }
                if (playerView.resizeMode != controller.currentResizeMode) {
                    playerView.resizeMode = controller.currentResizeMode
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("exoplayer_video_surface")
        )

        // Double-Tap / Button Skip -10s Visual Feedback Pill (Left)
        AnimatedVisibility(
            visible = activeSkipBadge == SkipFeedbackDirection.BACKWARD_10S,
            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            SkipFeedbackPill(
                isForward = false,
                label = stringResource(R.string.player_skip_back_10s)
            )
        }

        // Double-Tap / Button Skip +10s Visual Feedback Pill (Right)
        AnimatedVisibility(
            visible = activeSkipBadge == SkipFeedbackDirection.FORWARD_10S,
            enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            SkipFeedbackPill(
                isForward = true,
                label = stringResource(R.string.player_skip_forward_10s)
            )
        }

        // Buffering Indicator
        if (controller.isBuffering && controller.errorMessage == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (isFullscreen) 54.dp else 42.dp)
                    .testTag("player_buffering_indicator")
            )
        }

        // Error State Card
        if (controller.errorMessage != null) {
            PlayerErrorCard(
                errorMessage = controller.errorMessage.orEmpty(),
                onRetry = { controller.retry() },
                onClose = onBackClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
            )
        }

        // Locked Controls Minimal Pill
        AnimatedVisibility(
            visible = controlsVisible && isControlsLocked && controller.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
                .padding(16.dp)
        ) {
            Surface(
                onClick = { isControlsLocked = false },
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("player_unlock_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.cd_lock_controls),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Unlock Controls",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Full Custom Controls Overlay (Top Header, Center Play/Pause & Skip 10s, Bottom Scrub Bar)
        AnimatedVisibility(
            visible = controlsVisible && !isControlsLocked && controller.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.80f),
                            0.30f to Color.Black.copy(alpha = 0.22f),
                            0.70f to Color.Black.copy(alpha = 0.26f),
                            1.0f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                    .testTag("player_controls_overlay")
            ) {
                // 1. Top Control Header
                PlayerTopBar(
                    title = title,
                    subtitle = subtitle,
                    isFullscreen = isFullscreen,
                    showBackButton = showBackButton,
                    isMuted = controller.isMuted,
                    playbackSpeed = controller.playbackSpeed,
                    resizeLabel = controller.currentResizeLabel,
                    onBackClick = onBackClick,
                    onToggleMute = { controller.toggleMute() },
                    onCycleSpeed = { controller.cyclePlaybackSpeed() },
                    onCycleResize = { controller.cycleResizeMode() },
                    onLockControls = if (isFullscreen) {
                        { isControlsLocked = true }
                    } else {
                        null
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // 2. Center Playback Transport Bar: Skip -10s | Play / Pause / Replay | Skip +10s
                PlayerCenterTransportControls(
                    isPlaying = controller.isPlaying,
                    isEnded = controller.isEnded,
                    compact = !isFullscreen,
                    onSkipBackward10s = { controller.skipBackward10s() },
                    onTogglePlayPause = { controller.togglePlayPause() },
                    onSkipForward10s = { controller.skipForward10s() },
                    modifier = Modifier.align(Alignment.Center)
                )

                // 3. Bottom Timeline Scrubber & Action Bar
                PlayerBottomBar(
                    currentPositionMs = controller.currentPositionMs,
                    bufferedPositionMs = controller.bufferedPositionMs,
                    durationMs = controller.durationMs,
                    isFullscreen = isFullscreen,
                    hasNextEpisode = hasNextEpisode,
                    onSeekTo = { targetMs -> controller.seekToPosition(targetMs) },
                    onNextEpisodeClick = onNextEpisodeClick,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    subtitle: String,
    isFullscreen: Boolean,
    showBackButton: Boolean,
    isMuted: Boolean,
    playbackSpeed: Float,
    resizeLabel: String,
    onBackClick: (() -> Unit)?,
    onToggleMute: () -> Unit,
    onCycleSpeed: () -> Unit,
    onCycleResize: () -> Unit,
    onLockControls: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 12.dp, vertical = if (isFullscreen) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton && onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (isFullscreen) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleSmall
                },
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Mute / Unmute Button
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .size(48.dp)
                .testTag("player_mute_button")
        ) {
            Icon(
                imageVector = if (isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = stringResource(R.string.cd_mute_toggle),
                tint = if (isMuted) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Playback Speed Pill
        TextButton(
            onClick = onCycleSpeed,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("player_speed_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Speed,
                contentDescription = stringResource(R.string.player_speed),
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${playbackSpeed}x",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (isFullscreen) {
            TextButton(
                onClick = onCycleResize,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("player_resize_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AspectRatio,
                    contentDescription = stringResource(R.string.player_fit_mode),
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = resizeLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (onLockControls != null) {
                IconButton(
                    onClick = onLockControls,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = stringResource(R.string.cd_lock_controls),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCenterTransportControls(
    isPlaying: Boolean,
    isEnded: Boolean,
    compact: Boolean,
    onSkipBackward10s: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipForward10s: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sideButtonSize = if (compact) 52.dp else 60.dp
    val mainButtonSize = if (compact) 64.dp else 76.dp
    val sideIconSize = if (compact) 26.dp else 32.dp
    val mainIconSize = if (compact) 34.dp else 42.dp
    val buttonSpacing = if (compact) 22.dp else 34.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip Backward 10s Button
        Surface(
            onClick = onSkipBackward10s,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier
                .size(sideButtonSize)
                .testTag("player_rewind_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Replay10,
                    contentDescription = stringResource(R.string.cd_rewind_10),
                    tint = Color.White,
                    modifier = Modifier.size(sideIconSize)
                )
            }
        }

        // Primary Play / Pause / Replay Button
        Surface(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(mainButtonSize)
                .testTag("player_play_pause_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when {
                    isEnded -> Icons.Filled.Replay
                    isPlaying -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                }
                val cd = when {
                    isEnded -> stringResource(R.string.cd_replay)
                    isPlaying -> stringResource(R.string.cd_pause)
                    else -> stringResource(R.string.cd_play)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = cd,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(mainIconSize)
                )
            }
        }

        // Skip Forward 10s Button
        Surface(
            onClick = onSkipForward10s,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
            modifier = Modifier
                .size(sideButtonSize)
                .testTag("player_forward_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Forward10,
                    contentDescription = stringResource(R.string.cd_forward_10),
                    tint = Color.White,
                    modifier = Modifier.size(sideIconSize)
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomBar(
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    hasNextEpisode: Boolean,
    onSeekTo: (Long) -> Unit,
    onNextEpisodeClick: (() -> Unit)?,
    onToggleFullscreen: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val activeFraction = if (isDraggingScrubber) {
        scrubFraction
    } else {
        (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }
    val bufferedFraction = (bufferedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayedTimeMs = if (isDraggingScrubber) {
        (scrubFraction * safeDuration).toLong()
    } else {
        currentPositionMs
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.navigationBarsPadding() else Modifier)
            .padding(
                horizontal = if (isFullscreen) 18.dp else 12.dp,
                vertical = if (isFullscreen) 10.dp else 6.dp
            )
    ) {
        // Custom Buffered + Played Interactive Scrub Bar (48dp touch height for accessibility)
        CustomBufferedSeekBar(
            progressFraction = activeFraction,
            bufferedFraction = bufferedFraction,
            isDragging = isDraggingScrubber,
            onScrubChange = { fraction ->
                isDraggingScrubber = true
                scrubFraction = fraction
            },
            onScrubFinished = { fraction ->
                onSeekTo((fraction * safeDuration).toLong())
                isDraggingScrubber = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_seek_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatMillis(displayedTimeMs),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.testTag("player_position_text")
                )
                Text(
                    text = "/",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = formatMillis(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.testTag("player_duration_text")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasNextEpisode && onNextEpisodeClick != null) {
                    TextButton(
                        onClick = onNextEpisodeClick,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("player_next_ep_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.player_next_episode),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (onToggleFullscreen != null) {
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("player_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) {
                                Icons.Filled.FullscreenExit
                            } else {
                                Icons.Filled.Fullscreen
                            },
                            contentDescription = stringResource(R.string.cd_fullscreen),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomBufferedSeekBar(
    progressFraction: Float,
    bufferedFraction: Float,
    isDragging: Boolean,
    onScrubChange: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val bufferedColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val trackColor = Color.White.copy(alpha = 0.24f)

    var barWidthPx by remember { mutableFloatStateOf(1f) }
    var latestDragFraction by remember(progressFraction) { mutableFloatStateOf(progressFraction) }

    Box(
        modifier = modifier
            .height(36.dp)
            .onSizeChanged { barWidthPx = it.width.coerceAtLeast(1).toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / barWidthPx).coerceIn(0f, 1f)
                    onScrubFinished(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / barWidthPx).coerceIn(0f, 1f)
                        latestDragFraction = frac
                        onScrubChange(frac)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val frac = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                        latestDragFraction = frac
                        onScrubChange(frac)
                    },
                    onDragEnd = {
                        onScrubFinished(latestDragFraction)
                    },
                    onDragCancel = {
                        onScrubFinished(latestDragFraction)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = if (isDragging) 7.dp.toPx() else 4.5.dp.toPx()
            val centerY = size.height / 2f
            val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

            // Unplayed background track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = corner
            )

            // Buffered progress track
            val bufferedWidth = (size.width * bufferedFraction.coerceIn(0f, 1f))
            if (bufferedWidth > 0f) {
                drawRoundRect(
                    color = bufferedColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(bufferedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Active played track
            val playedWidth = (size.width * progressFraction.coerceIn(0f, 1f))
            if (playedWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(playedWidth, trackHeight),
                    cornerRadius = corner
                )
            }

            // Scrubber thumb
            val thumbRadius = if (isDragging) 9.dp.toPx() else 6.5.dp.toPx()
            val thumbX = playedWidth.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
            drawCircle(
                color = Color.White,
                radius = thumbRadius + 1.5.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = activeColor,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
        }
    }
}

@Composable
private fun SkipFeedbackPill(
    isForward: Boolean,
    label: String
) {
    Surface(
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isForward) Icons.Filled.Forward10 else Icons.Filled.Replay10,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun PlayerErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.clip(RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${stringResource(R.string.player_error_prefix)} $errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("player_retry_button")
                ) {
                    Text(stringResource(R.string.player_retry))
                }
                if (onClose != null) {
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
