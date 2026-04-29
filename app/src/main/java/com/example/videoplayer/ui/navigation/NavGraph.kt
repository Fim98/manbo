package com.example.videoplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.videoplayer.data.db.VideoEntity
import com.example.videoplayer.ui.screens.PlayerScreen
import com.example.videoplayer.ui.screens.RecentPlayScreen
import com.example.videoplayer.ui.screens.VideoListScreen
import com.example.videoplayer.ui.viewmodel.PlayerViewModel
import com.example.videoplayer.ui.viewmodel.VideoViewModel

object Routes {
    const val VIDEO_LIST = "videoList"
    const val RECENT_PLAY = "recentPlay"
    const val PLAYER = "player/{videoId}"

    fun playerRoute(videoId: Long) = "player/$videoId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    videoViewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.VIDEO_LIST,
        modifier = modifier
    ) {
        composable(Routes.VIDEO_LIST) {
            VideoListScreen(
                videos = videoViewModel.videos.value,
                onVideoClick = { videoId ->
                    navController.navigate(Routes.playerRoute(videoId))
                },
                onVideoLongClick = { video: VideoEntity ->
                    videoViewModel.deleteVideo(video)
                },
                onImport = { uris -> videoViewModel.importVideos(uris) }
            )
        }

        composable(Routes.RECENT_PLAY) {
            RecentPlayScreen(
                videos = videoViewModel.videos.value,
                history = videoViewModel.recentPlays.value,
                onVideoClick = { videoId ->
                    navController.navigate(Routes.playerRoute(videoId))
                }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: return@composable
            val video = videoViewModel.videos.value.find { it.id == videoId }

            if (video != null) {
                val playerViewModel: PlayerViewModel = viewModel()
                val history = videoViewModel.recentPlays.value.find { it.videoId == videoId }
                val startPosition = history?.position ?: 0L

                PlayerScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() }
                )

                // Trigger playback once
                androidx.compose.runtime.LaunchedEffect(video.uri) {
                    playerViewModel.play(video.uri, video.id, startPosition)
                }
            }
        }
    }
}
