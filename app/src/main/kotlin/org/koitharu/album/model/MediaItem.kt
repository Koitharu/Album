package org.koitharu.album.model

import android.net.Uri
import org.koitharu.album.repository.ThumbnailFetcher.Companion.thumbnailUri

data class MediaItem(
    val index: Int,
    val id: Long,
    val name: String?,
    val mimeType: String,
    val uri: Uri,
    val dateAdded: Long,
    val isFavorite: Boolean,
    val isTrashed: Boolean,
    val isVideo: Boolean,
    private val path: String?,
) {

    val thumbnail: Uri = uri.thumbnailUri()
}
