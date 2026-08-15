package org.koitharu.album.ui.album

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import org.koitharu.album.R
import org.koitharu.album.ui.album.AlbumIntent.OpenMedia
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.util.SetSystemBarsColorsEffect
import kotlin.math.min

@Composable
fun HomeScreenBanner(
    isExpanded: Boolean,
    albumScope: AlbumScope,
    overlayContent: @Composable (Modifier, Color) -> Unit,
) = with(albumScope.sharedTransitionScope) {
    val viewModel = hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(
        key = null
    ) {
        it.create(null)
    }
    val state by viewModel.collectState()
    val banner = state.banner
    if (banner == null || !isExpanded) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = { overlayContent(Modifier, LocalContentColor.current) },
        )
    } else {
        ImageBanner(
            modifier = Modifier
                .fillMaxWidth()
                .sharedElement(
                    rememberSharedContentState(key = "image_${state.banner?.id}"),
                    animatedVisibilityScope = albumScope.animatedVisibilityScope
                ),
            image = banner,
            gridState = albumScope.gridState,
            height = 240.dp,
            overlayContent = overlayContent,
            onClick = { viewModel.handleIntent(OpenMedia(banner)) }
        )
    }
}

@Composable
private fun ImageBanner(
    image: AlbumItem.Image,
    gridState: LazyGridState,
    height: Dp,
    modifier: Modifier = Modifier,
    overlayContent: @Composable (Modifier, Color) -> Unit,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val maxHeightPx = with(density) { height.toPx() }
    val scrollOffset by remember {
        derivedStateOf {
            val firstItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null || firstItem.index > 0) {
                maxHeightPx
            } else {
                min(maxHeightPx, -firstItem.offset.y.toFloat())
            }
        }
    }
    val currentHeaderHeight = with(density) { (maxHeightPx - scrollOffset).toDp() }
    val collapseFraction = (maxHeightPx - scrollOffset) / maxHeightPx
    val brush =
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent))
    if (currentHeaderHeight > 0.dp) {
        Box(
            modifier = Modifier
                .clickable(
                    role = Role.Image,
                    onClick = onClick,
                )
                .graphicsLayer {
                    clip = true
                    alpha = collapseFraction * collapseFraction
                    shape = GenericShape { size, _ ->
                        addRect(
                            Rect(
                                top = 0f,
                                left = 0f,
                                right = size.width,
                                bottom = maxHeightPx - scrollOffset
                            )
                        )
                    }
                }
                .then(modifier),
        ) {
            val transitionDuration = integerResource(android.R.integer.config_longAnimTime)
            AnimatedContent(
                targetState = image,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(transitionDuration)) togetherWith fadeOut(tween(transitionDuration))
                },
            ) { targetImage ->
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = scrollOffset / -2f
                        }
                        .height(height),
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(targetImage.uri)
                        .build(),
                    contentDescription = targetImage.name,
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp.coerceAtMost(currentHeaderHeight)),
                onDraw = {
                    drawRect(brush)
                }
            )
            overlayContent(
                Modifier.align(Alignment.TopEnd),
                MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}