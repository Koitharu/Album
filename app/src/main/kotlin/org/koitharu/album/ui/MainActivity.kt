package org.koitharu.album.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.album.ui.gallery.Gallery
import org.koitharu.album.ui.gallery.GalleryItem
import org.koitharu.album.ui.gallery.GalleryItem.Image
import org.koitharu.album.ui.gallery.GalleryViewModel
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.util.rememberPermissionCheck
import org.koitharu.album.ui.viewer.ImageViewer

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
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    )
                } else {
                    remember { mutableStateOf(true) }
                }
                if (isPermissionGranted) {
                    GalleryScreen()
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen() = SharedTransitionLayout {
    var selectedItem by remember { mutableStateOf<GalleryItem.Media?>(null) }
    val gridState = rememberLazyGridState()
    AnimatedContent(selectedItem) { openedItem ->
        when (openedItem) {
            is Image -> ImageViewer(
                item = openedItem,
                animatedVisibilityScope = this@AnimatedContent,
                sharedTransitionScope = this@SharedTransitionLayout,
                onClose = { selectedItem = null }
            )

            null -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val viewModel = hiltViewModel<GalleryViewModel>()
                Gallery(
                    pagingData = viewModel.content,
                    contentPadding = innerPadding,
                    gridState = gridState,
                    onImageClick = { selectedItem = it },
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout
                )
            }
        }
    }
}