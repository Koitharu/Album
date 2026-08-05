package org.koitharu.album.ui.viewer

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.memory.MemoryCache
import org.koitharu.album.ui.album.AlbumItem

@Composable
fun VideoViewer(
    modifier: Modifier,
    video: AlbumItem.Video,
    innerPadding: PaddingValues,
    isActive: Boolean,
    onClick: () -> Unit,
) = Box(
    modifier = modifier.fillMaxSize(),
) {
    var isOverlayVisible by remember { mutableStateOf(true) }
    VideoPlayer(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        uri = video.uri.toString(),
        onReady = { isOverlayVisible = false }
    )
    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = isOverlayVisible
    ) {
        val context = LocalPlatformContext.current
        val image = remember {
            SingletonImageLoader.get(context)
                .memoryCache
                ?.get(MemoryCache.Key(video.memoryCacheKey))
                ?.image
                ?.asPainter(context)
        }
        if (image != null) {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                painter = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun VideoPlayer(
    uri: String,
    modifier: Modifier,
    onReady: () -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri.toUri())
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setOnPreparedListener {
                    start()
                    onReady()
                }
            }
        },
        onRelease = {
            it.stopPlayback()
        },
    )
}