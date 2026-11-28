@file:OptIn(UnstableApi::class)

package io.github.ricolehn.fintage.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import io.github.ricolehn.fintage.ui.components.GoldButton
import io.github.ricolehn.fintage.ui.components.SmallCapsLabel
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase
import io.github.ricolehn.fintage.ui.theme.Palette
import io.github.ricolehn.fintage.ui.theme.glass
import io.github.ricolehn.fintage.util.Format
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    inPictureInPicture: Boolean,
    canPictureInPicture: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onClose: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val player = viewModel.player
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableLongStateOf(0L) }
    var showTracks by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controlsVisible, ui.isPlaying, interaction) {
        if (controlsVisible && ui.isPlaying) {
            delay(3_500)
            controlsVisible = false
        }
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            delay(700)
            seekFeedback = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setKeepContentOnPlayerReset(true)
                    this.player = player
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(true)
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                android.graphics.Color.BLACK,
                                null,
                            ),
                        )
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.1f)
                        setBottomPaddingFraction(0.06f)
                    }
                }
            },
            update = { view ->
                view.resizeMode = if (ui.zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPictureInPicture) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interaction = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2f) {
                                    viewModel.seekBy(-10_000)
                                    seekFeedback = "− 10 s"
                                } else {
                                    viewModel.seekBy(10_000)
                                    seekFeedback = "+ 10 s"
                                }
                            },
                        )
                    },
            )
        }

        if (ui.loading || ui.buffering) {
            LoadingIndicator(color = Palette.Gold, modifier = Modifier.align(Alignment.Center).size(64.dp))
        }

        seekFeedback?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && !inPictureInPicture && ui.error == null,
            enter = fadeIn(tween(260, easing = FintageEase)),
            exit = fadeOut(tween(420, easing = FintageEase)),
        ) {
            Controls(
                viewModel = viewModel,
                ui = ui,
                canPictureInPicture = canPictureInPicture,
                onEnterPictureInPicture = onEnterPictureInPicture,
                onTracks = { showTracks = true },
                onClose = onClose,
                onInteraction = { interaction = System.currentTimeMillis() },
            )
        }

        ui.error?.let { message ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 420.dp)
                    .glass(RoundedCornerShape(20.dp), strong = true)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Cannot play this title", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onClose) { Text("Close") }
                    GoldButton("Try again", onClick = viewModel::retry, modifier = Modifier.width(140.dp))
                }
            }
        }

        val upNext = ui.upNext
        if (upNext != null && !inPictureInPicture) {
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(24.dp)
                    .widthIn(max = 340.dp)
                    .glass(RoundedCornerShape(18.dp), strong = true)
                    .padding(16.dp),
            ) {
                SmallCapsLabel("Up next in ${ui.upNextSeconds}")
                Spacer(Modifier.height(4.dp))
                Text(
                    listOfNotNull(Format.episodeCode(upNext), upNext.name).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::cancelUpNext) { Text("Cancel") }
                    GoldButton("Play now", onClick = { viewModel.playNext() }, modifier = Modifier.width(130.dp))
                }
            }
        }
    }

    if (showTracks) {
        TrackSheet(viewModel, ui, onDismiss = { showTracks = false })
    }
}

@Composable
private fun Controls(
    viewModel: PlayerViewModel,
    ui: PlayerUi,
    canPictureInPicture: Boolean,
    onEnterPictureInPicture: () -> Unit,
    onTracks: () -> Unit,
    onClose: () -> Unit,
    onInteraction: () -> Unit,
) {
    val player = viewModel.player
    var position by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            if (!dragging) position = player.currentPosition
            duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0
            delay(400)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.7f),
                        0.3f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close", tint = Color.White)
                }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ui.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (canPictureInPicture) {
                    IconButton(onClick = onEnterPictureInPicture) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
                    }
                }
                IconButton(onClick = onTracks) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = "Audio and subtitles", tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(Icons.Rounded.Replay10, "Back 10 seconds", size = 56) {
                    viewModel.seekBy(-10_000)
                    onInteraction()
                }
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Palette.PlayWhite)
                        .clickable {
                            if (player.isPlaying) player.pause() else player.play()
                            onInteraction()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (ui.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (ui.isPlaying) "Pause" else "Play",
                        tint = Palette.Ink,
                        modifier = Modifier.size(40.dp),
                    )
                }
                RoundButton(Icons.Rounded.Forward30, "Forward 30 seconds", size = 56) {
                    viewModel.seekBy(30_000)
                    onInteraction()
                }
            }
            Spacer(Modifier.weight(1f))
            val shown = if (dragging) (dragValue * duration).toLong() else position
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(formatTime(shown), style = MaterialTheme.typography.labelMedium, color = Color.White)
                Slider(
                    value = if (dragging) dragValue else if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = {
                        dragging = true
                        dragValue = it
                        onInteraction()
                    },
                    onValueChangeFinished = {
                        player.seekTo((dragValue * duration).toLong())
                        position = (dragValue * duration).toLong()
                        dragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Palette.Gold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Text("−" + formatTime((duration - shown).coerceAtLeast(0)), style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpeedButton(ui.speed, onSpeed = {
                    viewModel.setSpeed(it)
                    onInteraction()
                })
                IconButton(onClick = {
                    viewModel.toggleZoom()
                    onInteraction()
                }) {
                    Icon(Icons.Rounded.ZoomOutMap, contentDescription = if (ui.zoom) "Fit to screen" else "Fill the screen", tint = if (ui.zoom) Palette.Gold else Color.White)
                }
                Spacer(Modifier.weight(1f))
                if (ui.session?.item?.type == "Episode") {
                    TextButton(onClick = viewModel::skipToNextEpisode) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Next episode", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, size: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
private fun SpeedButton(speed: Float, onSpeed: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Speed, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(if (speed == 1f) "Speed" else "${speed}×", color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
                DropdownMenuItem(
                    text = { Text(if (value == 1f) "Normal" else "${value}×") },
                    onClick = {
                        expanded = false
                        onSpeed(value)
                    },
                    trailingIcon = if (value == speed) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = Palette.Gold) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun TrackSheet(viewModel: PlayerViewModel, ui: PlayerUi, onDismiss: () -> Unit) {
    val session = ui.session ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            if (session.audioStreams.isNotEmpty()) {
                item { SmallCapsLabel("Audio", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
                items(session.audioStreams, key = { "a" + it.index }) { stream ->
                    TrackRow(Format.audioTrack(stream), selected = stream.index == session.audioIndex) {
                        viewModel.selectAudio(stream.index)
                        onDismiss()
                    }
                }
            }
            item { SmallCapsLabel("Subtitles", modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)) }
            item(key = "off") {
                TrackRow("Off", selected = session.subtitleIndex < 0) {
                    viewModel.selectSubtitle(-1)
                    onDismiss()
                }
            }
            items(session.subtitleStreams, key = { "s" + it.index }) { stream ->
                TrackRow(Format.subtitleTrack(stream), selected = stream.index == session.subtitleIndex) {
                    viewModel.selectSubtitle(stream.index)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun TrackRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Palette.Gold)
    }
}

private fun formatTime(millis: Long): String {
    val total = millis / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
