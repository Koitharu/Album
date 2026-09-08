package org.koitharu.album.ui.picker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import org.koitharu.album.ui.album.DateHeader
import org.koitharu.album.ui.album.FastScroller
import org.koitharu.album.ui.album.GalleryItemPlaceholder
import org.koitharu.album.ui.album.gridCell
import org.koitharu.album.ui.album.handleZoomGesture
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.common.ErrorImageFactory
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.picker.PickerIntent.OnDoneClick
import org.koitharu.album.ui.picker.PickerIntent.OnItemClick
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.isLoadFinished
import org.koitharu.album.util.toTitleCase

@Composable
fun PickerContent(
    state: PickerState,
    content: Flow<PagingData<AlbumItem>>,
    options: PickerOptions,
    snackbarHostState: SnackbarHostState,
    handleIntent: MviIntentHandler<PickerIntent>,
    onClose: () -> Unit,
) = Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(
                        if (options.isMultipleChoice) {
                            when {
                                options.isAllMediaAllowed -> R.string.pick_media
                                options.isImagesAllowed -> R.string.pick_images
                                options.isVideosAllowed -> R.string.pick_videos
                                else -> R.string.pick_media
                            }
                        } else {
                            when {
                                options.isAllMediaAllowed -> R.string.pick_media
                                options.isImagesAllowed -> R.string.pick_image
                                options.isVideosAllowed -> R.string.pick_video
                                else -> R.string.pick_media
                            }
                        }
                    ),
                )
            },
            subtitle = {
                val selectedCount = state.selectedItems.size
                if (selectedCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.items,
                            selectedCount,
                            selectedCount,
                        )
                    )
                }
            },
            navigationIcon = {
                IconButtonWithTooltip(
                    tooltip = stringResource(R.string.back),
                    onClick = onClose,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )
    },
    snackbarHost = {
        SnackbarHost(snackbarHostState)
    },
    bottomBar = {
        AnimatedVisibility(
            visible = state.selectedItems.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            BottomAppBar(
                floatingActionButton = {
                    Button(
                        onClick = { handleIntent(OnDoneClick) },
                    ) {
                        Text(text = stringResource(R.string.done))
                    }
                },
                actions = {},
            )
        }
    }
) { innerPadding ->
    val gridState = rememberLazyGridState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        val size = 42.dp
        val images = content.collectAsLazyPagingItems()
        if (images.itemCount == 0 && images.isLoadFinished()) {
            EmptyState(
                modifier = Modifier.fillMaxSize(),
                iconResId = R.drawable.ic_album,
                title = stringResource(R.string.no_media),
                message = stringResource(R.string.no_media_message),
            )
            return@Box
        }
        val selectedItems = state.selectedItems
        LazyVerticalGrid(
            modifier = Modifier.handleZoomGesture { zoom ->
                handleIntent(PickerIntent.UpdateScale(zoom))
            },
            state = gridState,
            contentPadding = innerPadding + PaddingValues(4.dp),
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

                    is AlbumItem.Image -> GalleryImageItem(
                        image = item,
                        isSelected = item.id in selectedItems,
                        onClick = { handleIntent(OnItemClick(item)) },
                    )

                    is AlbumItem.Video -> GalleryVideoItem(
                        image = item,
                        isSelected = item.id in selectedItems,
                        onClick = { handleIntent(OnItemClick(item)) },
                    )

                    null -> GalleryItemPlaceholder()
                }
            }
        }
        FastScroller(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .align(Alignment.TopEnd),
            gridState = gridState,
            dateProvider = { index ->
                images.peek(index)?.dateTime()
            }
        )
    }
}

@Composable
private fun GalleryImageItem(
    image: AlbumItem.Image,
    isSelected: Boolean,
    onClick: () -> Unit,
) = Box(
    modifier = Modifier
        .gridCell(isSelected)
        .clickable(
            onClick = onClick,
            role = Role.Image,
        ),
) {
    AsyncImage(
        modifier = Modifier.fillMaxSize(),
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

@Composable
private fun GalleryVideoItem(
    image: AlbumItem.Video,
    isSelected: Boolean,
    onClick: () -> Unit,
) = Box(
    modifier = Modifier
        .gridCell(isSelected)
        .clickable(
            onClick = onClick,
            role = Role.Image,
        ),
) {
    AsyncImage(
        modifier = Modifier.fillMaxSize(),
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