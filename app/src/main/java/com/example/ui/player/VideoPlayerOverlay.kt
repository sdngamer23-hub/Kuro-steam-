package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.ActivePlaybackSession

@Composable
fun VideoPlayerOverlay(
    session: ActivePlaybackSession,
    onBack: (positionMs: Long, durationMs: Long) -> Unit,
    onProgressTick: (positionMs: Long, durationMs: Long) -> Unit,
    onPlayNextEpisode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isLandscapeLocked by rememberSaveable { mutableStateOf(false) }

    val controller = rememberKuroPlayerController(
        streamUrl = session.streamUrl,
        startPositionMs = session.startPositionMs,
        autoPlay = true,
        onProgressReport = onProgressTick
    )

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exitAction = {
        controller.syncProgressFromPlayer()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack(controller.currentPositionMs, controller.durationMs)
    }

    BackHandler(onBack = exitAction)

    val hasNextEpisode = remember(session) {
        val eps = session.media.episodes
        val idx = eps.indexOfFirst { it.id == session.episode?.id }
        eps.size > 1 && idx in 0 until eps.lastIndex
    }

    val epSubtitle = session.episode?.let {
        "S${it.season} • E${it.number} — ${it.title}"
    } ?: session.media.subtitle

    KuroExoPlayerComponent(
        controller = controller,
        title = session.media.title,
        subtitle = epSubtitle,
        isFullscreen = true,
        showBackButton = true,
        hasNextEpisode = hasNextEpisode,
        onBackClick = exitAction,
        onNextEpisodeClick = onPlayNextEpisode,
        onToggleFullscreen = {
            isLandscapeLocked = !isLandscapeLocked
            activity?.requestedOrientation = if (isLandscapeLocked) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("video_player_overlay")
    )
}
