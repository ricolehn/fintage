package io.github.ricolehn.fintage.data

import androidx.compose.runtime.Immutable
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.ItemFields
import io.github.ricolehn.fintage.data.jellyfin.ItemQuery
import io.github.ricolehn.fintage.data.jellyfin.ItemsResult
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SortOption(val label: String, val sortBy: String, val sortOrder: String) {
    TopRated("Top rated", "CommunityRating,SortName", "Descending,Ascending"),
    Newest("Newest", "PremiereDate,ProductionYear,SortName", "Descending,Descending,Ascending"),
    RecentlyAdded("Recently added", "DateCreated,SortName", "Descending,Ascending"),
    Title("A–Z", "SortName", "Ascending"),
    Runtime("Shortest", "Runtime,SortName", "Ascending,Ascending"),
    Shuffle("Shuffle", "Random", "Ascending"),
}

enum class TypeFilter(val label: String) { All("All"), Movies("Movies"), Shows("Shows") }

@Immutable
data class SubGenre(val id: String, val name: String)

/** What a page of titles shows: a genre, an area, a decade, a studio, a library or a collection. */
@Immutable
data class BrowseSpec(
    val title: String,
    val subtitle: String? = null,
    val genreIds: List<String> = emptyList(),
    val subGenres: List<SubGenre> = emptyList(),
    val area: GenreArea? = null,
    val years: List<Int> = emptyList(),
    val studioIds: List<String> = emptyList(),
    val personIds: List<String> = emptyList(),
    val parentId: String? = null,
    val itemTypes: List<String> = listOf("Movie", "Series"),
    val defaultSort: SortOption = SortOption.TopRated,
    val favoritesOnly: Boolean = false,
    val recursive: Boolean = true,
) {
    val canFilterType: Boolean get() = "Movie" in itemTypes && "Series" in itemTypes

    companion object {
        fun forArea(area: AreaInfo) = BrowseSpec(
            title = area.area.title,
            subtitle = area.area.tagline,
            genreIds = area.genreIds,
            subGenres = area.genres.map { SubGenre(it.id, it.name) },
            area = area.area,
        )

        fun forGenre(id: String, name: String) = BrowseSpec(title = name, genreIds = listOf(id))
    }
}

@Immutable
data class BrowseFilters(
    val sort: SortOption,
    val type: TypeFilter = TypeFilter.All,
    val unwatchedOnly: Boolean = false,
    val subGenreId: String? = null,
)

@Immutable
data class Shelf(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val items: List<BaseItem>,
    val browse: BrowseSpec? = null,
    val area: GenreArea? = null,
)

@Immutable
data class HomeFeed(
    val spotlight: List<BaseItem>,
    val resume: List<BaseItem>,
    val nextUp: List<BaseItem>,
    val genres: List<GenreInfo>,
    val areas: List<AreaInfo>,
    val shelves: List<Shelf>,
    val libraries: List<BaseItem>,
)

@Immutable
data class Decade(val start: Int, val years: List<Int>) {
    val label: String get() = if (start < 1960) "Before 1960" else "${start}s"
}

@Immutable
data class ExploreFeed(
    val areas: List<AreaInfo>,
    val genres: List<GenreInfo>,
    val decades: List<Decade>,
    val collections: List<BaseItem>,
    val studios: List<BaseItem>,
)

@Immutable
data class SearchResults(
    val movies: List<BaseItem> = emptyList(),
    val shows: List<BaseItem> = emptyList(),
    val episodes: List<BaseItem> = emptyList(),
    val people: List<BaseItem> = emptyList(),
    val collections: List<BaseItem> = emptyList(),
) {
    val isEmpty: Boolean
        get() = movies.isEmpty() && shows.isEmpty() && episodes.isEmpty() && people.isEmpty() && collections.isEmpty()
}

@Immutable
data class ItemDetail(
    val item: BaseItem,
    val seasons: List<BaseItem> = emptyList(),
    val nextUp: BaseItem? = null,
    val similar: List<BaseItem> = emptyList(),
    val children: List<BaseItem> = emptyList(),
    val seasonEpisodes: List<BaseItem> = emptyList(),
)

private val VideoLibraryTypes = setOf("movies", "tvshows")

class MediaRepository(private val client: JellyfinClient) {
    private val cacheLock = Mutex()
    private var cachedPool: List<BaseItem>? = null
    private var cachedGenres: List<GenreInfo>? = null

    fun clearCache() {
        cachedPool = null
        cachedGenres = null
    }

    /** A random sample of the library with backdrops, used to illustrate genres and areas. */
    private suspend fun discoveryPool(): List<BaseItem> = cacheLock.withLock {
        cachedPool ?: client.items(
            ItemQuery(
                includeItemTypes = listOf("Movie", "Series"),
                sortBy = "Random",
                imageTypes = listOf("Backdrop"),
                fields = "Genres",
                limit = 400,
                enableTotalRecordCount = false,
            ),
        ).items.also { cachedPool = it }
    }

    suspend fun genres(): List<GenreInfo> {
        cacheLock.withLock { cachedGenres }?.let { return it }
        val raw = client.genres()
        val infos = raw.map {
            GenreInfo(
                id = it.id,
                name = it.name.orEmpty(),
                movieCount = it.movieCount ?: 0,
                seriesCount = it.seriesCount ?: 0,
                imageTag = it.imageTags["Primary"],
            )
        }.filter { it.name.isNotBlank() && it.count > 0 }
        val withCovers = GenreAreas.withCovers(infos, discoveryPool())
        cacheLock.withLock { cachedGenres = withCovers }
        return withCovers
    }

    suspend fun home(): HomeFeed = coroutineScope {
        val views = client.userViews()
        val resume = async { safe { client.resume(16) }.orEmpty() }
        val nextUp = async { safe { client.nextUp(16) }.orEmpty() }
        val spotlight = async {
            safe {
                client.items(
                    ItemQuery(
                        includeItemTypes = listOf("Movie", "Series"),
                        sortBy = "Random",
                        imageTypes = listOf("Backdrop"),
                        fields = ItemFields.SPOTLIGHT,
                        limit = 8,
                        enableTotalRecordCount = false,
                    ),
                ).items
            }.orEmpty()
        }
        val genres = async { safe { genres() }.orEmpty() }
        val topRated = async {
            safe {
                client.items(
                    ItemQuery(
                        includeItemTypes = listOf("Movie", "Series"),
                        sortBy = SortOption.TopRated.sortBy,
                        sortOrder = SortOption.TopRated.sortOrder,
                        limit = 18,
                        enableTotalRecordCount = false,
                    ),
                ).items
            }.orEmpty()
        }
        val lastPlayed = async {
            safe {
                client.items(
                    ItemQuery(
                        includeItemTypes = listOf("Movie", "Episode"),
                        sortBy = "DatePlayed",
                        sortOrder = "Descending",
                        filters = listOf("IsPlayed"),
                        limit = 1,
                        enableTotalRecordCount = false,
                    ),
                ).items.firstOrNull()
            }
        }
        val videoViews = views.filter { it.collectionType in VideoLibraryTypes }
        val latest = videoViews.map { view -> async { view to safe { client.latest(view.id, 16) }.orEmpty() } }

        val areas = GenreAreas.build(genres.await(), safe { discoveryPool() }.orEmpty())
        val areaShelves = areas.take(6).map { area ->
            async {
                area to safe {
                    client.items(
                        ItemQuery(
                            includeItemTypes = listOf("Movie", "Series"),
                            genreIds = area.genreIds,
                            sortBy = "Random",
                            limit = 16,
                            enableTotalRecordCount = false,
                        ),
                    ).items
                }.orEmpty()
            }
        }
        val because = lastPlayed.await()?.let { played ->
            val baseId = if (played.type == "Episode") played.seriesId ?: played.id else played.id
            val name = if (played.type == "Episode") played.seriesName ?: played.name else played.name
            val items = safe { client.similar(baseId, 16) }.orEmpty().filter { it.id != baseId }
            if (items.size >= 3 && name != null) Shelf("because", "Because you watched $name", null, items) else null
        }

        val areaList = areaShelves.awaitAll().filter { it.second.size >= 3 }.map { (area, items) ->
            Shelf(
                key = "area-${area.area.name}",
                title = area.area.title,
                subtitle = area.genres.take(3).joinToString(" · ") { it.name },
                items = items,
                browse = BrowseSpec.forArea(area),
                area = area.area,
            )
        }
        val latestList = latest.awaitAll().filter { it.second.isNotEmpty() }.map { (view, items) ->
            Shelf(
                key = "latest-${view.id}",
                title = "Recently added in ${view.name}",
                items = items,
                browse = BrowseSpec(
                    title = view.name.orEmpty(),
                    parentId = view.id,
                    itemTypes = if (view.collectionType == "tvshows") listOf("Series") else listOf("Movie"),
                    defaultSort = SortOption.RecentlyAdded,
                ),
            )
        }
        val top = topRated.await().takeIf { it.size >= 3 }?.let {
            Shelf("top", "Highly rated", "The favorites of critics and audiences", it,
                BrowseSpec(title = "Highly rated", defaultSort = SortOption.TopRated))
        }

        // areas alternate with the other shelves, so the page keeps changing as it scrolls
        val others = buildList {
            latestList.getOrNull(0)?.let(::add)
            because?.let(::add)
            latestList.getOrNull(1)?.let(::add)
            top?.let(::add)
            addAll(latestList.drop(2))
        }
        val shelves = buildList {
            val max = maxOf(areaList.size, others.size)
            for (i in 0 until max) {
                areaList.getOrNull(i)?.let(::add)
                others.getOrNull(i)?.let(::add)
            }
        }

        HomeFeed(
            spotlight = spotlight.await(),
            resume = resume.await(),
            nextUp = nextUp.await(),
            genres = genres.await(),
            areas = areas,
            shelves = shelves,
            libraries = views.filter { it.collectionType in VideoLibraryTypes + setOf("boxsets", "homevideos", "mixed") },
        )
    }

    suspend fun refreshContinue(): Pair<List<BaseItem>, List<BaseItem>> = coroutineScope {
        val resume = async { safe { client.resume(16) }.orEmpty() }
        val nextUp = async { safe { client.nextUp(16) }.orEmpty() }
        resume.await() to nextUp.await()
    }

    suspend fun explore(): ExploreFeed = coroutineScope {
        val views = client.userViews()
        val genres = genres()
        val collections = async {
            safe {
                client.items(ItemQuery(includeItemTypes = listOf("BoxSet"), sortBy = "SortName", limit = 40)).items
            }.orEmpty()
        }
        val studios = async {
            safe { client.studios() }.orEmpty()
                .filter { ((it.movieCount ?: 0) + (it.seriesCount ?: 0)) > 0 }
                .sortedByDescending { (it.movieCount ?: 0) + (it.seriesCount ?: 0) }
                .take(18)
        }
        val years = views.filter { it.collectionType in VideoLibraryTypes }
            .map { view -> async { safe { client.filters(view.id).years }.orEmpty() } }
            .awaitAll().flatten().distinct()
        val decades = years.groupBy { if (it < 1960) 1950 else it / 10 * 10 }
            .map { (start, list) -> Decade(start, if (start == 1950) list.sorted() else (start..start + 9).toList()) }
            .sortedByDescending { it.start }
        ExploreFeed(
            areas = GenreAreas.build(genres, safe { discoveryPool() }.orEmpty()),
            genres = genres,
            decades = decades,
            collections = collections.await(),
            studios = studios.await(),
        )
    }

    suspend fun browse(spec: BrowseSpec, filters: BrowseFilters, startIndex: Int, limit: Int): ItemsResult {
        val types = when (filters.type) {
            TypeFilter.All -> spec.itemTypes
            TypeFilter.Movies -> listOf("Movie")
            TypeFilter.Shows -> listOf("Series")
        }
        return client.items(
            ItemQuery(
                parentId = spec.parentId,
                includeItemTypes = types,
                recursive = spec.recursive,
                genreIds = filters.subGenreId?.let { listOf(it) } ?: spec.genreIds,
                years = spec.years,
                studioIds = spec.studioIds,
                personIds = spec.personIds,
                sortBy = filters.sort.sortBy,
                sortOrder = filters.sort.sortOrder,
                filters = buildList {
                    if (filters.unwatchedOnly) add("IsUnplayed")
                    if (spec.favoritesOnly) add("IsFavorite")
                },
                startIndex = startIndex,
                limit = limit,
            ),
        )
    }

    suspend fun search(term: String): SearchResults = coroutineScope {
        val movies = async { safe { client.items(ItemQuery(searchTerm = term, includeItemTypes = listOf("Movie"), sortBy = "SortName", limit = 24, enableTotalRecordCount = false)).items }.orEmpty() }
        val shows = async { safe { client.items(ItemQuery(searchTerm = term, includeItemTypes = listOf("Series"), sortBy = "SortName", limit = 24, enableTotalRecordCount = false)).items }.orEmpty() }
        val episodes = async { safe { client.items(ItemQuery(searchTerm = term, includeItemTypes = listOf("Episode"), sortBy = "SortName", limit = 16, enableTotalRecordCount = false)).items }.orEmpty() }
        val collections = async { safe { client.items(ItemQuery(searchTerm = term, includeItemTypes = listOf("BoxSet"), sortBy = "SortName", limit = 12, enableTotalRecordCount = false)).items }.orEmpty() }
        val people = async { safe { client.persons(term, 16) }.orEmpty() }
        SearchResults(movies.await(), shows.await(), episodes.await(), people.await(), collections.await())
    }

    suspend fun detail(id: String): ItemDetail = coroutineScope {
        val item = client.item(id)
        when (item.type) {
            "Series" -> {
                val seasons = async { safe { client.seasons(item.id) }.orEmpty() }
                val nextUp = async { safe { client.nextUp(1, item.id) }?.firstOrNull() }
                val similar = async { safe { client.similar(item.id) }.orEmpty() }
                ItemDetail(item, seasons = seasons.await(), nextUp = nextUp.await(), similar = similar.await())
            }
            "Season" -> {
                val seriesId = item.seriesId
                val seasons = async { seriesId?.let { safe { client.seasons(it) } }.orEmpty() }
                val similar = async { seriesId?.let { safe { client.similar(it) } }.orEmpty() }
                ItemDetail(item, seasons = seasons.await(), similar = similar.await())
            }
            "Episode" -> {
                val seriesId = item.seriesId
                val episodes = async {
                    if (seriesId != null) safe { client.episodes(seriesId, item.seasonId) }.orEmpty() else emptyList()
                }
                ItemDetail(item, seasonEpisodes = episodes.await())
            }
            "BoxSet" -> {
                val children = safe {
                    client.items(
                        ItemQuery(parentId = item.id, recursive = false, sortBy = "PremiereDate,ProductionYear,SortName", limit = 200),
                    ).items
                }.orEmpty()
                ItemDetail(item, children = children)
            }
            "Person" -> {
                val credits = safe {
                    client.items(
                        ItemQuery(
                            personIds = listOf(item.id),
                            includeItemTypes = listOf("Movie", "Series"),
                            sortBy = "PremiereDate,ProductionYear,SortName",
                            sortOrder = "Descending,Descending,Ascending",
                            limit = 200,
                        ),
                    ).items
                }.orEmpty()
                ItemDetail(item, children = credits)
            }
            else -> {
                val similar = safe { client.similar(item.id) }.orEmpty()
                ItemDetail(item, similar = similar)
            }
        }
    }

    suspend fun episodes(seriesId: String, seasonId: String): List<BaseItem> = client.episodes(seriesId, seasonId)

    suspend fun item(id: String): BaseItem = client.item(id)

    suspend fun setFavorite(id: String, favorite: Boolean) = client.setFavorite(id, favorite)

    suspend fun setPlayed(id: String, played: Boolean) = client.setPlayed(id, played)

    suspend fun libraries(): List<BaseItem> =
        client.userViews().filter { it.collectionType in VideoLibraryTypes + setOf("boxsets", "homevideos", "mixed") }

    /** The episode to start a show or a season with: the next one to watch, or the first one. */
    suspend fun firstEpisode(item: BaseItem): BaseItem? = when (item.type) {
        "Series" -> safe { client.nextUp(1, item.id) }?.firstOrNull() ?: safe { client.episodes(item.id, limit = 1) }?.firstOrNull()
        "Season" -> {
            val seriesId = item.seriesId
            if (seriesId == null) null else {
                val episodes = safe { client.episodes(seriesId, item.id) }.orEmpty()
                episodes.firstOrNull { it.userData?.played != true } ?: episodes.firstOrNull()
            }
        }
        else -> item
    }

    suspend fun randomEpisode(seriesId: String): BaseItem? = safe {
        client.items(
            ItemQuery(parentId = seriesId, includeItemTypes = listOf("Episode"), sortBy = "Random", limit = 1, enableTotalRecordCount = false),
        ).items.firstOrNull()
    }

    suspend fun localTrailer(id: String): BaseItem? = safe { client.localTrailers(id) }?.firstOrNull()

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
