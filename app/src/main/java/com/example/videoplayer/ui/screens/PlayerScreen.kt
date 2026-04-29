package com.example.videoplayer.ui.screens

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.example.videoplayer.ui.viewmodel.PlayerViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }

    // Hide system bars for immersive mode
    DisposableEffect(Unit) {
        activity?.let { act ->
            val window = act.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.let { act ->
                val window = act.window
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    // Pause/resume with lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.player.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.player.playbackState == androidx.media3.common.Player.STATE_READY) {
                        viewModel.player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Save progress and release on exit
    BackHandler {
        viewModel.saveProgress()
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures {
                    if (!isLocked) {
                        controlsVisible = !controlsVisible
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val width = size.width
                    val height = size.height
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        // Horizontal: seek
                        val seekMs = ((dragAmount.x / width) * 120_000L).toLong()
                        viewModel.player.seekTo(
                            (viewModel.player.currentPosition + seekMs).coerceIn(
                                0L,
                                viewModel.player.duration.coerceAtLeast(0L)
                            )
                        )
                    } else {
                        if (change.position.x < width / 2) {
                            // Left vertical: brightness
                            activity?.let { act ->
                                val window = act.window
                                val layout = window.attributes
                                layout.screenBrightness = (layout.screenBrightness - dragAmount.y / height * 0.5f)
                                    .coerceIn(0f, 1f)
                                window.attributes = layout
                            }
                        } else {
                            // Right vertical: volume
                            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
                                as android.media.AudioManager
                            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val newVolume = (currentVolume - (dragAmount.y / height * maxVolume).toInt())
                                .coerceIn(0, maxVolume)
                            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0)
                        }
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible && !isLocked) {
            PlayerControls(
                viewModel = viewModel,
                onBack = {
                    viewModel.saveProgress()
                    onBack()
                },
                onLock = { isLocked = true },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (isLocked) {
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Unlock", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlayerControls(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .statusBarsPadding()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatPosition(viewModel.player.currentPosition) + " / " +
                        formatPosition(viewModel.player.duration.coerceAtLeast(0)),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLock) {
                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.White)
            }
        }

        LinearProgressIndicator(
            progress = {
                val duration = viewModel.player.duration.coerceAtLeast(1)
                (viewModel.player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.player.seekBack() }) {
                Text("⏪", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = {
                if (viewModel.player.isPlaying) viewModel.player.pause()
                else viewModel.player.play()
            }) {
                Text(
                    if (viewModel.player.isPlaying) "⏸" else "▶",
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { viewModel.player.seekForward() }) {
                Text("⏩", color = Color.White)
            }
        }
    }
}

private fun formatPosition(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
