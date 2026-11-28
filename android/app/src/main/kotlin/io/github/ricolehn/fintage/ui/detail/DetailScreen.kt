package io.github.ricolehn.fintage.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.ItemDetail
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.MediaStream
import io.github.ricolehn.fintage.data.jellyfin.NameId
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FintageImage
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.GenrePill
import io.github.ricolehn.fintage.ui.components.GlassIconButton
import io.github.ricolehn.fintage.ui.components.GoldProgressBar
import io.github.ricolehn.fintage.ui.components.LandscapeCard
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.LogoOrTitle
import io.github.ricolehn.fintage.ui.components.MetaRow
import io.github.ricolehn.fintage.ui.components.MetaText
import io.github.ricolehn.fintage.ui.components.OfficialRatingPill
import io.github.ricolehn.fintage.ui.components.PersonCard
import io.github.ricolehn.fintage.ui.components.PlayPillButton
import io.github.ricolehn.fintage.ui.components.PlayedSeal
import io.github.ricolehn.fintage.ui.components.PosterCard
import io.github.ricolehn.fintage.ui.components.PosterRow
import io.github.ricolehn.fintage.ui.components.RatingStar
import io.github.ricolehn.fintage.ui.components.RowPadding
import io.github.ricolehn.fintage.ui.components.RowSpacing
import io.github.ricolehn.fintage.ui.components.SectionHeader
import io.github.ricolehn.fintage.ui.components.SmallCapsLabel
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.components.rememberBlurHash
import io.github.ricolehn.fintage.ui.theme.CardShape
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.Palette
import io.github.ricolehn.fintage.ui.theme.colorWash
import io.github.ricolehn.fintage.ui.theme.glass
import io.github.ricolehn.fintage.ui.theme.softGlow
import io.github.ricolehn.fintage.util.Format

private val MaxContentWidth = 760.dp

@Composable
fun DetailScreen(itemId: String) {
    val container = LocalAppContainer.current
    val viewModel: DetailViewModel = viewModel { DetailViewModel(itemId, container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshQuietly()
        onPauseOrDispose { }
    }
    val frosted by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12 } }
    val showTitle by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    Box(Modifier.fillMaxSize()) {
        when (val load = state.load) {
            Load.Loading -> LoadingBox()
            is Load.Failed -> ErrorBox(load.message, onRetry = viewModel::load)
            is Load.Ready -> DetailContent(
                detail = load.value,
                state = state,
                viewModel = viewModel,
                listState = listState,
                modifier = Modifier.hazeSource(hazeState),
            )
        }
        FrostedTopBar(
            hazeState = hazeState,
            frosted = frosted,
            title = state.item?.name,
            showTitle = showTitle,
            onBack = actions.back,
        )
    }
}

@Composable
private fun DetailContent(
    detail: ItemDetail,
    state: DetailState,
    viewModel: DetailViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val item = detail.item
    val images = LocalImages.current
    val actions = LocalAppActions.current
    val uriHandler = LocalUriHandler.current
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight
    val heroImage = if (item.type == "Season") images.poster(item, 600) else images.backdrop(item, 1600) ?: images.poster(item, 800)
    val glow = rememberBlurHash(heroImage?.blurHash, 32, 20)
    val play: (Boolean) -> Unit = { fromStart -> viewModel.play(fromStart, actions.play) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .colorWash(glow, height = 560.dp, alpha = if (Fintage.colors.isDark) 0.42f else 0.3f),
        contentPadding = PaddingValues(top = topPadding, bottom = LocalNavigationPadding.current.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "hero") {
            Hero(item, heroImage, glow)
        }
        item(key = "title") {
            TitleBlock(item, Modifier.widthIn(max = MaxContentWidth))
        }
        item(key = "meta") {
            MetaBlock(item, Modifier.widthIn(max = MaxContentWidth))
        }
        item(key = "actions") {
            ActionBlock(
                detail = detail,
                resolving = state.resolvingPlay,
                onPlay = { play(false) },
                onRestart = { play(true) },
                onToggleFavorite = viewModel::toggleFavorite,
                onTogglePlayed = viewModel::togglePlayed,
                onShuffle = { viewModel.shuffle(actions.play) },
                onTrailer = { viewModel.trailer(actions.play) { url -> runCatching { uriHandler.openUri(url) } } },
                modifier = Modifier.widthIn(max = MaxContentWidth),
            )
        }
        item(key = "overview") {
            Overview(item, Modifier.widthIn(max = MaxContentWidth))
        }
        val streams = item.streams
        if (item.type in setOf("Movie", "Episode", "Video", "MusicVideo") && streams.isNotEmpty()) {
            item(key = "tracks") {
                Tracks(
                    streams = streams,
                    audioIndex = state.audioIndex ?: item.mediaSources.firstOrNull()?.defaultAudioStreamIndex,
                    subtitleIndex = state.subtitleIndex ?: item.mediaSources.firstOrNull()?.defaultSubtitleStreamIndex ?: -1,
                    onAudio = viewModel::selectAudio,
                    onSubtitle = viewModel::selectSubtitle,
                    modifier = Modifier.widthIn(max = MaxContentWidth),
                )
            }
        }
        when (item.type) {
            "Series", "Season" -> seasonsAndEpisodes(detail, state, viewModel)
            "Episode" -> if (detail.seasonEpisodes.size > 1) {
                item(key = "season-episodes") {
                    val index = detail.seasonEpisodes.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    Section(item.seasonName ?: "More episodes", onMore = item.seasonId?.let { id -> { actions.openItemId(id) } }) {
                        val rowState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = index)
                        LazyRow(state = rowState, contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
                            items(detail.seasonEpisodes, key = { it.id }) { episode ->
                                LandscapeCard(
                                    episode,
                                    onClick = { if (episode.id != item.id) actions.openItemId(episode.id) },
                                    title = listOfNotNull(Format.episodeCode(episode), episode.name).joinToString(" · "),
                                    subtitle = if (episode.id == item.id) "Now viewing" else Format.runtime(episode.runTimeTicks),
                                    width = 200.dp,
                                )
                            }
                        }
                    }
                }
            }
            "BoxSet" -> if (detail.children.isNotEmpty()) {
                item(key = "collection") {
                    Section("In this collection", subtitle = "${detail.children.size} titles") {
                        PosterGrid(detail.children)
                    }
                }
            }
        }
        val cast = item.people.filter { it.type == "Actor" || it.type == "GuestStar" }.take(24)
        if (cast.isNotEmpty()) {
            item(key = "cast") {
                Section("Cast & Crew") {
                    LazyRow(contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
                        items(cast, key = { it.id + it.role.orEmpty() }) { person ->
                            PersonCard(person, onClick = { actions.openPerson(person.id) })
                        }
                    }
                }
            }
        }
        if (detail.similar.isNotEmpty()) {
            item(key = "similar") {
                Section("More Like This") { PosterRow(detail.similar) }
            }
        }
        item(key = "credits") {
            Credits(item, Modifier.widthIn(max = MaxContentWidth))
        }
    }
}

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    onMore: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    ) {
        SectionHeader(title, subtitle = subtitle, onMore = onMore)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/** The image of the page, whole in a card with rounded corners and a soft glow in its colors. */
@Composable
private fun Hero(item: BaseItem, image: io.github.ricolehn.fintage.data.ImageSource?, glow: androidx.compose.ui.graphics.ImageBitmap?) {
    val isPoster = item.type == "Season"
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = MaxContentWidth + 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.matchParentSize().softGlow(glow, alpha = if (Fintage.colors.isDark) 0.85f else 0.6f))
        val shape = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .padding(horizontal = if (isPoster) 0.dp else 16.dp, vertical = 18.dp)
                .then(if (isPoster) Modifier.width(220.dp).aspectRatio(2f / 3f) else Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().aspectRatio(16f / 9f))
                .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black.copy(alpha = 0.6f))
                .clip(shape)
                .background(Fintage.colors.cardGradient),
        ) {
            FintageImage(image, contentDescription = item.name, modifier = Modifier.fillMaxSize())
            if (item.userData?.played == true && item.type in setOf("Movie", "Episode")) {
                PlayedSeal(Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
    }
}

@Composable
private fun TitleBlock(item: BaseItem, modifier: Modifier = Modifier) {
    val images = LocalImages.current
    val actions = LocalAppActions.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (item.type) {
            "Episode" -> {
                val seriesLogo = images.seriesLogo(item)
                val seriesId = item.seriesId
                Box(
                    Modifier
                        .then(if (seriesId != null) Modifier.clickable { actions.openItemId(seriesId) } else Modifier)
                        .padding(bottom = 10.dp),
                ) {
                    LogoOrTitle(
                        logo = seriesLogo,
                        title = item.seriesName.orEmpty(),
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = Fintage.colors.dimText),
                        modifier = Modifier
                            .height(if (seriesLogo != null) 40.dp else 24.dp)
                            .widthIn(max = 200.dp),
                    )
                }
                Format.episodeLine(item)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText)
                }
                Text(
                    item.name.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
            }
            "Season" -> {
                val seriesId = item.seriesId
                Text(
                    item.seriesName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Fintage.colors.dimText,
                    modifier = if (seriesId != null) Modifier.clickable { actions.openItemId(seriesId) } else Modifier,
                )
                Text(item.name.orEmpty(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
            else -> {
                LogoOrTitle(
                    logo = images.logo(item, 800),
                    title = item.name.orEmpty(),
                    textStyle = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .heightIn(max = 110.dp),
                )
                val original = item.originalTitle
                if (!original.isNullOrBlank() && original != item.name) {
                    Spacer(Modifier.height(4.dp))
                    Text(original, style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun MetaBlock(item: BaseItem, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MetaRow {
            val year = if (item.type == "Episode") Format.date(item.premiereDate) ?: item.productionYear?.toString() else Format.years(item)
            year?.let { MetaText(it) }
            if (item.type != "Series" && item.type != "Season" && item.type != "BoxSet") Format.runtime(item.runTimeTicks)?.let { MetaText(it) }
            item.officialRating?.let { OfficialRatingPill(it) }
            Format.rating(item.communityRating)?.let { RatingStar(it) }
            if (item.type == "Movie" || item.type == "Episode") Format.endsAt(item)?.let { MetaText(it) }
        }
        val genres = item.genreItems.ifEmpty { item.genres.map { NameId(it, "") } }
        if (genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genres.forEach { genre ->
                    GenrePill(
                        genre.name.orEmpty(),
                        onClick = { if (genre.id.isNotEmpty()) actions.openBrowse(BrowseSpec.forGenre(genre.id, genre.name.orEmpty())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBlock(
    detail: ItemDetail,
    resolving: Boolean,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: () -> Unit,
    onShuffle: () -> Unit,
    onTrailer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = detail.item
    val progress = Format.progress(item)
    val resumable = (item.userData?.playbackPositionTicks ?: 0) > 0 && item.userData?.played != true
    val isShow = item.type == "Series" || item.type == "Season"
    val nextUp = detail.nextUp
    val label = when {
        item.type == "BoxSet" -> null
        isShow && nextUp != null -> {
            val code = Format.episodeCode(nextUp)
            val resume = (nextUp.userData?.playbackPositionTicks ?: 0) > 0
            listOfNotNull(if (resume) "Resume" else "Play", code).joinToString(" ")
        }
        resumable -> "Resume"
        else -> "Play"
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (label != null) {
            PlayPillButton(
                text = label,
                onClick = onPlay,
                progress = if (resumable) progress else null,
                loading = resolving,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
            if (resumable) {
                Format.remaining(item)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (resumable) GlassIconButton(Icons.Rounded.Replay, "Play from the beginning", onRestart)
            if ((item.localTrailerCount ?: 0) > 0 || item.remoteTrailers.isNotEmpty()) {
                GlassIconButton(Icons.Rounded.Theaters, "Trailer", onTrailer)
            }
            if (isShow) GlassIconButton(Icons.Rounded.Shuffle, "Shuffle", onShuffle)
            if (item.type != "BoxSet") {
                GlassIconButton(
                    Icons.Rounded.Check,
                    if (item.userData?.played == true) "Mark as unwatched" else "Mark as watched",
                    onTogglePlayed,
                    tint = if (item.userData?.played == true) Fintage.colors.gold else MaterialTheme.colorScheme.onSurface,
                )
            }
            GlassIconButton(
                if (item.userData?.isFavorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (item.userData?.isFavorite == true) "Remove from favorites" else "Add to favorites",
                onToggleFavorite,
                tint = if (item.userData?.isFavorite == true) Palette.Heart else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Overview(item: BaseItem, modifier: Modifier = Modifier) {
    val tagline = item.taglines.firstOrNull()
    val overview = item.overview
    if (tagline == null && overview.isNullOrBlank()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 26.dp)
            .animateContentSize(tween(300))
            .clickable { expanded = !expanded },
    ) {
        if (tagline != null) {
            Text(tagline, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
        }
        if (!overview.isNullOrBlank()) {
            Text(
                overview,
                style = MaterialTheme.typography.bodyMedium,
                color = Fintage.colors.dimText,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (!expanded && overview.length > 220) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = "Read more",
                    tint = Fintage.colors.dimText,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/** Audio and subtitles as cards of their own, so the chosen tracks are clear at a glance. */
@Composable
private fun Tracks(
    streams: List<MediaStream>,
    audioIndex: Int?,
    subtitleIndex: Int,
    onAudio: (Int) -> Unit,
    onSubtitle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val audio = streams.filter { it.type == "Audio" }
    val subtitles = streams.filter { it.type == "Subtitle" }
    val video = streams.firstOrNull { it.type == "Video" }
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (audio.isNotEmpty()) {
            val selected = audio.firstOrNull { it.index == audioIndex } ?: audio.firstOrNull { it.isDefault } ?: audio.first()
            TrackCard(
                icon = Icons.Rounded.Audiotrack,
                label = "Audio",
                value = Format.audioTrack(selected),
                options = audio.map { it.index to Format.audioTrack(it) },
                selected = selected.index,
                onSelect = onAudio,
            )
        }
        if (subtitles.isNotEmpty()) {
            val selected = subtitles.firstOrNull { it.index == subtitleIndex }
            TrackCard(
                icon = Icons.Rounded.ClosedCaption,
                label = "Subtitles",
                value = selected?.let { Format.subtitleTrack(it) } ?: "Off",
                options = listOf(-1 to "Off") + subtitles.map { it.index to Format.subtitleTrack(it) },
                selected = selected?.index ?: -1,
                onSelect = onSubtitle,
            )
        }
        if (video != null) {
            Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VideoSettings, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(Format.videoTrack(video), style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText)
            }
        }
    }
}

@Composable
private fun TrackCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(14.dp))
                .clickable(enabled = options.size > 1) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            SmallCapsLabel(label)
            Spacer(Modifier.width(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (options.size > 1) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = Fintage.colors.dimText)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (index, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    trailingIcon = if (index == selected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = Fintage.colors.gold) }
                    } else null,
                )
            }
        }
    }
}

private fun LazyListScope.seasonsAndEpisodes(detail: ItemDetail, state: DetailState, viewModel: DetailViewModel) {
    val item = detail.item
    val nextUp = detail.nextUp
    if (item.type == "Series" && nextUp != null) {
        item(key = "nextup") {
            val actions = LocalAppActions.current
            Section("Next Up") {
                Box(Modifier.padding(horizontal = 16.dp)) {
                    LandscapeCard(
                        nextUp,
                        onClick = { actions.play(PlayRequest(nextUp.id)) },
                        title = listOfNotNull(Format.episodeCode(nextUp), nextUp.name).joinToString(" · "),
                        subtitle = Format.remaining(nextUp) ?: Format.runtime(nextUp.runTimeTicks),
                        width = 300.dp,
                    )
                }
            }
        }
    }
    val seasons = detail.seasons
    if (item.type == "Series" && seasons.size > 1) {
        item(key = "season-tabs") {
            val selectedIndex = seasons.indexOfFirst { it.id == state.seasonId }.coerceAtLeast(0)
            Column(Modifier.fillMaxWidth().padding(top = 30.dp)) {
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = { HorizontalDivider(color = Fintage.colors.divider) },
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                            color = Fintage.colors.gold,
                            width = Dp.Unspecified,
                        )
                    },
                ) {
                    seasons.forEachIndexed { index, season ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { viewModel.selectSeason(season.id) },
                            text = { Text(season.name.orEmpty(), style = MaterialTheme.typography.titleSmall) },
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = Fintage.colors.dimText,
                        )
                    }
                }
            }
        }
    } else if (item.type == "Series" && seasons.size == 1) {
        item(key = "season-title") {
            Section(seasons.first().name ?: "Episodes") {}
        }
    } else if (item.type == "Season") {
        item(key = "season-title") {
            Section("Episodes", subtitle = state.episodes.size.takeIf { it > 0 }?.let { "$it episodes" }) {}
        }
    }
    if (state.episodesLoading) {
        item(key = "episodes-loading") { LoadingBox(Modifier.height(200.dp)) }
    } else {
        items(state.episodes, key = { "episode-" + it.id }) { episode ->
            EpisodeRow(episode, onToggleWatched = { viewModel.toggleEpisodePlayed(episode) }, modifier = Modifier.widthIn(max = MaxContentWidth))
        }
    }
}

/** An episode as in the list of the theme: a 16:9 image, its number and name, and the first lines of the plot. */
@Composable
private fun EpisodeRow(episode: BaseItem, onToggleWatched: () -> Unit, modifier: Modifier = Modifier) {
    val images = LocalImages.current
    val actions = LocalAppActions.current
    val progress = Format.progress(episode)
    Column(
        modifier
            .fillMaxWidth()
            .clickable { actions.openItemId(episode.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(148.dp)
                    .aspectRatio(16f / 9f)
                    .shadow(8.dp, CardShape)
                    .clip(CardShape)
                    .clickable { actions.play(PlayRequest(episode.id)) },
            ) {
                FintageImage(images.landscape(episode, 420), contentDescription = episode.name, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                if (episode.userData?.played == true) PlayedSeal(Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp))
                if (progress != null && progress > 0f && progress < 1f) {
                    GoldProgressBar(
                        progress,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    listOfNotNull(episode.indexNumber?.let { "$it." }, episode.name).joinToString(" "),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                MetaRow(centered = false) {
                    Format.runtime(episode.runTimeTicks)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
                    }
                    Format.rating(episode.communityRating)?.let {
                        Text("★ $it", style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
                    }
                }
            }
            GlassIconButton(
                Icons.Rounded.Check,
                if (episode.userData?.played == true) "Mark as unwatched" else "Mark as watched",
                onToggleWatched,
                size = 36.dp,
                glass = false,
                tint = if (episode.userData?.played == true) Fintage.colors.gold else Fintage.colors.dimText,
            )
        }
        val overview = episode.overview
        if (!overview.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                overview,
                style = MaterialTheme.typography.bodySmall,
                color = Fintage.colors.dimText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PosterGrid(items: List<BaseItem>) {
    val actions = LocalAppActions.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        val columns = ((maxWidth + 12.dp) / (118.dp)).toInt().coerceAtLeast(3)
        val cardWidth = (maxWidth - 12.dp * (columns - 1)) / columns
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = columns) {
            items.forEach { child -> PosterCard(child, onClick = { actions.openItem(child) }, width = cardWidth) }
        }
    }
}

/** The directors, writers, studios and genres at the very end, like the credits at the end of a film. */
@Composable
private fun Credits(item: BaseItem, modifier: Modifier = Modifier) {
    val actions = LocalAppActions.current
    val directors = item.people.filter { it.type == "Director" }
    val writers = item.people.filter { it.type == "Writer" }
    val studios = item.studios
    val genres = item.genreItems
    if (directors.isEmpty() && writers.isEmpty() && studios.isEmpty() && genres.isEmpty()) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 40.dp),
    ) {
        HorizontalDivider(color = Fintage.colors.divider)
        Spacer(Modifier.height(24.dp))
        val blocks = buildList<Pair<String, List<Pair<String, (() -> Unit)?>>>> {
            if (directors.isNotEmpty()) add((if (directors.size > 1) "Directors" else "Director") to directors.map { p -> p.name.orEmpty() to { actions.openPerson(p.id) } })
            if (writers.isNotEmpty()) add((if (writers.size > 1) "Writers" else "Writer") to writers.map { p -> p.name.orEmpty() to { actions.openPerson(p.id) } })
            if (studios.isNotEmpty()) add("Studios" to studios.map { s -> s.name.orEmpty() to { actions.openBrowse(BrowseSpec(title = s.name.orEmpty(), studioIds = listOf(s.id))) } })
            if (genres.isNotEmpty()) add("Genres" to genres.map { g -> g.name.orEmpty() to { actions.openBrowse(BrowseSpec.forGenre(g.id, g.name.orEmpty())) } })
        }
        blocks.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
                pair.forEach { (label, names) ->
                    Column(Modifier.weight(1f)) {
                        SmallCapsLabel(label)
                        Spacer(Modifier.height(8.dp))
                        names.forEach { (name, onClick) ->
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                                    .padding(vertical = 3.dp),
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
