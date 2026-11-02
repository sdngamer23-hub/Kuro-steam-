package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel
import com.example.ui.theme.KuroStreamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediaDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAnime = MediaItemModel(
        id = "anime_frieren_01",
        title = "Frieren: Beyond Journey's End",
        subtitle = "TV Series • 28 Episodes",
        category = MediaCategory.ANIME,
        rating = 9.3,
        releaseYear = "2023",
        genres = listOf("Adventure", "Drama", "Fantasy"),
        synopsis = "After a ten-year quest alongside hero Himmel and his party, elven mage Frieren defeats the Demon King and embarks on a new journey to understand humanity, fleeting memories, and the meaning of time.",
        posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
        backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
        streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        isLiveStreamReady = true,
        studioOrNetwork = "Madhouse",
        episodes = listOf(
            EpisodeItem(
                id = "ep_1",
                number = 1,
                season = 1,
                title = "The Journey's End",
                durationMinutes = 24,
                summary = "The victorious party returns to the royal capital.",
                thumbnailUrl = "",
                streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            EpisodeItem(
                id = "ep_2",
                number = 2,
                season = 1,
                title = "It Didn't Have to Be Magic",
                durationMinutes = 24,
                summary = "Frieren takes young Fern under her wing.",
                thumbnailUrl = "",
                streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            )
        )
    )

    @Test
    fun detailScreen_displaysPosterDescriptionAndMetadata() {
        var watchlistToggled = false
        var playedEpisode: EpisodeItem? = null

        composeTestRule.setContent {
            KuroStreamTheme(darkTheme = true) {
                MediaDetailScreen(
                    media = sampleAnime,
                    isInWatchlist = false,
                    onBack = {},
                    onPlayMedia = { ep -> playedEpisode = ep },
                    onToggleWatchlist = { watchlistToggled = true },
                    onSaveStreamOverride = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Poster & Hero Title
        composeTestRule.onNodeWithTag("detail_poster_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_poster_image", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_title_text")
            .assertIsDisplayed()
            .assertTextContains("Frieren: Beyond Journey's End")

        // Structured Metadata Tiles (Rating, Release Year, Studio, Episodes)
        composeTestRule.onNodeWithTag("media_detail_scroll_list")
            .performScrollToNode(hasTestTag("detail_metadata_section"))
        composeTestRule.onNodeWithTag("detail_meta_rating").assertIsDisplayed()
        composeTestRule.onNodeWithText("9.3 / 10").assertIsDisplayed()
        composeTestRule.onNodeWithText("2023").assertIsDisplayed()
        composeTestRule.onNodeWithText("Madhouse").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 Episodes").assertIsDisplayed()

        // Genres
        composeTestRule.onNodeWithTag("media_detail_scroll_list")
            .performScrollToNode(hasTestTag("detail_genres_row"))
        composeTestRule.onNodeWithText("Adventure").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fantasy").assertIsDisplayed()

        // Description / Synopsis & Expand Toggle
        composeTestRule.onNodeWithTag("media_detail_scroll_list")
            .performScrollToNode(hasTestTag("detail_description_section"))
        composeTestRule.onNodeWithTag("detail_synopsis_text")
            .assertIsDisplayed()
            .assertTextContains("elven mage Frieren", substring = true)

        composeTestRule.onNodeWithTag("detail_synopsis_toggle_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed()

        // Watchlist & Play Primary Actions
        composeTestRule.onNodeWithTag("media_detail_scroll_list")
            .performScrollToNode(hasTestTag("detail_play_primary_button"))
        composeTestRule.onNodeWithTag("detail_watchlist_action_button").performClick()
        assertTrue(watchlistToggled)

        composeTestRule.onNodeWithTag("detail_play_primary_button").performClick()
        assertEquals("ep_1", playedEpisode?.id)
    }
}
