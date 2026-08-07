package org.koitharu.album.ui.common

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import coil3.Uri
import org.koitharu.album.model.ImmutableDateTime
import org.koitharu.album.model.MediaItem

@Immutable
sealed interface AlbumItem {

    val id: Any

    fun label(context: Context): String

    data class DateHeader(
        val date: ImmutableDateTime,
    ) : AlbumItem {

        override val id get() = date.millis

        override fun label(context: Context): String {
            return DateUtils.formatDateTime(context, date.millis, DateUtils.FORMAT_SHOW_DATE)
        }
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

        val memoryCacheKey: String
            get() = "thumb_$id"

        override fun label(context: Context): String {
            return DateUtils.formatDateTime(context, dateAdded.millis, DateUtils.FORMAT_SHOW_DATE)
        }

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