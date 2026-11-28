package io.github.ricolehn.fintage.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.ricolehn.fintage.AppContainer
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.Images
import io.github.ricolehn.fintage.data.jellyfin.BaseItem

/** What a screen can ask of the app: open another page, play something or go back. */
@Stable
class AppActions(
    val openItem: (BaseItem) -> Unit,
    val openItemId: (String) -> Unit,
    val openPerson: (String) -> Unit,
    val openBrowse: (BrowseSpec) -> Unit,
    val openSettings: () -> Unit,
    val openExplore: () -> Unit,
    val play: (PlayRequest) -> Unit,
    val back: () -> Unit,
)

/** Starts the player for an item; null positions resume where the item was left. */
data class PlayRequest(
    val itemId: String,
    val startPositionTicks: Long? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
)

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("No AppContainer") }
val LocalImages = staticCompositionLocalOf<Images> { error("No Images") }
val LocalAppActions = staticCompositionLocalOf<AppActions> { error("No AppActions") }

/** The room the floating navigation bar or rail takes, so the content can scroll past it. */
val LocalNavigationPadding = compositionLocalOf { PaddingValues() }
