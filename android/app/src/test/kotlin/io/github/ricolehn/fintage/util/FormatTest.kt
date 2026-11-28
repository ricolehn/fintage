package io.github.ricolehn.fintage.util

import io.github.ricolehn.fintage.data.GenreArea
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.MediaStream
import io.github.ricolehn.fintage.data.jellyfin.UserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {
    @Test
    fun runtimes() {
        assertEquals("1h 58m", Format.runtime(118L * 60 * 1000 * TICKS_PER_MS))
        assertEquals("42m", Format.runtime(42L * 60 * 1000 * TICKS_PER_MS))
        assertEquals("2h", Format.runtime(120L * 60 * 1000 * TICKS_PER_MS))
        assertEquals(null, Format.runtime(0))
    }

    @Test
    fun trackNamesAreShort() {
        assertEquals("German · AC3 5.1", Format.audioTrack(MediaStream(index = 1, codec = "ac3", language = "deu", channels = 6)))
        assertEquals("German · AC3 5.1", Format.audioTrack(MediaStream(index = 1, codec = "ac3", language = "ger", channels = 6)))
        assertEquals("English · AAC Stereo", Format.audioTrack(MediaStream(index = 1, codec = "aac", language = "eng", channels = 2)))
        assertEquals("(Forced) German · SRT", Format.subtitleTrack(MediaStream(index = 3, codec = "subrip", language = "ger", isForced = true)))
        assertEquals("French · PGS", Format.subtitleTrack(MediaStream(index = 4, codec = "pgssub", language = "fre")))
        assertEquals("4K HEVC HDR10", Format.videoTrack(MediaStream(index = 0, codec = "hevc", width = 3840, height = 2160, videoRangeType = "HDR10")))
    }

    @Test
    fun episodesAndProgress() {
        val episode = BaseItem(
            id = "e", type = "Episode", name = "Glass", seriesName = "Harborlight", parentIndexNumber = 1, indexNumber = 2,
            runTimeTicks = 1000, userData = UserData(playbackPositionTicks = 250),
        )
        assertEquals("S1:E2", Format.episodeCode(episode))
        assertEquals("Season 1 · Episode 2", Format.episodeLine(episode))
        assertEquals("Harborlight", Format.cardTitle(episode))
        assertEquals("S1:E2 · Glass", Format.cardSubtitle(episode))
        assertEquals(0.25f, Format.progress(episode)!!, 0.001f)
    }

    @Test
    fun genresFindTheirAreasInEnglishAndGerman() {
        assertTrue(GenreArea.Suspense.matches("Thriller"))
        assertTrue(GenreArea.Suspense.matches("Krimi"))
        assertTrue(GenreArea.OtherWorlds.matches("Sci-Fi & Fantasy"))
        assertTrue(GenreArea.ActionAdventure.matches("Action & Adventure"))
        assertTrue(GenreArea.Laughs.matches("Komödie"))
        assertTrue(GenreArea.HeartAndDrama.matches("Liebesfilm"))
        assertTrue(GenreArea.RealStories.matches("Dokumentarfilm"))
        assertTrue(GenreArea.FamilyTime.matches("Anime"))
        assertFalse(GenreArea.ActionAdventure.matches("Software"))
        assertFalse(GenreArea.HeartAndDrama.matches("Comedy"))
        assertNotNull(GenreArea.entries.firstOrNull { it.matches("War & Politics") })
    }
}
