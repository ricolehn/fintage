package io.github.ricolehn.fintage.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseFilters
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.SortOption
import io.github.ricolehn.fintage.data.TypeFilter
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.GlassPillButton
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.MessageBox
import io.github.ricolehn.fintage.ui.components.PosterCard
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.components.color
import io.github.ricolehn.fintage.ui.components.icon
import io.github.ricolehn.fintage.ui.components.rememberBlurHash
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.colorWash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowseState(
    val filters: BrowseFilters,
    val items: List<BaseItem> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
)

class BrowseViewModel(val spec: BrowseSpec, private val repository: MediaRepository) : ViewModel() {
    private val _state = MutableStateFlow(BrowseState(filters = BrowseFilters(sort = spec.defaultSort)))
    val state: StateFlow<BrowseState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun setSort(sort: SortOption) = updateFilters { it.copy(sort = sort) }
    fun setType(type: TypeFilter) = updateFilters { it.copy(type = type) }
    fun toggleUnwatched() = updateFilters { it.copy(unwatchedOnly = !it.unwatchedOnly) }
    fun selectSubGenre(id: String?) = updateFilters { it.copy(subGenreId = id) }

    private fun updateFilters(change: (BrowseFilters) -> BrowseFilters) {
        _state.update { it.copy(filters = change(it.filters)) }
        reload()
    }

    fun reload() {
        job?.cancel()
        _state.update { it.copy(loading = true, items = emptyList(), error = null, endReached = false, loadingMore = false) }
        job = viewModelScope.launch { loadPage(0) }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.endReached || current.error != null) return
        _state.update { it.copy(loadingMore = true) }
        job = viewModelScope.launch { loadPage(current.items.size) }
    }

    private suspend fun loadPage(start: Int) {
        val filters = _state.value.filters
        val shuffle = filters.sort == SortOption.Shuffle
        try {
            val result = repository.browse(spec, filters, start, if (shuffle) 120 else 60)
            _state.update { current ->
                val items = (current.items + result.items).distinctBy { it.id }
                current.copy(
                    items = items,
                    total = result.totalRecordCount,
                    loading = false,
                    loadingMore = false,
                    endReached = shuffle || result.items.isEmpty() || items.size >= result.totalRecordCount,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, loadingMore = false, error = e.friendlyMessage()) }
        }
    }
}

@Composable
fun BrowseScreen(spec: BrowseSpec) {
    val container = LocalAppContainer.current
    val viewModel: BrowseViewModel = viewModel { BrowseViewModel(spec, container.repository) }
    val actions = LocalAppActions.current
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()
    val frosted by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 12 } }
    val showTitle by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 140 } }
    Box(Modifier.fillMaxSize()) {
        BrowseContent(
            viewModel = viewModel,
            gridState = gridState,
            modifier = Modifier.hazeSource(hazeState),
            header = { state -> BrowseHeader(spec, state) },
        )
        FrostedTopBar(hazeState = hazeState, frosted = frosted, title = spec.title, showTitle = showTitle, onBack = actions.back)
    }
}

/** The grid of a browse page, with its header, filters and paging; Library uses it too. */
@Composable
fun BrowseContent(
    viewModel: BrowseViewModel,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight,
    header: @Composable (BrowseState) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = LocalAppActions.current
    val spec = viewModel.spec
    val images = LocalImages.current
    val washHash = state.items.firstOrNull()?.let { images.backdrop(it)?.blurHash }
    val wash = rememberBlurHash(washHash, 32, 20)
    val area = spec.area

    LaunchedEffect(gridState, viewModel) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> if (last >= gridState.layoutInfo.totalItemsCount - 12) viewModel.loadMore() }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 106.dp),
        modifier = modifier
            .fillMaxSize()
            .then(
                if (area != null) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to area.color().copy(alpha = if (Fintage.colors.isDark) 0.34f else 0.22f),
                            0.32f to Color.Transparent,
                        ),
                    )
                } else {
                    Modifier.colorWash(wash, height = 420.dp, alpha = if (Fintage.colors.isDark) 0.4f else 0.28f)
                },
            ),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = LocalNavigationPadding.current.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                header(state)
                Spacer(Modifier.height(12.dp))
                FilterBar(viewModel, state)
            }
        }
        when {
            state.loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                LoadingBox(Modifier.height(320.dp))
            }
            state.error != null && state.items.isEmpty() -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                ErrorBox(state.error.orEmpty(), onRetry = viewModel::reload, modifier = Modifier.height(360.dp))
            }
            state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                MessageBox(
                    icon = Icons.Rounded.SearchOff,
                    title = "Nothing to show",
                    message = if (state.filters.unwatchedOnly) "You have watched everything here." else "There are no titles here yet.",
                    modifier = Modifier.height(320.dp),
                )
            }
            else -> {
                items(state.items, key = { it.id }, contentType = { "poster" }) { item ->
                    PosterCard(item, onClick = { actions.openItem(item) }, width = null)
                }
                if (state.loadingMore) {
                    item(key = "more", span = { GridItemSpan(maxLineSpan) }) { LoadingBox(Modifier.height(96.dp)) }
                }
            }
        }
    }
}

@Composable
private fun BrowseHeader(spec: BrowseSpec, state: BrowseState) {
    val area = spec.area
    Column(Modifier.padding(top = 8.dp)) {
        if (area != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(area.icon(), contentDescription = null, tint = area.color().copy(alpha = 1f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Genre area".uppercase(), style = MaterialTheme.typography.labelSmall, color = Fintage.colors.dimText)
            }
            Spacer(Modifier.height(6.dp))
        }
        Text(spec.title, style = MaterialTheme.typography.headlineLarge)
        val subtitle = listOfNotNull(
            spec.subtitle,
            if (!state.loading && state.total > 0) (if (state.total == 1) "1 title" else "${state.total} titles") else null,
        ).joinToString(" · ")
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText)
        }
    }
}

@Composable
private fun FilterBar(viewModel: BrowseViewModel, state: BrowseState) {
    val spec = viewModel.spec
    val filters = state.filters
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (spec.subGenres.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "all") {
                    FilterChip(
                        selected = filters.subGenreId == null,
                        onClick = { viewModel.selectSubGenre(null) },
                        label = { Text("All") },
                        colors = fintageChipColors(),
                    )
                }
                items(spec.subGenres, key = { it.id }) { genre ->
                    FilterChip(
                        selected = filters.subGenreId == genre.id,
                        onClick = { viewModel.selectSubGenre(if (filters.subGenreId == genre.id) null else genre.id) },
                        label = { Text(genre.name) },
                        colors = fintageChipColors(),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortChip(filters.sort, onSort = viewModel::setSort)
            FilterChip(
                selected = filters.unwatchedOnly,
                onClick = viewModel::toggleUnwatched,
                label = { Text("Unwatched") },
                leadingIcon = if (filters.unwatchedOnly) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
                colors = fintageChipColors(),
            )
            if (filters.sort == SortOption.Shuffle) {
                GlassPillButton(
                    "Again",
                    icon = Icons.Rounded.Shuffle,
                    onClick = viewModel::reload,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        if (spec.canFilterType) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TypeFilter.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = filters.type == type,
                        onClick = { viewModel.setType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, TypeFilter.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = Fintage.colors.chipSelected,
                            activeContentColor = Fintage.colors.onChipSelected,
                            inactiveContainerColor = Fintage.colors.glass,
                            activeBorderColor = Fintage.colors.glassBorder,
                            inactiveBorderColor = Fintage.colors.glassBorder,
                        ),
                    ) {
                        Text(type.label)
                    }
                }
            }
        }
    }
}

@Composable
fun fintageChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Fintage.colors.glass,
    selectedContainerColor = Fintage.colors.chipSelected,
    selectedLabelColor = Fintage.colors.onChipSelected,
    selectedLeadingIconColor = Fintage.colors.onChipSelected,
    selectedTrailingIconColor = Fintage.colors.onChipSelected,
)

@Composable
private fun SortChip(sort: SortOption, onSort: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = false,
            onClick = { expanded = true },
            label = { Text(sort.label) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            trailingIcon = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
            colors = fintageChipColors(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSort(option)
                    },
                    trailingIcon = if (option == sort) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
    }
}
