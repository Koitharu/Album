package org.koitharu.album.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import org.koitharu.album.ui.album.AlbumIntent
import org.koitharu.album.ui.album.AlbumIntent.HandleClick
import org.koitharu.album.ui.album.AlbumIntent.HandleLongClick
import org.koitharu.album.ui.album.AlbumScope
import org.koitharu.album.ui.album.GalleryImageItem
import org.koitharu.album.ui.album.GalleryItemPlaceholder
import org.koitharu.album.ui.album.GalleryVideoItem
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.search.SearchIntent.DoSearch

@Composable
fun SearchResults(
    query: String,
    albumScope: AlbumScope,
    contentPadding: PaddingValues,
    handleIntent: MviIntentHandler<AlbumIntent>
) {
    val viewModel = hiltViewModel<SearchViewModel>()
    LaunchedEffect(query) {
        viewModel.handleIntent(DoSearch(query))
    }
    val results = viewModel.searchResults.collectAsLazyPagingItems()
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = albumScope.gridState,
        contentPadding = contentPadding + PaddingValues(4.dp),
        columns = GridCells.Fixed(4),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            results.itemCount,
            contentType = results.itemContentType { it::class.java.simpleName },
            key = results.itemKey { it.id },
        ) { i ->
            when (val item = results[i]) {
                is AlbumItem.Image -> albumScope.GalleryImageItem(
                    image = item,
                    isSelected = false,
                    onLongClick = { handleIntent(HandleLongClick(item)) },
                    onClick = { handleIntent(HandleClick(item)) },
                )

                is AlbumItem.Video -> albumScope.GalleryVideoItem(
                    image = item,
                    isSelected = false,
                    onLongClick = { handleIntent(HandleLongClick(item)) },
                    onClick = { handleIntent(HandleClick(item)) },
                )

                null -> GalleryItemPlaceholder()
            }
        }
    }
}