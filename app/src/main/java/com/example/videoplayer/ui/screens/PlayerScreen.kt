package com.example.videoplayer.ui.screens

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.videoplayer.ui.viewmodel.PlayerViewModel
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    videoUri: String,
    startPosition: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val gsyPlayer = remember {
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        StandardGSYVideoPlayer(context).apply {
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
            GSYVideoType.setRenderType(GSYVideoType.TEXTURE)
            isIfCurrentIsFullscreen = true

            fullscreenButton.setOnClickListener {
                // Already fullscreen, do nothing
            }
            backButton.setOnClickListener {
                viewModel.saveProgress(currentPlayer.currentPositionWhenPlaying)
                onBack()
            }
        }
    }

    // Set up video and start playing
    DisposableEffect(videoUri) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        gsyPlayer.setUp(videoUri, true, null)
        if (startPosition > 0) {
            gsyPlayer.setSeekOnStart(startPosition)
        }
        gsyPlayer.startPlayLogic()
        viewModel.startProgressSaver { gsyPlayer.currentPlayer.currentPositionWhenPlaying }

        onDispose {
            viewModel.stop()
            viewModel.saveProgress(gsyPlayer.currentPlayer.currentPositionWhenPlaying)
            gsyPlayer.onVideoReset()
            gsyPlayer.release()

            activity?.let { act ->
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Lifecycle pause/resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> gsyPlayer.onVideoPause()
                Lifecycle.Event.ON_RESUME -> gsyPlayer.onVideoResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        viewModel.saveProgress(gsyPlayer.currentPlayer.currentPositionWhenPlaying)
        onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { gsyPlayer },
            modifier = Modifier.fillMaxSize()
        )
    }
}
