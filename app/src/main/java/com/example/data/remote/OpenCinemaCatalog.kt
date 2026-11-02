package com.example.data.remote

import com.example.data.model.EpisodeItem
import com.example.data.model.MediaCategory
import com.example.data.model.MediaItemModel

/**
 * Real, publicly hosted Creative Commons / Blender Studio Open-Movie & Open-Series MP4 and HLS streams
 * so users can immediately watch full HD films, sci-fi shorts, animated features, and multi-episode
 * series out of the box with zero Geo-restrictions or DRM failures.
 */
object OpenCinemaCatalog {

    const val TEARS_OF_STEEL_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    const val SINTEL_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    const val BIG_BUCK_BUNNY_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    const val ELEPHANTS_DREAM_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
    const val SUBARU_OUTBACK_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
    const val FOR_BIGGER_BLAZES_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    const val FOR_BIGGER_ESCAPES_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
    const val FOR_BIGGER_JOYRIDES_MP4 =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
    const val MUX_TEST_HLS =
        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

    private val openStreamsPool = listOf(
        TEARS_OF_STEEL_MP4,
        SINTEL_MP4,
        ELEPHANTS_DREAM_MP4,
        BIG_BUCK_BUNNY_MP4,
        FOR_BIGGER_ESCAPES_MP4,
        FOR_BIGGER_BLAZES_MP4
    )

    fun fallbackStreamForIndex(index: Int): String =
        openStreamsPool[Math.abs(index) % openStreamsPool.size]

    val featuredPremieres: List<MediaItemModel> = listOf(
        MediaItemModel(
            id = "open_movie_tears_of_steel",
            title = "Tears of Steel",
            subtitle = "Cyberpunk Live-Action & VFX Sci-Fi Film",
            category = MediaCategory.MOVIES,
            rating = 8.7,
            releaseYear = "2012",
            genres = listOf("Sci-Fi", "Cyberpunk", "Action"),
            synopsis = "In a dystopian future Amsterdam, a group of warriors and scientists gather at the Oude Kerk to stage a desperate stand against giant autonomous machines—replaying a pivotal memory from forty years earlier to save the world.",
            posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            streamUrl = TEARS_OF_STEEL_MP4,
            isLiveStreamReady = true,
            studioOrNetwork = "Blender Foundation • Mango",
            episodes = listOf(
                EpisodeItem(
                    id = "tos_ep1",
                    number = 1,
                    season = 1,
                    title = "Full Feature Film (1080p HD)",
                    durationMinutes = 12,
                    summary = "Thom and Celia's fateful confrontation echoed in the ruins of futuristic Amsterdam.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    streamUrl = TEARS_OF_STEEL_MP4
                )
            )
        ),
        MediaItemModel(
            id = "open_anime_sintel",
            title = "Sintel: Blade of the Dragon",
            subtitle = "Epic Fantasy Animated Feature",
            category = MediaCategory.ANIME,
            rating = 8.9,
            releaseYear = "2010",
            genres = listOf("Fantasy", "Adventure", "Animation"),
            synopsis = "A lone warrior named Sintel travels across frozen peaks and scorching deserts searching for Scales, a baby dragon she rescued and bonded with before he was snatched away by an ancient winged beast.",
            posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            streamUrl = SINTEL_MP4,
            isLiveStreamReady = true,
            studioOrNetwork = "Blender Studio • Durian",
            episodes = listOf(
                EpisodeItem(
                    id = "sintel_ep1",
                    number = 1,
                    season = 1,
                    title = "Act I & II — Full Animated Film",
                    durationMinutes = 15,
                    summary = "Sintel's perilous quest leads her to the lair of the guardian dragon.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    streamUrl = SINTEL_MP4
                )
            )
        ),
        MediaItemModel(
            id = "open_series_cyber_chronicles",
            title = "Machina & Dreamers",
            subtitle = "Open Anthology Web Series • 4 Episodes",
            category = MediaCategory.WEB_SERIES,
            rating = 8.4,
            releaseYear = "2024",
            genres = listOf("Anthology", "Sci-Fi", "Surreal"),
            synopsis = "An acclaimed open-cinema anthology web series exploring surreal mechanical worlds, rogue AI rebellions, and whimsical forest rivalries across four distinct standalone chapters.",
            posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            streamUrl = ELEPHANTS_DREAM_MP4,
            isLiveStreamReady = true,
            studioOrNetwork = "Open Cinema Originals",
            episodes = listOf(
                EpisodeItem(
                    id = "md_ep1",
                    number = 1,
                    season = 1,
                    title = "Proog's Labyrinth (Elephants Dream)",
                    durationMinutes = 11,
                    summary = "Emo and Proog navigate an infinite, shifting surrealist machine powered by human imagination.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                    streamUrl = ELEPHANTS_DREAM_MP4
                ),
                EpisodeItem(
                    id = "md_ep2",
                    number = 2,
                    season = 1,
                    title = "Oude Kerk Protocol (Tears of Steel)",
                    durationMinutes = 12,
                    summary = "Scientists race to recalibrate cybernetic memories before autonomous sentinels breach the dome.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    streamUrl = TEARS_OF_STEEL_MP4
                ),
                EpisodeItem(
                    id = "md_ep3",
                    number = 3,
                    season = 1,
                    title = "Ember Scales (Sintel)",
                    durationMinutes = 15,
                    summary = "A lone wanderer crosses treacherous snowfields in pursuit of a mythical dragon kin.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
                    streamUrl = SINTEL_MP4
                ),
                EpisodeItem(
                    id = "md_ep4",
                    number = 4,
                    season = 1,
                    title = "Woodland Reckoning (HLS Adaptive)",
                    durationMinutes = 10,
                    summary = "Adaptive bitrate HLS stream edition of the classic open-source animated comedy.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    streamUrl = MUX_TEST_HLS
                )
            )
        ),
        MediaItemModel(
            id = "open_movie_big_buck",
            title = "Big Buck Awakening",
            subtitle = "Animated Comedy Short Film",
            category = MediaCategory.MOVIES,
            rating = 8.1,
            releaseYear = "2008",
            genres = listOf("Animation", "Comedy", "Family"),
            synopsis = "When three mischievous woodland rodents push a gentle giant rabbit too far, he sets aside his peaceful morning routine to engineer playful traps across the meadow.",
            posterUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            backdropUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            streamUrl = BIG_BUCK_BUNNY_MP4,
            isLiveStreamReady = true,
            studioOrNetwork = "Peach Open Movie Project",
            episodes = listOf(
                EpisodeItem(
                    id = "bbb_ep1",
                    number = 1,
                    season = 1,
                    title = "Full Feature Short (HD)",
                    durationMinutes = 10,
                    summary = "Complete remastered open-cinema comedy feature.",
                    thumbnailUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    streamUrl = BIG_BUCK_BUNNY_MP4
                )
            )
        )
    )
}
