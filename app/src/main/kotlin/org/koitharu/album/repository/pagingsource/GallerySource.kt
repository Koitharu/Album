package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.koitharu.album.model.MediaItem
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.OrderDirection.DESC
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.getOrDefault
import org.koitharu.album.util.suspendLazy
import java.util.concurrent.TimeUnit

class GallerySource @AssistedInject constructor(
    @Assisted mediaQuery: MediaQuery,
    private val hiddenMediaRepository: HiddenMediaRepository,
    private val contentResolver: ContentResolver,
    private val legacyFavoritesRepository: LegacyFavoritesRepository,
) : PagingSource<Int, MediaItem>() {

    val projection = buildList(10) {
        add(FileColumns._ID)
        add(FileColumns.DISPLAY_NAME)
        add(FileColumns.MIME_TYPE)
        add(FileColumns.MEDIA_TYPE)
        add(FileColumns.DATE_ADDED)
        add(FileColumns.DATE_MODIFIED)
        if (Features.isNativeFavoritesSupported) {
            add(FileColumns.IS_FAVORITE)
        }
        if (Features.isRecycleBinSupported) {
            add(FileColumns.IS_TRASHED)
        }
        if (Features.isPathColumnSupported) {
            add(FileColumns.RELATIVE_PATH)
        } else {
            add(FileColumns.DATA)
        }
    }.toTypedArray()

    val selection = buildString {
        append("(")
        if (mediaQuery.includeImages) {
            append(FileColumns.MEDIA_TYPE)
            append(" = ?")
        }
        if (mediaQuery.includeVideos) {
            if (mediaQuery.includeImages) {
                append(" OR ")
            }
            append(FileColumns.MEDIA_TYPE)
            append(" = ?")
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

    val selectionArgs = buildList {
        if (mediaQuery.includeImages) {
            add(FileColumns.MEDIA_TYPE_IMAGE.toString())
        }
        if (mediaQuery.includeVideos) {
            add(FileColumns.MEDIA_TYPE_VIDEO.toString())
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
        mediaQuery.pathPattern?.let { add("%$it%") }
    }.toTypedArray()

    val queryUri: Uri = MediaStore.Files.getContentUri("external")
    val sourceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val totalCount = suspendLazy {
        queryCount()
    }

    init {
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                this@GallerySource.invalidate()
            }
        }
        contentResolver.registerContentObserver(queryUri, true, contentObserver)
        registerInvalidatedCallback {
            contentResolver.unregisterContentObserver(contentObserver)
        }
        registerInvalidatedCallback {
            sourceScope.cancel()
        }
    }

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val pageSize = state.config.pageSize
        val result = (anchorPosition / pageSize) * pageSize
        return result
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, MediaItem> = withContext(Dispatchers.IO) {
        val limit = params.loadSize
        val offset = params.key ?: 0
        val total = totalCount.getOrDefault(0)

        if (total == 0) {
            return@withContext LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null,
                itemsBefore = 0,
                itemsAfter = 0,
            )
        }

        val query = query(limit, offset)

        val result = query.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use emptyList()
            }
            val result = ArrayList<MediaItem>(cursor.count)
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(FileColumns.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
            val favoriteColumn = if (Features.isNativeFavoritesSupported) {
                cursor.getColumnIndex(FileColumns.IS_FAVORITE)
            } else {
                -1
            }
            val trashedColumn = if (Features.isRecycleBinSupported) {
                cursor.getColumnIndex(FileColumns.IS_TRASHED)
            } else {
                -1
            }
            val pathColumn = if (Features.isPathColumnSupported) {
                cursor.getColumnIndex(FileColumns.RELATIVE_PATH)
            } else {
                cursor.getColumnIndex(FileColumns.DATA)
            }

            do {
                val id = cursor.getLong(idColumn)
                val isVideo = cursor.getInt(mediaTypeColumn) == FileColumns.MEDIA_TYPE_VIDEO
                val contentUri = if (isVideo) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
                val isFavorite = if (favoriteColumn >= 0) {
                    cursor.getInt(favoriteColumn) > 0
                } else {
                    legacyFavoritesRepository.isFavorite(id)
                }
                val isTrashed = trashedColumn >= 0 && cursor.getInt(trashedColumn) > 0
                result.add(
                    MediaItem(
                        index = offset + result.size,
                        id = id,
                        name = cursor.getString(nameColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
                        uri = contentUri,
                        isVideo = isVideo,
                        isFavorite = isFavorite,
                        isTrashed = isTrashed,
                        dateAdded = cursor.getLong(dateAddedColumn),
                        dateModified = cursor.getLong(dateModifiedColumn),
                        path = if (pathColumn >= 0) {
                            cursor.getStringOrNull(pathColumn)
                        } else {
                            null
                        },
                    )
                )
            } while (cursor.moveToNext())
            result
        }
        LoadResult.Page(
            data = result,
            prevKey = if (offset > 0) {
                (offset - limit).coerceAtLeast(0)
            } else {
                null
            },
            nextKey = (offset + result.size).takeIf {
                it < total
            },
            itemsBefore = if (total > 0) {
                offset
            } else {
                COUNT_UNDEFINED
            },
            itemsAfter = if (total > 0) {
                (total - offset - result.size).coerceAtLeast(0)
            } else {
                COUNT_UNDEFINED
            },
        )
    }

    private suspend fun queryCount() = contentResolver.queryCompat(
        uri = queryUri,
        projection = arrayOf(MediaStore.Images.Media._ID),
        selection = selection,
        selectionArgs = selectionArgs,
    ).use {
        it.count
    }

    private suspend fun query(limit: Int, offset: Int) = contentResolver.queryCompat(
        uri = queryUri,
        projection = projection,
        selection = selection,
        selectionArgs = selectionArgs,
        orderBy = FileColumns.DATE_ADDED,
        orderDirection = DESC,
        offset = offset,
        limit = limit
    )

    @AssistedFactory
    interface Factory {

        fun create(query: MediaQuery): GallerySource
    }
}