package io.github.ricolehn.fintage.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * From Android 17 on, an app that targets it may only reach devices on the home network, like a Jellyfin
 * server at 192.168.1.20, after the user allowed it (the permission is part of Nearby devices). Without it,
 * connections to such addresses time out. Servers on the internet need no permission.
 */
object LocalNetwork {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    val isEnforced: Boolean get() = Build.VERSION.SDK_INT >= 37

    fun isGranted(context: Context): Boolean =
        !isEnforced || ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Whether Android would show the request again, or the user has to allow it in the settings. */
    fun canAskAgain(context: Context): Boolean {
        val activity = context.findActivity() ?: return true
        return activity.shouldShowRequestPermissionRationale(PERMISSION)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Whether the server is on the home network; a name is looked up first, which needs no permission. */
    suspend fun isLocalServer(address: String): Boolean = withContext(Dispatchers.IO) {
        val host = hostOf(address) ?: return@withContext false
        if (host.endsWith(".local", ignoreCase = true) || host.endsWith(".lan", ignoreCase = true)) return@withContext true
        runCatching { InetAddress.getAllByName(host).any(::isLocal) }.getOrDefault(false)
    }

    fun isLocal(address: InetAddress): Boolean = when (address) {
        is Inet4Address -> address.isSiteLocalAddress || address.isLinkLocalAddress
        is Inet6Address -> address.isLinkLocalAddress || address.isSiteLocalAddress ||
            (address.address[0].toInt() and 0xFE) == 0xFC
        else -> false
    }

    fun hostOf(address: String): String? {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return null
        val url = if ("://" in trimmed) trimmed else "http://$trimmed"
        return url.toHttpUrlOrNull()?.host
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
