package com.example.videoplayer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.VideoRepository
import com.example.videoplayer.data.db.AppDatabase
import com.example.videoplayer.data.db.VideoEntity
import com.example.videoplayer.util.VideoMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository.getRepository(
        AppDatabase.getDatabase(application)
    )

    val videos: StateFlow<List<VideoEntity>> = repository.getAllVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlays = repository.getRecentPlays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importVideos(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            for (uri in uris) {
                try {
                    // Persist URI permission for access across reboots
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                    val tempId = System.currentTimeMillis()
                    val metadata = withContext(Dispatchers.IO) {
                        VideoMetadataExtractor.extract(context, uri, tempId)
                    }
                    val fileName = getFileName(context, uri)

                    val entity = VideoEntity(
                        uri = uri.toString(),
                        fileName = fileName,
                        duration = metadata.duration,
                        thumbnailPath = metadata.thumbnailPath,
                        addedAt = System.currentTimeMillis()
                    )
                    val insertedId = repository.insertVideo(entity)

                    // Re-extract thumbnail with real ID if different from tempId
                    if (insertedId != tempId) {
                        val finalMetadata = withContext(Dispatchers.IO) {
                            VideoMetadataExtractor.extract(context, uri, insertedId)
                        }
                        repository.deleteVideo(entity)
                        repository.insertVideo(
                            entity.copy(
                                id = insertedId,
                                thumbnailPath = finalMetadata.thumbnailPath
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Skip files that fail to extract
                }
            }
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "Unknown"
    }
}
