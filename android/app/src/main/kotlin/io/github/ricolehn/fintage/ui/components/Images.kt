package io.github.ricolehn.fintage.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.ricolehn.fintage.data.ImageSource
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.util.BlurHash

@Composable
fun rememberBlurHash(hash: String?, width: Int = 24, height: Int = 24): ImageBitmap? =
    remember(hash, width, height) { BlurHash.decode(hash, width, height)?.asImageBitmap() }

/**
 * An image of the server with its BlurHash as a soft placeholder in its own colors, so a card never shows
 * an empty box while its image loads.
 */
@Composable
fun FintageImage(
    source: ImageSource?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    colorFilter: ColorFilter? = null,
) {
    val placeholder = rememberBlurHash(source?.blurHash)
    val context = LocalPlatformContext.current
    Box(modifier) {
        if (placeholder != null) {
            Image(
                bitmap = placeholder,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().background(Fintage.colors.cardGradient))
        }
        if (source != null) {
            val request = remember(source.url) {
                ImageRequest.Builder(context).data(source.url).crossfade(350).build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alignment = alignment,
                colorFilter = colorFilter,
            )
        }
    }
}

/** The clear logo of a title, or its name in large type when there is no logo or it fails to load. */
@Composable
fun LogoOrTitle(
    logo: ImageSource?,
    title: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    textAlign: TextAlign = TextAlign.Center,
    alignment: Alignment = Alignment.Center,
) {
    var failed by remember(logo?.url) { mutableStateOf(false) }
    if (logo == null || failed) {
        Text(
            text = title,
            style = textStyle,
            textAlign = textAlign,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    } else {
        val context = LocalPlatformContext.current
        val request = remember(logo.url) { ImageRequest.Builder(context).data(logo.url).crossfade(400).build() }
        AsyncImage(
            model = request,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            alignment = alignment,
            onState = { if (it is AsyncImagePainter.State.Error) failed = true },
        )
    }
}
