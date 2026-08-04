package org.koitharu.album.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumItem
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.util.SetLightBarsEffect
import org.koitharu.album.ui.util.formattedDateTime
import org.koitharu.album.ui.util.rememberWindowInsetsController
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.toadlink.ui.composables.IconButtonWithTooltip

@Composable
fun ViewerScreen(
    albumId: String?,
    media: AlbumItem.Media,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val albumViewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = albumId
    ) {
        it.create(albumId)
    }
    val localStoreOwner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides localStoreOwner) {
        val viewModel = hiltViewModel<ViewerViewModel, ViewerViewModel.Factory> {
            it.create(media)
        }
        val state by viewModel.collectState()
        MediaViewer(
            pagingData = albumViewModel.pagerContent,
            media = state.currentMedia,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            handleIntent = viewModel,
            onClose = { albumViewModel.handleIntent(CloseMedia) }
        )
    }
}

@Composable
private fun MediaViewer(
    pagingData: Flow<PagingData<AlbumItem.Media>>,
    media: AlbumItem.Media,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    onClose: () -> Unit,
) = AlbumTheme(darkTheme = true) {
    Scaffold { innerPadding ->
        val insetsController = rememberWindowInsetsController()
        var isUiVisible by remember { mutableStateOf(true) }
        if (insetsController != null) {
            SetLightBarsEffect(insetsController, isLight = false)
            DisposableEffect(Unit) {
                onDispose {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            LaunchedEffect(isUiVisible) {
                if (isUiVisible) {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
        BackHandler(onBack = onClose)
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val images = pagingData.collectAsLazyPagingItems()
            val pagerState = rememberPagerState(
                initialPage = computeInitialIndex(media, images),
                pageCount = { images.itemCount }
            )
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }
                    .mapNotNull { images.peek(it) }
                    .collect {
                        handleIntent(OnMediaChanged(it))
                    }
            }
            HorizontalPager(
                state = pagerState,
                snapPosition = SnapPosition.Center,
                key = images.itemKey { it.id },
            ) { page ->
                when (val item = images[page]) {
                    is AlbumItem.Image -> with(sharedTransitionScope) {
                        with(animatedVisibilityScope) {
                            ImagePage(
                                image = item,
                                isCurrentPage = page == pagerState.currentPage,
                                innerPadding = innerPadding,
                                { isUiVisible = !isUiVisible },
                            )
                        }
                    }

                    null -> EmptyPage()
                }
                val item = images[page]
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current)
                    ),
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
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
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                        end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = innerPadding.calculateBottomPadding(),
                    )
                    .align(Alignment.BottomStart),
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BottomBar(
                    modifier = Modifier.fillMaxWidth(),
                    mediaDate = formattedDateTime(media.dateAdded),
                    handleIntent = handleIntent,
                )
            }
        }
    }
}

context(sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope)
@Composable
private fun ImagePage(
    image: AlbumItem.Image,
    isCurrentPage: Boolean,
    innerPadding: PaddingValues,
    onToggleUiVisibility: () -> Unit,
) = with(sharedTransitionScope) {
    val modifier = if (isCurrentPage) {
        Modifier.sharedElement(
            rememberSharedContentState(key = "image_${image.id}"),
            animatedVisibilityScope = animatedVisibilityScope
        )
    } else {
        Modifier
    }
    ZoomableAsyncImage(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .placeholderMemoryCacheKey(image.memoryCacheKey)
            .build(),
        contentPadding = innerPadding,
        contentDescription = null,
        onClick = { onToggleUiVisibility() },
    )
}

@Composable
private fun EmptyPage() = Box(
    modifier = Modifier.fillMaxSize()
) {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.Center)
    )
}

@Composable
private fun BottomBar(
    modifier: Modifier,
    mediaDate: String?,
    handleIntent: MviIntentHandler<ViewerIntent>,
) = Column(
    modifier = modifier,
) {
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = mediaDate.orEmpty(),
        style = MaterialTheme.typography.titleSmall,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            vertical = 12.dp,
        ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.share),
            onClick = { /* TODO */ },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.share)
            )
        }
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.favorite),
            onClick = { /* TODO */ },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_star_outline),
                contentDescription = stringResource(R.string.favorite)
            )
        }
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.edit),
            onClick = { /* TODO */ },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_image),
                contentDescription = stringResource(R.string.edit)
            )
        }
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.delete),
            onClick = { /* TODO */ },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete)
            )
        }
    }
}

private fun computeInitialIndex(
    item: AlbumItem.Media,
    pagingData: LazyPagingItems<AlbumItem.Media>
): Int {
    val index = pagingData.itemSnapshotList.indexOfFirst { x ->
        x?.id == item.id
    }
    return if (index < 0) {
        item.index
    } else {
        index
    }
}