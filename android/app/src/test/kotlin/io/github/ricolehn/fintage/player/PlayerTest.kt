package io.github.ricolehn.fintage.player

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ricolehn.fintage.FintageApp
import io.github.ricolehn.fintage.data.jellyfin.ItemQuery
import io.github.ricolehn.fintage.ui.PlayRequest
import io.github.ricolehn.fintage.ui.theme.FintageTheme
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Prepares playback against a real Jellyfin server: the decision between playing the file directly and
 * a conversion, the side-loaded subtitles, and switching to an audio track the phone cannot decode.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w915dp-h412dp-xhdpi")
class PlayerTest {
    @get:Rule
    val compose = createComposeRule()

    private val server = System.getenv("FINTAGE_TEST_SERVER") ?: "http://127.0.0.1:8096"
    private val app get() = RuntimeEnvironment.getApplication() as FintageApp

    @Before
    fun setUp() {
        val reachable = runCatching {
            (URL("$server/System/Info/Public").openConnection() as HttpURLConnection).run {
                connectTimeout = 1500
                responseCode == 200
            }
        }.getOrDefault(false)
        assumeTrue(reachable)
        android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
            .copyPixelsToBuffer(java.nio.ByteBuffer.allocate(16))
        pump(5)
        val container = app.container
        if (container.sessions.currentSession == null) {
            runBlocking {
                val (url, info) = container.client.findServer(server)
                container.client.configure(url, null, null)
                container.sessions.signIn(url, info, container.client.authenticateByName("demo", "demo"))
            }
        }
        pump(5)
    }

    private fun pump(times: Int = 1) = repeat(times) {
        Thread.sleep(50)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun waitUntil(timeoutMillis: Long = 20_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            pump()
            if (condition()) return
        }
        error("Timed out")
    }

    @Test
    fun preparesDirectPlayAndSwitchesToAConversionForAudioThePhoneCannotDecode() {
        val itemId = runBlocking {
            app.container.client.items(ItemQuery(searchTerm = "Glass Harbor", includeItemTypes = listOf("Movie"))).items.first().id
        }
        val viewModel = PlayerViewModel(app)
        viewModel.start(PlayRequest(itemId, startPositionTicks = 0))
        waitUntil { viewModel.ui.value.session != null || viewModel.ui.value.error != null }
        val session = viewModel.ui.value.session
        assertNotNull("error: ${viewModel.ui.value.error}", session)
        assertEquals("DirectPlay", session!!.playMethod)
        assertEquals(1, session.audioIndex)
        val mediaItem = viewModel.player.currentMediaItem
        assertNotNull(mediaItem)
        assertTrue(mediaItem!!.localConfiguration!!.uri.toString().contains("/Videos/$itemId/stream"))
        assertEquals(2, mediaItem.localConfiguration!!.subtitleConfigurations.size)

        // a fresh start with the German AC3 track, which this test phone cannot decode: the server converts it
        viewModel.start(PlayRequest(itemId, startPositionTicks = 0, audioStreamIndex = 2))
        waitUntil { viewModel.ui.value.session?.audioIndex == 2 }
        val converted = viewModel.ui.value.session!!
        assertEquals("Transcode", converted.playMethod)
        assertTrue(viewModel.player.currentMediaItem!!.localConfiguration!!.uri.toString().contains("master.m3u8"))
        assertTrue(viewModel.player.currentMediaItem!!.localConfiguration!!.uri.toString().contains("AudioStreamIndex=2"))

        // the screen of the player: controls over the video, here over the loading state
        viewModel.player.stop()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            FintageTheme(darkTheme = true) {
                PlayerScreen(viewModel, inPictureInPicture = false, canPictureInPicture = true, onEnterPictureInPicture = {}, onClose = {})
            }
        }
        repeat(6) {
            compose.mainClock.advanceTimeBy(100)
        }
        val dir = File(System.getProperty("roborazzi.output.dir") ?: "build/screenshots").apply { mkdirs() }
        compose.onRoot().captureRoboImage(File(dir, "19-player.png").path)
        viewModel.player.release()
    }
}
