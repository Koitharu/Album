package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.koitharu.album.repository.LegacyFavoritesRepository

class AlbumSource @AssistedInject constructor(
    @Assisted bucketId: String?,
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
        if (bucketId != null) {
            append(" AND ")
            append(FileColumns.BUCKET_ID)
            append(" = ?")
        }
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        add("0")
        if (bucketId != null) {
            add(bucketId)
        }
    }.toTypedArray()

    @AssistedFactory
    interface Factory {

        fun create(bucketId: String?): AlbumSource
    }
}