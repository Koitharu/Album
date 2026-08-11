package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class FavoritesSource @Inject constructor(
    private val legacyFavoritesRepository: LegacyFavoritesRepository,
    contentResolver: ContentResolver,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository
) {

    override val selection = buildString {
        append('(')
        append(super.selection)
        append(") AND ")
        append(FileColumns.IS_TRASHED)
        append(" = ?")
        append(" AND ")
        if (Features.isNativeFavoritesSupported) {
            append(FileColumns.IS_FAVORITE)
            append(" = ?")
        } else {
            append(FileColumns._ID)
            append(" IN (")
            repeat(legacyFavoritesRepository.getFavoritesCount()) { i ->
                if (i != 0) {
                    append(",")
                }
                append("?")
            }
            append(")")
        }
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        add("0")
        if (Features.isNativeFavoritesSupported) {
            add("1")
        } else {
            legacyFavoritesRepository.getFavorites().mapTo(this) {
                it.toString()
            }
        }
    }.toTypedArray()
}