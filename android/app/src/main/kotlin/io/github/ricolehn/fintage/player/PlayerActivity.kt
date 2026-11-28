@file:OptIn(UnstableApi::class)

package io.github.ricolehn.fintage.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.MediaSession
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.theme.FintageTheme
import kotlinx.coroutines.launch

class PlayerActivity : ComponentActivity() {
    private val viewModel: PlayerViewModel by viewModels()
    private var mediaSession: MediaSession? = null
    private var inPictureInPicture by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBars()

        intent.toPlayRequest()?.let(viewModel::start) ?: finish()
        mediaSession = runCatching { MediaSession.Builder(this, viewModel.player).build() }.getOrNull()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { ui ->
                    updatePictureInPictureParams(ui.isPlaying, ui.videoWidth, ui.videoHeight)
                    if (ui.finished) finish()
                }
            }
        }

        setContent {
            FintageTheme(darkTheme = true, filmGrain = false) {
                PlayerScreen(
                    viewModel = viewModel,
                    inPictureInPicture = inPictureInPicture,
                    canPictureInPicture = supportsPictureInPicture(),
                    onEnterPictureInPicture = ::enterPictureInPicture,
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.toPlayRequest()?.let(viewModel::start)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun supportsPictureInPicture(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun pictureInPictureParams(playing: Boolean, width: Int, height: Int): PictureInPictureParams {
        val ratio = (width.toFloat() / height).coerceIn(1 / 2.39f, 2.39f)
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setAutoEnterEnabled(playing)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun updatePictureInPictureParams(playing: Boolean, width: Int, height: Int) {
        if (!supportsPictureInPicture()) return
        runCatching { setPictureInPictureParams(pictureInPictureParams(playing, width, height)) }
    }

    private fun enterPictureInPicture() {
        if (!supportsPictureInPicture()) return
        val ui = viewModel.ui.value
        runCatching { enterPictureInPictureMode(pictureInPictureParams(ui.isPlaying, ui.videoWidth, ui.videoHeight)) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // from Android 12 on, the window goes into picture-in-picture by itself while a video plays
        if (Build.VERSION.SDK_INT < 31 && viewModel.player.isPlaying) enterPictureInPicture()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPictureInPicture = isInPictureInPictureMode
        // the small window was closed rather than opened full screen: the player is done
        if (!isInPictureInPictureMode && lifecycle.currentState == Lifecycle.State.CREATED) finish()
    }

    override fun onStop() {
        super.onStop()
        // closing the picture-in-picture window or leaving without it pauses the video
        if (!isInPictureInPictureMode || isFinishing) viewModel.player.pause()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ITEM = "item_id"
        private const val EXTRA_START = "start_ticks"
        private const val EXTRA_AUDIO = "audio_index"
        private const val EXTRA_SUBTITLE = "subtitle_index"
        private const val NONE = Long.MIN_VALUE
        private const val NO_TRACK = Int.MIN_VALUE

        fun start(context: Context, request: PlayRequest) {
            val intent = Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_ITEM, request.itemId)
                .putExtra(EXTRA_START, request.startPositionTicks ?: NONE)
                .putExtra(EXTRA_AUDIO, request.audioStreamIndex ?: NO_TRACK)
                .putExtra(EXTRA_SUBTITLE, request.subtitleStreamIndex ?: NO_TRACK)
            context.startActivity(intent)
        }

        private fun Intent.toPlayRequest(): PlayRequest? {
            val itemId = getStringExtra(EXTRA_ITEM) ?: return null
            return PlayRequest(
                itemId = itemId,
                startPositionTicks = getLongExtra(EXTRA_START, NONE).takeIf { it != NONE },
                audioStreamIndex = getIntExtra(EXTRA_AUDIO, NO_TRACK).takeIf { it != NO_TRACK },
                subtitleStreamIndex = getIntExtra(EXTRA_SUBTITLE, NO_TRACK).takeIf { it != NO_TRACK },
            )
        }
    }
}
