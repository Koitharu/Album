package org.koitharu.album.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.CancelSelectionMode
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.folders.FolderItem

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
        val statusBarHeight = with(LocalDensity.current) {
            WindowInsets.statusBars.getTop(this).toDp()
        }
        val navBarHeight = with(LocalDensity.current) {
            WindowInsets.navigationBars.getTop(this).toDp()
        }
        Gallery(
            pagingData = viewModel.gridContent,
            state = state,
            contentPadding = innerPadding,
            scrollerPadding = PaddingValues(
                top = (innerPadding.calculateTopPadding() - albumScope.headerOffset.value)
                    .coerceAtLeast(statusBarHeight),
                bottom = innerPadding.calculateBottomPadding().coerceAtLeast(navBarHeight),
            ),
            albumScope = albumScope,
            handleIntent = viewModel,
            emptyContent = {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    iconResId = R.drawable.ic_album,
                    title = stringResource(R.string.no_media),
                    message = stringResource(R.string.no_media_message),
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
                .navigationBarsPadding()
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
