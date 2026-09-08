package org.koitharu.album.util

import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.asItemSnapshotListFlow
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.koitharu.album.ui.common.AlbumItem

fun LazyPagingItems<*>.isLoadFinished(): Boolean {
    return loadState.refresh is LoadState.NotLoading && loadState.append.endOfPaginationReached
}

suspend fun Flow<PagingData<AlbumItem>>.getSelectedItems(
    ids: Set<Long>,
): List<AlbumItem.Media> {
    val snapshot = asItemSnapshotListFlow().first()
    val result = ArrayList<AlbumItem.Media>(ids.size)
    for (item in snapshot) {
        if (item is AlbumItem.Media && item.id in ids) {
            result.add(item)
        }
    }
    return result
}