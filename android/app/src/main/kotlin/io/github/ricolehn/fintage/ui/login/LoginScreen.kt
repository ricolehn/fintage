package io.github.ricolehn.fintage.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ricolehn.fintage.R
import io.github.ricolehn.fintage.data.jellyfin.DiscoveredServer
import io.github.ricolehn.fintage.data.jellyfin.UserDto
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.components.FintageImage
import io.github.ricolehn.fintage.ui.components.GlassPillButton
import io.github.ricolehn.fintage.ui.components.GoldButton
import io.github.ricolehn.fintage.ui.theme.Fintage
import io.github.ricolehn.fintage.ui.theme.FintageEase
import io.github.ricolehn.fintage.ui.theme.Palette
import io.github.ricolehn.fintage.ui.theme.fintageBackground
import io.github.ricolehn.fintage.ui.theme.glass
import io.github.ricolehn.fintage.util.LocalNetwork

@Composable
fun LoginScreen(message: String?) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel {
        LoginViewModel(container.client, container.sessions, container.settings, message, LocalNetwork.isGranted(context))
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gold = Palette.Gold
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onLocalNetworkResult(granted)
    }
    LaunchedEffect(state.askLocalNetwork) {
        if (state.askLocalNetwork) {
            viewModel.localNetworkAsked()
            if (state.localNetworkDenied && !LocalNetwork.canAskAgain(context)) {
                LocalNetwork.openAppSettings(context)
            } else {
                permission.launch(LocalNetwork.PERMISSION)
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onLocalNetworkChanged(LocalNetwork.isGranted(context))
        onPauseOrDispose { }
    }

    Box(
        Modifier
            .fillMaxSize()
            .fintageBackground()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(gold.copy(alpha = 0.16f), gold.copy(alpha = 0f)),
                        center = Offset(size.width / 2f, size.height * 0.12f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.fintage_mark), contentDescription = null, modifier = Modifier.size(width = 62.dp, height = 57.dp))
            Spacer(Modifier.height(14.dp))
            Text("Fintage", style = MaterialTheme.typography.displaySmall)
            Text(
                "Your Jellyfin library, calm and cinematic",
                style = MaterialTheme.typography.bodyMedium,
                color = Fintage.colors.dimText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn(tween(380, easing = FintageEase)) togetherWith fadeOut(tween(160)) },
                label = "step",
                modifier = Modifier.widthIn(max = 440.dp),
            ) { step ->
                when (step) {
                    LoginStep.Server -> ServerStep(state, viewModel)
                    LoginStep.Credentials -> CredentialsStep(state, viewModel)
                }
            }
        }
    }

    state.quickConnectCode?.let { code ->
        AlertDialog(
            onDismissRequest = viewModel::cancelQuickConnect,
            title = { Text("Quick Connect") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "On a device where you are signed in to Jellyfin, open your profile, choose Quick Connect and enter this code:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Fintage.colors.dimText,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        code,
                        style = MaterialTheme.typography.displayMedium.copy(letterSpacing = 8.sp),
                        color = Fintage.colors.gold,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(20.dp))
                    LoadingIndicator(color = Fintage.colors.gold, modifier = Modifier.size(40.dp))
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = viewModel::cancelQuickConnect) { Text("Cancel") } },
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Fintage.colors.gold,
    focusedLabelColor = Fintage.colors.gold,
    cursorColor = Fintage.colors.gold,
    unfocusedBorderColor = Fintage.colors.glassBorder,
    focusedContainerColor = Fintage.colors.glass,
    unfocusedContainerColor = Fintage.colors.glass,
)

@Composable
private fun ServerStep(state: LoginState, viewModel: LoginViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text("Connect to your server", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            OutlinedTextField(
                value = state.address,
                onValueChange = viewModel::setAddress,
                label = { Text("Server address") },
                placeholder = { Text("192.168.1.20:8096") },
                leadingIcon = { Icon(Icons.Rounded.Dns, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { viewModel.connect() }),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            Spacer(Modifier.height(16.dp))
            GoldButton("Connect", onClick = viewModel::connect, loading = state.busy, enabled = state.address.isNotBlank(), modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(28.dp))
        if (!state.localNetworkGranted) {
            val context = LocalContext.current
            HomeNetworkCard(
                denied = state.localNetworkDenied && !LocalNetwork.canAskAgain(context),
                onAllow = viewModel::requestLocalNetwork,
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Fintage.colors.dimText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("On your network", style = MaterialTheme.typography.titleSmall, color = Fintage.colors.dimText, modifier = Modifier.weight(1f))
            if (state.discovering) {
                LoadingIndicator(color = Fintage.colors.dimText, modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = viewModel::discover) { Text("Search again") }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.discovered.isEmpty() && !state.discovering) {
            Text(
                "No server answered on this network. Type its address above.",
                style = MaterialTheme.typography.bodySmall,
                color = Fintage.colors.faintText,
            )
        }
        state.discovered.forEach { server -> DiscoveredRow(server, onClick = { viewModel.chooseServer(server) }) }
    }
}

/** Explains why Android asks for the home network, before the system asks. */
@Composable
private fun HomeNetworkCard(denied: Boolean, onAllow: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = Fintage.colors.gold)
            Spacer(Modifier.width(12.dp))
            Text("Servers at home", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (denied) {
                "To find and play from a server on your home network, allow Nearby devices for Fintage in the settings of the app. Servers on the internet work without it."
            } else {
                "To find and play from a Jellyfin server on your home network, Android asks you to allow Fintage to reach devices there. Servers on the internet work without it."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Fintage.colors.dimText,
        )
        Spacer(Modifier.height(14.dp))
        GlassPillButton(if (denied) "Open settings" else "Allow access", icon = Icons.Rounded.Wifi, onClick = onAllow)
    }
}

@Composable
private fun DiscoveredRow(server: DiscoveredServer, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .glass(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Dns, contentDescription = null, tint = Fintage.colors.gold)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(server.name.ifEmpty { "Jellyfin" }, style = MaterialTheme.typography.titleSmall)
            Text(server.address, style = MaterialTheme.typography.bodySmall, color = Fintage.colors.dimText)
        }
    }
}

@Composable
private fun CredentialsStep(state: LoginState, viewModel: LoginViewModel) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(Fintage.colors.glass)
                .border(0.75.dp, Fintage.colors.glassBorder, CircleShape)
                .clickable(onClick = viewModel::changeServer)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Dns, contentDescription = null, tint = Fintage.colors.gold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.serverInfo?.serverName ?: "Jellyfin", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Text("Change", style = MaterialTheme.typography.labelMedium, color = Fintage.colors.dimText)
        }
        Spacer(Modifier.height(22.dp))
        Text("Sign in to continue", style = MaterialTheme.typography.headlineMedium)
        if (state.publicUsers.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(state.publicUsers, key = { it.id }) { user ->
                    PublicUser(user, selected = user.name == state.username, onClick = { viewModel.setUsername(user.name.orEmpty()) })
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::setUsername,
                label = { Text("User") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::setPassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(state.error)
            Spacer(Modifier.height(18.dp))
            GoldButton("Sign In", onClick = viewModel::signIn, loading = state.busy, enabled = state.username.isNotBlank(), modifier = Modifier.fillMaxWidth())
            if (state.quickConnectEnabled) {
                Spacer(Modifier.height(12.dp))
                GlassPillButton(
                    "Use Quick Connect",
                    icon = Icons.Rounded.Key,
                    onClick = viewModel::startQuickConnect,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PublicUser(user: UserDto, selected: Boolean, onClick: () -> Unit) {
    val images = LocalImages.current
    Column(
        Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Fintage.colors.glass)
                .border(if (selected) 2.dp else 0.75.dp, if (selected) Fintage.colors.gold else Fintage.colors.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val image = images.user(user.id, user.primaryImageTag)
            if (image != null) {
                FintageImage(image, contentDescription = user.name, modifier = Modifier.fillMaxSize())
            } else {
                Text(user.name?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(user.name.orEmpty(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) {
        Spacer(Modifier.height(10.dp))
        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
