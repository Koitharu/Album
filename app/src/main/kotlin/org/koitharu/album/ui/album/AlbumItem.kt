package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable
import coil3.Uri
import kotlinx.datetime.LocalDateTime

@Immutable
sealed interface AlbumItem {

    val id: Any

    data class DateHeader(
        val date: Long,
    ) : AlbumItem {

        override val id get() = date
    }

    @Immutable
    sealed interface Media : AlbumItem {
        val index: Int
        val uri: Uri
        val thumbnail: Uri
        val name: String?
        val dateAdded: LocalDateTime
        val memoryCacheKey: String
    }

    data class Image(
        override val index: Int,
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