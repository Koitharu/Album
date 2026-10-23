package org.koitharu.album.ui.search

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.ui.album.AlbumActionMode
import org.koitharu.album.ui.album.Gallery
import org.koitharu.album.ui.common.SetSystemBarsColorsEffect
import org.koitharu.album.ui.gallery.GalleryEffect
import org.koitharu.album.ui.gallery.GalleryIntent.CancelSelectionMode
import org.koitharu.album.ui.gallery.GalleryIntent.UpdateSearchQuery
import org.koitharu.album.ui.gallery.galleryViewModel
import org.koitharu.album.ui.home.LocalNavBackStack
import org.koitharu.album.ui.home.NavDestination
import org.koitharu.album.util.FlowCollectEffect

@Composable
context(sharedTransitionScope: SharedTransitionScope)
fun SearchScreen(
    initialQuery: String
) {
    val viewModel = galleryViewModel(MediaQuery(searchQuery = initialQuery))
    val state by viewModel.collectState()
    val backStack = LocalNavBackStack.current
    FlowCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is GalleryEffect.OpenMedia -> backStack.add(
                NavDestination.MediaViewer(
                    query = state.query,
                    media = effect.media,
                )
            )

            is GalleryEffect.ShowError -> TODO()
        }
    }
    Scaffold(
        topBar = {
            with(sharedTransitionScope) {
                SearchBar(
                    modifier = Modifier
                        .statusBarsPadding(),
                    text = state.query.searchQuery.orEmpty(),
                    onTextChanged = { viewModel.handleIntent(UpdateSearchQuery(it)) },
                    onCollapse = { backStack.removeLastOrNull() },
                )
            }
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
                    emptyContent = { },
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
                    query = state.query,
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