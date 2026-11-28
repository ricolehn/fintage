package io.github.ricolehn.fintage.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.GenreInfo
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.SearchResults
import io.github.ricolehn.fintage.data.SettingsStore
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.PersonRef
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.GenrePill
import io.github.ricolehn.fintage.ui.components.LandscapeRow
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.MessageBox
import io.github.ricolehn.fintage.ui.components.PersonCard
import io.github.ricolehn.fintage.ui.components.PosterRow
import io.github.ricolehn.fintage.ui.components.RowPadding
import io.github.ricolehn.fintage.ui.components.RowSpacing
import io.github.ricolehn.fintage.ui.components.SectionHeader
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.ui.navigation.LocalReselect
import io.github.ricolehn.fintage.ui.theme.Fintage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUi {
    data object Idle : SearchUi
    data object Searching : SearchUi
    data class Results(val query: String, val results: SearchResults) : SearchUi
    data class Failed(val message: String) : SearchUi
}

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: MediaRepository, private val settings: SettingsStore) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _ui = MutableStateFlow<SearchUi>(SearchUi.Idle)
    val ui: StateFlow<SearchUi> = _ui.asStateFlow()

    private val _genres = MutableStateFlow<List<GenreInfo>>(emptyList())
    val genres: StateFlow<List<GenreInfo>> = _genres.asStateFlow()

    val recent: StateFlow<List<String>> = settings.preferences.map { it.recentSearches }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _query.map { it.trim() }.distinctUntilChanged().debounce(300).collectLatest { text ->
                if (text.length < 2) {
                    _ui.value = SearchUi.Idle
                    return@collectLatest
                }
                _ui.value = SearchUi.Searching
                _ui.value = try {
                    SearchUi.Results(text, repository.search(text))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    SearchUi.Failed(e.friendlyMessage())
                }
            }
        }
        viewModelScope.launch {
            _genres.value = try {
                repository.genres()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun remember() {
        val text = _query.value.trim()
        if (text.length >= 2) viewModelScope.launch { settings.addRecentSearch(text) }
    }

    fun clearRecent() {
        viewModelScope.launch { settings.clearRecentSearches() }
    }
}

@Composable
fun SearchScreen() {
    val container = LocalAppContainer.current
    val viewModel: SearchViewModel = viewModel { SearchViewModel(container.repository, container.settings) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val reselect = LocalReselect.current
    LaunchedEffect(reselect) {
        if (reselect > 0) {
            listState.animateScrollToItem(0)
            runCatching { focusRequester.requestFocus() }
        }
    }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight + 16.dp
    val bottomPadding = LocalNavigationPadding.current.calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        val contentModifier = Modifier
            .fillMaxSize()
            .imePadding()
            .hazeSource(hazeState)
        when (val current = ui) {
            SearchUi.Idle -> IdleContent(
                recent = recent,
                genres = genres,
                onRecent = { viewModel.setQuery(it) },
                onClearRecent = viewModel::clearRecent,
                modifier = contentModifier,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
            )
            SearchUi.Searching -> LoadingBox(contentModifier)
            is SearchUi.Failed -> MessageBox(Icons.Rounded.SearchOff, "Search failed", current.message, contentModifier)
            is SearchUi.Results -> {
                if (current.results.isEmpty) {
                    MessageBox(
                        Icons.Rounded.SearchOff,
                        "No results",
                        "Nothing in your library matches “${current.query}”.",
                        contentModifier,
                    )
                } else {
                    ResultsContent(
                        results = current.results,
                        onOpen = { viewModel.remember() },
                        modifier = contentModifier,
                        listState = listState,
                        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                    )
                }
            }
        }
        FrostedTopBar(
            hazeState = hazeState,
            frosted = true,
            titleContent = {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Movies, shows, people…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.remember()
                        focusManager.clearFocus()
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Fintage.colors.glass,
                        unfocusedContainerColor = Fintage.colors.glass,
                        focusedBorderColor = Fintage.colors.dimText,
                        unfocusedBorderColor = Fintage.colors.glassBorder,
                        cursorColor = Fintage.colors.gold,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp, top = 2.dp)
                        .height(54.dp)
                        .focusRequester(focusRequester),
                )
            },
        )
    }
}

@Composable
private fun IdleContent(
    recent: List<String>,
    genres: List<GenreInfo>,
    onRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    val actions = LocalAppActions.current
    LazyColumn(modifier = modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (recent.isNotEmpty()) {
            item(key = "recent-header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent searches", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClearRecent) { Text("Clear") }
                }
            }
            items(recent, key = { "recent-$it" }) { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRecent(text) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item(key = "recent-space") { Spacer(Modifier.height(12.dp)) }
        }
        if (genres.isNotEmpty()) {
            item(key = "genres") {
                Column {
                    SectionHeader("Browse by genre", subtitle = "Or find something by its mood", onMore = actions.openExplore)
                    Spacer(Modifier.height(14.dp))
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        genres.forEach { genre ->
                            GenrePill(genre.name, onClick = { actions.openBrowse(BrowseSpec.forGenre(genre.id, genre.name)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsContent(
    results: SearchResults,
    onOpen: () -> Unit,
    modifier: Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contentPadding: PaddingValues,
) {
    val actions = LocalAppActions.current
    val open: (BaseItem) -> Unit = {
        onOpen()
        actions.openItem(it)
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        resultRow("movies", "Movies", results.movies) { PosterRow(it, onClick = open) }
        resultRow("shows", "Shows", results.shows) { PosterRow(it, onClick = open) }
        resultRow("collections", "Collections", results.collections) { PosterRow(it, onClick = open) }
        resultRow("episodes", "Episodes", results.episodes) { LandscapeRow(it, cardWidth = 200.dp, onClick = open) }
        if (results.people.isNotEmpty()) {
            item(key = "people") {
                Column {
                    SectionHeader("People")
                    Spacer(Modifier.height(12.dp))
                    LazyRow(contentPadding = RowPadding, horizontalArrangement = RowSpacing) {
                        items(results.people, key = { it.id }) { person ->
                            PersonCard(
                                PersonRef(
                                    id = person.id,
                                    name = person.name,
                                    primaryImageTag = person.imageTags["Primary"],
                                    imageBlurHashes = person.imageBlurHashes,
                                ),
                                onClick = {
                                    onOpen()
                                    actions.openPerson(person.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultRow(
    key: String,
    title: String,
    items: List<BaseItem>,
    row: @Composable (List<BaseItem>) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = key) {
        Column {
            SectionHeader(title, subtitle = if (items.size == 1) "1 result" else "${items.size} results")
            Spacer(Modifier.height(12.dp))
            row(items)
        }
    }
}
