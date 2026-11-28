package io.github.ricolehn.fintage.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ricolehn.fintage.data.HomeFeed
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: MediaRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<HomeFeed>>(Load.Loading)
    val state: StateFlow<Load<HomeFeed>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var lastContinueRefresh = 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (_state.value !is Load.Ready) _state.value = Load.Loading
            try {
                _state.value = Load.Ready(repository.home())
                lastContinueRefresh = System.currentTimeMillis()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value !is Load.Ready) _state.value = Load.Failed(e.friendlyMessage())
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            repository.clearCache()
            try {
                _state.value = Load.Ready(repository.home())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_state.value !is Load.Ready) _state.value = Load.Failed(e.friendlyMessage())
            } finally {
                _refreshing.value = false
            }
        }
    }

    /** Updates Continue Watching and Next Up, e.g. after the player was closed. */
    fun refreshContinue() {
        val current = (_state.value as? Load.Ready)?.value ?: return
        val now = System.currentTimeMillis()
        if (now - lastContinueRefresh < 3_000) return
        lastContinueRefresh = now
        viewModelScope.launch {
            try {
                val (resume, nextUp) = repository.refreshContinue()
                val latest = (_state.value as? Load.Ready)?.value ?: current
                _state.value = Load.Ready(latest.copy(resume = resume, nextUp = nextUp))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}
