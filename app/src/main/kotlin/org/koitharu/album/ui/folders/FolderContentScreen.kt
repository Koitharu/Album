package org.koitharu.album.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumActionMode
import org.koitharu.album.ui.album.Gallery
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.gallery.GalleryEffect
import org.koitharu.album.ui.gallery.GalleryIntent
import org.koitharu.album.ui.gallery.GalleryIntent.CancelSelectionMode
import org.koitharu.album.ui.gallery.GalleryIntent.ClearRecycleBin
import org.koitharu.album.ui.gallery.GalleryIntent.OpenCamera
import org.koitharu.album.ui.gallery.galleryViewModel
import org.koitharu.album.ui.home.LocalNavBackStack
import org.koitharu.album.ui.home.NavDestination
import org.koitharu.album.util.FlowCollectEffect
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
context(sharedTransitionScope: SharedTransitionScope)
fun FolderContentScreen(
    folder: FolderItem,
) {
    val query = remember(folder) { folder.getMediaQuery() }
    val viewModel = galleryViewModel(query)
    val state by viewModel.collectState()
    val backStack = LocalNavBackStack.current
    FlowCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is GalleryEffect.OpenMedia -> backStack.add(
                NavDestination.MediaViewer(
                    query = query,
                    media = effect.media,
                )
            )

            is GalleryEffect.ShowError -> TODO()
        }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(text = folder.title())
                },
                navigationIcon = {
                    IconButtonWithTooltip(
                        tooltip = stringResource(R.string.back),
                        onClick = { backStack.removeLastOrNull() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    ContextMenu(
                        folder = folder,
                        handleIntent = viewModel,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            with(sharedTransitionScope) {
                Gallery(
                    pagingData = viewModel.gridContent,
                    state = state,
                    contentPadding = innerPadding,
                    scrollerPadding = innerPadding,
                    handleIntent = viewModel,
                    emptyContent = {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            iconResId = folder.iconId,
                            title = stringResource(
                                when (folder) {
                                    is FolderItem.Favorites -> R.string.empty_favorites
                                    is FolderItem.Hidden -> R.string.empty_hidden
                                    is FolderItem.RecycleBin -> R.string.empty_recycle_bin
                                    is FolderItem.Bucket,
                                    is FolderItem.Photos,
                                    is FolderItem.Videos -> R.string.folder_is_empty
                                }
                            ),
                            message = stringResource(
                                when (folder) {
                                    is FolderItem.Favorites -> R.string.empty_favorites_message
                                    is FolderItem.Hidden -> R.string.empty_hidden_message
                                    is FolderItem.RecycleBin -> R.string.empty_recycle_bin_message
                                    is FolderItem.Bucket,
                                    is FolderItem.Photos,
                                    is FolderItem.Videos -> R.string.empty_folder_message
                                }
                            ),
                        )
                    }
                )
            }
            BackHandler(
                enabled = state.selectedItems.isNotEmpty()
            ) {
                viewModel.handleIntent(CancelSelectionMode)
            }
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(24.dp),
                visible = state.selectedItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                AlbumActionMode(
                    query = query,
                    isRecycleBinEnabled = state.isRecycleBinEnabled,
                    selectedItemCount = state.selectedItems.size,
                    onCancel = { viewModel.handleIntent(CancelSelectionMode) },
                    handleIntent = viewModel,
                )
            }
        }
    }
    SetSystemBarsColorsEffect()
}

@Composable
private fun ContextMenu(
    folder: FolderItem,
    handleIntent: MviIntentHandler<GalleryIntent>,
) {
    when (folder) {
        is FolderItem.RecycleBin -> {
            var isDialogVisible by rememberSaveable { mutableStateOf(false) }
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.clear_recycle_bin),
                onClick = { isDialogVisible = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_sweep),
                    contentDescription = stringResource(R.string.clear_recycle_bin),
                )
            }
            if (isDialogVisible) {
                ClearRecycleBinDialog(
                    onConfirmed = {
                        handleIntent(ClearRecycleBin)
                    },
                    onDismissRequest = {
                        isDialogVisible = false
                    },
                )
            }
        }

        is FolderItem.Photos -> {
            IconButtonWithTooltip(
                tooltip = stringResource(R.string.open_camera),
                onClick = { handleIntent(OpenCamera) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = stringResource(R.string.open_camera),
                )
            }
        }

        else -> Unit
    }
}

@Composable
fun ClearRecycleBinDialog(
    onConfirmed: () -> Unit,
    onDismissRequest: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    title = {
        Text(
            text = stringResource(R.string.clear_recycle_bin)
        )
    },
    text = {
        Text(
            text = stringResource(R.string.clear_recycle_bin_confirmation)
        )
    },
    dismissButton = {
        TextButton(
            onClick = {
                onDismissRequest()
            }
        ) {
            Text(stringResource(android.R.string.cancel))
        }
    },
    confirmButton = {
        TextButton(
            onClick = {
                onConfirmed()
                onDismissRequest()
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
        ) {
            Text(stringResource(R.string.clear))
        }
    },
)