package org.koitharu.album.model

import coil3.Uri

data class MediaItem(
    val index: Int,
    val id: Long,
    val name: String?,
    val uri: Uri,
    val dateAdded: Long,
) {

    val thumbnail: Uri = uri.newBuilder()
        .scheme("thumb+" + uri.scheme)
        .build()
}
