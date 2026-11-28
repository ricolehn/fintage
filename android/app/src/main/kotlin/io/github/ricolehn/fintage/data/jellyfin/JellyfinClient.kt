package io.github.ricolehn.fintage.data.jellyfin

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/** Jellyfin writes PascalCase JSON; this maps it to camelCase Kotlin names. */
@OptIn(ExperimentalSerializationApi::class)
private object PascalCase : JsonNamingStrategy {
    override fun serialNameForJson(descriptor: SerialDescriptor, elementIndex: Int, serialName: String): String =
        serialName.replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalSerializationApi::class)
val JellyfinJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
    isLenient = true
    namingStrategy = PascalCase
}

/** Who this app is, sent with every request. */
data class ClientInfo(
    val clientName: String,
    val clientVersion: String,
    val deviceName: String,
    val deviceId: String,
)

open class JellyfinException(val code: Int, message: String) : IOException(message)

class UnauthorizedException : JellyfinException(401, "Your session has ended. Please sign in again.")

object ItemFields {
    const val CARD = "PrimaryImageAspectRatio,Genres"
    const val SPOTLIGHT = "PrimaryImageAspectRatio,Genres,Overview,Taglines"
    const val DETAIL =
        "Overview,Taglines,Genres,GenreItems,People,Studios,MediaSources,MediaStreams,ExternalUrls," +
            "RemoteTrailers,LocalTrailerCount,ProviderIds,ChildCount,RecursiveItemCount," +
            "PrimaryImageAspectRatio,ProductionLocations,OriginalTitle"
    const val EPISODE_LIST = "Overview,PrimaryImageAspectRatio,MediaStreams"
}

/** The filters of an /Items query. Lists of ids are separated by commas, as Jellyfin 12 expects. */
data class ItemQuery(
    val parentId: String? = null,
    val includeItemTypes: List<String> = emptyList(),
    val excludeItemTypes: List<String> = emptyList(),
    val recursive: Boolean = true,
    val genreIds: List<String> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val personIds: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val ids: List<String> = emptyList(),
    val excludeItemIds: List<String> = emptyList(),
    val searchTerm: String? = null,
    val sortBy: String = "SortName",
    val sortOrder: String = "Ascending",
    val filters: List<String> = emptyList(),
    val isFavorite: Boolean? = null,
    val isPlayed: Boolean? = null,
    val imageTypes: List<String> = emptyList(),
    val fields: String = ItemFields.CARD,
    val startIndex: Int = 0,
    val limit: Int? = 60,
    val enableTotalRecordCount: Boolean = true,
    val minCommunityRating: Double? = null,
)

class JellyfinClient(
    private val http: OkHttpClient,
    val clientInfo: ClientInfo,
) {
    @Volatile var baseUrl: String? = null
        private set
    @Volatile var accessToken: String? = null
        private set
    @Volatile var userId: String? = null
        private set

    private val unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the server rejects the saved token, so the app can return to the sign-in screen. */
    val unauthorizedEvents: SharedFlow<Unit> = unauthorized.asSharedFlow()

    private val probeClient: OkHttpClient by lazy {
        http.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun configure(baseUrl: String?, accessToken: String?, userId: String?) {
        this.baseUrl = baseUrl?.trimEnd('/')
        this.accessToken = accessToken
        this.userId = userId
    }

    fun authorizationHeader(token: String? = accessToken): String {
        val parts = buildList {
            add("Client=\"${clientInfo.clientName.headerSafe()}\"")
            add("Device=\"${clientInfo.deviceName.headerSafe()}\"")
            add("DeviceId=\"${clientInfo.deviceId.headerSafe()}\"")
            add("Version=\"${clientInfo.clientVersion.headerSafe()}\"")
            if (!token.isNullOrEmpty()) add("Token=\"${token.headerSafe()}\"")
        }
        return "MediaBrowser " + parts.joinToString(", ")
    }

    // region server and sign in

    /** Tries the address as typed and the usual Jellyfin ports, and returns the first server that answers. */
    suspend fun findServer(input: String): Pair<String, PublicSystemInfo> {
        val candidates = addressCandidates(input)
        if (candidates.isEmpty()) {
            throw JellyfinException(0, if (input.isBlank()) "Enter the address of your server." else "This is not a valid server address.")
        }
        var lastError: Throwable? = null
        for (candidate in candidates) {
            try {
                val info = publicInfo(candidate)
                if (info.id != null) return candidate to info
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                lastError = e
            }
        }
        throw JellyfinException(0, "No Jellyfin server answered at this address." +
            (lastError?.message?.let { " ($it)" } ?: ""))
    }

    suspend fun publicInfo(server: String): PublicSystemInfo {
        val url = "${server.trimEnd('/')}/System/Info/Public".toHttpUrl()
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        return decode(PublicSystemInfo.serializer(), execute(request, probeClient))
    }

    suspend fun publicUsers(): List<UserDto> =
        get("/Users/Public", emptyMap(), ListSerializer(UserDto.serializer()), authenticated = false)

    suspend fun authenticateByName(username: String, password: String): AuthenticationResult =
        post(
            "/Users/AuthenticateByName",
            emptyMap(),
            AuthenticateUserByName.serializer(),
            AuthenticateUserByName(username, password),
            AuthenticationResult.serializer(),
            authenticated = false,
        )

    suspend fun quickConnectEnabled(): Boolean =
        runCatching { get("/QuickConnect/Enabled", emptyMap(), Boolean.serializer(), authenticated = false) }
            .getOrDefault(false)

    suspend fun quickConnectInitiate(): QuickConnectResult =
        post("/QuickConnect/Initiate", emptyMap(), null, null, QuickConnectResult.serializer(), authenticated = false)

    suspend fun quickConnectState(secret: String): QuickConnectResult =
        get("/QuickConnect/Connect", mapOf("secret" to secret), QuickConnectResult.serializer(), authenticated = false)

    suspend fun authenticateWithQuickConnect(secret: String): AuthenticationResult =
        post(
            "/Users/AuthenticateWithQuickConnect",
            emptyMap(),
            QuickConnectDto.serializer(),
            QuickConnectDto(secret),
            AuthenticationResult.serializer(),
            authenticated = false,
        )

    suspend fun currentUser(): UserDto = get("/Users/Me", emptyMap(), UserDto.serializer())

    suspend fun logout() {
        runCatching { send("POST", "/Sessions/Logout", emptyMap(), null) }
    }

    // endregion

    // region library

    suspend fun userViews(): List<BaseItem> =
        get("/UserViews", mapOf("userId" to requireUser()), ItemsResult.serializer()).items

    suspend fun items(query: ItemQuery): ItemsResult =
        get("/Items", query.toParameters(requireUser()), ItemsResult.serializer())

    suspend fun item(id: String, fields: String = ItemFields.DETAIL): BaseItem =
        get("/Items/$id", mapOf("userId" to requireUser(), "fields" to fields), BaseItem.serializer())

    suspend fun resume(limit: Int = 16): List<BaseItem> =
        get(
            "/UserItems/Resume",
            mapOf(
                "userId" to requireUser(),
                "limit" to limit,
                "mediaTypes" to "Video",
                "fields" to ItemFields.CARD,
                "enableImageTypes" to "Primary,Backdrop,Thumb,Logo",
                "imageTypeLimit" to 1,
                "enableTotalRecordCount" to false,
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun nextUp(limit: Int = 16, seriesId: String? = null): List<BaseItem> =
        get(
            "/Shows/NextUp",
            mapOf(
                "userId" to requireUser(),
                "limit" to limit,
                "seriesId" to seriesId,
                "fields" to ItemFields.CARD + ",Overview",
                "enableImageTypes" to "Primary,Backdrop,Thumb,Logo",
                "imageTypeLimit" to 1,
                "enableTotalRecordCount" to false,
                "enableResumable" to false,
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun latest(parentId: String?, limit: Int = 16, includeItemTypes: String? = null): List<BaseItem> =
        get(
            "/Items/Latest",
            mapOf(
                "userId" to requireUser(),
                "parentId" to parentId,
                "limit" to limit,
                "includeItemTypes" to includeItemTypes,
                "fields" to ItemFields.CARD,
                "enableImageTypes" to "Primary,Backdrop,Thumb,Logo",
                "imageTypeLimit" to 1,
                "groupItems" to true,
            ),
            ListSerializer(BaseItem.serializer()),
        )

    suspend fun similar(id: String, limit: Int = 16): List<BaseItem> =
        get(
            "/Items/$id/Similar",
            mapOf("userId" to requireUser(), "limit" to limit, "fields" to ItemFields.CARD),
            ItemsResult.serializer(),
        ).items

    suspend fun seasons(seriesId: String): List<BaseItem> =
        get(
            "/Shows/$seriesId/Seasons",
            mapOf(
                "userId" to requireUser(),
                "fields" to "PrimaryImageAspectRatio,ChildCount,Overview",
                "enableImageTypes" to "Primary,Backdrop,Thumb",
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun episodes(seriesId: String, seasonId: String? = null, startItemId: String? = null, limit: Int? = null): List<BaseItem> =
        get(
            "/Shows/$seriesId/Episodes",
            mapOf(
                "userId" to requireUser(),
                "seasonId" to seasonId,
                "startItemId" to startItemId,
                "limit" to limit,
                "fields" to ItemFields.EPISODE_LIST,
                "enableImageTypes" to "Primary,Backdrop,Thumb",
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun genres(includeItemTypes: String = "Movie,Series", parentId: String? = null): List<BaseItem> =
        get(
            "/Genres",
            mapOf(
                "userId" to requireUser(),
                "parentId" to parentId,
                "includeItemTypes" to includeItemTypes,
                "fields" to "ItemCounts",
                "sortBy" to "SortName",
                "enableImages" to true,
                "enableTotalRecordCount" to false,
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun studios(includeItemTypes: String = "Movie,Series"): List<BaseItem> =
        get(
            "/Studios",
            mapOf(
                "userId" to requireUser(),
                "includeItemTypes" to includeItemTypes,
                "fields" to "ItemCounts",
                "enableImages" to true,
                "enableTotalRecordCount" to false,
            ),
            ItemsResult.serializer(),
        ).items

    suspend fun persons(searchTerm: String, limit: Int = 20): List<BaseItem> =
        get(
            "/Persons",
            mapOf("userId" to requireUser(), "searchTerm" to searchTerm, "limit" to limit, "enableImages" to true),
            ItemsResult.serializer(),
        ).items

    suspend fun filters(parentId: String, includeItemTypes: String? = null): QueryFilters =
        get(
            "/Items/Filters",
            mapOf("userId" to requireUser(), "parentId" to parentId, "includeItemTypes" to includeItemTypes),
            QueryFilters.serializer(),
        )

    suspend fun localTrailers(id: String): List<BaseItem> =
        get("/Items/$id/LocalTrailers", mapOf("userId" to requireUser()), ListSerializer(BaseItem.serializer()))

    suspend fun setFavorite(id: String, favorite: Boolean): UserData =
        decode(
            UserData.serializer(),
            send(if (favorite) "POST" else "DELETE", "/UserFavoriteItems/$id", mapOf("userId" to requireUser()), null),
        )

    suspend fun setPlayed(id: String, played: Boolean): UserData =
        decode(
            UserData.serializer(),
            send(if (played) "POST" else "DELETE", "/UserPlayedItems/$id", mapOf("userId" to requireUser()), null),
        )

    // endregion

    // region playback

    suspend fun playbackInfo(itemId: String, request: PlaybackInfoRequest): PlaybackInfoResponse =
        post(
            "/Items/$itemId/PlaybackInfo",
            mapOf("userId" to requireUser()),
            PlaybackInfoRequest.serializer(),
            request,
            PlaybackInfoResponse.serializer(),
        )

    suspend fun reportPlaybackStart(info: PlaybackStartInfo) {
        send("POST", "/Sessions/Playing", emptyMap(), JellyfinJson.encodeToString(PlaybackStartInfo.serializer(), info))
    }

    suspend fun reportPlaybackProgress(info: PlaybackStartInfo) {
        send(
            "POST",
            "/Sessions/Playing/Progress",
            emptyMap(),
            JellyfinJson.encodeToString(PlaybackStartInfo.serializer(), info),
        )
    }

    suspend fun reportPlaybackStopped(info: PlaybackStopInfo) {
        send(
            "POST",
            "/Sessions/Playing/Stopped",
            emptyMap(),
            JellyfinJson.encodeToString(PlaybackStopInfo.serializer(), info),
        )
    }

    fun directStreamUrl(itemId: String, mediaSourceId: String?, container: String?, playSessionId: String?): String {
        val url = requireBase().toHttpUrl().newBuilder()
            .addPathSegments("Videos/$itemId/stream")
            .addQueryParameter("static", "true")
            .apply {
                mediaSourceId?.let { addQueryParameter("mediaSourceId", it) }
                container?.let { addQueryParameter("container", it) }
                playSessionId?.let { addQueryParameter("playSessionId", it) }
                addQueryParameter("deviceId", clientInfo.deviceId)
                accessToken?.let { addQueryParameter("ApiKey", it) }
            }
        return url.build().toString()
    }

    /** Turns a path the server returned, like a transcoding or subtitle URL, into a full URL. */
    fun absoluteUrl(pathOrUrl: String): String =
        if (pathOrUrl.startsWith("http://", true) || pathOrUrl.startsWith("https://", true)) pathOrUrl
        else requireBase() + (if (pathOrUrl.startsWith("/")) pathOrUrl else "/$pathOrUrl")

    // endregion

    // region images

    fun imageUrl(itemId: String, type: String, tag: String?, maxWidth: Int, index: Int? = null, quality: Int = 90): String {
        val base = baseUrl ?: return ""
        val sb = StringBuilder(base).append("/Items/").append(itemId).append("/Images/").append(type)
        if (index != null) sb.append('/').append(index)
        sb.append("?maxWidth=").append(maxWidth).append("&quality=").append(quality)
        if (tag != null) sb.append("&tag=").append(tag)
        return sb.toString()
    }

    fun userImageUrl(userId: String, tag: String?, maxWidth: Int = 160): String? {
        val base = baseUrl ?: return null
        if (tag == null) return null
        return "$base/UserImage?userId=$userId&tag=$tag&maxWidth=$maxWidth&quality=90"
    }

    // endregion

    // region http

    private fun requireBase(): String = baseUrl ?: throw JellyfinException(0, "No server is selected.")

    private fun requireUser(): String = userId ?: throw UnauthorizedException()

    private fun buildUrl(path: String, query: Map<String, Any?>): HttpUrl {
        val builder = (requireBase() + path).toHttpUrl().newBuilder()
        for ((key, value) in query) {
            if (value != null) builder.addQueryParameter(key, value.toString())
        }
        return builder.build()
    }

    private suspend fun <T> get(
        path: String,
        query: Map<String, Any?>,
        deserializer: KSerializer<T>,
        authenticated: Boolean = true,
    ): T {
        val request = Request.Builder()
            .url(buildUrl(path, query))
            .header("Authorization", authorizationHeader(if (authenticated) accessToken else null))
            .header("Accept", "application/json")
            .get()
            .build()
        return decode(deserializer, execute(request, http, authenticated))
    }

    private suspend fun <B, T> post(
        path: String,
        query: Map<String, Any?>,
        bodySerializer: KSerializer<B>?,
        body: B?,
        deserializer: KSerializer<T>,
        authenticated: Boolean = true,
    ): T {
        val json = if (bodySerializer != null && body != null) JellyfinJson.encodeToString(bodySerializer, body) else ""
        val request = Request.Builder()
            .url(buildUrl(path, query))
            .header("Authorization", authorizationHeader(if (authenticated) accessToken else null))
            .header("Accept", "application/json")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return decode(deserializer, execute(request, http, authenticated))
    }

    private suspend fun send(method: String, path: String, query: Map<String, Any?>, jsonBody: String?): String {
        val body = when {
            jsonBody != null -> jsonBody.toRequestBody(JSON_MEDIA_TYPE)
            method == "POST" -> "".toRequestBody(JSON_MEDIA_TYPE)
            else -> null
        }
        val request = Request.Builder()
            .url(buildUrl(path, query))
            .header("Authorization", authorizationHeader())
            .header("Accept", "application/json")
            .method(method, body)
            .build()
        return execute(request, http, true)
    }

    private fun <T> decode(deserializer: KSerializer<T>, text: String): T =
        try {
            JellyfinJson.decodeFromString(deserializer, text.ifEmpty { "{}" })
        } catch (e: Exception) {
            throw JellyfinException(0, "The server sent an answer Fintage does not understand.")
        }

    private suspend fun execute(request: Request, client: OkHttpClient, authenticated: Boolean = false): String =
        withContext(Dispatchers.IO) {
            client.newCall(request).await().use { response ->
                val text = response.body.string()
                when {
                    response.isSuccessful -> text
                    response.code == 401 && authenticated -> {
                        unauthorized.tryEmit(Unit)
                        throw UnauthorizedException()
                    }
                    response.code == 401 -> throw JellyfinException(401, "The user name or password is wrong.")
                    else -> throw JellyfinException(
                        response.code,
                        "The server answered with an error (${response.code}).",
                    )
                }
            }
        }

    // endregion

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private fun String.headerSafe(): String =
            buildString { for (c in this@headerSafe) append(if (c.code in 32..126 && c != '"' && c != ',') c else '_') }

        /** The address as typed, and with the usual Jellyfin ports if none was given. */
        fun addressCandidates(input: String): List<String> {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return emptyList()
            val hasScheme = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
            val withoutScheme = trimmed.substringAfter("://")
            val hostPart = withoutScheme.substringBefore('/')
            val hasPort = Regex(":\\d+$").containsMatchIn(hostPart) && !hostPart.endsWith("]")
            val path = withoutScheme.removePrefix(hostPart)
            val result = LinkedHashSet<String>()
            if (hasScheme) {
                result += trimmed
                if (!hasPort) {
                    val scheme = trimmed.substringBefore("://").lowercase()
                    result += if (scheme == "http") "http://$hostPart:8096$path" else "https://$hostPart:8920$path"
                }
            } else if (hasPort) {
                result += "https://$trimmed"
                result += "http://$trimmed"
            } else {
                result += "https://$trimmed"
                result += "http://$hostPart:8096$path"
                result += "http://$trimmed"
                result += "https://$hostPart:8920$path"
            }
            return result.filter { it.toHttpUrlOrNull() != null }
        }
    }
}

private fun ItemQuery.toParameters(userId: String): Map<String, Any?> = buildMap {
    put("userId", userId)
    put("parentId", parentId)
    if (includeItemTypes.isNotEmpty()) put("includeItemTypes", includeItemTypes.joinToString(","))
    if (excludeItemTypes.isNotEmpty()) put("excludeItemTypes", excludeItemTypes.joinToString(","))
    put("recursive", recursive)
    if (genreIds.isNotEmpty()) put("genreIds", genreIds.joinToString(","))
    if (studioIds.isNotEmpty()) put("studioIds", studioIds.joinToString(","))
    if (personIds.isNotEmpty()) put("personIds", personIds.joinToString(","))
    if (years.isNotEmpty()) put("years", years.joinToString(","))
    if (ids.isNotEmpty()) put("ids", ids.joinToString(","))
    if (excludeItemIds.isNotEmpty()) put("excludeItemIds", excludeItemIds.joinToString(","))
    if (!searchTerm.isNullOrBlank()) put("searchTerm", searchTerm.trim())
    put("sortBy", sortBy)
    put("sortOrder", sortOrder)
    if (filters.isNotEmpty()) put("filters", filters.joinToString(","))
    put("isFavorite", isFavorite)
    put("isPlayed", isPlayed)
    if (imageTypes.isNotEmpty()) put("imageTypes", imageTypes.joinToString(","))
    put("fields", fields)
    put("enableImageTypes", "Primary,Backdrop,Logo,Thumb")
    put("imageTypeLimit", 1)
    put("startIndex", startIndex)
    put("limit", limit)
    put("enableTotalRecordCount", enableTotalRecordCount)
    put("minCommunityRating", minCommunityRating)
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!continuation.isCancelled) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}
