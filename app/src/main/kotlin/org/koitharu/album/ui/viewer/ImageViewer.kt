package org.koitharu.album.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowInsetsCompat
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import org.koitharu.album.R
import org.koitharu.album.ui.gallery.GalleryItem
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.util.rememberWindowInsetsController

@Composable
fun ImageViewer(
    item: GalleryItem.Image,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit,
) = AlbumTheme(darkTheme = true) {
    Scaffold { innerPadding ->
        val insetsController = rememberWindowInsetsController()
        var isUiVisible by remember { mutableStateOf(true) }
        LaunchedEffect(isUiVisible) {
            if (isUiVisible) {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            BackHandler(onBack = onClose)
            with(sharedTransitionScope) {
                ZoomableAsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = "image_${item.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.uri)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .placeholderMemoryCacheKey(item.memoryCacheKey)
                        .build(),
                    contentPadding = innerPadding,
                    contentDescription = null,
                    onClick = { isUiVisible = !isUiVisible },
                )
            }
            AnimatedVisibility(
                visible = isUiVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current)
                        ),
                    onClick = onClose,
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_back), null)
                }
            }
        }
    }
}