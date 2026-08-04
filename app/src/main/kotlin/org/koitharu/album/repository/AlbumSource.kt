package org.koitharu.album.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.PagingState
import coil3.toCoilUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.album.model.MediaItem
import org.koitharu.album.ui.util.runCancellable

class AlbumSource(
    private val contentResolver: ContentResolver,
) : PagingSource<Int, MediaItem>() {

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
    )

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int {
        return ((state.anchorPosition ?: 0) - state.config.initialLoadSize / 2).coerceAtLeast(0)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val limit = params.loadSize
        val offset = params.key ?: 0

        val count = withContext(Dispatchers.IO) {
            runCancellable { signal ->
                queryCount(signal)
            }
        }

        val query = withContext(Dispatchers.IO) {
            runCancellable { signal ->
                query(limit, offset, signal)
            }
        } ?: return LoadResult.Invalid()

        query.use { cursor ->
            if (!cursor.moveToFirst()) {
                return LoadResult.Invalid()
            }
            val result = ArrayList<MediaItem>(cursor.count)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val contentUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                result.add(
                    MediaItem(
                        index = offset + result.size,
                        id = id,
                        name = name,
                        uri = contentUri.toCoilUri(),
                        dateAdded = dateAdded,
                    )
                )
            }
            return LoadResult.Page(
                data = result,
                prevKey = if (offset > limit) offset - limit else null,
                nextKey = offset + result.size,
                itemsBefore = if (count > 0) {
                    offset
                } else {
                    COUNT_UNDEFINED
                },
                itemsAfter = if (count > 0) {
                    (count - offset - limit).coerceAtLeast(0)
                } else {
                    COUNT_UNDEFINED
                },
            )
        }
    }

    private fun queryCount(
        cancellationSignal: CancellationSignal,
    ) = contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID),
        null,
        null,
        null,
        cancellationSignal
    )?.use {
        it.count
    } ?: 0

    private fun query(
        limit: Int,
        offset: Int,
        cancellationSignal: CancellationSignal,
    ): Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.Media.DATE_ADDED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
        }

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            queryArgs,
            cancellationSignal,
        )
    } else {
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT $limit OFFSET $offset", // Sort order
            cancellationSignal
        )
    }
}