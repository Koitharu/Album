package org.koitharu.album.repository

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns

class AlbumSource(
    albumId: String?,
    contentResolver: ContentResolver
) : GallerySource(contentResolver) {

    override val selection = buildString {
        append('(')
        append(super.selection)
        append(") AND ")
        append(FileColumns.IS_TRASHED)
        append(" = ?")
        if (albumId != null) {
            append(" AND ")
            append(FileColumns.PARENT)
            append(" = ?")
        }
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        add("0")
        if (albumId != null) {
            add(albumId)
        }
    }.toTypedArray()
}