package io.github.ricolehn.fintage

import android.app.Application
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import io.github.ricolehn.fintage.data.Images
import io.github.ricolehn.fintage.data.MediaRepository
import io.github.ricolehn.fintage.data.SessionManager
import io.github.ricolehn.fintage.data.SettingsStore
import io.github.ricolehn.fintage.data.jellyfin.ClientInfo
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

/** Creates the few long-lived objects of the app and hands them to the screens. */
class AppContainer(context: Context, val appScope: CoroutineScope) {
    val settings = SettingsStore(context)

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val client = JellyfinClient(
        http = okHttp,
        clientInfo = ClientInfo(
            clientName = "Fintage",
            clientVersion = BuildConfig.VERSION_NAME,
            deviceName = deviceName(),
            deviceId = settings.deviceId,
        ),
    )

    val images = Images(client)
    val repository = MediaRepository(client)
    val sessions = SessionManager(settings, client, appScope, onSessionChanged = { repository.clearCache() })

    private fun deviceName(): String {
        val model = Build.MODEL.orEmpty()
        val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        return if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model".trim()
    }
}

class FintageApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.okHttp })) }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}

val Context.appContainer: AppContainer get() = (applicationContext as FintageApp).container
