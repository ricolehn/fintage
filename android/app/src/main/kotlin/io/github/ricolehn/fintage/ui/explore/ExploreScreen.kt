package io.github.ricolehn.fintage.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.ExploreFeed
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.SortOption
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.components.AreaTile
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.GenrePill
import io.github.ricolehn.fintage.ui.components.GenreTile
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.PosterRow
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.ui.navigation.LocalReselect
import io.github.ricolehn.fintage.ui.theme.Fintage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<ExploreFeed>>(Load.Loading)
    val state: StateFlow<Load<ExploreFeed>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Load.Loading
            try {
                _state.value = Load.Ready(repository.explore())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = Load.Failed(e.friendlyMessage())
            }
        }
    }
}

@Composable
fun ExploreScreen() {
    val container = LocalAppContainer.current
    val viewModel: ExploreViewModel = viewModel { ExploreViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()
    val reselect = LocalReselect.current
    LaunchedEffect(reselect) { if (reselect > 0) gridState.animateScrollToItem(0) }
    val frosted by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 12 } }
    val showTitle by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 120 } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            Load.Loading -> LoadingBox()
            is Load.Failed -> ErrorBox(current.message, onRetry = viewModel::load)
            is Load.Ready -> LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding,
                    bottom = LocalNavigationPadding.current.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                exploreContent(current.value)
            }
        }
        FrostedTopBar(hazeState = hazeState, frosted = frosted, title = "Explore", showTitle = showTitle)
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.exploreContent(feed: ExploreFeed) {
    fullWidth("title") {
        Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
            Text("Explore", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Wander through the genres of your library",
                style = MaterialTheme.typography.bodyMedium,
                color = Fintage.colors.dimText,
            )
        }
    }
    if (feed.areas.isNotEmpty()) {
        fullWidth("areas-header") { GridHeader("Genre areas", "Genres that belong together, side by side") }
        items(feed.areas, key = { "area-" + it.area.name }) { area ->
            val actions = LocalAppActions.current
            AreaTile(area, onClick = { actions.openBrowse(BrowseSpec.forArea(area)) }, aspectRatio = 1.05f, compact = true)
        }
    }
    if (feed.genres.isNotEmpty()) {
        fullWidth("genres-header") { GridHeader("All genres", "${feed.genres.size} genres") }
        items(feed.genres, key = { "genre-" + it.id }) { genre ->
            val actions = LocalAppActions.current
            GenreTile(genre, onClick = { actions.openBrowse(BrowseSpec.forGenre(genre.id, genre.name)) })
        }
    }
    if (feed.decades.isNotEmpty()) {
        fullWidth("decades") {
            val actions = LocalAppActions.current
            Column {
                GridHeader("Through the decades", null)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(feed.decades, key = { it.start }) { decade ->
                        GenrePill(
                            decade.label,
                            onClick = {
                                actions.openBrowse(BrowseSpec(title = decade.label, years = decade.years, defaultSort = SortOption.TopRated))
                            },
                        )
                    }
                }
            }
        }
    }
    if (feed.collections.isNotEmpty()) {
        fullWidth("collections") {
            Column(Modifier.padding(top = 8.dp)) {
                GridHeader("Collections", null)
                PosterRow(feed.collections, contentPadding = PaddingValues(0.dp))
            }
        }
    }
    if (feed.studios.isNotEmpty()) {
        fullWidth("studios") {
            val actions = LocalAppActions.current
            Column(Modifier.padding(top = 8.dp)) {
                GridHeader("Studios & networks", null)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    feed.studios.forEach { studio ->
                        GenrePill(
                            studio.name.orEmpty(),
                            onClick = { actions.openBrowse(BrowseSpec(title = studio.name.orEmpty(), studioIds = listOf(studio.id))) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridHeader(title: String, subtitle: String?) {
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
        }
    }
}
