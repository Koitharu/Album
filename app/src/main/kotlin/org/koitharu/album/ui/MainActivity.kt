package org.koitharu.album.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.album.AlbumContent
import org.koitharu.album.ui.album.AlbumItem.Media
import org.koitharu.album.ui.album.AlbumScope
import org.koitharu.album.ui.album.AlbumViewModel
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.util.rememberPermissionCheck
import org.koitharu.album.ui.viewer.ViewerScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbumTheme {
                val isPermissionGranted by
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionCheck(
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                } else {
                    remember { mutableStateOf(true) }
                }
                if (isPermissionGranted) {
                    GalleryScreen(albumId = null)
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(
    albumId: String?,
) = SharedTransitionLayout {
    val gridState = rememberLazyGridState()
    val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = albumId,
    ) {
        it.create(albumId)
    }
    val state by viewModel.collectState()
    AnimatedContent(state.openedItem) { openedItem ->
        when (openedItem) {
            is Media -> ViewerScreen(
                albumId = albumId,
                media = openedItem,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@AnimatedContent,
            )

            null -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                AlbumContent(
                    albumId = albumId,
                    innerPadding = innerPadding,
                    albumScope = AlbumScope(
                        gridState = gridState,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                )
            }
        }
    }
}