package org.koitharu.album.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import org.koitharu.album.R
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.ui.common.EmptyState
import org.koitharu.album.ui.gallery.GalleryEffect
import org.koitharu.album.ui.gallery.GalleryIntent.CancelSelectionMode
import org.koitharu.album.ui.gallery.galleryViewModel
import org.koitharu.album.ui.home.LocalNavBackStack
import org.koitharu.album.ui.home.NavDestination
import org.koitharu.album.util.FlowCollectEffect

@Composable
context(sharedTransitionScope: SharedTransitionScope)
fun AlbumContent(
    query: MediaQuery,
    title: String?,
    innerPadding: PaddingValues,
    gridState: LazyGridState,
    headerOffset: Dp,
) {
    val viewModel = galleryViewModel(query)
    val backStack = LocalNavBackStack.current
    FlowCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is GalleryEffect.OpenMedia -> backStack.add(
                NavDestination.MediaViewer(
                    query = query,
                    media = effect.media,
                )
            )

            is GalleryEffect.ShowError -> Unit
        }
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
            gridState = gridState,
            scrollerPadding = PaddingValues(
                top = (innerPadding.calculateTopPadding() - headerOffset)
                    .coerceAtLeast(statusBarHeight),
                bottom = innerPadding.calculateBottomPadding().coerceAtLeast(navBarHeight),
            ),
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
                query = state.query,
                isRecycleBinEnabled = state.isRecycleBinEnabled,
                selectedItemCount = state.selectedItems.size,
                onCancel = { viewModel.handleIntent(CancelSelectionMode) },
                handleIntent = viewModel,
            )
        }
    }
}
