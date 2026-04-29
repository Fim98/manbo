package com.example.videoplayer.ui.screens

import android.os.Build
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.videoplayer.MainActivity
import com.example.videoplayer.ui.viewmodel.PlayerViewModel
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import com.shuyu.gsyvideoplayer.utils.GSYVideoType

private val SPEED_OPTIONS = listOf(1f, 1.5f, 2f)

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    videoUri: String,
    startPosition: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentSpeed by remember { mutableStateOf(1f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var isPipMode by remember { mutableStateOf(false) }

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
            act.isPlayerActive = true
        }

        gsyPlayer.setUp(videoUri, true, null)
        if (startPosition > 0) {
            gsyPlayer.setSeekOnStart(startPosition)
        }
        gsyPlayer.startPlayLogic()
        viewModel.startProgressSaver { gsyPlayer.currentPlayer.currentPositionWhenPlaying }

        // Detect PiP mode changes via activity config changes
        val callback = Runnable { isPipMode = activity?.isInPictureInPictureMode == true }

        onDispose {
            callback.run()
            viewModel.stop()
            viewModel.saveProgress(gsyPlayer.currentPlayer.currentPositionWhenPlaying)
            gsyPlayer.onVideoReset()
            gsyPlayer.release()

            activity?.let { act ->
                act.isPlayerActive = false
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Lifecycle pause/resume + PiP state tracking
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            activity?.isInPictureInPictureMode == true
                    isPipMode = inPip
                    if (!inPip) {
                        gsyPlayer.onVideoPause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                            activity?.isInPictureInPictureMode == true
                    isPipMode = inPip
                    if (!inPip) {
                        gsyPlayer.onVideoResume()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity?.isPlayerActive == true) {
            activity.enterPipMode()
        } else {
            viewModel.saveProgress(gsyPlayer.currentPlayer.currentPositionWhenPlaying)
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { gsyPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // Hide overlays in PiP mode
        if (!isPipMode) {
            // Speed control (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                // Speed button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { speedMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${currentSpeed}x",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                DropdownMenu(
                    expanded = speedMenuExpanded,
                    onDismissRequest = { speedMenuExpanded = false },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.85f))
                ) {
                    SPEED_OPTIONS.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (speed == currentSpeed) "${speed}x  ✓" else "${speed}x",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                currentSpeed = speed
                                speedMenuExpanded = false
                                gsyPlayer.setSpeed(speed, true)
                            }
                        )
                    }
                }
            }
        }
    }
}
