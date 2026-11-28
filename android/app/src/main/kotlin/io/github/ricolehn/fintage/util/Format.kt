package io.github.ricolehn.fintage.util

import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.MediaStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

const val TICKS_PER_MS = 10_000L

object Format {
    fun runtime(ticks: Long?): String? {
        if (ticks == null || ticks <= 0) return null
        val minutes = ticks / TICKS_PER_MS / 60_000
        if (minutes < 1) return "${ticks / TICKS_PER_MS / 1000}s"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "${m}m"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    fun remaining(item: BaseItem): String? {
        val total = item.runTimeTicks ?: return null
        val position = item.userData?.playbackPositionTicks ?: 0
        if (position <= 0) return null
        return runtime(total - position)?.let { "$it left" }
    }

    fun endsAt(item: BaseItem): String? {
        val total = item.runTimeTicks ?: return null
        if (item.type == "Series" || item.type == "Season" || item.isFolder == true) return null
        val left = total - (item.userData?.playbackPositionTicks ?: 0)
        val end = LocalDateTime.now().plusNanos(left * 100)
        return "Ends at " + end.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    fun progress(item: BaseItem): Float? {
        val total = item.runTimeTicks ?: return null
        val position = item.userData?.playbackPositionTicks ?: 0
        if (total <= 0 || position <= 0) return item.userData?.playedPercentage?.takeIf { it > 0 }?.let { (it / 100).toFloat() }
        return (position.toFloat() / total).coerceIn(0f, 1f)
    }

    fun years(item: BaseItem): String? {
        val start = item.productionYear ?: return null
        if (item.type != "Series") return start.toString()
        val end = item.endDate?.let { parseYear(it) }
        return when {
            item.status == "Continuing" -> "$start – Present"
            end != null && end != start -> "$start – $end"
            else -> start.toString()
        }
    }

    fun date(value: String?): String? = runCatching {
        OffsetDateTime.parse(value).toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }.getOrNull()

    private fun parseYear(value: String): Int? = runCatching { OffsetDateTime.parse(value).year }.getOrNull()
        ?: value.take(4).toIntOrNull()

    /** S1:E6 */
    fun episodeCode(item: BaseItem): String? {
        val season = item.parentIndexNumber
        val episode = item.indexNumber ?: return null
        val end = item.indexNumberEnd?.let { "-$it" }.orEmpty()
        return if (season != null) "S$season:E$episode$end" else "E$episode$end"
    }

    /** Season 1 · Episode 6 */
    fun episodeLine(item: BaseItem): String? {
        val season = item.parentIndexNumber
        val episode = item.indexNumber
        return when {
            season != null && episode != null -> if (season == 0) "Special · Episode $episode" else "Season $season · Episode $episode"
            episode != null -> "Episode $episode"
            else -> item.seasonName
        }
    }

    fun rating(value: Float?): String? = value?.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f", it) }

    /** The subtitle under a card: the year of a movie, the years of a show, the code and name of an episode. */
    fun cardSubtitle(item: BaseItem): String? = when (item.type) {
        "Episode" -> listOfNotNull(episodeCode(item), item.name).joinToString(" · ").ifEmpty { null }
        "Season" -> item.seriesName
        "Series" -> years(item)
        "BoxSet" -> item.childCount?.let { "$it titles" }
        else -> item.productionYear?.toString()
    }

    fun cardTitle(item: BaseItem): String = when (item.type) {
        "Episode" -> item.seriesName ?: item.name.orEmpty()
        else -> item.name.orEmpty()
    }

    /** English · EAC3 5.1, like the short track names of the theme. */
    fun audioTrack(stream: MediaStream): String {
        val language = languageName(stream.language) ?: stream.title ?: "Unknown"
        val codec = stream.codec?.uppercase()?.replace("TRUEHD", "TrueHD")
        val channels = when (stream.channels) {
            null -> null
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> "${stream.channels}ch"
        }
        return listOfNotNull(language, listOfNotNull(codec, channels).joinToString(" ").ifEmpty { null })
            .joinToString(" · ")
    }

    fun subtitleTrack(stream: MediaStream): String {
        val language = languageName(stream.language) ?: stream.title ?: "Unknown"
        val codec = when (stream.codec?.lowercase()) {
            "subrip" -> "SRT"
            "pgssub" -> "PGS"
            "dvdsub" -> "VobSub"
            "webvtt" -> "VTT"
            null -> null
            else -> stream.codec.uppercase()
        }
        val prefix = when {
            stream.isForced -> "(Forced) "
            stream.isHearingImpaired -> "(SDH) "
            else -> ""
        }
        return prefix + listOfNotNull(language, codec).joinToString(" · ")
    }

    /** 4K HEVC HDR10, 1080p H264 SDR */
    fun videoTrack(stream: MediaStream): String {
        val width = stream.width ?: 0
        val height = stream.height ?: 0
        val resolution = when {
            width >= 3200 || height >= 1900 -> "4K"
            width >= 1800 || height >= 1000 -> "1080p"
            width >= 1200 || height >= 700 -> "720p"
            height > 0 -> "${height}p"
            else -> null
        }
        val range = stream.videoRangeType?.takeIf { it != "Unknown" }?.let {
            when {
                it.startsWith("DOVI") -> "Dolby Vision"
                else -> it
            }
        }
        return listOfNotNull(resolution, stream.codec?.uppercase(), range).joinToString(" ")
    }

    /** ISO 639-2 has a second, older code for some languages, which Jellyfin passes on as it finds it. */
    private val bibliographicCodes = mapOf(
        "ger" to "deu", "fre" to "fra", "dut" to "nld", "chi" to "zho", "cze" to "ces", "gre" to "ell",
        "per" to "fas", "rum" to "ron", "slo" to "slk", "alb" to "sqi", "arm" to "hye", "baq" to "eus",
        "bur" to "mya", "geo" to "kat", "ice" to "isl", "mac" to "mkd", "mao" to "mri", "may" to "msa",
        "tib" to "bod", "wel" to "cym",
    )

    /** The two letter code of each three letter code, e.g. deu to de, which every version of Java can name. */
    private val twoLetterCodes: Map<String, String> by lazy {
        Locale.getISOLanguages().mapNotNull { code ->
            runCatching { Locale.Builder().setLanguage(code).build().isO3Language to code }.getOrNull()
        }.toMap()
    }

    private fun languageName(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.equals("und", ignoreCase = true)) return null
        val lower = raw.lowercase()
        val code = bibliographicCodes[lower] ?: lower
        val short = if (code.length == 3) twoLetterCodes[code] ?: code else code
        val name = runCatching { Locale.Builder().setLanguage(short).build().getDisplayLanguage(Locale.ENGLISH) }.getOrNull()
        return name?.takeIf { it.isNotBlank() && !it.equals(short, ignoreCase = true) } ?: raw
    }
}
