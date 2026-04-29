package com.example.videoplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoplayer.data.VideoRepository
import com.example.videoplayer.data.db.AppDatabase
import com.example.videoplayer.data.db.PlayHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private var currentVideoId: Long = -1
    private var progressJob: Job? = null

    fun play(uri: String, videoId: Long, startPosition: Long = 0) {
        currentVideoId = videoId
        progressJob?.cancel()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        if (startPosition > 0) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
        startProgressSaver()
    }

    private fun startProgressSaver() {
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                if (currentVideoId > 0 && player.isPlaying) {
                    saveProgressInternal()
                }
            }
        }
    }

    private suspend fun saveProgressInternal() {
        if (currentVideoId > 0 && player.contentDuration > 0) {
            withContext(Dispatchers.IO) {
                repository.savePlayHistory(
                    currentVideoId,
                    player.currentPosition
                )
            }
        }
    }

    fun saveProgress() {
        if (currentVideoId > 0 && player.contentDuration > 0) {
            val position = player.currentPosition
            val videoId = currentVideoId
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.savePlayHistory(videoId, position)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        // Save synchronously since viewModelScope is cancelled
        if (currentVideoId > 0 && player.contentDuration > 0) {
            kotlinx.coroutines.runBlocking {
                repository.savePlayHistory(currentVideoId, player.currentPosition)
            }
        }
        player.release()
    }
}
