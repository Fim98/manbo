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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
    var accumulatedSeekMs by remember { mutableFloatStateOf(0f) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    // Slider state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }

    val exoPlayer = viewModel.player
    var currentPosition by remember { mutableLongStateOf(exoPlayer.currentPosition) }
    var duration by remember { mutableLongStateOf(exoPlayer.duration.coerceAtLeast(0)) }

    // Poll player position
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(200)
            if (!isSliderDragging) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0)
            }
        }
    }

    if (!isSliderDragging && duration > 0) {
        sliderPosition = (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
    }

    // Auto-hide after 4 seconds
    LaunchedEffect(controlsVisible, isSliderDragging) {
        if (controlsVisible && !isSliderDragging) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Auto-dismiss gesture feedback
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(800)
            gestureFeedback = null
        }
    }

    // Immersive mode
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

    // Lifecycle pause/resume
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
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    controlsVisible = !controlsVisible
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
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
                        accumulatedSeekMs = 0f
                        gestureFeedback = null
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val width = size.width
                    val height = size.height
                    if (abs(dragAmount.x) > abs(dragAmount.y)) {
                        accumulatedSeekMs += (dragAmount.x / width) * 120_000f
                        gestureFeedback = GestureFeedback.Seek((accumulatedSeekMs / 1000).toInt())
                    } else {
                        if (change.position.x < width / 2) {
                            activity?.let { act ->
                                val window = act.window
                                val attrs = window.attributes
                                val newBrightness = (attrs.screenBrightness - dragAmount.y / height * 0.5f).coerceIn(0f, 1f)
                                attrs.screenBrightness = newBrightness
                                window.attributes = attrs
                                gestureFeedback = GestureFeedback.Brightness((newBrightness * 100).toInt())
                            }
                        } else {
                            val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val new = (cur - (dragAmount.y / height * max).toInt()).coerceIn(0, max)
                            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, new, 0)
                            gestureFeedback = GestureFeedback.Volume(if (max > 0) new * 100 / max else 0)
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

        // Gesture feedback (centered pill)
        AnimatedVisibility(
            visible = gestureFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureFeedbackPill(feedback = gestureFeedback)
        }

        // Top gradient + back button
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Centered play/pause + skip controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = {
                    viewModel.player.seekTo((viewModel.player.currentPosition - 15000).coerceAtLeast(0))
                }) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "Rewind",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = {
                    if (viewModel.player.isPlaying) viewModel.player.pause()
                    else viewModel.player.play()
                }) {
                    Icon(
                        if (viewModel.player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                IconButton(onClick = {
                    viewModel.player.seekTo(viewModel.player.currentPosition + 15000)
                }) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "Forward",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Bottom gradient + slider + time
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 24.dp)
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isSliderDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isSliderDragging = false
                        val seekTo = (sliderPosition * duration).toLong()
                        viewModel.player.seekTo(seekTo)
                        currentPosition = seekTo
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPosition(currentPosition),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatPosition(duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private sealed class GestureFeedback {
    data class Brightness(val percent: Int) : GestureFeedback()
    data class Volume(val percent: Int) : GestureFeedback()
    data class Seek(val seconds: Int) : GestureFeedback()
}

@Composable
private fun GestureFeedbackPill(feedback: GestureFeedback?) {
    if (feedback == null) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (feedback) {
            is GestureFeedback.Brightness -> {
                Text(
                    "☀ ${feedback.percent}%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            is GestureFeedback.Volume -> {
                Text(
                    "🔊 ${feedback.percent}%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            is GestureFeedback.Seek -> {
                val sign = if (feedback.seconds >= 0) "+" else ""
                Text(
                    "$sign${feedback.seconds}s",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
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
