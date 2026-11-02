package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Dedicated Room DAO for managing the local 'watchlist' table where users save
 * and organize their favorite movies, anime, and web series titles.
 */
@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY is_favorite DESC, added_at DESC")
    fun observeAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist WHERE category = :category ORDER BY is_favorite DESC, added_at DESC")
    fun observeWatchlistByCategory(category: String): Flow<List<WatchlistEntity>>

    @Query(
        """
        SELECT * FROM watchlist
        WHERE title LIKE '%' || :query || '%'
           OR synopsis LIKE '%' || :query || '%'
           OR genres_csv LIKE '%' || :query || '%'
        ORDER BY is_favorite DESC, added_at DESC
        """
    )
    fun searchWatchlist(query: String): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE media_id = :mediaId)")
    fun observeIsInWatchlist(mediaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE media_id = :mediaId)")
    suspend fun isInWatchlist(mediaId: String): Boolean

    @Query("SELECT * FROM watchlist WHERE media_id = :mediaId LIMIT 1")
    suspend fun getWatchlistItemById(mediaId: String): WatchlistEntity?

    @Query("SELECT COUNT(*) FROM watchlist")
    fun observeWatchlistCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlistItem(item: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchlistItems(items: List<WatchlistEntity>)

    @Update
    suspend fun updateWatchlistItem(item: WatchlistEntity)

    @Query("UPDATE watchlist SET is_favorite = :isFavorite WHERE media_id = :mediaId")
    suspend fun updateFavoriteFlag(mediaId: String, isFavorite: Boolean)

    @Delete
    suspend fun deleteWatchlistEntity(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE media_id = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)

    @Query("DELETE FROM watchlist")
    suspend fun clearWatchlist()
}
