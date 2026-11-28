package io.github.ricolehn.fintage

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ricolehn.fintage.data.AppPreferences
import io.github.ricolehn.fintage.data.SessionState
import io.github.ricolehn.fintage.data.ThemeMode
import io.github.ricolehn.fintage.player.PlayerActivity
import io.github.ricolehn.fintage.ui.LocalAppContainer
import io.github.ricolehn.fintage.ui.LocalImages
import io.github.ricolehn.fintage.ui.LocalNetworkGate
import io.github.ricolehn.fintage.ui.login.LoginScreen
import io.github.ricolehn.fintage.ui.navigation.MainScaffold
import io.github.ricolehn.fintage.ui.navigation.NavigatorViewModel
import io.github.ricolehn.fintage.ui.theme.FintageEase
import io.github.ricolehn.fintage.ui.theme.FintageTheme
import io.github.ricolehn.fintage.ui.theme.fintageBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = appContainer
        splash.setKeepOnScreenCondition { container.sessions.state.value is SessionState.Loading }
        enableEdgeToEdge()

        setContent {
            val preferences by container.settings.preferences.collectAsStateWithLifecycle(AppPreferences())
            val dark = when (preferences.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
            }
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            FintageTheme(darkTheme = dark, filmGrain = preferences.filmGrain) {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalImages provides container.images,
                ) {
                    AppRoot(container)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(container: AppContainer) {
    val sessionState by container.sessions.state.collectAsStateWithLifecycle()
    Crossfade(
        targetState = sessionState,
        animationSpec = tween(450, easing = FintageEase),
        label = "session",
    ) { state ->
        when (state) {
            SessionState.Loading -> Box(Modifier.fillMaxSize().fintageBackground())
            is SessionState.SignedOut -> LoginScreen(message = state.message)
            is SessionState.SignedIn -> {
                val navigatorViewModel: NavigatorViewModel =
                    viewModel(key = "navigator-${state.session.serverUrl}-${state.session.userId}")
                val context = androidx.compose.ui.platform.LocalContext.current
                LocalNetworkGate(state.session.serverUrl) {
                    MainScaffold(
                        navigator = navigatorViewModel.navigator,
                        onPlay = { request -> PlayerActivity.start(context, request) },
                    )
                }
            }
        }
    }
}
