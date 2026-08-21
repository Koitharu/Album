package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository
import javax.inject.Inject

class HiddenMediaSource @Inject constructor(
    private val hiddenMediaRepository: HiddenMediaRepository,
    legacyFavoritesRepository: LegacyFavoritesRepository,
    contentResolver: ContentResolver,
) : GallerySource(
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository
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
        append(FileColumns._ID)
        append(" IN (")
        repeat(hiddenMediaRepository.getHiddenCount()) { i ->
            if (i != 0) {
                append(",")
            }
            append("?")
        }
        append(")")
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        if (Features.isRecycleBinSupported) {
            add("0")
        }
        hiddenMediaRepository.getHiddenIds().mapTo(this) {
            it.toString()
        }
    }.toTypedArray()

    init {
        sourceScope.launch {
            hiddenMediaRepository.observeHiddenCount()
                .drop(1)
                .collect {
                    invalidate()
                }
        }
    }
}