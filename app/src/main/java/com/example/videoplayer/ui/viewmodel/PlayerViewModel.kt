package com.example.videoplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.VideoRepository
import com.example.videoplayer.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    var currentVideoId: Long = -1
        private set

    private var progressJob: Job? = null

    fun setCurrentVideo(videoId: Long) {
        currentVideoId = videoId
    }

    fun startProgressSaver(getPosition: () -> Long) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                if (currentVideoId > 0) {
                    val position = getPosition()
                    withContext(Dispatchers.IO) {
                        repository.savePlayHistory(currentVideoId, position)
                    }
                }
            }
        }
    }

    fun saveProgress(position: Long) {
        if (currentVideoId > 0) {
            val videoId = currentVideoId
            viewModelScope.launch(Dispatchers.IO) {
                repository.savePlayHistory(videoId, position)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
