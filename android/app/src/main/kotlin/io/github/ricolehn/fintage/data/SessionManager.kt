package io.github.ricolehn.fintage.data

import io.github.ricolehn.fintage.data.jellyfin.AuthenticationResult
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import io.github.ricolehn.fintage.data.jellyfin.PublicSystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data class SignedOut(val message: String? = null) : SessionState
    data class SignedIn(val session: Session) : SessionState
}

class SessionManager(
    private val settings: SettingsStore,
    private val client: JellyfinClient,
    private val scope: CoroutineScope,
    private val onSessionChanged: () -> Unit = {},
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val currentSession: Session? get() = (state.value as? SessionState.SignedIn)?.session

    init {
        scope.launch {
            val saved = settings.session.first()
            if (saved != null) {
                client.configure(saved.serverUrl, saved.accessToken, saved.userId)
                _state.value = SessionState.SignedIn(saved)
            } else {
                _state.value = SessionState.SignedOut()
            }
        }
        scope.launch {
            client.unauthorizedEvents.collect {
                if (state.value is SessionState.SignedIn) signOut("Your session has ended. Please sign in again.", remote = false)
            }
        }
    }

    suspend fun signIn(serverUrl: String, info: PublicSystemInfo, result: AuthenticationResult) {
        val user = result.user ?: error("The server did not send the user.")
        val token = result.accessToken ?: error("The server did not send a token.")
        val session = Session(
            serverUrl = serverUrl.trimEnd('/'),
            serverName = info.serverName ?: "Jellyfin",
            serverId = info.id ?: result.serverId,
            userId = user.id,
            userName = user.name.orEmpty(),
            userImageTag = user.primaryImageTag,
            accessToken = token,
        )
        settings.saveSession(session)
        client.configure(session.serverUrl, session.accessToken, session.userId)
        onSessionChanged()
        _state.value = SessionState.SignedIn(session)
    }

    fun signOut(message: String? = null, remote: Boolean = true) {
        scope.launch {
            if (remote) client.logout()
            settings.clearSession()
            client.configure(client.baseUrl, null, null)
            onSessionChanged()
            _state.value = SessionState.SignedOut(message)
        }
    }
}
