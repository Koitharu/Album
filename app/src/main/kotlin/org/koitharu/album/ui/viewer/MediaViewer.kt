package org.koitharu.album.ui.viewer

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.theme.resolveThemeVariant
import org.koitharu.album.ui.viewer.ViewerIntent.Delete
import org.koitharu.album.ui.viewer.ViewerIntent.Favorite
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.album.ui.viewer.ViewerIntent.Recover
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.SetSystemBarsColorsEffect
import org.koitharu.album.util.formattedDateTime
import org.koitharu.album.util.rememberWindowInsetsController
import org.koitharu.album.util.shareMedia

@Composable
fun ViewerScreen(
    folder: FolderItem?,
    media: AlbumItem.Media,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val albumViewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = folder?.id
    ) {
        it.create(folder)
    }
    val localStoreOwner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides localStoreOwner) {
        val viewModel = hiltViewModel<ViewerViewModel, ViewerViewModel.Factory> {
            it.create(media)
        }
        val state by viewModel.collectState()
        val resources = LocalResources.current
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is ViewerEffect.OnError -> snackbarHostState.showSnackbar(
                        effect.error.message ?: resources.getString(R.string.error_message_generic)
                    )
                }
            }
        }
        PagerMediaViewer(
            pagingData = albumViewModel.pagerContent,
            media = state.currentMedia,
            snackbarHostState = snackbarHostState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            isRotationGestureEnabled = state.isRotationGestureEnabled,
            handleIntent = viewModel,
            onClose = { albumViewModel.handleIntent(CloseMedia) })
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
private fun PagerMediaViewer(
    pagingData: Flow<PagingData<AlbumItem.Media>>,
    media: AlbumItem.Media,
    isRotationGestureEnabled: Boolean,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    onClose: () -> Unit,
) = AlbumTheme(variant = resolveThemeVariant(isForViewer = true)) {
    val insetsController = rememberWindowInsetsController()
    var isUiVisible by remember { mutableStateOf(true) }
    if (insetsController != null) {
        SetSystemBarsColorsEffect(
            insetsController = insetsController,
            isLightStatusBar = false,
            isLightNavigationBar = false,
        )
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
    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                TopAppBar(
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                Color.Transparent,
                            )
                        )
                    ),
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
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
            }
        },
        bottomBar = {
            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth(),
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BottomBar(
                    modifier = Modifier.fillMaxWidth(),
                    media = media,
                    handleIntent = handleIntent,
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        contentWindowInsets = WindowInsets.systemBarsIgnoringVisibility,
    ) { _ ->
        BackHandler(onBack = onClose)
        val images = pagingData.collectAsLazyPagingItems(Dispatchers.Default)
        val initialIndex = remember(media) {
            computeInitialIndex(media, images)
        }
        if (initialIndex == -1) {
            SingleViewer(
                media = media,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                handleIntent = handleIntent,
                isRotationGestureEnabled = isRotationGestureEnabled,
                onClick = { isUiVisible = !isUiVisible },
                isActive = true,
            )
        } else {
            ViewerPager(
                images = images,
                initialIndex = initialIndex,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                handleIntent = handleIntent,
                isRotationGestureEnabled = isRotationGestureEnabled,
                onClick = { isUiVisible = !isUiVisible },
            )
        }
    }
}

@Composable
fun ViewerPager(
    images: LazyPagingItems<AlbumItem.Media>,
    initialIndex: Int,
    isRotationGestureEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    onClick: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.itemCount }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.mapNotNull { images.peek(it) }.collect {
            handleIntent(OnMediaChanged(it))
        }
    }
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        snapPosition = SnapPosition.Center,
        key = images.itemKey { it.id },
    ) { page ->
        SingleViewer(
            media = images[page],
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            handleIntent = handleIntent,
            isActive = page == pagerState.currentPage,
            isRotationGestureEnabled = isRotationGestureEnabled,
            onClick = onClick,
        )
    }
}

@Composable
fun SingleViewer(
    media: AlbumItem.Media?,
    isRotationGestureEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    with(sharedTransitionScope) {
        val modifier = if (isActive && media is AlbumItem.Media) {
            Modifier.sharedElement(
                rememberSharedContentState(key = "image_${media.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        } else {
            Modifier
        }
        when (media) {
            is AlbumItem.Image -> ImageViewer(
                modifier = modifier,
                image = media,
                onRotate = { angle -> handleIntent(ViewerIntent.Rotate(media, angle)) },
                isRotationGestureEnabled = isRotationGestureEnabled,
                onClick = onClick,
            )

            is AlbumItem.Video -> VideoViewer(
                modifier = modifier,
                video = media,
                isActive = isActive,
                onClick = onClick,
            )

            null -> EmptyPage()
        }
    }
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
    media: AlbumItem.Media,
    handleIntent: MviIntentHandler<ViewerIntent>,
) = Column(
    modifier = Modifier
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                )
            )
        )
        .then(modifier),
) {
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = formattedDateTime(media.dateAdded),
        style = MaterialTheme.typography.titleSmall,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                vertical = 12.dp,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val context = LocalContext.current
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.share),
            onClick = { shareMedia(context, media) },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = stringResource(R.string.share)
            )
        }
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.favorite),
            onClick = { handleIntent(Favorite(media, !media.isFavorite)) },
        ) {
            Crossfade(
                targetState = media.isFavorite,
            ) { fav ->
                Icon(
                    painter = painterResource(if (fav) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                    contentDescription = stringResource(R.string.favorite)
                )
            }
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
        if (media.isTrashed) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.restore),
                onClick = { handleIntent(Recover(media)) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recover),
                    contentDescription = stringResource(R.string.restore)
                )
            }
        } else {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.delete),
                onClick = { handleIntent(Delete(media)) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

private fun computeInitialIndex(
    item: AlbumItem.Media, pagingData: LazyPagingItems<AlbumItem.Media>
): Int {
    val snapshot = pagingData.itemSnapshotList
    // fast path
    if (item.index in snapshot.indices && snapshot[item.index]?.id == item.id) {
        return item.index
    }
    // slow path
    val index = pagingData.itemSnapshotList.indexOfFirst { x ->
        x?.id == item.id
    }
    if (index >= 0) {
        return index
    }
    // fallback
    return -1
}