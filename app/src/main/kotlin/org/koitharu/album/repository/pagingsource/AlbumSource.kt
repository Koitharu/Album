package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.provider.MediaStore.Files.FileColumns
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository

class AlbumSource @AssistedInject constructor(
    @Assisted bucketId: String?,
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
    private val hiddenMediaRepository: HiddenMediaRepository,
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
        if (bucketId != null) {
            append(" AND ")
            append(FileColumns.BUCKET_ID)
            append(" = ?")
        }
        append(" AND ")
        append(FileColumns._ID)
        append(" NOT IN (")
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
        if (bucketId != null) {
            add(bucketId)
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

    @AssistedFactory
    interface Factory {

        fun create(bucketId: String?): AlbumSource
    }
}