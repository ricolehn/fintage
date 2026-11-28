package io.github.ricolehn.fintage.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.ricolehn.fintage.ui.components.GoldButton
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.fintageBackground
import io.github.ricolehn.fintage.util.LocalNetwork

/**
 * Shows the app once it can reach the server. On Android 17, a server on the home network needs the
 * permission for devices nearby; until the user allows it, this explains why and asks for it. Servers on
 * the internet, and older versions of Android, go straight to the app.
 */
@Composable
fun LocalNetworkGate(serverUrl: String, content: @Composable () -> Unit) {
    if (!LocalNetwork.isEnforced) {
        content()
        return
    }
    val context = LocalContext.current
    var granted by remember { mutableStateOf(LocalNetwork.isGranted(context)) }
    var isLocal by remember(serverUrl) { mutableStateOf<Boolean?>(null) }
    var skipped by rememberSaveable { mutableStateOf(false) }
    var denied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(serverUrl) { isLocal = LocalNetwork.isLocalServer(serverUrl) }
    LifecycleResumeEffect(Unit) {
        granted = LocalNetwork.isGranted(context)
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result
        denied = !result
    }
    when {
        granted || skipped || isLocal == false -> content()
        isLocal == null -> Box(Modifier.fillMaxSize().fintageBackground())
        else -> {
            val settingsOnly = denied && !LocalNetwork.canAskAgain(context)
            Column(
                Modifier
                    .fillMaxSize()
                    .fintageBackground()
                    .safeDrawingPadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Fintage.colors.glass),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Fintage.colors.gold, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text("Your server is at home", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (settingsOnly) {
                        "Allow Nearby devices for Fintage in the settings of the app, so it can reach your Jellyfin server on your home network."
                    } else {
                        "Your Jellyfin server is on your home network. Android asks you to allow Fintage to reach devices there before it can show your library."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Fintage.colors.dimText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
                Spacer(Modifier.height(28.dp))
                GoldButton(
                    text = if (settingsOnly) "Open settings" else "Allow access",
                    onClick = {
                        if (settingsOnly) LocalNetwork.openAppSettings(context) else launcher.launch(LocalNetwork.PERMISSION)
                    },
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { skipped = true }) { Text("Continue anyway") }
            }
        }
    }
}
