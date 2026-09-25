package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.LegacyFavoritesRepository

class SearchSource @AssistedInject constructor(
    @Assisted query: String,
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository,
) {

    override val selection = buildString {
        append('(')
        append(super.selection)
        append(')')
        if (Features.isRecycleBinSupported) {
            append(" AND ")
            append(FileColumns.IS_TRASHED)
            append(" = ?")
        }
        append(" AND ")
        append(FileColumns.DISPLAY_NAME)
        append(" LIKE ?")
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        if (Features.isRecycleBinSupported) {
            add("0")
        }
        add("%$query%")
    }.toTypedArray()

    @AssistedFactory
    interface Factory {

        fun create(query: String): SearchSource
    }
}