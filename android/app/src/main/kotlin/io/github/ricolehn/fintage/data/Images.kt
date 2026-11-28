package io.github.ricolehn.fintage.data

import androidx.compose.runtime.Immutable
import io.github.ricolehn.fintage.data.jellyfin.BaseItem
import io.github.ricolehn.fintage.data.jellyfin.JellyfinClient
import io.github.ricolehn.fintage.data.jellyfin.PersonRef

@Immutable
data class ImageSource(val url: String, val blurHash: String?)

/** Picks the right image of an item for each place: poster, backdrop, landscape thumbnail or logo. */
class Images(private val client: JellyfinClient) {

    private fun hash(item: BaseItem, type: String, tag: String?): String? =
        tag?.let { item.imageBlurHashes[type]?.get(it) }

    private fun source(itemId: String, type: String, tag: String, width: Int, hash: String?, index: Int? = null) =
        ImageSource(client.imageUrl(itemId, type, tag, width, index), hash)

    fun poster(item: BaseItem, width: Int = 400): ImageSource? {
        if (item.type == "Episode") {
            val seriesTag = item.seriesPrimaryImageTag
            val seriesId = item.seriesId
            if (seriesTag != null && seriesId != null) return source(seriesId, "Primary", seriesTag, width, hash(item, "Primary", seriesTag))
        }
        item.imageTags["Primary"]?.let { return source(item.id, "Primary", it, width, hash(item, "Primary", it)) }
        val parentId = item.parentPrimaryImageItemId
        val parentTag = item.parentPrimaryImageTag
        if (parentId != null && parentTag != null) return source(parentId, "Primary", parentTag, width, hash(item, "Primary", parentTag))
        val seriesId = item.seriesId
        val seriesTag = item.seriesPrimaryImageTag
        if (seriesId != null && seriesTag != null) return source(seriesId, "Primary", seriesTag, width, hash(item, "Primary", seriesTag))
        return null
    }

    fun backdrop(item: BaseItem, width: Int = 1280): ImageSource? {
        item.backdropImageTags.firstOrNull()?.let { return source(item.id, "Backdrop", it, width, hash(item, "Backdrop", it), 0) }
        val parentId = item.parentBackdropItemId
        val parentTag = item.parentBackdropImageTags.firstOrNull()
        if (parentId != null && parentTag != null) {
            return source(parentId, "Backdrop", parentTag, width, hash(item, "Backdrop", parentTag), 0)
        }
        item.imageTags["Thumb"]?.let { return source(item.id, "Thumb", it, width, hash(item, "Thumb", it)) }
        return null
    }

    /** A 16:9 image: the still of an episode, or the thumb or backdrop of a movie or show. */
    fun landscape(item: BaseItem, width: Int = 720): ImageSource? {
        if (item.type == "Episode") {
            item.imageTags["Primary"]?.let { return source(item.id, "Primary", it, width, hash(item, "Primary", it)) }
            val thumbId = item.parentThumbItemId
            val thumbTag = item.parentThumbImageTag
            if (thumbId != null && thumbTag != null) return source(thumbId, "Thumb", thumbTag, width, hash(item, "Thumb", thumbTag))
            return backdrop(item, width)
        }
        item.imageTags["Thumb"]?.let { return source(item.id, "Thumb", it, width, hash(item, "Thumb", it)) }
        return backdrop(item, width) ?: poster(item, width)
    }

    fun logo(item: BaseItem, width: Int = 700): ImageSource? {
        item.imageTags["Logo"]?.let { return source(item.id, "Logo", it, width, hash(item, "Logo", it)) }
        val parentId = item.parentLogoItemId
        val parentTag = item.parentLogoImageTag
        if (parentId != null && parentTag != null) return source(parentId, "Logo", parentTag, width, hash(item, "Logo", parentTag))
        return null
    }

    fun seriesLogo(item: BaseItem, width: Int = 500): ImageSource? {
        val parentId = item.parentLogoItemId ?: return null
        val parentTag = item.parentLogoImageTag ?: return null
        return source(parentId, "Logo", parentTag, width, hash(item, "Logo", parentTag))
    }

    fun person(person: PersonRef, width: Int = 300): ImageSource? {
        val tag = person.primaryImageTag ?: return null
        return ImageSource(client.imageUrl(person.id, "Primary", tag, width), person.imageBlurHashes["Primary"]?.get(tag))
    }

    /** The image the server made for a genre or studio from the posters of its titles. */
    fun genre(id: String, tag: String, width: Int = 480): ImageSource = ImageSource(client.imageUrl(id, "Primary", tag, width), null)

    fun primary(item: BaseItem, width: Int = 400): ImageSource? =
        item.imageTags["Primary"]?.let { source(item.id, "Primary", it, width, hash(item, "Primary", it)) }

    fun user(userId: String, tag: String?): ImageSource? = client.userImageUrl(userId, tag)?.let { ImageSource(it, null) }
}
