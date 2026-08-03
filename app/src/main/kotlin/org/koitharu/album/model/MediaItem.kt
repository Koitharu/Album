package org.koitharu.album.model

import androidx.compose.runtime.Immutable
import coil3.Uri

@Immutable
data class MediaItem(
    val id: Long,
    val name: String?,
    val uri: Uri,
    val dateAdded: Long,
) {

    val thumbnail: Uri = uri.newBuilder()
        .scheme("thumb+" + uri.scheme)
        .build()
}
