package com.manbo.videoplayer.data

import com.manbo.videoplayer.data.db.AppDatabase
import com.manbo.videoplayer.data.db.PlayHistoryEntity
import com.manbo.videoplayer.data.db.VideoDao
import com.manbo.videoplayer.data.db.VideoEntity
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val dao: VideoDao) {

    fun getAllVideos(): Flow<List<VideoEntity>> = dao.getAllVideos()

    suspend fun getVideoById(id: Long): VideoEntity? = dao.getVideoById(id)

    suspend fun insertVideo(video: VideoEntity): Long = dao.insertVideo(video)

    suspend fun deleteVideo(video: VideoEntity) = dao.deleteVideo(video)

    fun getRecentPlays(): Flow<List<PlayHistoryEntity>> = dao.getHistoryWithExistingVideos()

    suspend fun getPlayHistory(videoId: Long): PlayHistoryEntity? = dao.getHistory(videoId)

    suspend fun savePlayHistory(videoId: Long, position: Long) {
        dao.upsertHistory(
            PlayHistoryEntity(
                videoId = videoId,
                position = position,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: VideoRepository? = null

        fun getRepository(database: AppDatabase): VideoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = VideoRepository(database.videoDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
