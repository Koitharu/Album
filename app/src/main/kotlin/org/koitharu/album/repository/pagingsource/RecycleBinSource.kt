package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import org.koitharu.album.model.MediaItem
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class RecycleBinSource @Inject constructor(
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository,
) {

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

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return super.load(params).also {
            Log.i("PAGSRC", "load($params) = $it")
        }
    }
}