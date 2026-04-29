package com.manbo.videoplayer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    videos: List<VideoEntity>,
    onVideoClick: (Long) -> Unit,
    onVideoLongClick: (VideoEntity) -> Unit,
    onImport: (List<android.net.Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) onImport(uris)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videos") },
                actions = {
                    IconButton(onClick = {
                        launcher.launch(arrayOf("video/*"))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Import video")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No videos yet. Tap + to import.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(
                        fileName = video.fileName,
                        duration = video.duration,
                        thumbnailPath = video.thumbnailPath,
                        onClick = { onVideoClick(video.id) },
                        onLongClick = { onVideoLongClick(video) }
                    )
                }
            }
        }
    }
}
