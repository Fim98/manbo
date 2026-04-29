package com.example.videoplayer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.videoplayer.data.db.PlayHistoryEntity
import com.example.videoplayer.data.db.VideoEntity
import com.example.videoplayer.ui.components.RecentPlayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentPlayScreen(
    videos: List<VideoEntity>,
    history: List<PlayHistoryEntity>,
    onVideoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val videoMap = remember(videos) { videos.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Recent") })
        },
        modifier = modifier
    ) { innerPadding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent plays yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(history, key = { it.videoId }) { entry ->
                    val video = videoMap[entry.videoId]
                    if (video != null) {
                        RecentPlayItem(
                            fileName = video.fileName,
                            thumbnailPath = video.thumbnailPath,
                            position = entry.position,
                            duration = video.duration,
                            lastPlayedAt = entry.lastPlayedAt,
                            onClick = { onVideoClick(video.id) }
                        )
                    }
                }
            }
        }
    }
}
