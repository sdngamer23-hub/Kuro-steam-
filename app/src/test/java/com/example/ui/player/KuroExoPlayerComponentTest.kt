package com.example.ui.player

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.KuroStreamTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KuroExoPlayerComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var controller: KuroPlayerController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        controller = KuroPlayerController(context, autoPlay = false)
    }

    @After
    fun tearDown() {
        controller.release()
    }

    @Test
    fun customPlayerRendersControlsAndHandlesPlayPauseAndSkip10s() {
        composeTestRule.setContent {
            KuroStreamTheme(darkTheme = true) {
                KuroExoPlayerComponent(
                    controller = controller,
                    title = "Tears of Steel",
                    subtitle = "S1 • E1 — Full Feature",
                    isFullscreen = true,
                    showBackButton = true,
                    onBackClick = {}
                )
            }
        }

        // Verify custom player container and primary transport buttons are displayed
        composeTestRule.onNodeWithTag("custom_exoplayer_component").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_play_pause_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_rewind_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_forward_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("player_seek_slider").assertIsDisplayed()

        // Initially paused (autoPlay = false)
        assertFalse(controller.isPlaying)

        // Click Play -> triggers play state
        composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
        assertTrue(controller.isPlaying)

        // Click Pause -> triggers pause state
        composeTestRule.onNodeWithTag("player_play_pause_button").performClick()
        assertFalse(controller.isPlaying)

        // Click Skip Forward +10s twice -> advances position by 20,000ms and records forward direction
        composeTestRule.onNodeWithTag("player_forward_button").performClick()
        assertEquals(10_000L, controller.currentPositionMs)
        assertEquals(SkipFeedbackDirection.FORWARD_10S, controller.lastSkipDirection)

        composeTestRule.onNodeWithTag("player_forward_button").performClick()
        assertEquals(20_000L, controller.currentPositionMs)

        // Click Skip Backward -10s -> rewinds by 10,000ms and records backward direction
        composeTestRule.onNodeWithTag("player_rewind_button").performClick()
        assertEquals(10_000L, controller.currentPositionMs)
        assertEquals(SkipFeedbackDirection.BACKWARD_10S, controller.lastSkipDirection)
    }
}
