package io.github.ricolehn.fintage.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.AreaInfo
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.GenreInfo
import io.github.ricolehn.fintage.data.HomeFeed
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.components.AreaTile
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FintageImage
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.GenrePill
import io.github.ricolehn.fintage.ui.components.GlassPillButton
import io.github.ricolehn.fintage.ui.components.LandscapeRow
import io.github.ricolehn.fintage.ui.components.LibraryTile
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.LogoOrTitle
import io.github.ricolehn.fintage.ui.components.MessageBox
import io.github.ricolehn.fintage.ui.components.PlayPillButton
import io.github.ricolehn.fintage.ui.components.PosterRow
import io.github.ricolehn.fintage.ui.components.RowPadding
import io.github.ricolehn.fintage.ui.components.RowSpacing
import io.github.ricolehn.fintage.ui.components.SectionHeader
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.components.UserAvatar
import io.github.ricolehn.fintage.ui.components.Wordmark
import io.github.ricolehn.fintage.ui.components.color
import io.github.ricolehn.fintage.ui.components.rememberBlurHash
import io.github.ricolehn.fintage.ui.navigation.LocalReselect
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase
import io.github.ricolehn.fintage.ui.theme.softGlow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    val reselect = LocalReselect.current
    LaunchedEffect(reselect) { if (reselect > 0) listState.animateScrollToItem(0) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshContinue()
        onPauseOrDispose { }
    }
    val frosted by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12 } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            Load.Loading -> LoadingBox()
            is Load.Failed -> ErrorBox(current.message, onRetry = viewModel::load)
            is Load.Ready -> {
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = viewModel::refresh,
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.LoadingIndicator(
                            state = pullState,
                            isRefreshing = refreshing,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = topPadding),
                        )
                    },
                ) {
                    HomeContent(
                        feed = current.value,
                        listState = listState,
                        contentPadding = PaddingValues(top = topPadding + 4.dp, bottom = LocalNavigationPadding.current.calculateBottomPadding()),
                        modifier = Modifier.hazeSource(hazeState),
                    )
                }
            }
        }
        FrostedTopBar(
            hazeState = hazeState,
            frosted = frosted,
            titleContent = { Wordmark() },
            actions = { UserAvatar(container.sessions.currentSession, onClick = actions.openSettings) },
        )
    }
}

@Composable
fun HomeContent(feed: HomeFeed, listState: LazyListState, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val playItem: (BaseItem) -> Unit = { item ->
        scope.launch {
            val episode = container.repository.firstEpisode(item)
            if (episode != null) actions.play(PlayRequest(episode.id))
        }
    }
    val isEmpty = feed.spotlight.isEmpty() && feed.resume.isEmpty() && feed.shelves.isEmpty() && feed.genres.isEmpty()
    if (isEmpty) {
        MessageBox(
            icon = Icons.Rounded.MovieFilter,
            title = "Nothing here yet",
            message = "Your libraries are empty. Add movies or shows to your Jellyfin server and they will appear here.",
            modifier = modifier.padding(contentPadding),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (feed.genres.isNotEmpty()) {
            item(key = "genres", contentType = "genres") {
                GenreChips(feed.genres, onAll = actions.openExplore, onGenre = { actions.openBrowse(BrowseSpec.forGenre(it.id, it.name)) })
            }
        }
        if (feed.spotlight.isNotEmpty()) {
            item(key = "spotlight", contentType = "spotlight") {
                Spotlight(feed.spotlight, onOpen = actions.openItem, onPlay = playItem)
            }
        }
        if (feed.resume.isNotEmpty()) {
            item(key = "resume", contentType = "landscape") {
                Column {
                    SectionHeader("Continue Watching")
                    Spacer(Modifier.height(12.dp))
                    LandscapeRow(feed.resume, onClick = { actions.play(PlayRequest(it.id)) })
                }
            }
        }
        if (feed.nextUp.isNotEmpty()) {
            item(key = "nextup", contentType = "landscape") {
                Column {
                    SectionHeader("Next Up")
                    Spacer(Modifier.height(12.dp))
                    LandscapeRow(feed.nextUp)
                }
            }
        }
        if (feed.areas.isNotEmpty()) {
            item(key = "areas", contentType = "areas") {
                AreasRow(feed.areas, onMore = actions.openExplore, onArea = { actions.openBrowse(BrowseSpec.forArea(it)) })
            }
        }
        items(feed.shelves, key = { it.key }, contentType = { "shelf" }) { shelf ->
            Column {
                SectionHeader(
                    title = shelf.title,
                    subtitle = shelf.subtitle,
                    onMore = shelf.browse?.let { spec -> { actions.openBrowse(spec) } },
                    accent = shelf.area?.color(),
                )
                Spacer(Modifier.height(12.dp))
                PosterRow(shelf.items)
            }
        }
        if (feed.libraries.isNotEmpty()) {
            item(key = "libraries", contentType = "libraries") {
                Column {
                    SectionHeader("Your libraries")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
                        items(feed.libraries, key = { it.id }) { library ->
                            LibraryTile(library, onClick = { actions.openItem(library) })
                        }
                    }
                }
            }
        }
    }
}

/** The genres of the server as pills at the top of Home, one tap from each of them. */
@Composable
private fun GenreChips(genres: List<GenreInfo>, onAll: () -> Unit, onGenre: (GenreInfo) -> Unit) {
    LazyRow(contentPadding = RowPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") {
            GlassPillButton(
                text = "All genres",
                icon = Icons.Rounded.Category,
                onClick = onAll,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        items(genres, key = { it.id }) { genre ->
            GenrePill(genre.name, onClick = { onGenre(genre) })
        }
    }
}

/**
 * The spotlight: a carousel of titles, each whole in a card with rounded corners, with a soft glow in the
 * colors of the current one behind it.
 */
@Composable
private fun Spotlight(items: List<BaseItem>, onOpen: (BaseItem) -> Unit, onPlay: (BaseItem) -> Unit) {
    val images = LocalImages.current
    val carouselState = rememberCarouselState { items.size }
    val current = items.getOrNull(carouselState.currentItem) ?: items.first()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val itemWidth = (maxWidth - 40.dp).coerceAtMost(760.dp)
        val cardHeight = itemWidth * 9f / 16f
        Crossfade(
            targetState = images.backdrop(current)?.blurHash,
            animationSpec = tween(900, easing = FintageEase),
            label = "glow",
            modifier = Modifier.matchParentSize(),
        ) { hash ->
            val glow = rememberBlurHash(hash, 32, 20)
            Box(Modifier.fillMaxSize().softGlow(glow, alpha = if (Fintage.colors.isDark) 0.7f else 0.5f))
        }
        Column(Modifier.padding(vertical = 4.dp)) {
            HorizontalCenteredHeroCarousel(
                state = carouselState,
                preferredItemWidth = itemWidth,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight),
            ) { index ->
                val item = items[index]
                val info = carouselItemDrawInfo
                Box(
                    Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .clickable { onOpen(item) },
                ) {
                    FintageImage(images.backdrop(item, 1280), contentDescription = item.name, modifier = Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.Black.copy(alpha = 0.05f),
                                    1f to Color.Black.copy(alpha = 0.72f),
                                ),
                            ),
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .graphicsLayer {
                                val range = info.maxSize - info.minSize
                                alpha = if (range <= 0f) 1f else ((info.size - info.minSize) / range * 1.6f - 0.6f).coerceIn(0f, 1f)
                            },
                    ) {
                        LogoOrTitle(
                            logo = images.logo(item, 600),
                            title = item.name.orEmpty(),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                            textAlign = TextAlign.Start,
                            alignment = Alignment.BottomStart,
                            modifier = Modifier
                                .widthIn(max = 230.dp)
                                .height(52.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            listOfNotNull(item.productionYear?.toString(), *item.genres.take(2).toTypedArray()).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayPillButton(text = "Play", onClick = { onPlay(current) }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                GlassPillButton(
                    text = "Details",
                    icon = Icons.Rounded.Info,
                    onClick = { onOpen(current) },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            PageDots(count = items.size, current = carouselState.currentItem, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                Modifier
                    .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (selected) Fintage.colors.gold else Fintage.colors.dimText.copy(alpha = 0.4f)),
            )
        }
    }
}

/** The genre areas as large tiles, between the rows of the home page. */
@Composable
private fun AreasRow(areas: List<AreaInfo>, onMore: () -> Unit, onArea: (AreaInfo) -> Unit) {
    Column {
        SectionHeader("Explore by mood", subtitle = "Genre areas of your library", onMore = onMore)
        Spacer(Modifier.height(12.dp))
        LazyRow(contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
            items(areas, key = { it.area.name }) { area ->
                AreaTile(area, onClick = { onArea(area) }, modifier = Modifier.width(272.dp))
            }
            item(key = "more") {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(175.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(Fintage.colors.glass)
                        .clickable(onClick = onMore),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        Spacer(Modifier.height(6.dp))
                        Text("All genres", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
