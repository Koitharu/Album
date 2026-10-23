package org.koitharu.album.ui.search

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.ui.album.Gallery
import org.koitharu.album.ui.gallery.GalleryEffect
import org.koitharu.album.ui.gallery.galleryViewModel
import org.koitharu.album.ui.home.LocalNavBackStack
import org.koitharu.album.ui.home.NavDestination
import org.koitharu.album.util.FlowCollectEffect

@Composable
context(sharedTransitionScope: SharedTransitionScope)
fun SearchResults(
    query: String,
    contentPadding: PaddingValues,
) = Box(
    modifier = Modifier.fillMaxSize(),
) {
    val mediaQuery = MediaQuery(searchQuery = query)
    val viewModel = galleryViewModel(mediaQuery)
    val backStack = LocalNavBackStack.current
    FlowCollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is GalleryEffect.OpenMedia -> backStack.add(
                NavDestination.MediaViewer(
                    query = mediaQuery,
                    media = effect.media,
                )
            )

            is GalleryEffect.ShowError -> Unit
        }
    }
    val state by viewModel.collectState()
    with(sharedTransitionScope) {
        Gallery(
            pagingData = viewModel.gridContent,
            state = state,
            contentPadding = contentPadding,
            scrollerPadding = contentPadding,
            handleIntent = viewModel,
            emptyContent = { },
        )
    }
}