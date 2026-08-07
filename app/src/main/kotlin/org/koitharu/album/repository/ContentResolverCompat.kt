package org.koitharu.album.repository

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.album.repository.OrderDirection.ASC
import org.koitharu.album.repository.OrderDirection.DESC
import org.koitharu.album.util.runCancellable

suspend fun ContentResolver.queryCompat(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    orderBy: String? = null,
    orderDirection: OrderDirection = ASC,
    offset: Int = -1,
    limit: Int = -1,
): Cursor? = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val queryArgs = Bundle(6).apply {
            if (limit > 0) {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            }
            if (offset > 0) {
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            if (orderBy != null) {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(orderBy)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    when (orderDirection) {
                        ASC -> ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                        DESC -> ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    }
                )
            }
            if (selection != null) {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    selection
                )
            }
            if (selectionArgs != null) {
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    selectionArgs
                )
            }
        }
        runCancellable { cancellationSignal ->
            query(
                uri,
                projection,
                queryArgs,
                cancellationSignal,
            )
        }
    } else {
        val sortOrder = buildString {
            if (orderBy != null) {
                append(orderBy)
                append(' ')
                append(
                    when (orderDirection) {
                        ASC -> "ASC"
                        DESC -> "DESC"
                    }
                )
            }
            if (limit > 0) {
                if (isNotEmpty()) {
                    append(' ')
                }
                append("LIMIT ")
                append(limit)
            }
            if (offset > 0) {
                if (isNotEmpty()) {
                    append(' ')
                }
                append("OFFSET ")
                append(offset)
            }
        }
        runCancellable { cancellationSignal ->
            query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder.takeUnless { it.isEmpty() },
                cancellationSignal
            )
        }
    }
}

enum class OrderDirection {
    ASC, DESC;
}