package io.github.ricolehn.fintage.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Dark, Light }

data class Session(
    val serverUrl: String,
    val serverName: String,
    val serverId: String?,
    val userId: String,
    val userName: String,
    val userImageTag: String?,
    val accessToken: String,
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.Dark,
    val filmGrain: Boolean = true,
    /** 0 picks the best quality the connection allows. */
    val maxBitrate: Long = 0,
    val autoPlayNext: Boolean = true,
    val recentSearches: List<String> = emptyList(),
)

private val Context.fintageStore by preferencesDataStore(name = "fintage")

class SettingsStore(private val context: Context) {
    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val serverName = stringPreferencesKey("server_name")
        val serverId = stringPreferencesKey("server_id")
        val userId = stringPreferencesKey("user_id")
        val userName = stringPreferencesKey("user_name")
        val userImageTag = stringPreferencesKey("user_image_tag")
        val accessToken = stringPreferencesKey("access_token")
        val lastServer = stringPreferencesKey("last_server")
        val themeMode = stringPreferencesKey("theme_mode")
        val filmGrain = booleanPreferencesKey("film_grain")
        val maxBitrate = longPreferencesKey("max_bitrate")
        val autoPlayNext = booleanPreferencesKey("auto_play_next")
        val recentSearches = stringPreferencesKey("recent_searches")
    }

    /** A stable id for this installation; Jellyfin keeps one session per device id. */
    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("fintage_device", Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().replace("-", "").also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    val session: Flow<Session?> = context.fintageStore.data.map { it.toSession() }.distinctUntilChanged()

    val lastServer: Flow<String?> = context.fintageStore.data.map { it[Keys.lastServer] }

    val preferences: Flow<AppPreferences> = context.fintageStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.Dark,
            filmGrain = prefs[Keys.filmGrain] ?: true,
            maxBitrate = prefs[Keys.maxBitrate] ?: 0L,
            autoPlayNext = prefs[Keys.autoPlayNext] ?: true,
            recentSearches = prefs[Keys.recentSearches]?.split('\n')?.filter { it.isNotBlank() }.orEmpty(),
        )
    }.distinctUntilChanged()

    private fun Preferences.toSession(): Session? {
        val url = this[Keys.serverUrl] ?: return null
        val token = this[Keys.accessToken] ?: return null
        val userId = this[Keys.userId] ?: return null
        return Session(
            serverUrl = url,
            serverName = this[Keys.serverName] ?: "Jellyfin",
            serverId = this[Keys.serverId],
            userId = userId,
            userName = this[Keys.userName] ?: "",
            userImageTag = this[Keys.userImageTag],
            accessToken = token,
        )
    }

    suspend fun saveSession(session: Session) {
        context.fintageStore.edit { prefs ->
            prefs[Keys.serverUrl] = session.serverUrl
            prefs[Keys.serverName] = session.serverName
            session.serverId?.let { prefs[Keys.serverId] = it } ?: prefs.remove(Keys.serverId)
            prefs[Keys.userId] = session.userId
            prefs[Keys.userName] = session.userName
            session.userImageTag?.let { prefs[Keys.userImageTag] = it } ?: prefs.remove(Keys.userImageTag)
            prefs[Keys.accessToken] = session.accessToken
            prefs[Keys.lastServer] = session.serverUrl
        }
    }

    suspend fun clearSession() {
        context.fintageStore.edit { prefs ->
            prefs.remove(Keys.accessToken)
            prefs.remove(Keys.userId)
            prefs.remove(Keys.userName)
            prefs.remove(Keys.userImageTag)
            prefs.remove(Keys.serverUrl)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) = context.fintageStore.edit { it[Keys.themeMode] = mode.name }

    suspend fun setFilmGrain(enabled: Boolean) = context.fintageStore.edit { it[Keys.filmGrain] = enabled }

    suspend fun setMaxBitrate(bitrate: Long) = context.fintageStore.edit { it[Keys.maxBitrate] = bitrate }

    suspend fun setAutoPlayNext(enabled: Boolean) = context.fintageStore.edit { it[Keys.autoPlayNext] = enabled }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        context.fintageStore.edit { prefs ->
            val current = prefs[Keys.recentSearches]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            prefs[Keys.recentSearches] = updated.joinToString("\n")
        }
    }

    suspend fun clearRecentSearches() = context.fintageStore.edit { it.remove(Keys.recentSearches) }
}
