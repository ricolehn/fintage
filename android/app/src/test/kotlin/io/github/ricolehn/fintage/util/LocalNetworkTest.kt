package io.github.ricolehn.fintage.util

import java.net.InetAddress
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {
    private fun local(address: String) = LocalNetwork.isLocal(InetAddress.getByName(address))

    @Test
    fun homeNetworkAddressesAreLocal() {
        assertTrue(local("192.168.1.20"))
        assertTrue(local("10.0.0.5"))
        assertTrue(local("172.20.3.4"))
        assertTrue(local("169.254.10.1"))
        assertTrue(local("fe80::1"))
        assertTrue(local("fd12:3456:789a::1"))
    }

    @Test
    fun internetAndLoopbackAddressesAreNotLocal() {
        assertFalse(local("8.8.8.8"))
        assertFalse(local("172.32.0.1"))
        assertFalse(local("2a00:1450:4001:80b::200e"))
        assertFalse(local("127.0.0.1"))
    }

    @Test
    fun serverAddressesAsTyped() = runBlocking {
        assertEquals("192.168.1.20", LocalNetwork.hostOf("192.168.1.20:8096"))
        assertEquals("media.example.com", LocalNetwork.hostOf("https://media.example.com/jellyfin"))
        assertTrue(LocalNetwork.isLocalServer("192.168.1.20:8096"))
        assertTrue(LocalNetwork.isLocalServer("http://jellyfin.local:8096"))
        assertFalse(LocalNetwork.isLocalServer("http://127.0.0.1:8096"))
        assertFalse(LocalNetwork.isLocalServer(""))
    }
}
