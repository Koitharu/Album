package org.koitharu.album.ui.wallpaper

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import me.saket.telephoto.zoomable.OverzoomEffect
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomLimit
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContent
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace
import me.saket.telephoto.zoomable.spatial.SpatialRect
import org.koitharu.album.R
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.ui.wallpaper.WallpaperIntent.ApplyWallpaper
import org.koitharu.album.util.IconButtonWithTooltip
import org.koitharu.album.util.times

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun WallpaperScreen(
    state: WallpaperState,
    snackbarHostState: SnackbarHostState,
    handleIntent: MviIntentHandler<WallpaperIntent>,
    onBack: () -> Unit,
) {

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.set_as_wallpaper),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                subtitle = {
                    Text(
                        text = stringResource(R.string.set_as_wallpaper_hint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButtonWithTooltip(
                        tooltip = stringResource(R.string.back),
                        onClick = onBack,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = stringResource(state.target.label),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isLoading,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        WallpaperTarget.entries.forEach { target ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(target.label)) },
                                onClick = {
                                    handleIntent(WallpaperIntent.SetTarget(target))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { handleIntent(ApplyWallpaper) },
                    enabled = !state.isLoading,
                ) {
                    Text(
                        text = stringResource(R.string.apply),
                        maxLines = 1,
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            var viewport by remember { mutableStateOf(Rect.Zero) }
            val zoomableState = rememberZoomableState(
                zoomSpec = ZoomSpec(
                    minimum = ZoomLimit(1f, OverzoomEffect.Disabled),
                )
            )
            LaunchedEffect(zoomableState.contentTransformation, viewport) {
                val visibleImageRegion = with(zoomableState.coordinateSystem) {
                    SpatialRect(viewport, CoordinateSpace.Viewport)
                        .rectIn(CoordinateSpace.ZoomableContent)
                }
                handleIntent(WallpaperIntent.SetCropRect(visibleImageRegion))
            }
            ZoomableAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = state.imageUri,
                state = rememberZoomableImageState(zoomableState),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                contentPadding = if (viewport.isEmpty) {
                    PaddingValues.Zero
                } else {
                    with(LocalDensity.current) {
                        PaddingValues.Absolute(
                            left = viewport.left.toDp(),
                            top = viewport.top.toDp(),
                            right = (viewport.right - viewport.width).toDp(),
                            bottom = (viewport.bottom - viewport.height).toDp(),
                        )
                    }
                }
            )
            val configuration = LocalConfiguration.current
            val screenRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp
            Grid(
                modifier = Modifier.fillMaxSize(),
                innerPadding = 12.dp,
                aspectRatio = screenRatio,
                onViewportChanged = { viewport = it },
            )
            AnimatedVisibility(
                modifier = Modifier.fillMaxSize(),
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun Grid(
    modifier: Modifier,
    innerPadding: Dp,
    aspectRatio: Float,
    onViewportChanged: (Rect) -> Unit
) {
    val dimColor = MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.8f)
    val insetsColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val gridColor = LocalContentColor.current
    val systemBars = WindowInsets.systemBars
    Canvas(
        modifier = modifier,
    ) {
        val canvasRatio = size.width / size.height
        val halfWidth: Float
        val halfHeight: Float
        val padding = innerPadding.toPx()
        if (canvasRatio < aspectRatio) {
            halfWidth = size.width / 2f - padding
            halfHeight = size.width / aspectRatio / 2f - padding
        } else {
            halfHeight = size.height / 2f - padding
            halfWidth = size.height * aspectRatio / 2f - padding
        }
        val center = size.center
        val rect = Rect(
            left = center.x - halfWidth,
            top = center.y - halfHeight,
            right = center.x + halfWidth,
            bottom = center.y + halfHeight,
        )
        clipRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            clipOp = ClipOp.Difference,
        ) {
            drawRect(color = dimColor)
        }
        drawRect(
            color = gridColor,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(1.dp.toPx())
        )
        val innerRect = rect * 0.6f
        drawRect(
            color = gridColor,
            topLeft = innerRect.topLeft,
            size = innerRect.size,
            style = Stroke(1.dp.toPx()),
        )
        drawLine(
            color = gridColor,
            start = rect.topCenter,
            end = rect.bottomCenter,
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = gridColor,
            start = rect.centerLeft,
            end = rect.centerRight,
            strokeWidth = 1.dp.toPx(),
        )

        val scale = rect.width / size.width
        val statusBarHeight = systemBars.getTop(this)
        if (statusBarHeight > 0) {
            val scaledHeight = statusBarHeight * scale
            drawRect(
                color = insetsColor,
                topLeft = rect.topLeft,
                size = Size(
                    width = rect.width,
                    height = scaledHeight,
                ),
            )
        }
        val navBarHeight = systemBars.getBottom(this)
        if (navBarHeight > 0) {
            val scaledHeight = navBarHeight * scale
            drawRect(
                color = insetsColor,
                topLeft = Offset(
                    x = rect.left,
                    y = rect.bottom - scaledHeight,
                ),
                size = Size(
                    width = rect.width,
                    height = scaledHeight,
                ),
            )
        }
        onViewportChanged(rect)
    }
}

@Composable
@Preview(showSystemUi = true)
private fun PreviewWallpaperScreen() = AlbumTheme {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Green.toArgb())
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        WallpaperScreen(
            state = WallpaperState(
                target = WallpaperTarget.HOME_SCREEN,
                imageUri = "stub",
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = { /* no-op */ },
            handleIntent = MviIntentHandler.NoOp,
        )
    }
}

@Composable
@Preview
private fun PreviewWallpaperScreenLoading() = AlbumTheme {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Green.toArgb())
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        WallpaperScreen(
            state = WallpaperState(
                target = WallpaperTarget.HOME_SCREEN,
                imageUri = "stub",
                isLoading = true,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = { /* no-op */ },
            handleIntent = MviIntentHandler.NoOp,
        )
    }
}

@Composable
@Preview(device = TABLET)
private fun PreviewWallpaperScreenLandscape() = AlbumTheme {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Green.toArgb())
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        WallpaperScreen(
            state = WallpaperState(
                target = WallpaperTarget.HOME_SCREEN,
                imageUri = "stub",
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = { /* no-op */ },
            handleIntent = MviIntentHandler.NoOp,
        )
    }
}