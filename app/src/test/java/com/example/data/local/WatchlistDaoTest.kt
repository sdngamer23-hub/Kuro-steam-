package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WatchlistDaoTest {

    private lateinit var database: KuroStreamDatabase
    private lateinit var watchlistDao: WatchlistDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            KuroStreamDatabase::class.java
        ).allowMainThreadQueries().build()
        watchlistDao = database.watchlistDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertObserveFilterPinFavoriteAndDeleteWatchlistItems() = runTest {
        val animeItem = WatchlistEntity(
            mediaId = "anime_sintel",
            title = "Sintel: Blade of the Dragon",
            category = MediaCategory.ANIME.name,
            posterUrl = "https://example.com/sintel.jpg",
            backdropUrl = "https://example.com/sintel_bg.jpg",
            rating = 8.9,
            releaseYear = "2010",
            genresCsv = "Fantasy, Adventure",
            studioOrNetwork = "Blender Studio",
            streamUrl = "https://example.com/sintel.mp4",
            synopsis = "A lone warrior searches for her dragon companion.",
            isFavorite = false,
            addedAt = 1_000L
        )

        val movieItem = WatchlistEntity(
            mediaId = "movie_tears_of_steel",
            title = "Tears of Steel",
            category = MediaCategory.MOVIES.name,
            posterUrl = "https://example.com/tos.jpg",
            backdropUrl = "https://example.com/tos_bg.jpg",
            rating = 8.7,
            releaseYear = "2012",
            genresCsv = "Sci-Fi, Cyberpunk",
            studioOrNetwork = "Mango Project",
            streamUrl = "https://example.com/tos.mp4",
            synopsis = "Warriors and scientists stage a stand in future Amsterdam.",
            isFavorite = false,
            addedAt = 2_000L
        )

        // 1. Insert both items
        watchlistDao.upsertWatchlistItems(listOf(animeItem, movieItem))

        // 2. Verify count and ordering (most recent added_at first when neither is pinned)
        assertEquals(2, watchlistDao.observeWatchlistCount().first())
        val allSaved = watchlistDao.observeAllWatchlist().first()
        assertEquals("movie_tears_of_steel", allSaved[0].mediaId)
        assertEquals("anime_sintel", allSaved[1].mediaId)

        // 3. Pin the older anime title as Favorite -> moves to index 0
        watchlistDao.updateFavoriteFlag("anime_sintel", true)
        val afterFavorite = watchlistDao.observeAllWatchlist().first()
        assertEquals("anime_sintel", afterFavorite[0].mediaId)
        assertTrue(afterFavorite[0].isFavorite)

        // 4. Filter by category (ANIME vs MOVIES)
        val animeOnly = watchlistDao.observeWatchlistByCategory(MediaCategory.ANIME.name).first()
        assertEquals(1, animeOnly.size)
        assertEquals("Sintel: Blade of the Dragon", animeOnly.first().title)

        val moviesOnly = watchlistDao.observeWatchlistByCategory(MediaCategory.MOVIES.name).first()
        assertEquals(1, moviesOnly.size)
        assertEquals("Tears of Steel", moviesOnly.first().title)

        // 5. Search by title/genre keyword
        val searchMatches = watchlistDao.searchWatchlist("Cyberpunk").first()
        assertEquals(1, searchMatches.size)
        assertEquals("movie_tears_of_steel", searchMatches.first().mediaId)

        // 6. Verify membership check & delete
        assertTrue(watchlistDao.isInWatchlist("anime_sintel"))
        assertNotNull(watchlistDao.getWatchlistItemById("anime_sintel"))

        watchlistDao.deleteByMediaId("anime_sintel")
        assertFalse(watchlistDao.isInWatchlist("anime_sintel"))
        assertNull(watchlistDao.getWatchlistItemById("anime_sintel"))

        // 7. Clear remaining watchlist
        watchlistDao.clearWatchlist()
        assertEquals(0, watchlistDao.observeWatchlistCount().first())
    }
}
