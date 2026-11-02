package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a saved movie, anime, or web series title in the user's local watchlist.
 */
@Entity(
    tableName = "watchlist",
    indices = [
        Index(value = ["category"]),
        Index(value = ["is_favorite", "added_at"])
    ]
)
data class WatchlistEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "poster_url")
    val posterUrl: String,

    @ColumnInfo(name = "backdrop_url")
    val backdropUrl: String,

    @ColumnInfo(name = "rating")
    val rating: Double,

    @ColumnInfo(name = "release_year")
    val releaseYear: String,

    @ColumnInfo(name = "genres_csv")
    val genresCsv: String = "",

    @ColumnInfo(name = "studio_or_network")
    val studioOrNetwork: String = "",

    @ColumnInfo(name = "stream_url")
    val streamUrl: String,

    @ColumnInfo(name = "synopsis")
    val synopsis: String,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
