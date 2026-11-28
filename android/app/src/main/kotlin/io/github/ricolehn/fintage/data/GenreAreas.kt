package io.github.ricolehn.fintage.data

import androidx.compose.runtime.Immutable
import io.github.ricolehn.fintage.data.jellyfin.BaseItem

/**
 * Genre areas group the genres of the server into a few places to wander through, like the aisles of a
 * video store. A genre belongs to an area when its name matches one of the words of the area, in English
 * or German, so the areas work with the genres of TMDB, TVDB and AniDB in both languages.
 */
enum class GenreArea(
    val title: String,
    val tagline: String,
    /** the hue of the tint of the area, in degrees */
    val hue: Float,
    private val keywords: List<String>,
) {
    ActionAdventure(
        "Action & Adventure", "Chases, quests and great escapes", 18f,
        listOf("action", "adventure", "abenteuer", "martial arts", "kampfsport", "superhero", "superheld", "disaster",
            "katastrophe", "heist", "spy", "agent", "western", "war", "krieg", "kriegsfilm", "military", "eastern"),
    ),
    Suspense(
        "Suspense & Mystery", "Twists, crimes and secrets", 198f,
        listOf("thriller", "mystery", "crime", "krimi", "kriminalfilm", "suspense", "detective", "detektiv", "noir",
            "verbrechen", "true crime", "psychothriller"),
    ),
    Horror(
        "After Midnight", "Horror and the uncanny", 352f,
        listOf("horror", "grusel", "slasher", "supernatural", "übernatürlich", "paranormal", "zombie", "vampire",
            "vampir", "monster", "gore"),
    ),
    OtherWorlds(
        "Other Worlds", "Science fiction and fantasy", 262f,
        listOf("science fiction", "sci-fi", "scifi", "fantasy", "space", "weltraum", "cyberpunk", "dystopia",
            "dystopie", "time travel", "zeitreise", "mecha", "isekai"),
    ),
    Laughs(
        "Laughs", "Comedies to lighten the mood", 44f,
        listOf("comedy", "komödie", "komoedie", "sitcom", "parody", "parodie", "satire", "stand-up", "humor",
            "humour", "slapstick"),
    ),
    HeartAndDrama(
        "Heart & Drama", "Love stories and lives that move", 336f,
        listOf("drama", "romance", "romantik", "romantic", "liebesfilm", "liebe", "melodrama", "soap",
            "coming of age", "slice of life"),
    ),
    FamilyTime(
        "Family Time", "Animation and stories for all ages", 150f,
        listOf("family", "familie", "familienfilm", "kids", "kinder", "children", "kinderfilm", "animation",
            "anime", "zeichentrick", "trickfilm", "animated", "cartoon"),
    ),
    RealStories(
        "Real Stories", "Documentaries, history and lives", 96f,
        listOf("documentary", "dokumentation", "dokumentarfilm", "doku", "history", "historie", "historisch",
            "geschichte", "biography", "biografie", "biographie", "news", "nachrichten", "reality", "nature", "natur",
            "sport", "politics", "politik", "true story"),
    ),
    MusicAndStage(
        "Music & Stage", "Concerts, musicals and dance", 300f,
        listOf("music", "musik", "musical", "concert", "konzert", "dance", "tanz", "musikfilm"),
    ),
    ;

    fun matches(genreName: String): Boolean {
        val name = genreName.lowercase().trim()
        return keywords.any { keyword ->
            name == keyword || Regex("(^|[^\\p{L}])" + Regex.escape(keyword) + "($|[^\\p{L}])").containsMatchIn(name)
        }
    }
}

@Immutable
data class GenreInfo(
    val id: String,
    val name: String,
    val movieCount: Int,
    val seriesCount: Int,
    /** an item of the genre with a backdrop, for the tile of the genre */
    val cover: BaseItem? = null,
    /** the server's own image of the genre, used when no item with a backdrop was found */
    val imageTag: String? = null,
) {
    val count: Int get() = movieCount + seriesCount
}

@Immutable
data class AreaInfo(
    val area: GenreArea,
    val genres: List<GenreInfo>,
    /** up to four items of the area with backdrops, for the collage of its tile */
    val covers: List<BaseItem>,
) {
    val genreIds: List<String> get() = genres.map { it.id }
    val count: Int get() = genres.sumOf { it.count }
}

object GenreAreas {
    /** Puts the genres of the server into their areas; areas without genres are left out. */
    fun build(genres: List<GenreInfo>, pool: List<BaseItem>): List<AreaInfo> =
        GenreArea.entries.mapNotNull { area ->
            val matched = genres.filter { area.matches(it.name) && it.count > 0 }
            if (matched.isEmpty()) return@mapNotNull null
            val names = matched.map { it.name }.toSet()
            val covers = pool.filter { item -> item.genres.any { it in names } && item.backdropImageTags.isNotEmpty() }
                .distinctBy { it.id }
                .take(4)
            AreaInfo(area, matched.sortedByDescending { it.count }, covers)
        }.sortedByDescending { it.count }

    /** Gives each genre an item with a backdrop from the pool, preferring items not used for another genre. */
    fun withCovers(genres: List<GenreInfo>, pool: List<BaseItem>): List<GenreInfo> {
        val used = HashSet<String>()
        return genres.sortedBy { it.count }.map { genre ->
            val candidates = pool.filter { genre.name in it.genres && it.backdropImageTags.isNotEmpty() }
            val cover = candidates.firstOrNull { it.id !in used } ?: candidates.firstOrNull()
            cover?.let { used += it.id }
            genre.copy(cover = cover)
        }.sortedByDescending { it.count }
    }
}
