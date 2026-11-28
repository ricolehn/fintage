package io.github.ricolehn.fintage.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase

/** The milk glass of the app bar of the theme: the page glows through a blur with a faint white haze. */
@Composable
fun fintageHazeStyle(): HazeStyle {
    val background = MaterialTheme.colorScheme.background
    val dark = Fintage.colors.isDark
    return HazeStyle(
        backgroundColor = background,
        tints = if (dark) {
            listOf(HazeTint(Color.White.copy(alpha = 0.035f)), HazeTint(background.copy(alpha = 0.6f)))
        } else {
            listOf(HazeTint(Color(0xFFFAF7F1).copy(alpha = 0.45f)), HazeTint(background.copy(alpha = 0.5f)))
        },
        blurRadius = 26.dp,
        noiseFactor = 0.04f,
        fallbackTint = HazeTint(background.copy(alpha = 0.94f)),
    )
}

/**
 * The app bar of a page. It is clear while the page is at its top and turns into frosted glass with a clean
 * bottom edge as soon as content scrolls under it.
 */
@Composable
fun FrostedTopBar(
    hazeState: HazeState,
    frosted: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    showTitle: Boolean = true,
    onBack: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val glass by animateFloatAsState(if (frosted) 1f else 0f, tween(320, easing = FintageEase), label = "frost")
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, tween(320, easing = FintageEase), label = "title")
    val style = fintageHazeStyle()
    val edge = Fintage.colors.glassBorder
    Column(
        modifier
            .fillMaxWidth()
            .hazeEffect(hazeState, style) { alpha = glass }
            .drawBehind {
                if (glass > 0f) {
                    drawLine(edge.copy(alpha = edge.alpha * glass), Offset(0f, size.height), Offset(size.width, size.height), 1f)
                }
            },
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Box(
                Modifier
                    .weight(1f)
                    .alpha(titleAlpha),
            ) {
                when {
                    titleContent != null -> titleContent()
                    title != null -> Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = if (onBack != null) 4.dp else 4.dp),
                    )
                }
            }
            actions()
        }
        bottomContent?.invoke()
    }
}

/** The height of the frosted app bar without the status bar. */
val TopBarHeight = 56.dp
