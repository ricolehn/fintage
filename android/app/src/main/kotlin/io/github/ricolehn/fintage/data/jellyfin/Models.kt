package io.github.ricolehn.fintage.data.jellyfin

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/*
 * The parts of the Jellyfin 12 API that Fintage uses. The server writes its JSON in PascalCase;
 * JellyfinJson maps it to these camelCase names, and unknown fields are ignored.
 */

@Immutable
@Serializable
data class BaseItem(
    val id: String,
    val name: String? = null,
    val originalTitle: String? = null,
    val serverId: String? = null,
    val type: String? = null,
    val collectionType: String? = null,
    val isFolder: Boolean? = null,
    val mediaType: String? = null,
    val productionYear: Int? = null,
    val premiereDate: String? = null,
    val endDate: String? = null,
    val dateCreated: String? = null,
    val officialRating: String? = null,
    val communityRating: Float? = null,
    val criticRating: Float? = null,
    val runTimeTicks: Long? = null,
    val overview: String? = null,
    val taglines: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val genreItems: List<NameId> = emptyList(),
    val studios: List<NameId> = emptyList(),
    val people: List<PersonRef> = emptyList(),
    val productionLocations: List<String> = emptyList(),
    val imageTags: Map<String, String> = emptyMap(),
    val backdropImageTags: List<String> = emptyList(),
    val imageBlurHashes: Map<String, Map<String, String>> = emptyMap(),
    val parentBackdropItemId: String? = null,
    val parentBackdropImageTags: List<String> = emptyList(),
    val parentLogoItemId: String? = null,
    val parentLogoImageTag: String? = null,
    val parentThumbItemId: String? = null,
    val parentThumbImageTag: String? = null,
    val parentPrimaryImageItemId: String? = null,
    val parentPrimaryImageTag: String? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seriesPrimaryImageTag: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val parentIndexNumber: Int? = null,
    val userData: UserData? = null,
    val childCount: Int? = null,
    val recursiveItemCount: Int? = null,
    val status: String? = null,
    val primaryImageAspectRatio: Double? = null,
    val localTrailerCount: Int? = null,
    val remoteTrailers: List<MediaUrl> = emptyList(),
    val externalUrls: List<ExternalUrl> = emptyList(),
    val mediaSources: List<MediaSource> = emptyList(),
    val mediaStreams: List<MediaStream> = emptyList(),
    val movieCount: Int? = null,
    val seriesCount: Int? = null,
    val episodeCount: Int? = null,
)

@Immutable
@Serializable
data class NameId(val name: String? = null, val id: String)

@Immutable
@Serializable
data class PersonRef(
    val id: String,
    val name: String? = null,
    val role: String? = null,
    val type: String? = null,
    val primaryImageTag: String? = null,
    val imageBlurHashes: Map<String, Map<String, String>> = emptyMap(),
)

@Immutable
@Serializable
data class UserData(
    val playbackPositionTicks: Long = 0,
    val playedPercentage: Double? = null,
    val unplayedItemCount: Int? = null,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val played: Boolean = false,
    val lastPlayedDate: String? = null,
)

@Immutable
@Serializable
data class MediaUrl(val url: String? = null, val name: String? = null)

@Immutable
@Serializable
data class ExternalUrl(val name: String? = null, val url: String? = null)

@Immutable
@Serializable
data class MediaSource(
    val id: String? = null,
    val name: String? = null,
    val path: String? = null,
    val protocol: String? = null,
    val container: String? = null,
    val size: Long? = null,
    val bitrate: Long? = null,
    val runTimeTicks: Long? = null,
    val isRemote: Boolean = false,
    val supportsDirectPlay: Boolean = false,
    val supportsDirectStream: Boolean = false,
    val supportsTranscoding: Boolean = false,
    val transcodingUrl: String? = null,
    val transcodingSubProtocol: String? = null,
    val transcodingContainer: String? = null,
    val eTag: String? = null,
    val mediaStreams: List<MediaStream> = emptyList(),
    val defaultAudioStreamIndex: Int? = null,
    val defaultSubtitleStreamIndex: Int? = null,
)

@Immutable
@Serializable
data class MediaStream(
    val index: Int,
    val type: String? = null,
    val codec: String? = null,
    val language: String? = null,
    val title: String? = null,
    val displayTitle: String? = null,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isHearingImpaired: Boolean = false,
    val isExternal: Boolean = false,
    val isTextSubtitleStream: Boolean = false,
    val supportsExternalStream: Boolean = false,
    val deliveryMethod: String? = null,
    val deliveryUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val channels: Int? = null,
    val channelLayout: String? = null,
    val bitRate: Long? = null,
    val bitDepth: Int? = null,
    val profile: String? = null,
    val videoRange: String? = null,
    val videoRangeType: String? = null,
    val averageFrameRate: Float? = null,
)

@Serializable
data class ItemsResult(
    val items: List<BaseItem> = emptyList(),
    val totalRecordCount: Int = 0,
    val startIndex: Int = 0,
)

@Serializable
data class PublicSystemInfo(
    val localAddress: String? = null,
    val serverName: String? = null,
    val version: String? = null,
    val productName: String? = null,
    val id: String? = null,
    val startupWizardCompleted: Boolean? = null,
)

@Immutable
@Serializable
data class UserDto(
    val id: String,
    val name: String? = null,
    val serverId: String? = null,
    val primaryImageTag: String? = null,
    val hasPassword: Boolean = true,
    val configuration: UserConfiguration? = null,
    val policy: UserPolicy? = null,
)

@Immutable
@Serializable
data class UserConfiguration(
    val audioLanguagePreference: String? = null,
    val subtitleLanguagePreference: String? = null,
    val subtitleMode: String? = null,
    val playDefaultAudioTrack: Boolean = true,
    val enableNextEpisodeAutoPlay: Boolean = true,
)

@Immutable
@Serializable
data class UserPolicy(val isAdministrator: Boolean = false)

@Serializable
data class AuthenticationResult(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val serverId: String? = null,
)

@Serializable
data class AuthenticateUserByName(val username: String, val pw: String)

@Serializable
data class QuickConnectDto(val secret: String)

@Serializable
data class QuickConnectResult(
    val authenticated: Boolean = false,
    val secret: String = "",
    val code: String = "",
)

@Serializable
data class QueryFilters(
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val officialRatings: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

@Serializable
data class PlaybackInfoRequest(
    val userId: String,
    val maxStreamingBitrate: Long? = null,
    val startTimeTicks: Long? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val mediaSourceId: String? = null,
    val deviceProfile: DeviceProfile,
    val enableDirectPlay: Boolean = true,
    val enableDirectStream: Boolean = true,
    val enableTranscoding: Boolean = true,
    val allowVideoStreamCopy: Boolean = true,
    val allowAudioStreamCopy: Boolean = true,
    val autoOpenLiveStream: Boolean = true,
)

@Serializable
data class PlaybackInfoResponse(
    val mediaSources: List<MediaSource> = emptyList(),
    val playSessionId: String? = null,
    val errorCode: String? = null,
)

@Serializable
data class PlaybackStartInfo(
    val itemId: String,
    val mediaSourceId: String? = null,
    val playSessionId: String? = null,
    val playMethod: String,
    val positionTicks: Long = 0,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val canSeek: Boolean = true,
)

@Serializable
data class PlaybackStopInfo(
    val itemId: String,
    val mediaSourceId: String? = null,
    val playSessionId: String? = null,
    val positionTicks: Long = 0,
    val failed: Boolean = false,
)

/* The device profile tells the server what this phone can play, so it plays files directly where it can
   and converts only what it must. */

@Serializable
data class DeviceProfile(
    val name: String,
    val maxStreamingBitrate: Long,
    val maxStaticBitrate: Long,
    val musicStreamingTranscodingBitrate: Long = 384_000,
    val directPlayProfiles: List<DirectPlayProfile>,
    val transcodingProfiles: List<TranscodingProfile>,
    val containerProfiles: List<ContainerProfile> = emptyList(),
    val codecProfiles: List<CodecProfile> = emptyList(),
    val subtitleProfiles: List<SubtitleProfile>,
)

@Serializable
data class DirectPlayProfile(
    val container: String,
    val type: String,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
)

@Serializable
data class TranscodingProfile(
    val container: String,
    val type: String,
    val videoCodec: String,
    val audioCodec: String,
    val protocol: String,
    val context: String = "Streaming",
    val maxAudioChannels: String? = null,
    val minSegments: Int = 1,
    val segmentLength: Int = 0,
    val breakOnNonKeyFrames: Boolean = true,
    val copyTimestamps: Boolean = false,
    val enableSubtitlesInManifest: Boolean = false,
    val estimateContentLength: Boolean = false,
    val enableMpegtsM2TsMode: Boolean = false,
    val transcodeSeekInfo: String = "Auto",
    val enableAudioVbrEncoding: Boolean = true,
    val conditions: List<ProfileCondition> = emptyList(),
)

@Serializable
data class ContainerProfile(
    val type: String,
    val container: String? = null,
    val conditions: List<ProfileCondition> = emptyList(),
)

@Serializable
data class CodecProfile(
    val type: String,
    val codec: String? = null,
    val container: String? = null,
    val conditions: List<ProfileCondition> = emptyList(),
    val applyConditions: List<ProfileCondition> = emptyList(),
)

@Serializable
data class ProfileCondition(
    val condition: String,
    val property: String,
    val value: String,
    val isRequired: Boolean = false,
)

@Serializable
data class SubtitleProfile(val format: String, val method: String)
