package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore.Files.FileColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class VideosSource @Inject constructor(
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository
) {

    override val selection = buildString {
        append(FileColumns.MEDIA_TYPE)
        append(" = ?")
        if (Features.isRecycleBinSupported) {
            append(" AND ")
            append(FileColumns.IS_TRASHED)
            append(" = ?")
        }
    }

    override val selectionArgs = buildList {
        add(FileColumns.MEDIA_TYPE_VIDEO.toString())
        if (Features.isRecycleBinSupported) {
            add("0")
        }
    }.toTypedArray()
}