package io.github.ricolehn.fintage.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase

/** Scales a pressed card down a little, with the slow ease of the theme. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.965f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(if (pressed) 120 else 420, easing = FintageEase),
        label = "press",
    )
    return this.scale(scale)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onMore: (() -> Unit)? = null,
    accent: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onMore != null) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onMore)
                else Modifier,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (accent != null) {
            AccentBar(accent, Modifier.height(if (subtitle != null) 36.dp else 18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Fintage.colors.dimText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onMore != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .border(1.25.dp, Fintage.colors.dimText.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "See all",
                    tint = Fintage.colors.dimText,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** The white pill of the theme (espresso in the light theme), for Play and Resume. */
@Composable
fun PlayPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.PlayArrow,
    progress: Float? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = Fintage.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.98f)
            .heightIn(min = 52.dp)
            .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(colors.playContainer.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.playContent,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(icon, contentDescription = null, tint = colors.playContent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = colors.playContent, maxLines = 1)
        }
        if (progress != null && progress > 0f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(colors.gold),
            )
        }
    }
}

/** The gold button of the forms of the theme, like Sign In. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.98f)
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Fintage.colors.goldGradient, alpha = if (enabled) 1f else 0.45f)
            .border(0.75.dp, Color(0x66FFF3D6), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), enabled = enabled && !loading, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFF1B160D), strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B160D))
        }
    }
}

/** A quiet glass pill, like Use Quick Connect. */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
) {
    val colors = Fintage.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction)
            .clip(CircleShape)
            .background(colors.glass)
            .border(0.75.dp, colors.glassBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** A round icon button on glass, used for the actions under the play button. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 46.dp,
    glass: Boolean = true,
) {
    val colors = Fintage.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.92f)
            .size(size)
            .clip(CircleShape)
            .then(if (glass) Modifier.background(colors.glass).border(0.75.dp, colors.glassBorder, CircleShape) else Modifier)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(bounded = false), role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** The star of the rating in the colors of TMDB, where the rating comes from. */
@Composable
fun RatingStar(value: String, modifier: Modifier = Modifier) {
    val brush = Fintage.colors.starGradient
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.Star,
            contentDescription = "Rating",
            modifier = Modifier
                .size(17.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache { onDrawWithContent { drawContent(); drawRect(brush, blendMode = BlendMode.SrcAtop) } },
        )
        Spacer(Modifier.width(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OfficialRatingPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
            .border(1.dp, Fintage.colors.dimText.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Small capitals over a value, like AUDIO or DIRECTORS in the theme. */
@Composable
fun SmallCapsLabel(text: String, modifier: Modifier = Modifier, color: Color = Fintage.colors.dimText) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/** A row of short facts separated by quiet dots: 2025 · 1h 58m · PG-13 · ★ 7.9 */
@Composable
fun MetaRow(
    modifier: Modifier = Modifier,
    centered: Boolean = true,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, if (centered) Alignment.CenterHorizontally else Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun MetaText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f), maxLines = 1)
}

@Composable
fun GoldProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 3.dp) {
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.28f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(CircleShape)
                .background(Fintage.colors.gold),
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator(color = Fintage.colors.gold)
    }
}

@Composable
fun MessageBox(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Fintage.colors.glass),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Fintage.colors.dimText,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onAction, colors = ButtonDefaults.filledTonalButtonColors()) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageBox(
        icon = Icons.Rounded.CloudOff,
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        actionLabel = "Try again",
        onAction = onRetry,
    )
}

/** A pill for a genre on glass; genres lead to their pages everywhere in the app. */
@Composable
fun GenrePill(name: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    val colors = Fintage.colors
    val interaction = remember { MutableInteractionSource() }
    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onChipSelected else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
            .pressScale(interaction)
            .clip(CircleShape)
            .background(if (selected) colors.chipSelected else colors.glass)
            .border(0.75.dp, if (selected) Color.Transparent else colors.glassBorder, CircleShape)
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

fun Modifier.verticalFade(brush: Brush) = this.drawWithCache {
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}
