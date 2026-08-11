package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class PhotosSource @Inject constructor(
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository
) {

    override val selection = buildString {
        append(FileColumns.MEDIA_TYPE)
        append(" = ? AND")
        append(FileColumns.IS_TRASHED)
        append(" = ? AND ")
        if (Features.isPathColumnSupported) {
            append(FileColumns.RELATIVE_PATH)
        } else {
            append(FileColumns.DATA)
        }
        append(" LIKE ?")
    }

    override val selectionArgs = buildList {
        add(FileColumns.MEDIA_TYPE_IMAGE.toString())
        add("0")
        add("%DCIM%")
    }.toTypedArray()
}