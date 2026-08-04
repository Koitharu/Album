package org.koitharu.album.ui.album

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.MviIntentHandler

@Composable
fun AlbumContent(
    albumId: String?,
    innerPadding: PaddingValues,
    albumScope: AlbumScope,
) {
    val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = albumId
    ) {
        it.create(albumId)
    }
    val state by viewModel.collectState()
    Gallery(
        pagingData = viewModel.gridContent,
        state = state,
        contentPadding = innerPadding,
        albumScope = albumScope,
        handleIntent = viewModel,
    )
}

@Composable
private fun Gallery(
    pagingData: Flow<PagingData<AlbumItem>>,
    state: AlbumState,
    contentPadding: PaddingValues,
    albumScope: AlbumScope,
    handleIntent: MviIntentHandler<AlbumIntent>,
) {
    val size = 42.dp
    val images = pagingData.collectAsLazyPagingItems()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            modifier = Modifier.handleZoomGesture { zoom ->
                handleIntent(UpdateScale(zoom))
            },
            state = albumScope.gridState,
            contentPadding = contentPadding + PaddingValues(4.dp),
            columns = GridCells.Adaptive(size * state.scale),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                images.itemCount,
                span = { i ->
                    GridItemSpan(
                        when (images[i]) {
                            is AlbumItem.DateHeader -> maxLineSpan
                            is AlbumItem.Image,
                            null -> 1
                        }
                    )
                },
                contentType = images.itemContentType { it::class.java.simpleName },
                key = images.itemKey { it.id },
            ) { i ->
                when (val item = images[i]) {
                    is AlbumItem.DateHeader -> DateHeader(item.date)
                    is AlbumItem.Image -> GalleryImageItem(
                        image = item,
                        albumScope = albumScope,
                        onClick = { handleIntent(AlbumIntent.OpenMedia(item)) },
                    )

                    null -> GalleryItemPlaceholder()
                }
            }
        }
//        FastScroller(
//            modifier = Modifier
//                .padding(
//                    top = contentPadding.calculateTopPadding(),
//                    bottom = contentPadding.calculateBottomPadding(),
//                )
//                .align(Alignment.TopEnd)
//                .fillMaxHeight(),
//            gridState = gridState,
//            text = "Test",
//        )
    }
}

@Composable
private fun GalleryImageItem(
    image: AlbumItem.Image,
    albumScope: AlbumScope,
    onClick: () -> Unit
) = Surface(
    modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth(),
    onClick = onClick,
) {
    with(albumScope.sharedTransitionScope) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "image_${image.id}"),
                    animatedVisibilityScope = albumScope.animatedVisibilityScope
                ),
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.thumbnail)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCacheKey(image.memoryCacheKey)
                .build(),
            contentDescription = image.name,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun GalleryItemPlaceholder() = Surface(
    modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceDim,
) {}

@Composable
private fun DateHeader(
    date: Long,
) {
    val text = DateUtils.formatDateTime(
        LocalContext.current,
        date,
        0
    )
    Text(
        modifier = Modifier
            .padding(
                vertical = 6.dp,
                horizontal = 12.dp,
            )
            .fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}