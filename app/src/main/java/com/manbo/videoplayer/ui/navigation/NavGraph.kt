package com.manbo.videoplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.manbo.videoplayer.data.db.VideoEntity
import com.manbo.videoplayer.ui.screens.PlayerScreen
import com.manbo.videoplayer.ui.screens.RecentPlayScreen
import com.manbo.videoplayer.ui.screens.VideoListScreen
import com.manbo.videoplayer.ui.viewmodel.PlayerViewModel
import com.manbo.videoplayer.ui.viewmodel.VideoViewModel

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
    val videos by videoViewModel.videos.collectAsState()
    val recentPlays by videoViewModel.recentPlays.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.VIDEO_LIST,
        modifier = modifier
    ) {
        composable(Routes.VIDEO_LIST) {
            VideoListScreen(
                videos = videos,
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
                videos = videos,
                history = recentPlays,
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
            val video = videos.find { it.id == videoId }

            if (video != null) {
                val playerViewModel: PlayerViewModel = viewModel(backStackEntry)
                val history = recentPlays.find { it.videoId == videoId }
                val startPosition = history?.position ?: 0L

                playerViewModel.setCurrentVideo(video.id)

                DisposableEffect(Unit) {
                    onDispose {
                        playerViewModel.stop()
                    }
                }

                PlayerScreen(
                    viewModel = playerViewModel,
                    videoUri = video.uri,
                    startPosition = startPosition,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
