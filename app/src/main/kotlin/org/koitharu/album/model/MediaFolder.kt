package org.koitharu.album.model

import android.net.Uri

data class MediaFolder(
    val id: String,
    val name: String,
    var size: Int,
    val thumbnail: Uri,
)