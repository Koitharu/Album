package org.koitharu.album.ui.viewer

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.CloseMedia
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.common.OptionsMenu
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.editor.ImageEditorActivity
import org.koitharu.album.ui.folders.FolderItem
import org.koitharu.album.ui.info.MediaInfoBottomSheet
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.theme.resolveThemeVariant
import org.koitharu.album.ui.viewer.ViewerEffect.OnError
import org.koitharu.album.ui.viewer.ViewerEffect.OpenImageEditor
import org.koitharu.album.ui.viewer.ViewerIntent.CloseInfo
import org.koitharu.album.ui.viewer.ViewerIntent.Delete
import org.koitharu.album.ui.viewer.ViewerIntent.Edit
import org.koitharu.album.ui.viewer.ViewerIntent.Favorite
import org.koitharu.album.ui.viewer.ViewerIntent.OnMediaChanged
import org.koitharu.album.ui.viewer.ViewerIntent.OpenInExternalApp
import org.koitharu.album.ui.viewer.ViewerIntent.OpenInfo
import org.koitharu.album.ui.viewer.ViewerIntent.Print
import org.koitharu.album.ui.viewer.ViewerIntent.Recover
import org.koitharu.album.ui.viewer.ViewerIntent.Share
import org.koitharu.album.ui.viewer.ViewerIntent.UseAs
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.formattedDateTime
import org.koitharu.album.util.rememberWindowInsetsController
import org.koitharu.album.util.slideUpToClose

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
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is OnError -> snackbarHostState.showSnackbar(
                        effect.error.message ?: resources.getString(R.string.error_message_generic)
                    )

                    is OpenImageEditor -> context.startActivity(
                        Intent(
                            context,
                            ImageEditorActivity::class.java
                        ).setData(effect.uri)
                            .putExtra(ImageEditorActivity.EXTRA_NAME, effect.name)
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
            isVideosMutedOnStart = state.isVideosMutedOnStart,
            handleIntent = viewModel,
            onClose = { albumViewModel.handleIntent(CloseMedia) }
        )
        state.infoBottomSheetImage?.let {
            MediaInfoBottomSheet(
                image = it,
                onDismissRequest = { viewModel.handleIntent(CloseInfo) },
            )
        }
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
private fun PagerMediaViewer(
    pagingData: Flow<PagingData<AlbumItem.Media>>,
    media: AlbumItem.Media,
    isRotationGestureEnabled: Boolean,
    isVideosMutedOnStart: Boolean,
    snackbarHostState: SnackbarHostState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    onClose: () -> Unit,
) = AlbumTheme(variant = resolveThemeVariant(isForViewer = true)) {
    val insetsController = rememberWindowInsetsController()
    var isUiVisible by remember { mutableStateOf(true) }
    if (insetsController != null) {
        SetSystemBarsColorsEffect(insetsController = insetsController)
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
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
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
                    },
                    actions = {
                        OptionMenu(
                            media = media,
                            handleIntent = handleIntent
                        )
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
    ) { innerPadding ->
        BackHandler(onBack = onClose)
        val images = pagingData.collectAsLazyPagingItems(Dispatchers.Default)
        val initialIndex = remember(media) {
            computeInitialIndex(media, images)
        }
        if (initialIndex == -1) {
            SingleViewer(
                modifier = Modifier
                    .fillMaxSize()
                    .slideUpToClose(onClose),
                media = media,
                contentPadding = innerPadding,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                handleIntent = handleIntent,
                isRotationGestureEnabled = isRotationGestureEnabled,
                isVideosMutedOnStart = isVideosMutedOnStart,
                setUiVisible = { isUiVisible = it },
                isUiVisible = isUiVisible,
                isActive = true,
            )
        } else {
            ViewerPager(
                modifier = Modifier
                    .fillMaxSize()
                    .slideUpToClose(onClose),
                images = images,
                contentPadding = innerPadding,
                initialIndex = initialIndex,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                handleIntent = handleIntent,
                isRotationGestureEnabled = isRotationGestureEnabled,
                isUiVisible = isUiVisible,
                setUiVisible = { isUiVisible = it },
                isVideosMutedOnStart = isVideosMutedOnStart,
            )
        }
    }
}

@Composable
fun ViewerPager(
    modifier: Modifier,
    images: LazyPagingItems<AlbumItem.Media>,
    initialIndex: Int,
    isRotationGestureEnabled: Boolean,
    isVideosMutedOnStart: Boolean,
    contentPadding: PaddingValues,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    isUiVisible: Boolean,
    setUiVisible: (Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.itemCount }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .flatMapLatest { index ->
                snapshotFlow { images.peek(index) }
            }.filterNotNull()
            .collect {
                handleIntent(OnMediaChanged(it))
            }
    }
    HorizontalPager(
        modifier = modifier,
        state = pagerState,
        snapPosition = SnapPosition.Center,
        key = images.itemKey { it.id },
    ) { page ->
        SingleViewer(
            modifier = Modifier.fillMaxSize(),
            media = images[page],
            contentPadding = contentPadding,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            handleIntent = handleIntent,
            isActive = page == pagerState.currentPage,
            isUiVisible = isUiVisible,
            isRotationGestureEnabled = isRotationGestureEnabled,
            isVideosMutedOnStart = isVideosMutedOnStart,
            setUiVisible = setUiVisible,
        )
    }
}

@Composable
fun SingleViewer(
    modifier: Modifier,
    contentPadding: PaddingValues,
    media: AlbumItem.Media?,
    isRotationGestureEnabled: Boolean,
    isVideosMutedOnStart: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    handleIntent: MviIntentHandler<ViewerIntent>,
    isActive: Boolean,
    isUiVisible: Boolean,
    setUiVisible: (Boolean) -> Unit,
) {
    with(sharedTransitionScope) {
        val modifier = if (isActive && media is AlbumItem.Media) {
            modifier.sharedElement(
                rememberSharedContentState(key = "image_${media.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        } else {
            modifier
        }
        when (media) {
            is AlbumItem.Image -> ImageViewer(
                modifier = modifier,
                image = media,
                onRotated = { angle -> handleIntent(ViewerIntent.Rotate(media, angle)) },
                isRotationGestureEnabled = isRotationGestureEnabled,
                onClick = { setUiVisible(!isUiVisible) },
            )

            is AlbumItem.Video -> VideoViewer(
                modifier = modifier,
                video = media,
                contentPadding = contentPadding,
                isUiVisible = isUiVisible,
                setUiVisible = setUiVisible,
                startMuted = isVideosMutedOnStart,
            )

            null -> EmptyPage()
        }
    }
}

@Composable
private fun EmptyPage() = Box(
    modifier = Modifier.fillMaxSize()
) {
    LoadingIndicator(
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
                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
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
        IconButtonWithTooltip(
            tooltip = stringResource(R.string.share),
            onClick = { handleIntent(Share(media)) },
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
        if (media is AlbumItem.Image) {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.edit),
                onClick = { handleIntent(Edit(media)) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_image),
                    contentDescription = stringResource(R.string.edit)
                )
            }
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

@Composable
private fun OptionMenu(
    media: AlbumItem.Media,
    handleIntent: MviIntentHandler<ViewerIntent>,
) = OptionsMenu { dismiss ->
    DropdownMenuItem(
        text = {
            Text(stringResource(R.string.open_with))
        },
        onClick = {
            handleIntent(OpenInExternalApp(media))
            dismiss()
        }
    )
    if (media is AlbumItem.Image) {
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.use_as))
            },
            onClick = {
                handleIntent(UseAs(media))
                dismiss()
            }
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.print))
            },
            onClick = {
                handleIntent(Print(media))
                dismiss()
            }
        )
        DropdownMenuItem(
            text = {
                Text(stringResource(R.string.info))
            },
            onClick = {
                handleIntent(OpenInfo(media))
                dismiss()
            }
        )
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