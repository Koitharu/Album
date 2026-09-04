package org.koitharu.album.ui.folders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumActionMode
import org.koitharu.album.ui.album.AlbumIntent.CancelSelectionMode
import org.koitharu.album.ui.album.AlbumScope
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.album.Gallery
import org.koitharu.album.ui.common.AlbumItem.Media
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.viewer.ViewerScreen
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun FolderContentScreen(
    folder: FolderItem,
    onClose: () -> Unit,
) {
    val localStoreOwner = rememberViewModelStoreOwner()
    CompositionLocalProvider(LocalViewModelStoreOwner provides localStoreOwner) {
        val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
            key = folder.id,
        ) {
            it.create(folder)
        }
        val gridState = rememberLazyGridState()
        val state by viewModel.collectState()
        BackHandler(onBack = onClose)
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        SharedTransitionLayout {
            AnimatedContent(state.openedItem) { openedItem ->
                when (openedItem) {
                    is Media -> ViewerScreen(
                        folder = folder,
                        media = openedItem,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )

                    null -> Scaffold(
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(text = folder.title())
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
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Gallery(
                                pagingData = viewModel.gridContent,
                                state = state,
                                contentPadding = innerPadding,
                                scrollerPadding = innerPadding,
                                albumScope = AlbumScope(
                                    gridState = gridState,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                ),
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
                            BackHandler(
                                enabled = state.selectedItems.isNotEmpty()
                            ) {
                                viewModel.handleIntent(CancelSelectionMode)
                            }
                            AnimatedVisibility(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(24.dp),
                                visible = state.selectedItems.isNotEmpty(),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            ) {
                                AlbumActionMode(
                                    folder = state.folder,
                                    isRecycleBinEnabled = state.isRecycleBinEnabled,
                                    selectedItemCount = state.selectedItems.size,
                                    onCancel = { viewModel.handleIntent(CancelSelectionMode) },
                                    handleIntent = viewModel,
                                )
                            }
                        }
                    }
                }
            }
        }
        SetSystemBarsColorsEffect()
    }
}