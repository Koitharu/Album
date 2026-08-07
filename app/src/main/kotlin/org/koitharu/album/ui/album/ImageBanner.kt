package org.koitharu.album.ui.album

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.preferEndFirstIntrinsicSize
import coil3.compose.useExistingImageAsPlaceholder
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.koitharu.album.ui.common.AlbumItem
import kotlin.math.min

@Composable
fun ImageBanner(
    image: AlbumItem.Image?,
    gridState: LazyGridState,
    height: Dp,
    modifier: Modifier = Modifier,
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
                    enabled = image != null,
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
            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = scrollOffset / -2f
                    }
                    .height(height),
                contentScale = ContentScale.Crop,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image?.uri)
                    .crossfade(2_000)
                    .useExistingImageAsPlaceholder(true)
                    .preferEndFirstIntrinsicSize(true)
                    .build(),
                contentDescription = image?.name,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp.coerceAtMost(currentHeaderHeight)),
                onDraw = {
                    drawRect(brush)
                }
            )
        }
    }
}