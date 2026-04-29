package com.manbo.videoplayer.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY addedAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Insert
    suspend fun insertVideo(video: VideoEntity): Long

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("SELECT * FROM play_history ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history WHERE videoId = :videoId")
    suspend fun getHistory(videoId: Long): PlayHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(history: PlayHistoryEntity)

    @Query("""
        SELECT h.* FROM play_history h
        INNER JOIN videos v ON h.videoId = v.id
        ORDER BY h.lastPlayedAt DESC
    """)
    fun getHistoryWithExistingVideos(): Flow<List<PlayHistoryEntity>>
}
