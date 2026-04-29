package com.manbo.videoplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val fileName: String,
    val duration: Long,
    val thumbnailPath: String,
    val addedAt: Long
)
