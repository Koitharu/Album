package org.koitharu.album.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.toCoilUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.album.model.MediaItem
import org.koitharu.album.repository.OrderDirection.DESC
import org.koitharu.album.util.getOrDefault
import org.koitharu.album.util.runCancellable
import org.koitharu.album.util.suspendLazy

abstract class GallerySource(
    private val contentResolver: ContentResolver,
) : PagingSource<Int, MediaItem>() {

    val projection = arrayOf(
        FileColumns._ID,
        FileColumns.DISPLAY_NAME,
        FileColumns.MIME_TYPE,
        FileColumns.MEDIA_TYPE,
        FileColumns.DATE_ADDED,
    )
    open val selection = "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?"
    open val selectionArgs = arrayOf(
        FileColumns.MEDIA_TYPE_IMAGE.toString(),
        FileColumns.MEDIA_TYPE_VIDEO.toString(),
    )
    open val queryUri: Uri = MediaStore.Files.getContentUri("external")

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
        super.registerInvalidatedCallback {
            contentResolver.unregisterContentObserver(contentObserver)
        }
    }

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val pageSize = state.config.pageSize
        val result =  (anchorPosition / pageSize) * pageSize
        Log.i("ALBUMSRC", "refresh(key = $result)")
        return result
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, MediaItem> = withContext(Dispatchers.IO) {
        val limit = params.loadSize
        val offset = params.key ?: 0
        val total = totalCount.getOrDefault(0)

        val query = query(limit, offset) ?: return@withContext LoadResult.Invalid()

        query.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@withContext LoadResult.Invalid()
            }
            val result = ArrayList<MediaItem>(cursor.count)
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(FileColumns.DATE_ADDED)

            do {
                val id = cursor.getLong(idColumn)
                val isVideo = cursor.getInt(mediaTypeColumn) == FileColumns.MEDIA_TYPE_VIDEO
                val contentUri = if (isVideo) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }
                result.add(
                    MediaItem(
                        index = offset + result.size,
                        id = id,
                        name = cursor.getString(nameColumn),
                        mimeType = cursor.getString(mimeTypeColumn),
                        uri = contentUri.toCoilUri(),
                        isVideo = isVideo,
                        dateAdded = cursor.getLong(dateAddedColumn),
                    )
                )
            } while (cursor.moveToNext())
            if (result.isEmpty()) {
                return@withContext LoadResult.Invalid()
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
            ).also {
                Log.i("ALBUMSRC", "page($offset, $limit) = $it")
            }
        }
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