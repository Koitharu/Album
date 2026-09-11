package org.koitharu.album.ui.album

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.ColorImage
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import org.koitharu.album.R
import org.koitharu.album.model.format
import org.koitharu.album.ui.album.AlbumIntent.HandleClick
import org.koitharu.album.ui.album.AlbumIntent.HandleLongClick
import org.koitharu.album.ui.album.AlbumIntent.UpdateScale
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.ErrorImageFactory
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.util.isLoadFinished
import org.koitharu.album.util.toTitleCase

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
    if (images.itemCount == 0 && images.isLoadFinished()) {
        emptyContent()
        return
    }
    val selectedItems = state.selectedItems
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
                    item.date.format("LLLL yyyy").toTitleCase()
                )

                is AlbumItem.Image -> albumScope.GalleryImageItem(
                    image = item,
                    isSelected = item.id in selectedItems,
                    onLongClick = { handleIntent(HandleLongClick(item)) },
                    onClick = { handleIntent(HandleClick(item)) },
                )

                is AlbumItem.Video -> albumScope.GalleryVideoItem(
                    image = item,
                    isSelected = item.id in selectedItems,
                    onLongClick = { handleIntent(HandleLongClick(item)) },
                    onClick = { handleIntent(HandleClick(item)) },
                )

                null -> GalleryItemPlaceholder()
            }
        }
    }
    FastScroller(
        modifier = Modifier
            .padding(scrollerPadding)
            .fillMaxHeight()
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
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) = Box(
    modifier = Modifier
        .gridCell(isSelected)
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
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
                .placeholder(ColorImage(MaterialTheme.colorScheme.surfaceContainer.toArgb()))
                .error(ErrorImageFactory())
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
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) = Box(
    modifier = Modifier
        .gridCell(isSelected)
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
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
                .placeholder(ColorImage(MaterialTheme.colorScheme.surfaceContainer.toArgb()))
                .error(ErrorImageFactory())
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

@Composable
fun GalleryItemPlaceholder() = Box(
    modifier = Modifier.gridCell(isSelected = false)
        .background(MaterialTheme.colorScheme.surfaceContainer)
) {

}

@Composable
fun Modifier.gridCell(isSelected: Boolean): Modifier {
    val factor by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(),
    )
    return fillMaxWidth()
        .aspectRatio(1f)
        .then(
            if (factor >= 0.01f) {
                val checkmark = painterResource(R.drawable.ic_check_circle)
                val tint = LocalContentColor.current
                val foreground = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.6f * factor)
                Modifier
                    .border(4.dp * factor, MaterialTheme.colorScheme.outline)
                    .drawWithContent {
                        drawContent()
                        drawRect(foreground)
                        with(checkmark) {
                            val padding = 6.dp.toPx()
                            translate(left = padding, top = padding) {
                                scale(
                                    scale = factor,
                                    pivot = intrinsicSize.center,
                                ) {
                                    draw(
                                        size = intrinsicSize,
                                        colorFilter = ColorFilter.tint(tint)
                                    )
                                }
                            }
                        }
                    }
            } else {
                Modifier
            }
        )
}

@Composable
fun DateHeader(
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