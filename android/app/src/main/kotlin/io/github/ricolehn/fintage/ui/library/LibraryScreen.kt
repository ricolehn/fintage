package io.github.ricolehn.fintage.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.SortOption
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.browse.BrowseContent
import io.github.ricolehn.fintage.ui.browse.BrowseViewModel
import io.github.ricolehn.fintage.ui.browse.fintageChipColors
import io.github.ricolehn.fintage.ui.components.ErrorBox
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.LoadingBox
import io.github.ricolehn.fintage.ui.components.MessageBox
import io.github.ricolehn.fintage.ui.components.UserAvatar
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.ui.navigation.LocalReselect
import io.github.ricolehn.fintage.ui.navigation.forLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibrariesViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<List<BaseItem>>>(Load.Loading)
    val state: StateFlow<Load<List<BaseItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = Load.Loading
            try {
                _state.value = Load.Ready(repository.libraries())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = Load.Failed(e.friendlyMessage())
            }
        }
    }
}

private const val FAVORITES = "favorites"

private val FavoritesSpec = BrowseSpec(
    title = "Favorites",
    subtitle = "Everything you marked with a heart",
    favoritesOnly = true,
    itemTypes = listOf("Movie", "Series", "BoxSet"),
    defaultSort = SortOption.Title,
)

@Composable
fun LibraryScreen() {
    val container = LocalAppContainer.current
    val actions = LocalAppActions.current
    val librariesViewModel: LibrariesViewModel = viewModel { LibrariesViewModel(container.repository) }
    val state by librariesViewModel.state.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        when (val current = state) {
            Load.Loading -> LoadingBox()
            is Load.Failed -> ErrorBox(current.message, onRetry = librariesViewModel::load)
            is Load.Ready -> {
                val libraries = current.value
                if (libraries.isEmpty()) {
                    MessageBox(Icons.Rounded.VideoLibrary, "No libraries", "This server has no libraries of movies or shows for you yet.")
                } else {
                    val selectedId = selected ?: libraries.first().id
                    val spec = if (selectedId == FAVORITES) FavoritesSpec else (libraries.firstOrNull { it.id == selectedId } ?: libraries.first()).let { BrowseSpec.forLibrary(it) }
                    key(selectedId) {
                        val gridState = rememberLazyGridState()
                        val reselect = LocalReselect.current
                        LaunchedEffect(reselect) { if (reselect > 0) gridState.animateScrollToItem(0) }
                        val browseViewModel: BrowseViewModel = viewModel(key = "library-$selectedId") { BrowseViewModel(spec, container.repository) }
                        BrowseContent(
                            viewModel = browseViewModel,
                            gridState = gridState,
                            modifier = Modifier.hazeSource(hazeState),
                            header = {
                                Column(Modifier.padding(top = 4.dp)) {
                                    Text("Library", style = MaterialTheme.typography.headlineLarge)
                                    Spacer(Modifier.height(12.dp))
                                    LibraryChips(libraries, selectedId, onSelect = { selected = it })
                                }
                            },
                        )
                    }
                }
            }
        }
        FrostedTopBar(
            hazeState = hazeState,
            frosted = true,
            title = "Library",
            showTitle = false,
            actions = { UserAvatar(container.sessions.currentSession, onClick = actions.openSettings) },
        )
    }
}

@Composable
private fun LibraryChips(libraries: List<BaseItem>, selectedId: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(0.dp)) {
        item(key = FAVORITES) {
            FilterChip(
                selected = selectedId == FAVORITES,
                onClick = { onSelect(FAVORITES) },
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                colors = fintageChipColors(),
            )
        }
        items(libraries, key = { it.id }) { library ->
            FilterChip(
                selected = selectedId == library.id,
                onClick = { onSelect(library.id) },
                label = { Text(library.name.orEmpty()) },
                colors = fintageChipColors(),
            )
        }
    }
}
