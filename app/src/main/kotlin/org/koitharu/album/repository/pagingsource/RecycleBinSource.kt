package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class RecycleBinSource @Inject constructor(
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository
) : GallerySource(contentResolver, legacyFavoritesRepository) {

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