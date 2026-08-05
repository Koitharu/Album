package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable
import coil3.Uri
import org.koitharu.album.model.ImmutableDateTime
import org.koitharu.album.model.MediaItem

@Immutable
sealed interface AlbumItem {

    val id: Any

    data class DateHeader(
        val date: ImmutableDateTime,
    ) : AlbumItem {

        override val id get() = date.millis
    }

    @Immutable
    sealed interface Media : AlbumItem {
        val index: Int
        val uri: Uri
        val thumbnail: Uri
        val name: String?
        val dateAdded: ImmutableDateTime
        val mimeType: String

        val memoryCacheKey: String
            get() = "thumb_$id"

        companion object {

            operator fun invoke(mediaItem: MediaItem): Media = if (mediaItem.isVideo) {
                Video(
                    index = mediaItem.index,
                    id = mediaItem.id,
                    uri = mediaItem.uri,
                    thumbnail = mediaItem.thumbnail,
                    name = mediaItem.name,
                    mimeType = mediaItem.mimeType,
                    dateAdded = ImmutableDateTime.ofSeconds(mediaItem.dateAdded),
                )
            } else {
                Image(
                    index = mediaItem.index,
                    id = mediaItem.id,
                    uri = mediaItem.uri,
                    thumbnail = mediaItem.thumbnail,
                    name = mediaItem.name,
                    mimeType = mediaItem.mimeType,
                    dateAdded = ImmutableDateTime.ofSeconds(mediaItem.dateAdded),
                )
            }
        }
    }

    data class Image(
        override val index: Int,
        override val id: Long,
        override val uri: Uri,
        override val thumbnail: Uri,
        override val name: String?,
        override val dateAdded: ImmutableDateTime,
        override val mimeType: String,
    ) : Media

    data class Video(
        override val index: Int,
        override val id: Long,
        override val uri: Uri,
        override val thumbnail: Uri,
        override val name: String?,
        override val dateAdded: ImmutableDateTime,
        override val mimeType: String,
    ) : Media
}