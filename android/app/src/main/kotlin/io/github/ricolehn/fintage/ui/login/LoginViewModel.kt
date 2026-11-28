package io.github.ricolehn.fintage.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ricolehn.fintage.data.SessionManager
import io.github.ricolehn.fintage.data.SettingsStore
import io.github.ricolehn.fintage.data.jellyfin.DiscoveredServer
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import io.github.ricolehn.fintage.data.jellyfin.PublicSystemInfo
import io.github.ricolehn.fintage.data.jellyfin.ServerDiscovery
import io.github.ricolehn.fintage.data.jellyfin.UserDto
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.util.LocalNetwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LoginStep { Server, Credentials }

data class LoginState(
    val step: LoginStep = LoginStep.Server,
    val address: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val discovering: Boolean = false,
    val discovered: List<DiscoveredServer> = emptyList(),
    val serverUrl: String? = null,
    val serverInfo: PublicSystemInfo? = null,
    val publicUsers: List<UserDto> = emptyList(),
    val quickConnectEnabled: Boolean = false,
    val username: String = "",
    val password: String = "",
    val quickConnectCode: String? = null,
    /** whether the app may reach devices on the home network (always true before Android 17) */
    val localNetworkGranted: Boolean = true,
    /** asks the screen to show the system request for the home network */
    val askLocalNetwork: Boolean = false,
    /** the user said no; once Android stops asking, the settings of the app are the way to allow it */
    val localNetworkDenied: Boolean = false,
)

class LoginViewModel(
    private val client: JellyfinClient,
    private val sessions: SessionManager,
    private val settings: SettingsStore,
    message: String?,
    localNetworkGranted: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState(error = message, localNetworkGranted = localNetworkGranted))
    val state: StateFlow<LoginState> = _state.asStateFlow()
    private var quickConnectJob: Job? = null
    private var connectAfterPermission = false

    init {
        viewModelScope.launch {
            val last = settings.lastServer.first()
            if (!last.isNullOrBlank() && _state.value.address.isEmpty()) _state.update { it.copy(address = last) }
        }
        discover()
    }

    fun discover() {
        if (!_state.value.localNetworkGranted) return
        viewModelScope.launch {
            _state.update { it.copy(discovering = true) }
            val servers = ServerDiscovery.discover()
            _state.update { it.copy(discovering = false, discovered = servers) }
        }
    }

    fun setAddress(value: String) = _state.update { it.copy(address = value, error = null) }
    fun setUsername(value: String) = _state.update { it.copy(username = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun chooseServer(server: DiscoveredServer) {
        _state.update { it.copy(address = server.address) }
        connect()
    }

    /** The permission may change while the screen is away, e.g. in the settings of the app. */
    fun onLocalNetworkChanged(granted: Boolean) {
        val before = _state.value.localNetworkGranted
        _state.update { it.copy(localNetworkGranted = granted, localNetworkDenied = it.localNetworkDenied && !granted) }
        if (granted && !before) discover()
    }

    fun requestLocalNetwork() = _state.update { it.copy(askLocalNetwork = true) }

    fun localNetworkAsked() = _state.update { it.copy(askLocalNetwork = false) }

    fun onLocalNetworkResult(granted: Boolean) {
        _state.update { it.copy(localNetworkGranted = granted, localNetworkDenied = !granted) }
        val retry = connectAfterPermission
        connectAfterPermission = false
        if (granted) {
            discover()
            if (retry) connect()
        } else if (retry) {
            _state.update {
                it.copy(error = "This server is on your home network. Allow Fintage to reach devices there to connect to it.")
            }
        }
    }

    fun connect() {
        val address = _state.value.address
        if (address.isBlank() || _state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            if (!_state.value.localNetworkGranted && LocalNetwork.isEnforced && LocalNetwork.isLocalServer(address)) {
                connectAfterPermission = true
                _state.update { it.copy(busy = false, askLocalNetwork = true) }
                return@launch
            }
            try {
                val (url, info) = client.findServer(address)
                client.configure(url, null, null)
                val users = try { client.publicUsers() } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
                val quickConnect = client.quickConnectEnabled()
                _state.update {
                    it.copy(
                        busy = false,
                        step = LoginStep.Credentials,
                        serverUrl = url,
                        serverInfo = info,
                        publicUsers = users,
                        quickConnectEnabled = quickConnect,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.friendlyMessage()) }
            }
        }
    }

    fun changeServer() {
        cancelQuickConnect()
        _state.update { it.copy(step = LoginStep.Server, error = null, password = "") }
    }

    fun signIn() {
        val current = _state.value
        val url = current.serverUrl ?: return
        val info = current.serverInfo ?: return
        if (current.username.isBlank() || current.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val result = client.authenticateByName(current.username.trim(), current.password)
                sessions.signIn(url, info, result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = e.friendlyMessage()) }
            }
        }
    }

    fun startQuickConnect() {
        val url = _state.value.serverUrl ?: return
        val info = _state.value.serverInfo ?: return
        quickConnectJob?.cancel()
        quickConnectJob = viewModelScope.launch {
            try {
                val started = client.quickConnectInitiate()
                _state.update { it.copy(quickConnectCode = started.code, error = null) }
                while (isActive) {
                    delay(3_000)
                    val status = client.quickConnectState(started.secret)
                    if (status.authenticated) {
                        val result = client.authenticateWithQuickConnect(started.secret)
                        _state.update { it.copy(quickConnectCode = null) }
                        sessions.signIn(url, info, result)
                        break
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(quickConnectCode = null, error = e.friendlyMessage()) }
            }
        }
    }

    fun cancelQuickConnect() {
        quickConnectJob?.cancel()
        _state.update { it.copy(quickConnectCode = null) }
    }
}
