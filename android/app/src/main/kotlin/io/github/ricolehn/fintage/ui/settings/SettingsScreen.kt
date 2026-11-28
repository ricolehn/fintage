package io.github.ricolehn.fintage.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.ricolehn.fintage.BuildConfig
import io.github.ricolehn.fintage.data.AppPreferences
import io.github.ricolehn.fintage.data.ThemeMode
import io.github.ricolehn.fintage.ui.LocalAppActions
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalNavigationPadding
import io.github.ricolehn.fintage.ui.components.FrostedTopBar
import io.github.ricolehn.fintage.ui.components.SmallCapsLabel
import io.github.ricolehn.fintage.ui.components.TopBarHeight
import io.github.ricolehn.fintage.ui.components.UserAvatar
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.glass
import kotlinx.coroutines.launch

private val Bitrates = listOf(
    0L to "Automatic",
    120_000_000L to "120 Mbps · 4K",
    60_000_000L to "60 Mbps · 4K",
    40_000_000L to "40 Mbps · 1080p",
    20_000_000L to "20 Mbps · 1080p",
    10_000_000L to "10 Mbps · 720p",
    4_000_000L to "4 Mbps · 720p",
    2_000_000L to "2 Mbps · 480p",
)

fun bitrateLabel(value: Long) = Bitrates.firstOrNull { it.first == value }?.second ?: "${value / 1_000_000} Mbps"

@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val actions = LocalAppActions.current
    val preferences by container.settings.preferences.collectAsStateWithLifecycle(AppPreferences())
    val session = container.sessions.currentSession
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val listState = rememberLazyListState()
    val frosted by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 12 } }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TopBarHeight

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topPadding,
                bottom = LocalNavigationPadding.current.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "account") {
                Row(
                    Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UserAvatar(session, onClick = {}, size = 56.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session?.userName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(session?.serverName.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Fintage.colors.dimText)
                        Text(session?.serverUrl.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Fintage.colors.faintText)
                    }
                }
            }
            item(key = "appearance") {
                SettingsGroup("Appearance") {
                    Column(Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = preferences.themeMode == mode,
                                    onClick = { scope.launch { container.settings.setThemeMode(mode) } },
                                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = Fintage.colors.chipSelected,
                                        activeContentColor = Fintage.colors.onChipSelected,
                                        inactiveContainerColor = Fintage.colors.glass,
                                        activeBorderColor = Fintage.colors.glassBorder,
                                        inactiveBorderColor = Fintage.colors.glassBorder,
                                    ),
                                ) { Text(mode.name) }
                            }
                        }
                    }
                    SwitchRow(
                        icon = Icons.Rounded.Grain,
                        title = "Film grain",
                        subtitle = "A faint grain over the background, like film",
                        checked = preferences.filmGrain,
                        onChange = { scope.launch { container.settings.setFilmGrain(it) } },
                    )
                }
            }
            item(key = "playback") {
                SettingsGroup("Playback") {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        SettingRow(
                            icon = Icons.Rounded.Hd,
                            title = "Streaming quality",
                            subtitle = bitrateLabel(preferences.maxBitrate),
                            onClick = { expanded = true },
                            trailing = { Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = Fintage.colors.dimText) },
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Bitrates.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        expanded = false
                                        scope.launch { container.settings.setMaxBitrate(value) }
                                    },
                                    trailingIcon = if (value == preferences.maxBitrate) {
                                        { Icon(Icons.Rounded.Check, contentDescription = null, tint = Fintage.colors.gold) }
                                    } else null,
                                )
                            }
                        }
                    }
                    SwitchRow(
                        icon = Icons.Rounded.SkipNext,
                        title = "Play the next episode",
                        subtitle = "Start the next episode when one ends",
                        checked = preferences.autoPlayNext,
                        onChange = { scope.launch { container.settings.setAutoPlayNext(it) } },
                    )
                }
            }
            item(key = "server") {
                SettingsGroup("Server") {
                    SettingRow(
                        icon = Icons.Rounded.Dns,
                        title = session?.serverName ?: "Server",
                        subtitle = "Jellyfin · ${session?.serverUrl.orEmpty()}",
                    )
                }
            }
            item(key = "signout") {
                OutlinedButton(
                    onClick = { container.sessions.signOut() },
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
            }
            item(key = "about") {
                Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fintage ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelLarge, color = Fintage.colors.dimText)
                    Text(
                        "The calm, cinematic look of the Fintage theme for Jellyfin, made native",
                        style = MaterialTheme.typography.bodySmall,
                        color = Fintage.colors.faintText,
                    )
                }
            }
        }
        FrostedTopBar(hazeState = hazeState, frosted = frosted, title = "Settings", onBack = actions.back)
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
    ) {
        SmallCapsLabel(title, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 6.dp))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp))) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
        }
        trailing?.invoke()
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Fintage.colors.gold,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    )
}
