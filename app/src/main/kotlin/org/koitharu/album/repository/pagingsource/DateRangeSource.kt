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
import java.util.concurrent.TimeUnit

class DateRangeSource @AssistedInject constructor(
    @Assisted("from") dateFrom: Long,
    @Assisted("to") dateTo: Long,
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
        when {
            dateFrom != 0L && dateTo != 0L -> {
                append(" AND ")
                append(FileColumns.DATE_ADDED)
                append(" BETWEEN ? AND ?")
            }

            dateFrom != 0L -> {
                append(" AND ")
                append(FileColumns.DATE_ADDED)
                append(" >= ?")
            }

            dateTo != 0L -> {
                append(" AND ")
                append(FileColumns.DATE_ADDED)
                append(" <= ?")
            }
        }
    }

    override val selectionArgs = buildList {
        addAll(super.selectionArgs)
        if (Features.isRecycleBinSupported) {
            add("0")
        }
        if (dateFrom != 0L) {
            add(TimeUnit.MILLISECONDS.toSeconds(dateFrom).toString())
        }
        if (dateTo != 0L) {
            add(TimeUnit.MILLISECONDS.toSeconds(dateTo).toString())
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

        fun create(
            @Assisted("from") dateFrom: Long,
            @Assisted("to") dateTo: Long,
        ): DateRangeSource
    }
}