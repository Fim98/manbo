package com.example.videoplayer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.videoplayer.ui.navigation.NavGraph
import com.example.videoplayer.ui.navigation.Routes
import com.example.videoplayer.ui.viewmodel.VideoViewModel

data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun VideoplayerApp() {
    val navController = rememberNavController()
    val videoViewModel: VideoViewModel = viewModel()

    val tabs = listOf(
        TabItem(Routes.VIDEO_LIST, "Videos", Icons.AutoMirrored.Filled.List),
        TabItem(Routes.RECENT_PLAY, "Recent", Icons.Default.PlayArrow)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(Routes.VIDEO_LIST, Routes.RECENT_PLAY)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            videoViewModel = videoViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
