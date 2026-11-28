package io.github.ricolehn.fintage.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.SortOption
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.ui.AppActions
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.browse.BrowseScreen
import io.github.ricolehn.fintage.ui.components.fintageHazeStyle
import io.github.ricolehn.fintage.ui.detail.DetailScreen
import io.github.ricolehn.fintage.ui.explore.ExploreScreen
import io.github.ricolehn.fintage.ui.home.HomeScreen
import io.github.ricolehn.fintage.ui.library.LibraryScreen
import io.github.ricolehn.fintage.ui.person.PersonScreen
import io.github.ricolehn.fintage.ui.search.SearchScreen
import io.github.ricolehn.fintage.ui.settings.SettingsScreen
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase
import io.github.ricolehn.fintage.ui.theme.fintageBackground

/** Counts the taps on the open tab, so its first page can scroll back to the top. */
val LocalReselect = compositionLocalOf { 0 }

private val RailWidth = 88.dp
private val BarHeight = 64.dp

/** A page of titles for a library of the server. */
fun BrowseSpec.Companion.forLibrary(view: BaseItem): BrowseSpec = BrowseSpec(
    title = view.name.orEmpty(),
    parentId = view.id,
    itemTypes = when (view.collectionType) {
        "movies" -> listOf("Movie")
        "tvshows" -> listOf("Series")
        "boxsets" -> listOf("BoxSet")
        else -> listOf("Movie", "Series", "Video")
    },
    defaultSort = SortOption.Title,
)

fun appActions(navigator: AppNavigator, onPlay: (PlayRequest) -> Unit) = AppActions(
    openItem = { item ->
        when (item.type) {
            "Person" -> navigator.navigate(PersonRoute(item.id))
            "Genre" -> navigator.navigate(BrowseRoute(BrowseSpec.forGenre(item.id, item.name.orEmpty())))
            "Studio" -> navigator.navigate(BrowseRoute(BrowseSpec(title = item.name.orEmpty(), studioIds = listOf(item.id))))
            "CollectionFolder", "UserView" -> navigator.navigate(BrowseRoute(BrowseSpec.forLibrary(item)))
            else -> navigator.navigate(DetailRoute(item.id))
        }
    },
    openItemId = { navigator.navigate(DetailRoute(it)) },
    openPerson = { navigator.navigate(PersonRoute(it)) },
    openBrowse = { navigator.navigate(BrowseRoute(it)) },
    openSettings = { navigator.navigate(SettingsRoute()) },
    openExplore = { navigator.selectTab(Tab.Explore) },
    play = onPlay,
    back = { navigator.back() },
)

@Composable
fun MainScaffold(navigator: AppNavigator, onPlay: (PlayRequest) -> Unit) {
    val actions = remember(navigator) { appActions(navigator, onPlay) }
    val rootHaze = rememberHazeState()
    val provider = remember {
        entryProvider<Route> {
            entry<TabRoot>(clazzContentKey = { it.key }) { root ->
                when (root.tab) {
                    Tab.Home -> HomeScreen()
                    Tab.Explore -> ExploreScreen()
                    Tab.Library -> LibraryScreen()
                    Tab.Search -> SearchScreen()
                }
            }
            entry<DetailRoute>(clazzContentKey = { it.key }) { DetailScreen(it.itemId) }
            entry<PersonRoute>(clazzContentKey = { it.key }) { PersonScreen(it.personId) }
            entry<BrowseRoute>(clazzContentKey = { it.key }) { BrowseScreen(it.spec) }
            entry<SettingsRoute>(clazzContentKey = { it.key }) { SettingsScreen() }
        }
    }
    // every tab keeps the state and view models of its pages while another tab is open
    val entriesByTab: Map<Tab, List<NavEntry<Route>>> = Tab.entries.associateWith { tab ->
        key(tab) {
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<Route>(),
                rememberViewModelStoreNavEntryDecorator<Route>(),
            )
            rememberDecoratedNavEntries(navigator.stack(tab), decorators, provider)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().fintageBackground()) {
        val wide = maxWidth >= 600.dp
        val systemBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val navigationPadding = if (wide) PaddingValues(bottom = systemBottom + 16.dp) else PaddingValues(bottom = BarHeight + systemBottom + 16.dp)

        CompositionLocalProvider(
            LocalAppActions provides actions,
            LocalNavigationPadding provides navigationPadding,
            LocalReselect provides navigator.reselectCount,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = if (wide) RailWidth else 0.dp)
                    .hazeSource(rootHaze),
            ) {
                NavDisplay(
                    entries = entriesByTab.getValue(navigator.currentTab),
                    modifier = Modifier.fillMaxSize(),
                    sceneStrategies = listOf(SinglePaneSceneStrategy()),
                    transitionSpec = {
                        (fadeIn(tween(320, easing = FintageEase)) + scaleIn(tween(460, easing = FintageEase), initialScale = 0.985f))
                            .togetherWith(fadeOut(tween(160)))
                    },
                    popTransitionSpec = {
                        fadeIn(tween(260, easing = FintageEase))
                            .togetherWith(fadeOut(tween(200)) + scaleOut(tween(320, easing = FintageEase), targetScale = 0.985f))
                    },
                    onBack = { navigator.back() },
                )
            }
            if (wide) {
                FintageRail(navigator, Modifier.align(Alignment.CenterStart))
            } else {
                FrostedNavigationBar(rootHaze, navigator, Modifier.align(Alignment.BottomCenter))
            }
        }
        // the first page of a tab goes back to Home
        BackHandler(enabled = navigator.currentStack.size == 1 && navigator.currentTab != Tab.Home) {
            navigator.back()
        }
    }
}

@Composable
private fun FrostedNavigationBar(hazeState: HazeState, navigator: AppNavigator, modifier: Modifier = Modifier) {
    val colors = Fintage.colors
    val edge = colors.glassBorder
    // no fillMaxWidth here: the bar fills the width by itself, and a minimum width would stretch every item
    ShortNavigationBar(
        modifier = modifier
            .hazeEffect(hazeState, fintageHazeStyle())
            .drawBehind { drawLine(edge, Offset(0f, 0f), Offset(size.width, 0f), 1f) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Tab.entries.forEach { tab ->
            val selected = navigator.currentTab == tab
            ShortNavigationBarItem(
                selected = selected,
                onClick = { navigator.selectTab(tab) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onChipSelected,
                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = colors.chipSelected,
                    unselectedIconColor = colors.dimText,
                    unselectedTextColor = colors.dimText,
                ),
            )
        }
    }
}

@Composable
private fun FintageRail(navigator: AppNavigator, modifier: Modifier = Modifier) {
    val colors = Fintage.colors
    val edge = colors.glassBorder
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(RailWidth)
            .drawBehind { drawLine(edge, Offset(size.width, 0f), Offset(size.width, size.height), 1f) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        header = {
            val actions = LocalAppActions.current
            IconButton(onClick = actions.openSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = colors.dimText)
            }
        },
    ) {
        Spacer(Modifier.height(12.dp))
        Tab.entries.forEach { tab ->
            val selected = navigator.currentTab == tab
            NavigationRailItem(
                selected = selected,
                onClick = { navigator.selectTab(tab) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.onChipSelected,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = colors.chipSelected,
                    unselectedIconColor = colors.dimText,
                    unselectedTextColor = colors.dimText,
                ),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
