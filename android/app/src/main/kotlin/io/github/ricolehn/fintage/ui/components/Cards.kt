package io.github.ricolehn.fintage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ricolehn.fintage.data.AreaInfo
import io.github.ricolehn.fintage.data.GenreArea
import io.github.ricolehn.fintage.data.GenreInfo
import io.github.ricolehn.fintage.data.ImageSource
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.PersonRef
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.theme.CardShape
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.Palette
import io.github.ricolehn.fintage.util.Format

private val CardShadowColor = Color.Black.copy(alpha = 0.55f)

/** The frame of every card: a soft layered shadow, rounded corners and a faint ring. */
@Composable
private fun Modifier.cardFrame(shape: androidx.compose.ui.graphics.Shape = CardShape): Modifier =
    this
        .shadow(8.dp, shape, ambientColor = CardShadowColor, spotColor = CardShadowColor)
        .clip(shape)
        .border(0.75.dp, Fintage.colors.ring, shape)

@Composable
private fun Modifier.cardClick(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this
        .pressScale(interaction)
        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
}

@Composable
fun PlayedSeal(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(22.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(Fintage.colors.goldGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, contentDescription = "Watched", tint = Color(0xFF1B160D), modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .heightIn(min = 22.dp)
            .widthIn(min = 22.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun MiniPlayButton(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(30.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Palette.PlayWhite.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun CardText(title: String, subtitle: String?, align: TextAlign = TextAlign.Center) {
    Spacer(Modifier.height(7.dp))
    Text(
        title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
    if (!subtitle.isNullOrEmpty()) {
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Fintage.colors.dimText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A poster with the title and year below it, as in the rows and grids of the theme. */
@Composable
fun PosterCard(
    item: BaseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 112.dp,
    showText: Boolean = true,
) {
    val images = LocalImages.current
    val progress = Format.progress(item)
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .cardClick(onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .cardFrame(),
        ) {
            FintageImage(images.poster(item, 360), contentDescription = item.name, modifier = Modifier.fillMaxSize())
            val unplayed = item.userData?.unplayedItemCount ?: 0
            when {
                item.userData?.played == true -> PlayedSeal(Modifier.align(Alignment.TopEnd).padding(6.dp))
                item.type == "Series" && unplayed > 0 -> CountBadge(unplayed, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
            if (progress != null && progress > 0f && progress < 1f && item.userData?.played != true) {
                GoldProgressBar(
                    progress,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }
        if (showText) CardText(Format.cardTitle(item), Format.cardSubtitle(item))
    }
}

/**
 * A 16:9 card for Continue Watching, Next Up and episodes, with the gold progress line and the small white
 * play button of the theme. A movie or show without a thumb shows its backdrop with its logo on top.
 */
@Composable
fun LandscapeCard(
    item: BaseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 176.dp,
    title: String = Format.cardTitle(item),
    subtitle: String? = Format.cardSubtitle(item),
    showPlay: Boolean = true,
) {
    val images = LocalImages.current
    val image = images.landscape(item, 640)
    val progress = Format.progress(item)
    val showLogo = item.type != "Episode" && item.imageTags["Thumb"] == null
    val logo = if (showLogo) images.logo(item, 400) else null
    Column(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .cardClick(onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .cardFrame(),
        ) {
            FintageImage(image, contentDescription = item.name, modifier = Modifier.fillMaxSize())
            if (logo != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
                )
                LogoOrTitle(
                    logo = logo,
                    title = item.name.orEmpty(),
                    textStyle = MaterialTheme.typography.titleSmall.copy(color = Color.White),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.62f)
                        .height(40.dp),
                )
            }
            if (item.userData?.played == true) PlayedSeal(Modifier.align(Alignment.TopEnd).padding(6.dp))
            if (progress != null && progress > 0f && progress < 1f) {
                GoldProgressBar(
                    progress,
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(if (showPlay) 0.72f else 1f)
                        .padding(start = 10.dp, end = if (showPlay) 0.dp else 10.dp, bottom = 12.dp),
                )
            }
            if (showPlay) MiniPlayButton(Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        CardText(title, subtitle)
    }
}

fun GenreArea.color(): Color = Color.hsl(hue, 0.5f, 0.42f)

fun GenreArea.icon(): ImageVector = when (this) {
    GenreArea.ActionAdventure -> Icons.Rounded.Bolt
    GenreArea.Suspense -> Icons.Rounded.Fingerprint
    GenreArea.Horror -> Icons.Rounded.Nightlight
    GenreArea.OtherWorlds -> Icons.Rounded.RocketLaunch
    GenreArea.Laughs -> Icons.Rounded.SentimentVerySatisfied
    GenreArea.HeartAndDrama -> Icons.Rounded.Favorite
    GenreArea.FamilyTime -> Icons.Rounded.Diversity3
    GenreArea.RealStories -> Icons.Rounded.Public
    GenreArea.MusicAndStage -> Icons.Rounded.MusicNote
}

/**
 * The tile of a genre area: a collage of its titles in the tint of the area, its icon, name and tagline,
 * and the genres it holds.
 */
@Composable
fun AreaTile(
    area: AreaInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.55f,
    compact: Boolean = false,
) {
    val images = LocalImages.current
    val tint = area.area.color()
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .aspectRatio(aspectRatio)
            .cardClick(onClick)
            .cardFrame(shape),
    ) {
        val covers = area.covers.mapNotNull { images.backdrop(it, 560) }
        when {
            covers.size >= 3 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                FintageImage(covers[0], null, Modifier.weight(1.5f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FintageImage(covers[1], null, Modifier.weight(1f).fillMaxWidth())
                    FintageImage(covers[2], null, Modifier.weight(1f).fillMaxWidth())
                }
            }
            covers.isNotEmpty() -> FintageImage(covers[0], null, Modifier.fillMaxSize())
            else -> Box(Modifier.fillMaxSize().background(tint))
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.72f), tint.copy(alpha = 0.18f), Color.Transparent))),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = 0.1f),
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        Box(
            Modifier
                .padding(12.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(0.75.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(area.area.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                area.area.title,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                area.genres.take(4).joinToString(" · ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The tile of a genre: a title of the genre as its backdrop, with the name and the number of titles. */
@Composable
fun GenreTile(genre: GenreInfo, onClick: () -> Unit, modifier: Modifier = Modifier, aspectRatio: Float = 16f / 10f) {
    val images = LocalImages.current
    val image: ImageSource? = genre.cover?.let { images.backdrop(it, 480) }
        ?: genre.imageTag?.let { images.genre(genre.id, it) }
    Box(
        modifier
            .aspectRatio(aspectRatio)
            .cardClick(onClick)
            .cardFrame(RoundedCornerShape(14.dp)),
    ) {
        FintageImage(image, contentDescription = null, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.45f to Color.Black.copy(alpha = 0.18f),
                        1f to Color.Black.copy(alpha = 0.8f),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                genre.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (genre.count == 1) "1 title" else "${genre.count} titles",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
fun PersonCard(person: PersonRef, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 96.dp) {
    val images = LocalImages.current
    Column(
        modifier
            .width(width)
            .cardClick(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width - 8.dp)
                .cardFrame(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val image = images.person(person, 280)
            if (image != null) {
                FintageImage(image, contentDescription = person.name, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(Fintage.colors.cardGradient), contentAlignment = Alignment.Center) {
                    Text(
                        person.name.orEmpty().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.titleLarge,
                        color = Fintage.colors.dimText,
                    )
                }
            }
        }
        CardText(person.name.orEmpty(), person.role?.takeIf { it.isNotBlank() }?.let { if (person.type == "Actor") "as $it" else it } ?: person.type)
    }
}

/** The tile of a library, with the image the server made for it. */
@Composable
fun LibraryTile(library: BaseItem, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp = 176.dp) {
    val images = LocalImages.current
    Box(
        modifier
            .width(width)
            .aspectRatio(16f / 9f)
            .cardClick(onClick)
            .cardFrame(),
        contentAlignment = Alignment.Center,
    ) {
        FintageImage(images.primary(library, 560), contentDescription = null, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
        Text(
            library.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
