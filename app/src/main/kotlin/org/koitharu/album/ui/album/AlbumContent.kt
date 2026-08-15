package org.koitharu.album.ui.album

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.OpenMedia
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.util.formattedDateTime

@Composable
fun AlbumContent(
    folder: FolderItem?,
    innerPadding: PaddingValues,
    albumScope: AlbumScope,
) {
    val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = folder?.id
    ) {
        it.create(folder)
    }
    val state by viewModel.collectState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Gallery(
            pagingData = viewModel.gridContent,
            state = state,
            contentPadding = innerPadding,
            scrollerPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            albumScope = albumScope,
            handleIntent = viewModel,
            emptyContent = { }
        )
    }
}

@Composable
fun BoxScope.Gallery(
    pagingData: Flow<PagingData<AlbumItem>>,
    state: AlbumState,
    contentPadding: PaddingValues,
    scrollerPadding: PaddingValues,
    albumScope: AlbumScope,
    handleIntent: MviIntentHandler<AlbumIntent>,
    emptyContent: @Composable BoxScope.() -> Unit,
) {
    val size = 42.dp
    val images = pagingData.collectAsLazyPagingItems()

    if (images.itemCount == 0 && images.loadState.refresh is LoadState.NotLoading) {
        emptyContent()
        return
    }
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
                        is AlbumItem.Media,
                        null -> 1
                    }
                )
            },
            contentType = images.itemContentType { it::class.java.simpleName },
            key = images.itemKey { it.id },
        ) { i ->
            when (val item = images[i]) {
                is AlbumItem.DateHeader -> DateHeader(
                    formattedDateTime(
                        item.date,
                        DateUtils.FORMAT_SHOW_DATE
                    )
                )

                is AlbumItem.Image -> albumScope.GalleryImageItem(
                    image = item,
                ) { handleIntent(OpenMedia(item)) }

                is AlbumItem.Video -> albumScope.GalleryVideoItem(
                    image = item,
                ) { handleIntent(OpenMedia(item)) }

                null -> GalleryItemPlaceholder()
            }
        }
    }
    val context = LocalContext.current
    FastScroller(
        modifier = Modifier
            .padding(scrollerPadding)
            .align(Alignment.TopEnd),
        gridState = albumScope.gridState,
        dateProvider = { index ->
            images.peek(index)?.dateTime()
        }
    )
}

@Composable
private fun AlbumScope.GalleryImageItem(
    image: AlbumItem.Image,
    onClick: () -> Unit
) = Surface(
    modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth(),
    onClick = onClick,
) {
    with(sharedTransitionScope) {
        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "image_${image.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
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
private fun AlbumScope.GalleryVideoItem(
    image: AlbumItem.Video,
    onClick: () -> Unit
) = Surface(
    modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth(),
    onClick = onClick,
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        with(sharedTransitionScope) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedElement(
                        rememberSharedContentState(key = "image_${image.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
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
        Image(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .align(Alignment.Center),
            painter = painterResource(R.drawable.ic_play_circle),
            alpha = 0.6f,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.primaryFixedDim,
            ),
            contentDescription = null,
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
    date: String,
) {
    Text(
        modifier = Modifier
            .padding(
                vertical = 6.dp,
                horizontal = 12.dp,
            )
            .fillMaxWidth(),
        text = date,
        style = MaterialTheme.typography.titleMedium,
    )
}