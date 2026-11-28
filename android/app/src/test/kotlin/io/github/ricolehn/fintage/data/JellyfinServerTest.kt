package io.github.ricolehn.fintage.data

import io.github.ricolehn.fintage.data.jellyfin.ClientInfo
import io.github.ricolehn.fintage.data.jellyfin.DeviceProfile
import io.github.ricolehn.fintage.data.jellyfin.DirectPlayProfile
import io.github.ricolehn.fintage.data.jellyfin.ItemQuery
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import io.github.ricolehn.fintage.data.jellyfin.PlaybackInfoRequest
import io.github.ricolehn.fintage.data.jellyfin.PlaybackStartInfo
import io.github.ricolehn.fintage.data.jellyfin.PlaybackStopInfo
import io.github.ricolehn.fintage.data.jellyfin.SubtitleProfile
import io.github.ricolehn.fintage.data.jellyfin.TranscodingProfile
import io.github.ricolehn.fintage.data.jellyfin.UnauthorizedException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Runs the client against a real Jellyfin server. It is skipped unless a server answers at
 * FINTAGE_TEST_SERVER (default http://127.0.0.1:8096) with the user demo / demo and a library of
 * movies and shows with genres.
 */
class JellyfinServerTest {
    companion object {
        private val server = System.getenv("FINTAGE_TEST_SERVER") ?: "http://127.0.0.1:8096"
        private lateinit var client: JellyfinClient
        private lateinit var repository: MediaRepository

        @JvmStatic
        @BeforeClass
        fun signIn() {
            val reachable = runCatching {
                val connection = URL("$server/System/Info/Public").openConnection() as HttpURLConnection
                connection.connectTimeout = 1500
                connection.readTimeout = 3000
                connection.responseCode == 200
            }.getOrDefault(false)
            assumeTrue("No test server at $server", reachable)
            client = JellyfinClient(OkHttpClient(), ClientInfo("Fintage", "test", "JVM test", "fintage-test-device"))
            runBlocking {
                val (url, info) = client.findServer(server.removePrefix("http://"))
                assertEquals(server, url)
                client.configure(url, null, null)
                val auth = client.authenticateByName("demo", "demo")
                client.configure(url, auth.accessToken, auth.user!!.id)
                assertNotNull(info.serverName)
            }
            repository = MediaRepository(client)
        }
    }

    @Test
    fun addressCandidatesCoverTheUsualPorts() {
        assertEquals(
            listOf("https://jelly.local", "http://jelly.local:8096", "http://jelly.local", "https://jelly.local:8920"),
            JellyfinClient.addressCandidates("jelly.local"),
        )
        assertEquals(listOf("https://10.0.0.2:8096", "http://10.0.0.2:8096"), JellyfinClient.addressCandidates("10.0.0.2:8096/"))
        assertEquals(listOf("https://media.example.com/jellyfin", "https://media.example.com:8920/jellyfin"),
            JellyfinClient.addressCandidates("https://media.example.com/jellyfin"))
    }

    @Test
    fun homeFeedHasSpotlightGenresAndAreas(): Unit = runBlocking {
        val feed = repository.home()
        assertTrue("spotlight", feed.spotlight.isNotEmpty())
        assertTrue("genres", feed.genres.size >= 10)
        assertTrue("areas", feed.areas.size >= 5)
        assertTrue("every genre tile has a cover", feed.genres.all { it.cover != null || it.imageTag != null })
        val suspense = feed.areas.first { it.area == GenreArea.Suspense }
        assertTrue(suspense.genres.map { it.name }.containsAll(listOf("Thriller", "Mystery", "Crime")))
        assertTrue("shelves", feed.shelves.size >= 4)
        assertTrue("libraries", feed.libraries.size == 2)
        println("Areas: " + feed.areas.joinToString { "${it.area.title} (${it.genres.joinToString("/") { g -> g.name }})" })
        println("Shelves: " + feed.shelves.joinToString { "${it.title} [${it.items.size}]" })
    }

    @Test
    fun areaBrowseOnlyReturnsItsGenres(): Unit = runBlocking {
        val feed = repository.home()
        val area = feed.areas.first { it.area == GenreArea.Horror }
        val spec = BrowseSpec.forArea(area)
        val result = repository.browse(spec, BrowseFilters(SortOption.TopRated), 0, 50)
        val names = area.genres.map { it.name }.toSet()
        assertTrue(result.items.isNotEmpty())
        assertTrue(result.items.all { item -> item.genres.any { it in names } })
        val movies = repository.browse(spec, BrowseFilters(SortOption.Title, TypeFilter.Movies), 0, 50)
        assertTrue(movies.items.all { it.type == "Movie" })
    }

    @Test
    fun exploreHasDecadesStudiosAndGenres(): Unit = runBlocking {
        val feed = repository.explore()
        assertTrue(feed.decades.map { it.start }.containsAll(listOf(2020, 2010)))
        assertTrue(feed.studios.isNotEmpty())
        val decade = feed.decades.first { it.start == 2010 }
        val result = repository.browse(BrowseSpec(title = decade.label, years = decade.years), BrowseFilters(SortOption.Newest), 0, 50)
        assertTrue(result.items.isNotEmpty() && result.items.all { (it.productionYear ?: 0) in 2010..2019 })
    }

    @Test
    fun searchFindsMoviesShowsAndPeople(): Unit = runBlocking {
        assertTrue(repository.search("neon").movies.any { it.name == "Neon Tides" })
        assertTrue(repository.search("harbor").shows.any { it.name == "Harborlight" })
        assertTrue(repository.search("mara").people.any { it.name == "Mara Ellison" })
    }

    @Test
    fun seriesDetailHasSeasonsEpisodesAndNextUp(): Unit = runBlocking {
        val series = client.items(ItemQuery(searchTerm = "Harborlight", includeItemTypes = listOf("Series"))).items.first()
        val detail = repository.detail(series.id)
        assertEquals(2, detail.seasons.size)
        assertTrue(detail.item.people.isNotEmpty())
        val episodes = repository.episodes(series.id, detail.seasons.first().id)
        assertEquals(4, episodes.size)
        val episode = repository.detail(episodes[1].id)
        assertEquals(4, episode.seasonEpisodes.size)
        assertNotNull(episode.item.parentLogoImageTag)
        assertNotNull(repository.firstEpisode(series))
    }

    @Test
    fun playbackDecisionsAndProgressReports(): Unit = runBlocking {
        val profile = DeviceProfile(
            name = "test",
            maxStreamingBitrate = 20_000_000,
            maxStaticBitrate = 100_000_000,
            directPlayProfiles = listOf(DirectPlayProfile("mkv,mp4", "Video", "h264", "aac,mp3")),
            transcodingProfiles = listOf(TranscodingProfile("ts", "Video", "h264", "aac", "hls", maxAudioChannels = "6")),
            subtitleProfiles = listOf(SubtitleProfile("subrip", "External"), SubtitleProfile("srt", "External")),
        )
        val userId = client.userId!!
        val h264 = client.items(ItemQuery(searchTerm = "Glass Harbor", includeItemTypes = listOf("Movie"))).items.first()
        val direct = client.playbackInfo(h264.id, PlaybackInfoRequest(userId = userId, mediaSourceId = h264.id, deviceProfile = profile, audioStreamIndex = 1))
        val directSource = direct.mediaSources.first()
        assertTrue(directSource.supportsDirectPlay)
        assertTrue(directSource.mediaStreams.any { it.type == "Subtitle" && it.deliveryMethod == "External" && it.deliveryUrl != null })

        val german = client.playbackInfo(h264.id, PlaybackInfoRequest(userId = userId, mediaSourceId = h264.id, deviceProfile = profile, audioStreamIndex = 2))
        assertNotNull("AC3 audio needs a conversion", german.mediaSources.first().transcodingUrl)

        val hevc = client.items(ItemQuery(searchTerm = "Neon Tides", includeItemTypes = listOf("Movie"))).items.first()
        val transcode = client.playbackInfo(hevc.id, PlaybackInfoRequest(userId = userId, deviceProfile = profile))
        val url = transcode.mediaSources.first().transcodingUrl
        assertNotNull(url)
        assertTrue(url!!.contains("master.m3u8"))

        val start = PlaybackStartInfo(itemId = h264.id, mediaSourceId = directSource.id, playSessionId = direct.playSessionId,
            playMethod = "DirectPlay", positionTicks = 0)
        client.reportPlaybackStart(start)
        client.reportPlaybackProgress(start.copy(positionTicks = 100_000_000))
        client.reportPlaybackStopped(PlaybackStopInfo(itemId = h264.id, mediaSourceId = directSource.id,
            playSessionId = direct.playSessionId, positionTicks = 120_000_000))
        // the clips of the test library are shorter than the 5 minutes Jellyfin needs to keep a resume
        // position, so the report shows up as a play count instead
        val data = client.item(h264.id).userData
        assertTrue("the report reached the server", data != null && (data.playCount > 0 || data.playbackPositionTicks > 0))
        client.setPlayed(h264.id, false)
    }

    @Test
    fun favoritesAndQuickConnect(): Unit = runBlocking {
        val movie = client.items(ItemQuery(searchTerm = "Copper Sky", includeItemTypes = listOf("Movie"))).items.first()
        assertTrue(client.setFavorite(movie.id, true).isFavorite)
        val favorites = repository.browse(BrowseSpec(title = "Favorites", favoritesOnly = true), BrowseFilters(SortOption.Title), 0, 20)
        assertTrue(favorites.items.any { it.id == movie.id })
        assertTrue(!client.setFavorite(movie.id, false).isFavorite)
        assertTrue(client.quickConnectEnabled())
        assertTrue(client.quickConnectInitiate().code.isNotEmpty())
    }

    @Test(expected = UnauthorizedException::class)
    fun aRevokedTokenSignsOut(): Unit = runBlocking {
        val other = JellyfinClient(OkHttpClient(), ClientInfo("Fintage", "test", "JVM test", "fintage-test-device-2"))
        other.configure(server, "not-a-token", client.userId)
        other.userViews()
    }
}
