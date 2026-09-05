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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.asPainter
import coil3.memory.MemoryCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.koitharu.album.R
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.formatTimeSeconds
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun VideoViewer(
    modifier: Modifier,
    video: AlbumItem.Video,
    contentPadding: PaddingValues,
    isUiVisible: Boolean,
    startMuted: Boolean,
    setUiVisible: (Boolean) -> Unit,
) = Box(
    modifier = modifier
        .clickable(
            onClick = { setUiVisible(!isUiVisible) },
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
        helperText = video.mimeType.substringAfterLast('/'),
        startMuted = startMuted,
        isUiVisible = isUiVisible,
        setUiVisible = setUiVisible,
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
    helperText: String,
    contentPadding: PaddingValues,
    isUiVisible: Boolean,
    startMuted: Boolean,
    onReady: () -> Unit,
    setUiVisible: (Boolean) -> Unit,
) = Box(
    modifier = modifier,
) {
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var pendingSeek by remember { mutableIntStateOf(0) }
    val volumeController = remember { VolumeController(startMuted) }
    val position = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(isPlaying) {
        snapshotFlow {
            isPlaying
        }.transformLatest {
            if (it) {
                delay(2.seconds)
                emit(false)
            } else {
                emit(true)
            }
        }.collect {
            setUiVisible(it)
        }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(uri.toUri())
                setOnPreparedListener {
                    duration = it.duration
                    volumeController.setMediaPlayer(it)
                    seekTo(0)
                    onReady()
                }
                setOnCompletionListener {
                    isPlaying = false
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
            volumeController.setMediaPlayer(null)
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
            helperText = helperText,
            onPlayPauseClick = { isPlaying = !isPlaying },
            duration = duration,
            position = position.value,
            volume = volumeController.floatValue,
            onMuteUnmuteClick = {
                volumeController.muteOrUnmute()
            },
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
    helperText: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onMuteUnmuteClick: () -> Unit,
    volume: Float,
    duration: Int,
    position: Float,
    onSeek: (Int) -> Unit,
) = Box(
    modifier = modifier,
) {
    val controlsBackground = MaterialTheme.colorScheme.background.copy(alpha = 0.74f)
    IconButton(
        modifier = Modifier
            .size(82.dp)
            .background(
                color = controlsBackground,
                shape = CircleShape,
            )
            .align(Alignment.Center),
        onClick = onPlayPauseClick,
    ) {
        AnimatedContent(
            modifier = Modifier
                .padding(12.dp),
            targetState = isPlaying,
            contentAlignment = Alignment.Center,
        ) { playing ->
            if (playing) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_pause_circle),
                    contentDescription = stringResource(R.string.pause),
                )
            } else {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_play_circle),
                    contentDescription = stringResource(R.string.play),
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(
                color = controlsBackground,
                shape = MaterialTheme.shapes.large,
            )
            .align(Alignment.BottomCenter),
    ) {
        Row(
            modifier = Modifier
                .padding(
                    top = 12.dp,
                    start = 12.dp,
                    end = 12.dp,
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier
                    .weight(1f),
                value = position,
                onValueChange = { onSeek(it.fastRoundToInt()) },
                valueRange = 0f..duration.toFloat(),
            )
            Spacer(
                modifier = Modifier.width(8.dp)
            )
            IconButton(
                onClick = onMuteUnmuteClick,
            ) {
                AnimatedContent(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp),
                    targetState = volume <= VolumeController.MUTE_THRESHOLD,
                    contentAlignment = Alignment.Center,
                ) { isMuted ->
                    if (isMuted) {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.ic_volume_off),
                            contentDescription = stringResource(R.string.unmute),
                        )
                    } else {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.ic_volume_on),
                            contentDescription = stringResource(R.string.mute),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = remember(position) {
                    position.roundToInt().formatTimeSeconds()
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = remember(duration) {
                    duration.formatTimeSeconds()
                },
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(
                modifier = Modifier.width(8.dp)
            )
            Text(
                modifier = Modifier
                    .width(46.dp),
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
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
        onMuteUnmuteClick = { /* no-op */ },
        volume = 0f,
        helperText = "mp4",
    )
}