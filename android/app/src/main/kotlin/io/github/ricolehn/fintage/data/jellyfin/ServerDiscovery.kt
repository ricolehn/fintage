package io.github.ricolehn.fintage.data.jellyfin

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class DiscoveredServer(
    val address: String = "",
    val id: String = "",
    val name: String = "",
    val endpointAddress: String? = null,
)

/** Finds Jellyfin servers on the home network: they answer a broadcast on UDP port 7359. */
object ServerDiscovery {
    suspend fun discover(timeoutMillis: Int = 2500): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, DiscoveredServer>()
        runCatching {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.soTimeout = 400
                val message = "who is JellyfinServer?".toByteArray()
                socket.send(DatagramPacket(message, message.size, InetAddress.getByName("255.255.255.255"), 7359))
                val buffer = ByteArray(4096)
                val end = System.currentTimeMillis() + timeoutMillis
                while (isActive && System.currentTimeMillis() < end) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length)
                        val server = runCatching { JellyfinJson.decodeFromString(DiscoveredServer.serializer(), text) }
                            .getOrNull()
                        if (server != null && server.address.isNotEmpty()) found[server.id.ifEmpty { server.address }] = server
                    } catch (_: SocketTimeoutException) {
                        // keep listening until the time is up
                    }
                }
            }
        }
        found.values.toList()
    }
}
