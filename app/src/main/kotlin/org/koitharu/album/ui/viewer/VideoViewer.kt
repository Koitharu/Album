package org.koitharu.album.ui.viewer

import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.memory.MemoryCache
import kotlinx.coroutines.launch
import org.koitharu.album.R
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.formatTimeSeconds
import kotlin.math.roundToInt

@Composable
fun VideoViewer(
    modifier: Modifier,
    video: AlbumItem.Video,
    contentPadding: PaddingValues,
    isUiVisible: Boolean,
    onClick: () -> Unit,
) = Box(
    modifier = modifier
        .clickable(
            onClick = onClick,
            indication = null,
            interactionSource = null,
        ),
) {
    var isOverlayVisible by remember { mutableStateOf(true) }
    VideoPlayer(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = contentPadding,
        uri = video.uri.toString(),
        isUiVisible = isUiVisible,
        onComplete = {
            if (!isUiVisible) {
                onClick()
            }
        },
        onReady = { isOverlayVisible = false }
    )
    AnimatedVisibility(
        modifier = Modifier.fillMaxSize(),
        visible = isOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut(),
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
    contentPadding: PaddingValues,
    isUiVisible: Boolean,
    onComplete: () -> Unit,
    onReady: () -> Unit,
) = Box(
    modifier = modifier,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var pendingSeek by remember { mutableIntStateOf(0) }
    val position = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri.toUri())
                setOnPreparedListener {
                    duration = it.duration
                    seekTo(0)
                    onReady()
                }
                setOnCompletionListener {
                    isPlaying = false
                    onComplete()
                }
                val positionUpdater = object : Runnable {
                    override fun run() {
                        scope.launch {
                            position.animateTo(
                                targetValue = currentPosition.toFloat(),
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = LinearEasing,
                                ),
                            )
                        }
                        postDelayed(this, 200)
                    }
                }
                post(positionUpdater)
                tag = positionUpdater
            }
        },
        onRelease = { videoView ->
            videoView.stopPlayback()
            (videoView.tag as? Runnable)?.let {
                videoView.removeCallbacks(it)
            }
            videoView.tag = null
        },
        update = { videoView ->
            if (isPlaying) {
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            } else {
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            }
            if (pendingSeek >= 0) {
                videoView.seekTo(pendingSeek)
                pendingSeek = -1
            }
        }
    )
    AnimatedVisibility(
        visible = isUiVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        VideoControls(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            isPlaying = isPlaying,
            onPlayPauseClick = { isPlaying = !isPlaying },
            duration = duration,
            position = position.value,
            onSeek = {
                scope.launch {
                    position.snapTo(it.toFloat())
                    pendingSeek = it
                }
            },
        )
    }
}

@Composable
private fun VideoControls(
    modifier: Modifier,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    duration: Int,
    position: Float,
    onSeek: (Int) -> Unit,
) = Box(
    modifier = modifier,
) {
    IconButton(
        modifier = Modifier
            .size(64.dp)
            .align(Alignment.Center),
        onClick = onPlayPauseClick,
    ) {
        val iconTint = MaterialTheme.colorScheme.primaryFixedDim.copy(alpha = 0.6f)
        AnimatedContent(
            modifier = Modifier,
            targetState = isPlaying,
            contentAlignment = Alignment.Center,
        ) { playing ->
            if (playing) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_pause_circle),
                    contentDescription = stringResource(R.string.pause),
                    tint = iconTint,
                )
            } else {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_play_circle),
                    contentDescription = stringResource(R.string.play),
                    tint = iconTint,
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    ) {
        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            value = position,
            onValueChange = { onSeek(it.fastRoundToInt()) },
            valueRange = 0f..duration.toFloat(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = remember(position) {
                    position.roundToInt().formatTimeSeconds()
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = remember(duration) {
                    duration.formatTimeSeconds()
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewVideoControls() = AlbumTheme {
    VideoControls(
        modifier = Modifier.fillMaxSize(),
        isPlaying = false,
        onPlayPauseClick = { /* no-op */ },
        duration = 64000,
        position = 12000f,
        onSeek = { /* no-op */ },
    )
}