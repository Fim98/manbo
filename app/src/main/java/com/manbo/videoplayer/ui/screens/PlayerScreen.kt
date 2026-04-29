package com.manbo.videoplayer.ui.screens

import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.manbo.videoplayer.MainActivity
import com.manbo.videoplayer.ui.viewmodel.PlayerViewModel
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var currentSpeed by remember { mutableStateOf(1f) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var isPipMode by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }

    val gsyPlayer = remember {
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        StandardGSYVideoPlayer(context).apply {
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
            GSYVideoType.setRenderType(GSYVideoType.TEXTURE)
            isIfCurrentIsFullscreen = true
            setIsTouchWiget(true)
            setIsTouchWigetFull(true)

            backButton.visibility = android.view.View.GONE

            fullscreenButton.setOnClickListener {
                activity?.let { act ->
                    if (act.isPlayerLandscapeFullscreen()) {
                        act.exitPlayerLandscapeFullscreen()
                    } else {
                        act.enterPlayerLandscapeFullscreen()
                    }
                }
            }

        }
    }

    DisposableEffect(activity, isLandscape) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            act.setPlayerCutoutEnabled(true)
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            act.isPlayerActive = true
        }

        onDispose { }
    }

    // Set up video and start playing
    DisposableEffect(videoUri) {

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
                act.setPlayerCutoutEnabled(false)
                act.exitPlayerLandscapeFullscreen()
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(isLandscape) {
        gsyPlayer.isIfCurrentIsFullscreen = isLandscape
        if (!isLandscape) {
            isControlsLocked = false
            speedMenuExpanded = false
        }
        onDispose { }
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
        if (activity?.isPlayerLandscapeFullscreen() == true) {
            activity.exitPlayerLandscapeFullscreen()
            return@BackHandler
        }
        viewModel.saveProgress(gsyPlayer.currentPlayer.currentPositionWhenPlaying)
        onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { gsyPlayer },
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { isControlsLocked }
        )

        // Hide overlays in PiP mode
        if (!isPipMode && !isControlsLocked) {
            // Speed control (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = if (isLandscape) 16.dp else 48.dp, end = 16.dp)
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

        if (!isPipMode && isLandscape) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        isControlsLocked = !isControlsLocked
                        if (isControlsLocked) {
                            speedMenuExpanded = false
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isControlsLocked) "Unlock controls" else "Lock controls",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
