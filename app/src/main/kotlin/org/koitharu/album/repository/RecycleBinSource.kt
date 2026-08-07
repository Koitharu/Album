package org.koitharu.album.repository

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns

class RecycleBinSource(
    contentResolver: ContentResolver
) : GallerySource(contentResolver) {

    override val selection = buildString {
        append('(')
        append(super.selection)
        append(") AND ")
        append(FileColumns.IS_TRASHED)
        append(" = ?")
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        add("1")
    }.toTypedArray()
}