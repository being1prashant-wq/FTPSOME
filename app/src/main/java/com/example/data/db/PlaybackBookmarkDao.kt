package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PlaybackBookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackBookmarkDao {
    @Query("SELECT * FROM playback_bookmarks ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentBookmarks(): Flow<List<PlaybackBookmark>>

    @Query("SELECT * FROM playback_bookmarks WHERE fullPath = :fullPath LIMIT 1")
    suspend fun getBookmark(fullPath: String): PlaybackBookmark?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBookmark(bookmark: PlaybackBookmark)

    @Query("DELETE FROM playback_bookmarks WHERE fullPath = :fullPath")
    suspend fun deleteBookmark(fullPath: String)

    @Query("DELETE FROM playback_bookmarks")
    suspend fun clearAll()
}
