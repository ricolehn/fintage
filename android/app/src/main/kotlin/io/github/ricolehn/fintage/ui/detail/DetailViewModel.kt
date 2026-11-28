package io.github.ricolehn.fintage.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ricolehn.fintage.data.ItemDetail
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.MediaStream
import io.github.ricolehn.fintage.data.jellyfin.UserData
import io.github.ricolehn.fintage.ui.Load
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.friendlyMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailState(
    val load: Load<ItemDetail> = Load.Loading,
    val seasonId: String? = null,
    val episodes: List<BaseItem> = emptyList(),
    val episodesLoading: Boolean = false,
    /** the chosen audio track, or null for the default of the server */
    val audioIndex: Int? = null,
    /** the chosen subtitle track, -1 for none, or null for the default of the server */
    val subtitleIndex: Int? = null,
    val resolvingPlay: Boolean = false,
) {
    val detail: ItemDetail? get() = (load as? Load.Ready)?.value
    val item: BaseItem? get() = detail?.item
}

class DetailViewModel(private val itemId: String, private val repository: MediaRepository) : ViewModel() {
    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()
    private var episodesJob: Job? = null
    private var lastRefresh = 0L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(load = Load.Loading) }
            try {
                val detail = repository.detail(itemId)
                lastRefresh = System.currentTimeMillis()
                _state.update { it.copy(load = Load.Ready(detail)) }
                val item = detail.item
                when (item.type) {
                    "Series" -> {
                        val seasons = detail.seasons
                        val initial = detail.nextUp?.seasonId?.takeIf { id -> seasons.any { it.id == id } }
                            ?: seasons.firstOrNull { (it.indexNumber ?: 0) > 0 && it.userData?.played != true }?.id
                            ?: seasons.firstOrNull { (it.indexNumber ?: 0) > 0 }?.id
                            ?: seasons.firstOrNull()?.id
                        if (initial != null) selectSeason(initial)
                    }
                    "Season" -> selectSeason(item.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(load = Load.Failed(e.friendlyMessage())) }
            }
        }
    }

    /** Updates the progress and the watched state after the player was closed. */
    fun refreshQuietly() {
        val current = _state.value.detail ?: return
        val now = System.currentTimeMillis()
        if (now - lastRefresh < 3_000) return
        lastRefresh = now
        viewModelScope.launch {
            try {
                val fresh = repository.detail(itemId)
                _state.update { it.copy(load = Load.Ready(fresh.copy(similar = current.similar.ifEmpty { fresh.similar }))) }
                val seriesId = fresh.item.seriesId ?: fresh.item.id
                val seasonId = _state.value.seasonId
                if (seasonId != null && (fresh.item.type == "Series" || fresh.item.type == "Season")) {
                    val episodes = repository.episodes(seriesId, seasonId)
                    _state.update { it.copy(episodes = episodes) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun selectSeason(seasonId: String) {
        val item = _state.value.item ?: return
        val seriesId = if (item.type == "Series") item.id else item.seriesId ?: return
        _state.update { it.copy(seasonId = seasonId, episodesLoading = true) }
        episodesJob?.cancel()
        episodesJob = viewModelScope.launch {
            try {
                val episodes = repository.episodes(seriesId, seasonId)
                _state.update { it.copy(episodes = episodes, episodesLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(episodes = emptyList(), episodesLoading = false) }
            }
        }
    }

    fun selectAudio(index: Int) = _state.update { it.copy(audioIndex = index) }

    fun selectSubtitle(index: Int) = _state.update { it.copy(subtitleIndex = index) }

    private fun updateUserData(change: (UserData) -> UserData) {
        _state.update { state ->
            val detail = state.detail ?: return@update state
            val item = detail.item
            state.copy(load = Load.Ready(detail.copy(item = item.copy(userData = change(item.userData ?: UserData())))))
        }
    }

    fun toggleFavorite() {
        val item = _state.value.item ?: return
        val favorite = item.userData?.isFavorite != true
        updateUserData { it.copy(isFavorite = favorite) }
        viewModelScope.launch {
            try {
                repository.setFavorite(item.id, favorite)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                updateUserData { it.copy(isFavorite = !favorite) }
            }
        }
    }

    fun togglePlayed() {
        val item = _state.value.item ?: return
        val played = item.userData?.played != true
        updateUserData { it.copy(played = played, playbackPositionTicks = 0) }
        viewModelScope.launch {
            try {
                repository.setPlayed(item.id, played)
                lastRefresh = 0
                refreshQuietly()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                updateUserData { it.copy(played = !played) }
            }
        }
    }

    fun toggleEpisodePlayed(episode: BaseItem) {
        val played = episode.userData?.played != true
        _state.update { state ->
            state.copy(episodes = state.episodes.map {
                if (it.id == episode.id) it.copy(userData = (it.userData ?: UserData()).copy(played = played, playbackPositionTicks = 0)) else it
            })
        }
        viewModelScope.launch {
            try {
                repository.setPlayed(episode.id, played)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    /** Works out what Play starts: the item itself, or the next episode of a show or season. */
    fun play(fromStart: Boolean, onReady: (PlayRequest) -> Unit) {
        val state = _state.value
        val detail = state.detail ?: return
        val item = detail.item
        when (item.type) {
            "Series", "Season" -> {
                val nextUp = if (item.type == "Series") detail.nextUp else null
                val inSeason = state.episodes.firstOrNull { it.userData?.played != true } ?: state.episodes.firstOrNull()
                val target = nextUp ?: inSeason
                if (target != null) {
                    onReady(PlayRequest(target.id, startPositionTicks = if (fromStart) 0 else null))
                    return
                }
                _state.update { it.copy(resolvingPlay = true) }
                viewModelScope.launch {
                    val episode = try {
                        repository.firstEpisode(item)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    _state.update { it.copy(resolvingPlay = false) }
                    episode?.let { onReady(PlayRequest(it.id, startPositionTicks = if (fromStart) 0 else null)) }
                }
            }
            else -> onReady(
                PlayRequest(
                    itemId = item.id,
                    startPositionTicks = if (fromStart) 0 else null,
                    audioStreamIndex = state.audioIndex,
                    subtitleStreamIndex = state.subtitleIndex,
                ),
            )
        }
    }

    fun shuffle(onReady: (PlayRequest) -> Unit) {
        val item = _state.value.item ?: return
        val seriesId = if (item.type == "Series") item.id else item.seriesId ?: return
        viewModelScope.launch {
            val episode = try {
                repository.randomEpisode(seriesId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            episode?.let { onReady(PlayRequest(it.id, startPositionTicks = 0)) }
        }
    }

    fun trailer(onLocal: (PlayRequest) -> Unit, onRemote: (String) -> Unit) {
        val item = _state.value.item ?: return
        if ((item.localTrailerCount ?: 0) > 0) {
            viewModelScope.launch {
                val trailer = try {
                    repository.localTrailer(item.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (trailer != null) onLocal(PlayRequest(trailer.id, startPositionTicks = 0))
                else item.remoteTrailers.firstOrNull()?.url?.let(onRemote)
            }
        } else {
            item.remoteTrailers.firstOrNull()?.url?.let(onRemote)
        }
    }
}

/** The streams of the first version of an item. */
val BaseItem.streams: List<MediaStream>
    get() = mediaSources.firstOrNull()?.mediaStreams?.takeIf { it.isNotEmpty() } ?: mediaStreams
