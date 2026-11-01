package org.koitharu.album.repository.mediastore

import android.provider.MediaStore.Files.FileColumns
import dagger.Reusable
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.model.MediaQuery.Companion.PATTERN_DCIM
import org.koitharu.album.model.VideosAvailability
import org.koitharu.album.model.VideosAvailability.ALL
import org.koitharu.album.model.VideosAvailability.CAMERA
import org.koitharu.album.model.VideosAvailability.NONE
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.SettingsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Reusable
class QueryBuilder @Inject constructor(
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val settingsRepository: SettingsRepository,
    private val legacyFavoritesRepository: LegacyFavoritesRepository,
) {

    suspend operator fun invoke(mediaQuery: MediaQuery): QueryParams {
        val videosAvailability = settingsRepository.getVideosAvailability()
        return QueryParams(
            selection = getSelection(mediaQuery, videosAvailability),
            selectionArgs = getSelectionArgs(mediaQuery, videosAvailability),
        )
    }

    private fun getSelection(
        mediaQuery: MediaQuery,
        videosAvailability: VideosAvailability,
    ) = buildString {
        append("(")
        if (mediaQuery.includeImages) {
            append(FileColumns.MEDIA_TYPE)
            append(" = ?")
        }
        if (mediaQuery.includeVideos) {
            when (videosAvailability) {
                ALL -> {
                    if (mediaQuery.includeImages) {
                        append(" OR ")
                    }
                    append(FileColumns.MEDIA_TYPE)
                    append(" = ?")
                }

                CAMERA -> {
                    if (mediaQuery.includeImages) {
                        append(" OR ")
                    }
                    append("(")
                    append(FileColumns.MEDIA_TYPE)
                    append(" = ? AND ")
                    if (Features.isPathColumnSupported) {
                        append(FileColumns.RELATIVE_PATH)
                    } else {
                        append(FileColumns.DATA)
                    }
                    append(" LIKE ?)")
                }

                NONE -> Unit
            }
        }
        append(")")
        if (Features.isRecycleBinSupported) {
            append(" AND ")
            append(FileColumns.IS_TRASHED)
            append(" = ?")
        }
        if (mediaQuery.bucketIds.isNotEmpty()) {
            append(" AND ")
            append(FileColumns.BUCKET_ID)
            if (mediaQuery.bucketIds.size == 1) {
                append(" = ?")
            } else {
                mediaQuery.bucketIds.joinTo(
                    buffer = this,
                    separator = ", ",
                    prefix = " IN (",
                    postfix = ")",
                    transform = { "?" },
                )
            }
        }
        if (mediaQuery.isFavorites) {
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
        append(" AND ")
        append(FileColumns._ID)
        if (!mediaQuery.isHidden) {
            append(" NOT")
        }
        append(" IN (")
        repeat(hiddenMediaRepository.getHiddenCount()) { i ->
            if (i != 0) {
                append(",")
            }
            append("?")
        }
        append(")")
        if (mediaQuery.dateRange != null) {
            append(" AND ")
            append(FileColumns.DATE_ADDED)
            append(" BETWEEN ? AND ?")
        }
        if (mediaQuery.searchQuery != null) {
            append(" AND ")
            append(FileColumns.DISPLAY_NAME)
            append(" LIKE ?")
        }
        if (mediaQuery.pathPattern != null) {
            append(" AND ")
            if (Features.isPathColumnSupported) {
                append(FileColumns.RELATIVE_PATH)
            } else {
                append(FileColumns.DATA)
            }
            append(" LIKE ?")
        }
    }

    private fun getSelectionArgs(
        mediaQuery: MediaQuery,
        videosAvailability: VideosAvailability,
    ) = buildList {
        if (mediaQuery.includeImages) {
            add(FileColumns.MEDIA_TYPE_IMAGE.toString())
        }
        if (mediaQuery.includeVideos) {
            when (videosAvailability) {
                ALL -> add(FileColumns.MEDIA_TYPE_VIDEO.toString())
                CAMERA -> {
                    add(FileColumns.MEDIA_TYPE_VIDEO.toString())
                    add(PATTERN_DCIM)
                }

                NONE -> Unit
            }
        }
        if (Features.isRecycleBinSupported) {
            add(if (mediaQuery.isTrashed) "1" else "0")
        }
        addAll(mediaQuery.bucketIds)
        if (mediaQuery.isFavorites) {
            if (Features.isNativeFavoritesSupported) {
                add("1")
            } else {
                legacyFavoritesRepository.getFavorites().mapTo(this) {
                    it.toString()
                }
            }
        }
        hiddenMediaRepository.getHiddenIds().mapTo(this) { it.toString() }
        mediaQuery.dateRange?.let {
            add(TimeUnit.MILLISECONDS.toSeconds(it.first).toString())
            add(TimeUnit.MILLISECONDS.toSeconds(it.last).toString())
        }
        mediaQuery.searchQuery?.let { add("%$it%") }
        mediaQuery.pathPattern?.let { add(it) }
    }

    data class QueryParams(
        val selection: String,
        val selectionArgs: List<String>,
    )
}