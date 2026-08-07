package org.koitharu.album.repository

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class FavoritesSource(
    private val favoritesRepository: FavoritesRepository,
    contentResolver: ContentResolver,
) : GallerySource(contentResolver) {

    override val selection = buildString {
        append('(')
        append(super.selection)
        append(") AND ")
        append(FileColumns.IS_TRASHED)
        append(" = ?")
        append(" AND ")
        append(FileColumns.IS_FAVORITE)
        append(" = ?")
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        add("0")
        add("1")
    }.toTypedArray()

    init {
        val sourceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        registerInvalidatedCallback {
            sourceScope.cancel()
        }

    }
}