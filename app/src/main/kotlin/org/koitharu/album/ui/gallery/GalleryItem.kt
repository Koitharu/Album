package org.koitharu.album.ui.gallery

import androidx.compose.runtime.Immutable
import coil3.Uri
import kotlinx.datetime.LocalDateTime

@Immutable
sealed interface GalleryItem {

    val id: Any

    data class DateHeader(
        val date: Long,
    ) : GalleryItem {

        override val id get() = date
    }

    sealed interface Media : GalleryItem {
        val uri: Uri
        val thumbnail: Uri
        val name: String?
        val dateAdded: LocalDateTime
        val memoryCacheKey: String
    }

    data class Image(
        override val id: Long,
        override val uri: Uri,
        override val thumbnail: Uri,
        override val name: String?,
        override val dateAdded: LocalDateTime,
    ) : Media {

        override val memoryCacheKey: String
            get() = "thumb_$id"
    }
}