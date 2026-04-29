package com.example.videoplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.videoplayer.data.VideoRepository
import com.example.videoplayer.data.db.AppDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private var currentVideoId: Long = -1

    fun play(uri: String, videoId: Long, startPosition: Long = 0) {
        currentVideoId = videoId
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        if (startPosition > 0) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
        startProgressSaver()
    }

    private fun startProgressSaver() {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                if (currentVideoId > 0 && player.isPlaying) {
                    saveProgress()
                }
            }
        }
    }

    fun saveProgress() {
        if (currentVideoId > 0 && player.contentDuration > 0) {
            viewModelScope.launch {
                repository.savePlayHistory(
                    currentVideoId,
                    player.currentPosition
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgress()
        player.release()
    }
}
