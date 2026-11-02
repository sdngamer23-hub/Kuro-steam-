package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val mediaId: String,
    val title: String,
    val episodeId: String,
    val episodeTitle: String,
    val episodeNumber: Int,
    val category: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0.02f, 1f)
        } else {
            0f
        }
}

@Entity(tableName = "custom_streams")
data class CustomStreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val streamUrl: String,
    val posterUrl: String,
    val synopsis: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_stream_overrides")
data class StreamOverrideEntity(
    @PrimaryKey val mediaId: String,
    val customStreamUrl: String,
    val updatedAt: Long = System.currentTimeMillis()
)
