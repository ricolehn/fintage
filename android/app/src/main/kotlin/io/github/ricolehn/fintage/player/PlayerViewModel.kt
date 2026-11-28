@file:OptIn(UnstableApi::class)

package io.github.ricolehn.fintage.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.github.ricolehn.fintage.appContainer
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.DeviceProfiles
import io.github.ricolehn.fintage.data.jellyfin.ItemFields
import io.github.ricolehn.fintage.data.jellyfin.MediaSource
import io.github.ricolehn.fintage.data.jellyfin.MediaStream
import io.github.ricolehn.fintage.data.jellyfin.PlaybackInfoRequest
import io.github.ricolehn.fintage.data.jellyfin.PlaybackStartInfo
import io.github.ricolehn.fintage.data.jellyfin.PlaybackStopInfo
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.friendlyMessage
import io.github.ricolehn.fintage.util.Format
import io.github.ricolehn.fintage.util.TICKS_PER_MS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What is playing: the item, the version and the tracks the server settled on, and how it is played. */
data class PlaybackSession(
    val item: BaseItem,
    val source: MediaSource,
    val playSessionId: String?,
    val playMethod: String,
    val audioIndex: Int?,
    val subtitleIndex: Int,
) {
    val audioStreams: List<MediaStream> get() = source.mediaStreams.filter { it.type == "Audio" }
    val subtitleStreams: List<MediaStream> get() = source.mediaStreams.filter { it.type == "Subtitle" }
}

data class PlayerUi(
    val title: String = "",
    val subtitle: String? = null,
    val loading: Boolean = true,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val error: String? = null,
    val session: PlaybackSession? = null,
    val upNext: BaseItem? = null,
    val upNextSeconds: Int = 0,
    val finished: Boolean = false,
    val speed: Float = 1f,
    val zoom: Boolean = false,
    val videoWidth: Int = 16,
    val videoHeight: Int = 9,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val container = application.appContainer
    private val client = container.client

    private val _ui = MutableStateFlow(PlayerUi())
    val ui: StateFlow<PlayerUi> = _ui.asStateFlow()

    private var request: PlayRequest? = null
    private var session: PlaybackSession? = null
    private var tracksApplied = false
    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var upNextJob: Job? = null
    private var fallbackTried = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _ui.update { it.copy(isPlaying = isPlaying) }
            reportProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _ui.update { it.copy(buffering = playbackState == Player.STATE_BUFFERING, loading = it.loading && playbackState != Player.STATE_READY) }
            if (playbackState == Player.STATE_ENDED) onEnded()
        }

        override fun onTracksChanged(tracks: Tracks) {
            if (!tracksApplied && !tracks.isEmpty) {
                tracksApplied = true
                applyTracks(tracks)
            }
        }

        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _ui.update { it.copy(videoWidth = videoSize.width, videoHeight = (videoSize.height / videoSize.pixelWidthHeightRatio.coerceAtLeast(0.01f)).toInt().coerceAtLeast(1)) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val current = session
            // a file the phone said it could play but cannot: ask the server once for a conversion instead
            if (current != null && current.playMethod != "Transcode" && !fallbackTried && error.errorCode in 3000..5999) {
                fallbackTried = true
                val position = player.currentPosition * TICKS_PER_MS
                loadJob?.cancel()
                loadJob = viewModelScope.launch {
                    reportStopped(failed = true)
                    prepare(current.item.id, position, current.audioIndex, current.subtitleIndex, forceTranscode = true)
                }
                return
            }
            _ui.update { it.copy(error = playbackErrorMessage(error), loading = false) }
            reportStopped(failed = true)
        }
    }

    val player: ExoPlayer = run {
        val dataSource = OkHttpDataSource.Factory(container.okHttp)
            .setDefaultRequestProperties(mapOf("Authorization" to client.authorizationHeader()))
        val renderers = DefaultRenderersFactory(application).setEnableDecoderFallback(true)
        ExoPlayer.Builder(application, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(application).setDataSourceFactory(dataSource))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(30_000)
            .build()
            .also { it.addListener(playerListener) }
    }

    /** Starts the request, unless it is already playing. */
    fun start(newRequest: PlayRequest) {
        if (newRequest == request) return
        request = newRequest
        fallbackTried = false
        upNextJob?.cancel()
        _ui.update { PlayerUi(speed = it.speed) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reportStopped()
            prepare(newRequest.itemId, newRequest.startPositionTicks, newRequest.audioStreamIndex, newRequest.subtitleStreamIndex)
        }
    }

    fun retry() {
        val current = request ?: return
        request = null
        start(current.copy(startPositionTicks = player.currentPosition.takeIf { it > 0 }?.times(TICKS_PER_MS) ?: current.startPositionTicks))
    }

    private suspend fun prepare(
        itemId: String,
        startTicks: Long?,
        audioIndex: Int?,
        subtitleIndex: Int?,
        forceTranscode: Boolean = false,
    ) {
        try {
            _ui.update { it.copy(loading = true, error = null) }
            val item = client.item(itemId, ItemFields.DETAIL)
            val resumeTicks = item.userData?.let { if (it.played) 0L else it.playbackPositionTicks } ?: 0L
            val startPosition = startTicks ?: resumeTicks
            val preferences = container.settings.preferences.first()
            val maxBitrate = if (preferences.maxBitrate > 0) preferences.maxBitrate else 140_000_000L
            val profile = withContext(Dispatchers.Default) { DeviceProfiles.build(getApplication(), maxBitrate) }
            val info = client.playbackInfo(
                itemId,
                PlaybackInfoRequest(
                    userId = client.userId.orEmpty(),
                    maxStreamingBitrate = maxBitrate,
                    startTimeTicks = startPosition,
                    audioStreamIndex = audioIndex,
                    subtitleStreamIndex = subtitleIndex,
                    mediaSourceId = item.mediaSources.firstOrNull()?.id ?: item.id,
                    deviceProfile = profile,
                    enableDirectPlay = !forceTranscode,
                    enableDirectStream = !forceTranscode,
                ),
            )
            if (info.errorCode != null) error(errorCodeMessage(info.errorCode))
            val source = info.mediaSources.firstOrNull() ?: error("The server has no playable version of this title.")
            val (url, method) = when {
                source.supportsDirectPlay -> client.directStreamUrl(itemId, source.id, source.container, info.playSessionId) to "DirectPlay"
                source.transcodingUrl != null -> client.absoluteUrl(source.transcodingUrl) to "Transcode"
                source.supportsDirectStream -> client.directStreamUrl(itemId, source.id, source.container, info.playSessionId) to "DirectStream"
                else -> error("This title cannot be played on this phone.")
            }
            val chosenAudio = audioIndex ?: source.defaultAudioStreamIndex
            val chosenSubtitle = subtitleIndex ?: source.defaultSubtitleStreamIndex ?: -1
            val newSession = PlaybackSession(item, source, info.playSessionId, method, chosenAudio, chosenSubtitle)
            session = newSession
            tracksApplied = false

            val subtitles = source.mediaStreams
                .filter { it.type == "Subtitle" && it.deliveryMethod == "External" && it.deliveryUrl != null }
                .mapNotNull { stream ->
                    val mime = subtitleMime(stream.codec) ?: return@mapNotNull null
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(client.absoluteUrl(stream.deliveryUrl!!)))
                        .setId(subtitleTrackId(stream.index))
                        .setMimeType(mime)
                        .setLanguage(stream.language)
                        .setLabel(Format.subtitleTrack(stream))
                        .setSelectionFlags(if (stream.isForced) C.SELECTION_FLAG_FORCED else 0)
                        .build()
                }
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaId(itemId)
                .apply { if (method == "Transcode" && source.transcodingSubProtocol == "hls") setMimeType(MimeTypes.APPLICATION_M3U8) }
                .setSubtitleConfigurations(subtitles)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(titleOf(item))
                        .setSubtitle(subtitleOf(item))
                        .build(),
                )
                .build()
            player.setMediaItem(mediaItem, startPosition / TICKS_PER_MS)
            player.prepare()
            player.playWhenReady = true
            _ui.update { it.copy(title = titleOf(item), subtitle = subtitleOf(item), session = newSession, upNext = null) }

            client.reportPlaybackStart(startInfo(newSession, startPosition, paused = false))
            startProgressLoop()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = e.friendlyMessage()) }
        }
    }

    private fun titleOf(item: BaseItem) = if (item.type == "Episode") item.seriesName ?: item.name.orEmpty() else item.name.orEmpty()

    private fun subtitleOf(item: BaseItem): String? = when (item.type) {
        "Episode" -> listOfNotNull(Format.episodeCode(item), item.name).joinToString(" · ")
        else -> item.productionYear?.toString()
    }

    /** Picks the chosen audio track inside the file and the chosen side-loaded subtitles. */
    private fun applyTracks(tracks: Tracks) {
        val current = session ?: return
        val builder = player.trackSelectionParameters.buildUpon()
            // subtitles inside the file are never picked by themselves; Fintage picks the ones the server sends
            .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
            .setPreferredTextLanguage(null)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        val subtitleStream = current.subtitleStreams.firstOrNull { it.index == current.subtitleIndex }
        val textGroup = if (subtitleStream != null && subtitleStream.deliveryMethod == "External") {
            tracks.groups.firstOrNull { group ->
                group.type == C.TRACK_TYPE_TEXT &&
                    (0 until group.length).any { group.getTrackFormat(it).id?.endsWith(subtitleTrackId(subtitleStream.index)) == true }
            }
        } else null
        if (textGroup != null) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .addOverride(TrackSelectionOverride(textGroup.mediaTrackGroup, 0))
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        }
        if (current.playMethod != "Transcode") {
            // the audio tracks of the file come in the same order as the audio streams of the server
            val streams = current.audioStreams.filter { !it.isExternal }.sortedBy { it.index }
            val position = streams.indexOfFirst { it.index == current.audioIndex }
            val groups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            builder.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            if (position in groups.indices && groups.size == streams.size) {
                builder.addOverride(TrackSelectionOverride(groups[position].mediaTrackGroup, 0))
            }
        }
        player.trackSelectionParameters = builder.build()
    }

    fun selectSubtitle(index: Int) {
        val current = session ?: return
        if (index == current.subtitleIndex) return
        val stream = current.subtitleStreams.firstOrNull { it.index == index }
        val previous = current.subtitleStreams.firstOrNull { it.index == current.subtitleIndex }
        val burnedIn = stream?.deliveryMethod == "Encode" || previous?.deliveryMethod == "Encode"
        if (!burnedIn) {
            session = current.copy(subtitleIndex = index)
            _ui.update { it.copy(session = session) }
            applyTracks(player.currentTracks)
            reportProgress()
        } else {
            restartAt(current.audioIndex, index)
        }
    }

    fun selectAudio(index: Int) {
        val current = session ?: return
        if (index == current.audioIndex) return
        val stream = current.audioStreams.firstOrNull { it.index == index }
        val playable = current.playMethod != "Transcode" && stream != null && isDecodable(stream)
        if (playable) {
            session = current.copy(audioIndex = index)
            _ui.update { it.copy(session = session) }
            applyTracks(player.currentTracks)
            reportProgress()
        } else {
            restartAt(index, current.subtitleIndex)
        }
    }

    /** Whether the direct stream of the file can switch to this audio track inside the player. */
    private fun isDecodable(stream: MediaStream): Boolean {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val streams = session?.audioStreams?.filter { !it.isExternal }?.sortedBy { it.index } ?: return false
        val position = streams.indexOfFirst { it.index == stream.index }
        return position in groups.indices && groups.size == streams.size && groups[position].isTrackSupported(0)
    }

    /** Asks the server again from the current position, e.g. for an audio track it has to convert. */
    private fun restartAt(audioIndex: Int?, subtitleIndex: Int) {
        val current = session ?: return
        val position = player.currentPosition * TICKS_PER_MS
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reportStopped()
            prepare(current.item.id, position, audioIndex, subtitleIndex)
        }
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _ui.update { it.copy(speed = speed) }
    }

    fun toggleZoom() = _ui.update { it.copy(zoom = !it.zoom) }

    fun seekBy(millis: Long) {
        val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + millis).coerceIn(0, duration))
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                if (player.isPlaying) reportProgress()
            }
        }
    }

    private fun startInfo(current: PlaybackSession, positionTicks: Long, paused: Boolean) = PlaybackStartInfo(
        itemId = current.item.id,
        mediaSourceId = current.source.id,
        playSessionId = current.playSessionId,
        playMethod = current.playMethod,
        positionTicks = positionTicks,
        audioStreamIndex = current.audioIndex,
        subtitleStreamIndex = current.subtitleIndex,
        isPaused = paused,
    )

    private fun reportProgress() {
        val current = session ?: return
        val info = startInfo(current, player.currentPosition * TICKS_PER_MS, paused = !player.isPlaying)
        container.appScope.launch { runCatching { client.reportPlaybackProgress(info) } }
    }

    private fun reportStopped(failed: Boolean = false) {
        val current = session ?: return
        session = null
        progressJob?.cancel()
        val info = PlaybackStopInfo(
            itemId = current.item.id,
            mediaSourceId = current.source.id,
            playSessionId = current.playSessionId,
            positionTicks = player.currentPosition * TICKS_PER_MS,
            failed = failed,
        )
        container.appScope.launch { runCatching { client.reportPlaybackStopped(info) } }
    }

    private fun onEnded() {
        val current = session ?: return
        reportStopped()
        val item = current.item
        val seriesId = item.seriesId
        upNextJob?.cancel()
        upNextJob = viewModelScope.launch {
            val autoPlay = container.settings.preferences.first().autoPlayNext
            val next = if (item.type == "Episode" && seriesId != null && autoPlay) {
                runCatching { client.episodes(seriesId, startItemId = item.id, limit = 2) }.getOrNull()?.getOrNull(1)
            } else null
            if (next == null) {
                _ui.update { it.copy(finished = true) }
                return@launch
            }
            for (seconds in 8 downTo 1) {
                _ui.update { it.copy(upNext = next, upNextSeconds = seconds) }
                delay(1_000)
            }
            playNext(next)
        }
    }

    fun playNext(next: BaseItem? = _ui.value.upNext) {
        if (next == null) return
        upNextJob?.cancel()
        request = PlayRequest(next.id, startPositionTicks = 0)
        fallbackTried = false
        _ui.update { it.copy(upNext = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            reportStopped()
            prepare(next.id, 0, null, null)
        }
    }

    fun skipToNextEpisode() {
        val current = session ?: return
        val seriesId = current.item.seriesId ?: return
        viewModelScope.launch {
            val next = runCatching { client.episodes(seriesId, startItemId = current.item.id, limit = 2) }
                .getOrNull()?.getOrNull(1) ?: return@launch
            playNext(next)
        }
    }

    fun cancelUpNext() {
        upNextJob?.cancel()
        _ui.update { it.copy(upNext = null, finished = true) }
    }

    override fun onCleared() {
        reportStopped()
        player.removeListener(playerListener)
        player.release()
    }

    private fun playbackErrorMessage(error: PlaybackException): String = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "The connection to your server was lost."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "The server refused the stream."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "This phone cannot decode this video. Try a lower streaming quality in the settings."
        else -> "Playback stopped: ${error.errorCodeName.removePrefix("ERROR_CODE_").replace('_', ' ').lowercase()}."
    }

    private fun errorCodeMessage(code: String) = when (code) {
        "NotAllowed" -> "You are not allowed to play this title."
        "NoCompatibleStream" -> "The server cannot make a stream this phone can play."
        "RateLimitExceeded" -> "The server is busy. Try again in a moment."
        else -> "The server cannot play this title ($code)."
    }

    companion object {
        fun subtitleTrackId(index: Int) = "jf-sub-$index"

        fun subtitleMime(codec: String?): String? = when (codec?.lowercase()) {
            "srt", "subrip" -> MimeTypes.APPLICATION_SUBRIP
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "vtt", "webvtt" -> MimeTypes.TEXT_VTT
            "ttml" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }
}
