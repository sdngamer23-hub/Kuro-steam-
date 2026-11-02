package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamingDao {
    @Query("SELECT * FROM watch_progress ORDER BY updatedAt DESC")
    fun observeWatchProgress(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchProgress(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE mediaId = :mediaId")
    suspend fun deleteWatchProgress(mediaId: String)

    @Query("SELECT * FROM custom_streams ORDER BY createdAt DESC")
    fun observeCustomStreams(): Flow<List<CustomStreamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStream(stream: CustomStreamEntity): Long

    @Query("DELETE FROM custom_streams WHERE id = :id")
    suspend fun deleteCustomStream(id: Long)

    @Query("SELECT * FROM media_stream_overrides")
    fun observeStreamOverrides(): Flow<List<StreamOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreamOverride(overrideEntity: StreamOverrideEntity)
}

@Database(
    entities = [
        WatchlistEntity::class,
        WatchProgressEntity::class,
        CustomStreamEntity::class,
        StreamOverrideEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KuroStreamDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun streamingDao(): StreamingDao

    companion object {
        @Volatile
        private var INSTANCE: KuroStreamDatabase? = null

        fun getInstance(context: Context): KuroStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KuroStreamDatabase::class.java,
                    "kurostream_local.db"
                ).fallbackToDestructiveMigration(true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
