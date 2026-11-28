package io.github.ricolehn.fintage.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** A tile of soft noise; drawn at a few percent over the page it looks like the grain of film. */
private val grainTile: Bitmap by lazy {
    val size = 160
    val random = Random(7)
    // white pixels of random opacity; the color is applied when the tile is drawn
    val pixels = IntArray(size * size) { (random.nextInt(256) shl 24) or 0x00FFFFFF }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

/**
 * The page of Fintage: the base color with a faint glow at the top and a film grain over it.
 */
@Composable
fun Modifier.fintageBackground(): Modifier {
    val colors = LocalFintageColors.current
    val base = androidx.compose.material3.MaterialTheme.colorScheme.background
    val grain = LocalFilmGrain.current
    val grainBrush = remember {
        ShaderBrush(ImageShader(grainTile.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    return drawBehind {
        drawRect(base)
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.backgroundGlow, colors.backgroundGlow.copy(alpha = 0f)),
                center = Offset(size.width / 2f, -size.height * 0.08f),
                radius = maxOf(size.width * 1.1f, size.height * 0.6f),
            ),
        )
        if (grain) {
            drawRect(
                brush = grainBrush,
                alpha = if (colors.isDark) 0.045f else 0.05f,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.grain, androidx.compose.ui.graphics.BlendMode.SrcIn),
            )
        }
    }
}

/** A glass panel with a hairline edge, like the panels of the web theme. */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(16.dp), strong: Boolean = false): Modifier {
    val colors = LocalFintageColors.current
    return clip(shape)
        .background(if (strong) colors.glassStrong else colors.glass, shape)
        .border(0.75.dp, colors.glassBorder, shape)
}

/**
 * A soft glow in the colors of an image, for the area behind a card: the image is decoded from its BlurHash,
 * so it is already soft. It fills the bounds of this element, which the caller makes a little larger than
 * the card, and fades out in an ellipse towards the edges.
 */
fun Modifier.softGlow(glow: ImageBitmap?, alpha: Float = 0.6f): Modifier {
    if (glow == null) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawBehind {
            drawImage(
                image = glow,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(glow.width, glow.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                alpha = alpha,
            )
            val radius = size.width / 2f
            scale(scaleX = 1f, scaleY = size.height / size.width, pivot = center) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black, Color.Black.copy(alpha = 0.55f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    topLeft = Offset(0f, center.y - radius),
                    size = Size(size.width, size.width),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
            }
        }
}

/**
 * A wash of color over the top of a page, in the colors of its image. The image fades out towards the bottom
 * of the wash, so the page with its glow and grain shows through without an edge.
 */
fun Modifier.colorWash(glow: ImageBitmap?, height: Dp, alpha: Float = 0.45f): Modifier {
    if (glow == null) return this
    return drawWithCache {
        val h = height.toPx()
        val brush = WashBrush(glow.asAndroidBitmap(), alpha)
        onDrawBehind { drawRect(brush, size = Size(size.width, h)) }
    }
}

/** The image stretched over the area, faded out from the top to the bottom in one shader. */
private class WashBrush(private val image: Bitmap, private val alpha: Float) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val imageShader = android.graphics.BitmapShader(image, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
        imageShader.setLocalMatrix(android.graphics.Matrix().apply { setScale(size.width / image.width, size.height / image.height) })
        val top = (alpha * 255).toInt().coerceIn(0, 255)
        val fade = android.graphics.LinearGradient(
            0f, 0f, 0f, size.height,
            intArrayOf(top shl 24, (top * 0.55f).toInt() shl 24, (top * 0.18f).toInt() shl 24, 0),
            floatArrayOf(0f, 0.45f, 0.75f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        return android.graphics.ComposeShader(imageShader, fade, android.graphics.PorterDuff.Mode.DST_IN)
    }

    override fun equals(other: Any?) = other is WashBrush && other.image === image && other.alpha == alpha
    override fun hashCode() = image.hashCode() * 31 + alpha.hashCode()
}

@Composable
fun Modifier.hairlineBottom(): Modifier {
    val color = LocalFintageColors.current.glassBorder
    return drawBehind {
        drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
    }
}

@Composable
fun Modifier.hairlineTop(): Modifier {
    val color = LocalFintageColors.current.glassBorder
    return drawBehind { drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1f) }
}
