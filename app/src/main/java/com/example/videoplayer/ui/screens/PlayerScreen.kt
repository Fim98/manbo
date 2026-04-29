package com.example.videoplayer.ui.screens

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.videoplayer.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
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
    var accumulatedSeekMs by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    // Gesture feedback state
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    // Slider state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }

    // Track player position for slider
    val exoPlayer = viewModel.player
    var currentPosition by remember { mutableLongStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableLongStateOf(exoPlayer.duration.coerceAtLeast(0)) }

    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(reason: Int) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0)
            }
        }
        exoPlayer.addListener(listener)
        // Poll position for smooth slider
        while (true) {
            delay(250)
            if (!isSliderDragging) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0)
            }
        }
    }

    // Update slider from position (when not dragging)
    if (!isSliderDragging && duration > 0) {
        sliderPosition = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(controlsVisible, isSliderDragging) {
        if (controlsVisible && !isSliderDragging) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Auto-dismiss gesture feedback after 1 second
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1000)
            gestureFeedback = null
        }
    }

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
                    if (viewModel.player.playbackState == Player.STATE_READY) {
                        viewModel.player.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                detectDragGestures(
                    onDragStart = { isSeeking = true },
                    onDragEnd = {
                        isSeeking = false
                        if (abs(accumulatedSeekMs) > 1000) {
                            viewModel.player.seekTo(
                                (viewModel.player.currentPosition + accumulatedSeekMs.toLong())
                                    .coerceIn(0, viewModel.player.duration.coerceAtLeast(0))
                            )
                        }
                        accumulatedSeekMs = 0f
                        gestureFeedback = null
                    },
                    onDragCancel = {
                        isSeeking = false
                        accumulatedSeekMs = 0f
                        gestureFeedback = null
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val width = size.width
                    val height = size.height
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        // Horizontal: accumulate seek delta
                        accumulatedSeekMs += (dragAmount.x / width) * 120_000f
                        val seekSeconds = (accumulatedSeekMs / 1000).toInt()
                        gestureFeedback = GestureFeedback.Seek(seekSeconds)
                    } else {
                        if (change.position.x < width / 2) {
                            // Left vertical: brightness
                            activity?.let { act ->
                                val window = act.window
                                val layout = window.attributes
                                val newBrightness = (layout.screenBrightness - dragAmount.y / height * 0.5f)
                                    .coerceIn(0f, 1f)
                                layout.screenBrightness = newBrightness
                                window.attributes = layout
                                gestureFeedback = GestureFeedback.Brightness((newBrightness * 100).toInt())
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
                            val volumePercent = if (maxVolume > 0) (newVolume * 100 / maxVolume) else 0
                            gestureFeedback = GestureFeedback.Volume(volumePercent)
                        }
                    }
                }
            }
    ) {
        // Video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = viewModel.player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture feedback overlay (centered)
        AnimatedVisibility(
            visible = gestureFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureFeedbackOverlay(feedback = gestureFeedback)
        }

        // Top controls bar
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopControlBar(
                onBack = {
                    viewModel.saveProgress()
                    onBack()
                },
                onLock = { isLocked = true }
            )
        }

        // Bottom controls bar
        AnimatedVisibility(
            visible = controlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControlBar(
                currentPosition = currentPosition,
                duration = duration,
                sliderPosition = sliderPosition,
                isPlaying = viewModel.player.isPlaying,
                onSeekChanged = { position ->
                    isSliderDragging = true
                    sliderPosition = position
                },
                onSeekFinished = { position ->
                    isSliderDragging = false
                    val seekTo = (position * duration).toLong()
                    viewModel.player.seekTo(seekTo)
                    currentPosition = seekTo
                },
                onPlayPause = {
                    if (viewModel.player.isPlaying) viewModel.player.pause()
                    else viewModel.player.play()
                },
                onRewind = { viewModel.player.seekTo((viewModel.player.currentPosition - 10000).coerceAtLeast(0)) },
                onForward = { viewModel.player.seekTo(viewModel.player.currentPosition + 10000) }
            )
        }

        // Locked state: only unlock button
        if (isLocked) {
            IconButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Unlock",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// Sealed class for gesture feedback types
private sealed class GestureFeedback {
    data class Brightness(val percent: Int) : GestureFeedback()
    data class Volume(val percent: Int) : GestureFeedback()
    data class Seek(val seconds: Int) : GestureFeedback()
}

// Gesture feedback overlay composable
@Composable
private fun GestureFeedbackOverlay(feedback: GestureFeedback?) {
    if (feedback == null) return

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (feedback) {
                is GestureFeedback.Brightness -> {
                    Text(text = "☀", fontSize = 32.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${feedback.percent}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is GestureFeedback.Volume -> {
                    Text(text = "🔊", fontSize = 32.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${feedback.percent}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                is GestureFeedback.Seek -> {
                    Text(
                        text = if (feedback.seconds >= 0) "⏩" else "⏪",
                        fontSize = 32.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (feedback.seconds >= 0) "+" else ""}${feedback.seconds}s",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Top control bar: back + lock
@Composable
private fun TopControlBar(
    onBack: () -> Unit,
    onLock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.Black.copy(alpha = 0.5f)
            )
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onLock) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Lock",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Bottom control bar: time + slider + play controls
@Composable
private fun BottomControlBar(
    currentPosition: Long,
    duration: Long,
    sliderPosition: Float,
    isPlaying: Boolean,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Time row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPosition(currentPosition),
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = formatPosition(duration),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Seek slider
        Slider(
            value = sliderPosition,
            onValueChange = onSeekChanged,
            onValueChangeFinished = { onSeekFinished(sliderPosition) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Play controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRewind) {
                Icon(
                    Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onForward) {
                Icon(
                    Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
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
