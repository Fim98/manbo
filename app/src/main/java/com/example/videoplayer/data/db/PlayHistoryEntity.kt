package com.example.videoplayer.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
    foreignKeys = [ForeignKey(
        entity = VideoEntity::class,
        parentColumns = ["id"],
        childColumns = ["videoId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlayHistoryEntity(
    @PrimaryKey val videoId: Long,
    val position: Long,
    val lastPlayedAt: Long
)
