package io.github.ricolehn.fintage.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ricolehn.fintage.R
import io.github.ricolehn.fintage.data.Session
import io.github.ricolehn.fintage.data.Shelf
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.theme.Fintage

val RowPadding = PaddingValues(horizontal = 16.dp)
val RowSpacing = Arrangement.spacedBy(12.dp)

@Composable
fun PosterRow(
    items: List<BaseItem>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 112.dp,
    contentPadding: PaddingValues = RowPadding,
    onClick: ((BaseItem) -> Unit)? = null,
) {
    val actions = LocalAppActions.current
    LazyRow(modifier = modifier, contentPadding = contentPadding, horizontalArrangement = RowSpacing) {
        items(items, key = { it.id }) { item ->
            PosterCard(item, onClick = { (onClick ?: actions.openItem)(item) }, width = cardWidth)
        }
    }
}

@Composable
fun LandscapeRow(
    items: List<BaseItem>,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 176.dp,
    onClick: ((BaseItem) -> Unit)? = null,
) {
    val actions = LocalAppActions.current
    LazyRow(modifier = modifier, contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
        items(items, key = { it.id }) { item ->
            LandscapeCard(item, onClick = { (onClick ?: actions.openItem)(item) }, width = cardWidth)
        }
    }
}

/** A section of the home page: a header that leads to the full page, and a row of posters. */
@Composable
fun ShelfSection(shelf: Shelf, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    Column(modifier) {
        SectionHeader(
            title = shelf.title,
            subtitle = shelf.subtitle,
            onMore = shelf.browse?.let { spec -> { actions.openBrowse(spec) } },
        )
        Spacer(Modifier.height(12.dp))
        PosterRow(shelf.items)
    }
}

/** The film frame of the icon and the name, for the app bar. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.fintage_mark), contentDescription = null, modifier = Modifier.size(width = 27.dp, height = 25.dp))
        Spacer(Modifier.width(10.dp))
        Text("Fintage", style = MaterialTheme.typography.titleLarge)
    }
}

/** The picture of the user, or their initial, which opens the settings. */
@Composable
fun UserAvatar(session: Session?, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val images = LocalImages.current
    val source = session?.let { images.user(it.userId, it.userImageTag) }
    Box(
        modifier
            .padding(end = 8.dp)
            .size(size)
            .clip(CircleShape)
            .background(Fintage.colors.glass)
            .border(0.75.dp, Fintage.colors.glassBorder, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (source != null) {
            FintageImage(source, contentDescription = "Settings", modifier = Modifier.fillMaxSize())
        } else {
            Text(
                session?.userName?.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

/** A small colored bar in front of a title, in the tint of a genre area. */
@Composable
fun AccentBar(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}
