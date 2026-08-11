package org.koitharu.album.repository

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        val queryArgs = Bundle(7).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Features.isRecycleBinSupported) {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
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

fun ContentResolver.observeChanges(
    uri: Uri,
    kickstart: Boolean,
): Flow<Unit> = callbackFlow {
    val contentObserver = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            trySendBlocking(Unit)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    if (kickstart) {
        send(Unit)
    }
    awaitClose { unregisterContentObserver(contentObserver) }
}