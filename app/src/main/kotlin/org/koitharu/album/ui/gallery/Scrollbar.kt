package org.koitharu.album.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koitharu.album.R

@Composable
fun FastScroller(
    gridState: LazyGridState,
    text: String?,
    modifier: Modifier = Modifier,
) {
    var thumbOffset by remember { mutableFloatStateOf(0f) }
    var isExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .gridFastScroller(
                gridState = gridState,
                onThumbOffsetChanged = { currentOffset ->
                    thumbOffset = currentOffset
                }
            )
            .then(modifier)
    ) {
        val handleShape = RoundedCornerShape(
            topStart = 4.dp,
            bottomStart = 4.dp,
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    translationY = thumbOffset
                }
                .background(MaterialTheme.colorScheme.surface)
                .shadow(4.dp, handleShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pan_vertical),
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
            )
            AnimatedVisibility(isExpanded && text != null) {
                Text(
                    text = text.orEmpty(),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

private fun Modifier.gridFastScroller(
    gridState: LazyGridState,
    onThumbOffsetChanged: (Float) -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var trackHeight by remember { mutableFloatStateOf(0f) }

    val totalItemsCount by remember {
        derivedStateOf { gridState.layoutInfo.totalItemsCount }
    }
    val columnsCount by remember {
        derivedStateOf { gridState.layoutInfo.maxSpan }
    }

    // Calculate total rows to map scroll percentage correctly
    val totalRows by remember {
        derivedStateOf {
            if (columnsCount <= 0) 1 else (totalItemsCount + columnsCount - 1) / columnsCount
        }
    }

    // React to grid scrolling to update thumb position
    val scrollFraction by remember {
        derivedStateOf {
            val firstVisibleItem = gridState.firstVisibleItemIndex
            val firstVisibleRow = if (columnsCount <= 0) 0 else firstVisibleItem / columnsCount
            if (totalRows <= 1) 0f else firstVisibleRow.toFloat() / (totalRows - 1)
        }
    }

    // Sync state changes back to the UI block
    onThumbOffsetChanged(scrollFraction * trackHeight)

    this
        .onGloballyPositioned { coordinates ->
            trackHeight = coordinates.size.height.toFloat()
        }
        .pointerInput(totalRows, trackHeight) {
            if (trackHeight <= 0f || totalRows <= 1) return@pointerInput

            var dragAccumulator = 0f
            detectVerticalDragGestures(
                onDragStart = { offset ->
                    dragAccumulator = offset.y
                },
                onDragEnd = { dragAccumulator = 0f },
                onDragCancel = { dragAccumulator = 0f },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    dragAccumulator = (dragAccumulator + dragAmount).coerceIn(0f, trackHeight)

                    // Map drag position to the target row index
                    val rawFraction = dragAccumulator / trackHeight
                    val targetRow =
                        (rawFraction * (totalRows - 1)).toInt().coerceIn(0, totalRows - 1)
                    val targetItemIndex = targetRow * columnsCount

                    coroutineScope.launch {
                        gridState.scrollToItem(targetItemIndex)
                    }
                }
            )
        }
}
