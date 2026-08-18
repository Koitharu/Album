package org.koitharu.album.ui.common

import android.net.Uri
import androidx.compose.runtime.Immutable
import org.koitharu.album.model.ImmutableDateTime
import org.koitharu.album.model.MediaItem
import org.koitharu.album.repository.ThumbnailFetcher.Companion.thumbnailUri

@Immutable
sealed interface AlbumItem {

    val id: Any

    fun dateTime(): ImmutableDateTime?

    data class DateHeader(
        val date: ImmutableDateTime,
    ) : AlbumItem {

        override val id get() = date.millis

        override fun dateTime(): ImmutableDateTime = date
    }

    @Immutable
    sealed interface Media : AlbumItem {
        override val id: Long
        val index: Int
        val uri: Uri
        val thumbnail: Uri
        val name: String?
        val dateAdded: ImmutableDateTime
        val mimeType: String
        val isFavorite: Boolean
        val isTrashed: Boolean

        val memoryCacheKey: String
            get() = "thumb_$id"

        fun copyWithFavoriteState(isFavorite: Boolean): Media

        override fun dateTime(): ImmutableDateTime? = dateAdded

        companion object {

            operator fun invoke(mediaItem: MediaItem): Media {
                val versionedUri = mediaItem.uri.buildUpon()
                    .appendQueryParameter("v", mediaItem.dateModified.toString())
                    .build()
                return if (mediaItem.isVideo) {
                    Video(
                        index = mediaItem.index,
                        id = mediaItem.id,
                        uri = versionedUri,
                        thumbnail = versionedUri.thumbnailUri(),
                        name = mediaItem.name,
                        mimeType = mediaItem.mimeType,
                        dateAdded = ImmutableDateTime.ofSeconds(mediaItem.dateAdded),
                        isFavorite = mediaItem.isFavorite,
                        isTrashed = mediaItem.isTrashed,
                    )
                } else {
                    Image(
                        index = mediaItem.index,
                        id = mediaItem.id,
                        uri = versionedUri,
                        thumbnail = versionedUri.thumbnailUri(),
                        name = mediaItem.name,
                        mimeType = mediaItem.mimeType,
                        dateAdded = ImmutableDateTime.ofSeconds(mediaItem.dateAdded),
                        isFavorite = mediaItem.isFavorite,
                        isTrashed = mediaItem.isTrashed,
                    )
                }
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
        override val isFavorite: Boolean,
        override val isTrashed: Boolean,
    ) : Media {

        override fun copyWithFavoriteState(isFavorite: Boolean) = copy(
            isFavorite = isFavorite
        )
    }

    data class Video(
        override val index: Int,
        override val id: Long,
        override val uri: Uri,
        override val thumbnail: Uri,
        override val name: String?,
        override val dateAdded: ImmutableDateTime,
        override val mimeType: String,
        override val isFavorite: Boolean,
        override val isTrashed: Boolean,
    ) : Media {

        override fun copyWithFavoriteState(isFavorite: Boolean) = copy(
            isFavorite = isFavorite
        )
    }
}