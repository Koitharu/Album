package org.koitharu.album.ui.gallery

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow

@Composable
fun Gallery(
    pagingData: Flow<PagingData<GalleryItem>>,
    contentPadding: PaddingValues,
    gridState: LazyGridState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onImageClick: (GalleryItem.Media) -> Unit,
) {
    val size = 42.dp
    var scale by remember { mutableFloatStateOf(2f) }
    val images = pagingData.collectAsLazyPagingItems()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            modifier = Modifier.handleZoomGesture { zoom ->
                scale = (scale * zoom).coerceIn(1f, 5f)
            },
            state = gridState,
            contentPadding = contentPadding,
            columns = GridCells.Adaptive(size * scale),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                images.itemCount,
                span = { i ->
                    GridItemSpan(
                        when (images[i]) {
                            is GalleryItem.DateHeader -> maxLineSpan
                            is GalleryItem.Image,
                            null -> 1
                        }
                    )
                },
                key = images.itemKey { it.id },
            ) { i ->
                when (val item = images[i]) {
                    is GalleryItem.DateHeader -> DateHeader(item.date)
                    is GalleryItem.Image -> GalleryImageItem(
                        image = item,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = onImageClick,
                    )

                    null -> GalleryItemPlaceholder()
                }
            }
        }
        FastScroller(
            modifier = Modifier
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
                .align(Alignment.TopEnd)
                .fillMaxHeight(),
            gridState = gridState,
            text = "Test",
        )
    }
}

@Composable
private fun GalleryImageItem(
    image: GalleryItem.Image,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: (GalleryItem.Image) -> Unit
) = Surface(
    modifier = Modifier
        .aspectRatio(1f)
        .fillMaxWidth(),
    onClick = { onClick(image) },
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