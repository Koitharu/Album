package org.koitharu.album.model

import android.net.Uri
import java.io.File

data class MediaFolder(
    val id: String,
    val name: String,
    var size: Int,
    val thumbnail: Uri,
    var path: String?,
) {

    fun isDCIM(): Boolean = path?.let {
        it.startsWith("DCIM") || it.contains("${File.separator}DCIM")
    } == true
}