package org.koitharu.album.repository.pagingsource

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.core.database.getStringOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.koitharu.album.model.MediaItem
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.OrderDirection.DESC
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.getOrDefault
import org.koitharu.album.util.runCancellable
import org.koitharu.album.util.suspendLazy

abstract class GallerySource(
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
    open val selection = "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?"
    open val selectionArgs = arrayOf(
        FileColumns.MEDIA_TYPE_IMAGE.toString(),
        FileColumns.MEDIA_TYPE_VIDEO.toString(),
    )
    open val queryUri: Uri = MediaStore.Files.getContentUri("external")
    val sourceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val totalCount = suspendLazy {
        runCancellable { signal ->
            queryCount(signal)
        }
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

        val query = query(limit, offset) ?: return@withContext LoadResult.Invalid()

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

    private fun queryCount(
        cancellationSignal: CancellationSignal,
    ) = contentResolver.query(
        queryUri,
        arrayOf(MediaStore.Images.Media._ID),
        selection,
        selectionArgs,
        null,
        cancellationSignal
    )?.use {
        it.count
    } ?: 0

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
}