package io.github.ricolehn.fintage.ui

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeUp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.ricolehn.fintage.AppContainer
import io.github.ricolehn.fintage.FintageApp
import io.github.ricolehn.fintage.data.BrowseSpec
import io.github.ricolehn.fintage.data.GenreArea
import io.github.ricolehn.fintage.data.jellyfin.ItemQuery
import io.github.ricolehn.fintage.ui.login.LoginScreen
import io.github.ricolehn.fintage.ui.navigation.AppNavigator
import io.github.ricolehn.fintage.ui.navigation.BrowseRoute
import io.github.ricolehn.fintage.ui.navigation.DetailRoute
import io.github.ricolehn.fintage.ui.navigation.MainScaffold
import io.github.ricolehn.fintage.ui.navigation.PersonRoute
import io.github.ricolehn.fintage.ui.navigation.SettingsRoute
import io.github.ricolehn.fintage.ui.navigation.Tab
import io.github.ricolehn.fintage.ui.theme.FintageTheme
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
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
 * Renders the real screens of the app against a real Jellyfin server and saves them as images, so the
 * look can be checked without a phone. Skipped unless a server with the user demo / demo answers at
 * FINTAGE_TEST_SERVER (default http://127.0.0.1:8096).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w412dp-h915dp-xhdpi")
class AppScreenshots {
    @get:Rule
    val compose = createComposeRule()

    private val server = System.getenv("FINTAGE_TEST_SERVER") ?: "http://127.0.0.1:8096"
    private val outputDir = File(System.getProperty("roborazzi.output.dir") ?: "build/screenshots")
    private val container: AppContainer get() = (RuntimeEnvironment.getApplication() as FintageApp).container

    private fun serverReachable() = runCatching {
        val connection = URL("$server/System/Info/Public").openConnection() as HttpURLConnection
        connection.connectTimeout = 1500
        connection.readTimeout = 3000
        connection.responseCode == 200
    }.getOrDefault(false)

    @Before
    fun setUp() {
        assumeTrue("No test server at $server", serverReachable())
        warmUpNativeGraphics()
        compose.mainClock.autoAdvance = false
    }

    /**
     * The native graphics of Robolectric look up their Java classes the first time they are used; when that
     * happens on a background thread (an image decoding there), the lookup fails and the process aborts.
     * Using them once on the main thread first avoids it.
     */
    private fun warmUpNativeGraphics() {
        val bitmap = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsToBuffer(java.nio.ByteBuffer.allocate(4 * 4 * 4))
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
        android.graphics.Canvas(bitmap).apply {
            concat(android.graphics.Matrix().apply { setScale(2f, 2f) })
            drawColor(android.graphics.Color.BLACK)
        }
        val matrix = android.graphics.Matrix()
        matrix.getValues(FloatArray(9))
    }

    private fun signIn() {
        pump(5)
        val c = container
        if (c.sessions.currentSession != null) return
        runBlocking {
            val (url, info) = c.client.findServer(server)
            c.client.configure(url, null, null)
            val auth = c.client.authenticateByName("demo", "demo")
            c.sessions.signIn(url, info, auth)
        }
        pump(5)
    }

    private fun show(dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            FintageTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalImages provides container.images,
                ) { content() }
            }
        }
    }

    /** Lets the main thread run what the network and image threads posted, and moves the clock on. */
    private fun pump(times: Int = 1) {
        repeat(times) {
            Thread.sleep(60)
            shadowOf(Looper.getMainLooper()).idle()
            compose.mainClock.advanceTimeBy(100)
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 30_000) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            pump()
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
        }
        capture("timeout-" + text.filter { it.isLetterOrDigit() })
        println(compose.onRoot().printToString(maxDepth = 30))
        error("Timed out waiting for \"$text\"")
    }

    /** Waits for the images: they load on other threads and arrive with a crossfade. */
    private fun settle() = pump(45)

    private fun capture(name: String) {
        outputDir.mkdirs()
        compose.onRoot().captureRoboImage(File(outputDir, "$name.png").path)
    }

    private fun scroll(times: Int = 1) {
        repeat(times) {
            compose.onRoot().performTouchInput { swipeUp(startY = bottom * 0.82f, endY = top + bottom * 0.18f, durationMillis = 400) }
            pump(12)
        }
    }

    private fun idOf(name: String, type: String): String = runBlocking {
        container.client.items(ItemQuery(searchTerm = name, includeItemTypes = listOf(type))).items.first().id
    }

    @Test
    fun login() {
        val c = container
        pump(5)
        if (c.sessions.currentSession != null) c.sessions.signOut()
        pump(5)
        show { LoginScreen(message = null) }
        waitForText("Connect to your server")
        settle()
        capture("00-login-server")
        compose.onNodeWithText("Server address").performTextClearance()
        compose.onNodeWithText("Server address").performTextInput(server.removePrefix("http://"))
        compose.onNodeWithText("Connect").performClick()
        waitForText("Sign in to continue")
        settle()
        capture("00-login-sign-in")
    }

    @Test
    fun home() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        waitForText("All genres")
        settle()
        capture("01-home")
        scroll(1)
        settle()
        capture("02-home-scrolled")
        scroll(2)
        settle()
        capture("03-home-shelves")
    }

    @Test
    fun exploreAndBrowse() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        compose.runOnIdle { navigator.selectTab(Tab.Explore) }
        waitForText("Genre areas")
        settle()
        capture("04-explore")
        scroll(2)
        settle()
        capture("05-explore-genres")
        val area = runBlocking { container.repository.home().areas.first { it.area == GenreArea.Suspense } }
        compose.runOnIdle { navigator.navigate(BrowseRoute(BrowseSpec.forArea(area))) }
        waitForText("titles")
        settle()
        capture("06-browse-area")
    }

    @Test
    fun movieDetail() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        val id = idOf("Neon Tides", "Movie")
        compose.runOnIdle { navigator.navigate(DetailRoute(id)) }
        waitForText("AUDIO")
        settle()
        capture("07-movie")
        scroll(2)
        settle()
        capture("08-movie-more")
    }

    @Test
    fun seriesDetail() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        val id = idOf("Harborlight", "Series")
        compose.runOnIdle { navigator.navigate(DetailRoute(id)) }
        waitForText("Next Up")
        settle()
        capture("09-series")
        scroll(2)
        settle()
        capture("10-series-episodes")
        val episode = runBlocking { container.repository.episodes(id, container.repository.detail(id).seasons.first().id)[1].id }
        compose.runOnIdle { navigator.navigate(DetailRoute(episode)) }
        waitForText("Season 1 · Episode 2")
        settle()
        capture("11-episode")
    }

    @Test
    fun searchLibraryPersonSettings() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        compose.runOnIdle { navigator.selectTab(Tab.Search) }
        waitForText("Browse by genre")
        settle()
        capture("12-search-empty")
        compose.onNodeWithText("Movies, shows, people…").performTextInput("the")
        waitForText("results")
        settle()
        capture("13-search-results")
        compose.runOnIdle { navigator.selectTab(Tab.Library) }
        waitForText("Favorites")
        settle()
        capture("14-library")
        val person = runBlocking { container.client.persons("Mara").first().id }
        compose.runOnIdle {
            navigator.selectTab(Tab.Home)
            navigator.navigate(PersonRoute(person))
        }
        waitForText("Known for")
        settle()
        capture("15-person")
        compose.runOnIdle { navigator.navigate(SettingsRoute()) }
        waitForText("Streaming quality")
        settle()
        capture("16-settings")
    }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-xhdpi")
    fun tablet() {
        signIn()
        val navigator = AppNavigator()
        show { MainScaffold(navigator, onPlay = {}) }
        waitForText("All genres")
        settle()
        capture("20-tablet-home")
        val id = idOf("Echoes of Saturn", "Movie")
        compose.runOnIdle { navigator.navigate(DetailRoute(id)) }
        waitForText("Play")
        settle()
        capture("21-tablet-movie")
    }

    @Test
    fun lightTheme() {
        signIn()
        val navigator = AppNavigator()
        show(dark = false) { MainScaffold(navigator, onPlay = {}) }
        waitForText("All genres")
        settle()
        capture("17-home-light")
        val id = idOf("Paper Kingdoms", "Movie")
        compose.runOnIdle { navigator.navigate(DetailRoute(id)) }
        waitForText("AUDIO")
        settle()
        capture("18-movie-light")
    }
}
