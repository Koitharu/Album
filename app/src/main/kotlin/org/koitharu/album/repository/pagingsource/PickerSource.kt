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

class PickerSource @AssistedInject constructor(
    @Assisted("videos") includeVideos: Boolean,
    @Assisted("images") includeImages: Boolean,
    @Assisted("fav") isFavoriteOnly: Boolean,
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
        if (includeImages) {
            append(FileColumns.MEDIA_TYPE)
            append(" = ?")
        }
        if (includeVideos) {
            if (includeImages) {
                append(" OR ")
            }
            append(FileColumns.MEDIA_TYPE)
            append(" = ?")
        }
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
        } else {
            val hidden = hiddenMediaRepository.getHiddenCount()
            if (hidden > 0) {
                append(" AND ")
                append(FileColumns._ID)
                append(" NOT IN (")
                repeat(hidden) { i ->
                    if (i != 0) {
                        append(",")
                    }
                    append("?")
                }
                append(")")
            }
        }
        if (isFavoriteOnly) {
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
    }

    override val selectionArgs = buildList {
        if (includeImages) {
            add(FileColumns.MEDIA_TYPE_IMAGE.toString())
        }
        if (includeVideos) {
            add(FileColumns.MEDIA_TYPE_VIDEO.toString())
        }
        if (Features.isRecycleBinSupported) {
            add("0")
        }
        if (bucketId != null) {
            add(bucketId)
        } else {
            hiddenMediaRepository.getHiddenIds().mapTo(this) {
                it.toString()
            }
        }
        if (isFavoriteOnly) {
            if (Features.isNativeFavoritesSupported) {
                add("1")
            } else {
                legacyFavoritesRepository.getFavorites().mapTo(this) {
                    it.toString()
                }
            }
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
            @Assisted("videos") includeVideos: Boolean,
            @Assisted("images") includeImages: Boolean,
            @Assisted("fav") isFavoriteOnly: Boolean,
            bucketId: String?
        ): PickerSource
    }
}