package org.koitharu.album.ui.album

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koitharu.album.ui.common.AlbumItem
import kotlin.math.min

@Composable
fun HomeScreenBanner(
    banner: AlbumItem.Image,
    albumScope: AlbumScope,
    overlayContent: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
) = with(albumScope.sharedTransitionScope) {
    ImageBanner(
        modifier = Modifier
            .fillMaxWidth()
            .sharedElement(
                rememberSharedContentState(key = banner.sharedContentKey),
                animatedVisibilityScope = albumScope.animatedVisibilityScope
            ),
        image = banner,
        gridState = albumScope.gridState,
        height = 240.dp,
        overlayContent = overlayContent,
        onOffsetChanged = { albumScope.headerOffset.value = it },
        onClick = onClick,
    )
}

@Composable
private fun ImageBanner(
    image: AlbumItem.Image,
    gridState: LazyGridState,
    height: Dp,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit,
    onOffsetChanged: (Dp) -> Unit,
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
    LaunchedEffect(currentHeaderHeight) {
        onOffsetChanged(height - currentHeaderHeight)
    }
    val collapseFraction = (maxHeightPx - scrollOffset) / maxHeightPx
    val brush =
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent))
    Box(
        modifier = Modifier
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
        Crossfade(
            modifier = Modifier
                .clickable(
                    role = Role.Image,
                    onClick = onClick,
                ),
            targetState = image,
            animationSpec = tween(transitionDuration),
        ) { targetImage ->
            AsyncImage(
                model = targetImage.uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = scrollOffset / -2f
                    }
                    .height(height),
                contentScale = ContentScale.Crop,
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
        overlayContent()
    }
}