package com.manbo.videoplayer

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.manbo.videoplayer.ui.theme.VideoplayerTheme

class MainActivity : ComponentActivity() {

    var isPlayerActive: Boolean = false
    var videoAspectRatio: Rational = Rational(16, 9)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setPlayerCutoutEnabled(enabled = false)
        setContent {
            VideoplayerTheme {
                VideoplayerApp()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerActive) {
            enterPipMode()
        }
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(videoAspectRatio)
                .build()
            enterPictureInPictureMode(params)
        }
    }

    fun enterPlayerLandscapeFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    fun exitPlayerLandscapeFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun isPlayerLandscapeFullscreen(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun setPlayerCutoutEnabled(enabled: Boolean) {
        applyPlayerCutoutMode(enabled = enabled)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            isPlayerActive = false
        }
    }

    private fun applyPlayerCutoutMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = when {
                !enabled -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
}
