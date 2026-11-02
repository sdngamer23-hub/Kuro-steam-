package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Jikan v4 Anime API DTOs
@JsonClass(generateAdapter = true)
data class JikanListResponse(
    @Json(name = "data") val data: List<JikanAnimeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JikanAnimeDto(
    @Json(name = "mal_id") val malId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "title_english") val titleEnglish: String? = null,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "score") val score: Double? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "episodes") val episodes: Int? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "images") val images: JikanImagesDto? = null,
    @Json(name = "trailer") val trailer: JikanTrailerDto? = null,
    @Json(name = "genres") val genres: List<JikanNamedDto> = emptyList(),
    @Json(name = "studios") val studios: List<JikanNamedDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JikanImagesDto(
    @Json(name = "jpg") val jpg: JikanImageSetDto? = null
)

@JsonClass(generateAdapter = true)
data class JikanImageSetDto(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "large_image_url") val largeImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanTrailerDto(
    @Json(name = "images") val images: JikanTrailerImageDto? = null
)

@JsonClass(generateAdapter = true)
data class JikanTrailerImageDto(
    @Json(name = "maximum_image_url") val maximumImageUrl: String? = null,
    @Json(name = "large_image_url") val largeImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanNamedDto(
    @Json(name = "name") val name: String
)

interface JikanApiService {
    @GET("v4/top/anime")
    suspend fun getTopAnime(
        @Query("limit") limit: Int = 16,
        @Query("sfw") sfw: Boolean = true
    ): JikanListResponse

    @GET("v4/top/anime")
    suspend fun getTopAnimeMovies(
        @Query("type") type: String = "movie",
        @Query("limit") limit: Int = 10,
        @Query("sfw") sfw: Boolean = true
    ): JikanListResponse

    @GET("v4/anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 14,
        @Query("sfw") sfw: Boolean = true
    ): JikanListResponse
}

// TVMaze Web Series API DTOs
@JsonClass(generateAdapter = true)
data class TvMazeShowDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "premiered") val premiered: String? = null,
    @Json(name = "rating") val rating: TvMazeRatingDto? = null,
    @Json(name = "image") val image: TvMazeImageDto? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "network") val network: TvMazeNetworkDto? = null,
    @Json(name = "webChannel") val webChannel: TvMazeNetworkDto? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeRatingDto(
    @Json(name = "average") val average: Double? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeImageDto(
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "original") val original: String? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeNetworkDto(
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class TvMazeSearchItemDto(
    @Json(name = "show") val show: TvMazeShowDto
)

@JsonClass(generateAdapter = true)
data class TvMazeEpisodeDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "season") val season: Int? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "image") val image: TvMazeImageDto? = null,
    @Json(name = "summary") val summary: String? = null
)

interface TvMazeApiService {
    @GET("shows")
    suspend fun getPopularShows(@Query("page") page: Int = 0): List<TvMazeShowDto>

    @GET("search/shows")
    suspend fun searchShows(@Query("q") query: String): List<TvMazeSearchItemDto>

    @GET("shows/{id}/episodes")
    suspend fun getShowEpisodes(@Path("id") showId: Int): List<TvMazeEpisodeDto>
}

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val jikanApi: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JikanApiService::class.java)
    }

    val tvMazeApi: TvMazeApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.tvmaze.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TvMazeApiService::class.java)
    }
}
