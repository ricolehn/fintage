package io.github.ricolehn.fintage.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import io.github.ricolehn.fintage.data.BrowseSpec
import java.util.concurrent.atomic.AtomicLong

enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Explore("Explore", Icons.Outlined.Explore, Icons.Rounded.Explore),
    Library("Library", Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
    Search("Search", Icons.Outlined.Search, Icons.Rounded.Search),
}

private val counter = AtomicLong()
private fun newKey(prefix: String) = "$prefix-${counter.incrementAndGet()}"

/** A page of the app. Every route has a unique key, so the same title can be open twice in a stack. */
sealed interface Route {
    val key: String
}

data class TabRoot(val tab: Tab) : Route {
    override val key: String = "root-${tab.name}"
}

data class DetailRoute(val itemId: String, override val key: String = newKey("detail")) : Route

data class PersonRoute(val personId: String, override val key: String = newKey("person")) : Route

data class BrowseRoute(val spec: BrowseSpec, override val key: String = newKey("browse")) : Route

data class SettingsRoute(override val key: String = newKey("settings")) : Route

/**
 * Each tab keeps its own stack of pages, so going to another tab and back returns to the same page, with
 * its scroll position. Back goes up the stack, and from the first page of a tab back to Home.
 */
@Stable
class AppNavigator {
    var currentTab by mutableStateOf(Tab.Home)
        private set

    /** Counts the taps on the tab that is already open, so its page can scroll back to the top. */
    var reselectCount by mutableIntStateOf(0)
        private set

    private val stacks: Map<Tab, SnapshotStateList<Route>> =
        Tab.entries.associateWith { mutableStateListOf<Route>(TabRoot(it)) }

    fun stack(tab: Tab): SnapshotStateList<Route> = stacks.getValue(tab)

    val currentStack: SnapshotStateList<Route> get() = stack(currentTab)

    fun navigate(route: Route) {
        currentStack.add(route)
    }

    fun selectTab(tab: Tab) {
        if (tab == currentTab) {
            val stack = stack(tab)
            if (stack.size > 1) stack.removeRange(1, stack.size) else reselectCount++
        } else {
            currentTab = tab
        }
    }

    fun back(): Boolean {
        val stack = currentStack
        return when {
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                true
            }
            currentTab != Tab.Home -> {
                currentTab = Tab.Home
                true
            }
            else -> false
        }
    }

    val canGoBack: Boolean get() = currentStack.size > 1 || currentTab != Tab.Home
}

/** Keeps the navigator through configuration changes. */
class NavigatorViewModel : ViewModel() {
    val navigator = AppNavigator()
}
