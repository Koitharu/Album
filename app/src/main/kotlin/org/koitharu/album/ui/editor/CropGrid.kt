package org.koitharu.album.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.koitharu.album.util.coerceAtLeast
import org.koitharu.album.util.div
import org.koitharu.album.util.toFrameOffset
import kotlin.math.roundToInt

@Composable
fun CropGrid(
    modifier: Modifier,
    contentPadding: PaddingValues,
    lineColor: Color,
    dimColor: Color,
    frame: FrameOffset,
    onFrameChanged: (FrameOffset) -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    val density = LocalDensity.current
    val boxSize = with(density) {
        Size(
            width = maxWidth.toPx(),
            height = maxHeight.toPx(),
        )
    }
    val scaledFrame = remember(frame, boxSize) {
        frame.scaleToSize(Size(1f, 1f), boxSize)
    }
    var topLeft by remember(scaledFrame) {
        mutableStateOf(scaledFrame.topLeft)
    }
    var bottomRight by remember(scaledFrame) {
        mutableStateOf(scaledFrame.bottomRight)
    }
    val padding = contentPadding.toFrameOffset()
    val onDragEnd = {
        onFrameChanged(
            FrameOffset(topLeft = topLeft, bottomRight = bottomRight)
                .scaleToSize(boxSize, Size(1f, 1f)),
        )
    }
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val bounds = Rect(
            top = topLeft.y + padding.top,
            left = topLeft.x + padding.left,
            right = size.width - bottomRight.x - padding.right,
            bottom = boxSize.height - bottomRight.y - padding.bottom,
        )
        val innerBounds = bounds / 3f
        val strokeWidth = 1.dp.toPx()
        // dim
        clipRect(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom,
            clipOp = ClipOp.Difference,
        ) {
            drawRect(color = dimColor)
        }
        // top
        drawLine(
            color = lineColor,
            start = bounds.topLeft,
            end = bounds.topRight,
            strokeWidth = strokeWidth
        )
        // left
        drawLine(
            color = lineColor,
            start = bounds.topLeft,
            end = bounds.bottomLeft,
            strokeWidth = strokeWidth
        )
        // right
        drawLine(
            color = lineColor,
            start = bounds.topRight,
            end = bounds.bottomRight,
            strokeWidth = strokeWidth
        )
        // bottom
        drawLine(
            color = lineColor,
            start = bounds.bottomLeft,
            end = bounds.bottomRight,
            strokeWidth = strokeWidth
        )
        // inner left
        drawLine(
            color = lineColor,
            start = Offset(
                x = innerBounds.left,
                y = bounds.top,
            ),
            end = Offset(
                x = innerBounds.left,
                y = bounds.bottom,
            ),
            strokeWidth = strokeWidth
        )
        // inner top
        drawLine(
            color = lineColor,
            start = Offset(
                x = bounds.left,
                y = innerBounds.top,
            ),
            end = Offset(
                x = bounds.right,
                y = innerBounds.top,
            ),
            strokeWidth = strokeWidth
        )
        // inner right
        drawLine(
            color = lineColor,
            start = Offset(
                x = innerBounds.right,
                y = bounds.top,
            ),
            end = Offset(
                x = innerBounds.right,
                y = bounds.bottom,
            ),
            strokeWidth = strokeWidth
        )
        // inner bottom
        drawLine(
            color = lineColor,
            start = Offset(
                x = bounds.left,
                y = innerBounds.bottom,
            ),
            end = Offset(
                x = bounds.right,
                y = innerBounds.bottom,
            ),
            strokeWidth = strokeWidth
        )
    }
    // top left
    CornerHandle(
        color = lineColor,
        position = topLeft + padding.topLeft,
        onDragEnd = onDragEnd
    ) { change ->
        topLeft = (topLeft + change).coerceAtLeast(Offset.Zero)
    }
    // top right
    CornerHandle(
        color = lineColor,
        position = Offset(
            x = boxSize.width - bottomRight.x - padding.right,
            y = topLeft.y + padding.top,
        ),
        onDragEnd = onDragEnd
    ) { change ->
        topLeft = topLeft.copy(y = (topLeft.y + change.y).coerceAtLeast(0f))
        bottomRight = bottomRight.copy(x = (bottomRight.x - change.x).coerceAtLeast(0f))
    }
    // bottom left
    CornerHandle(
        color = lineColor,
        position = Offset(
            x = topLeft.x + padding.left,
            y = boxSize.height - bottomRight.y - padding.bottom,
        ),
        onDragEnd = onDragEnd
    ) { change ->
        topLeft = topLeft.copy(x = (topLeft.x + change.x).coerceAtLeast(0f))
        bottomRight = bottomRight.copy(y = (bottomRight.y - change.y).coerceAtLeast(0f))
    }
    // bottom right
    CornerHandle(
        color = lineColor,
        position = Offset(
            x = boxSize.width - bottomRight.x - padding.right,
            y = boxSize.height - bottomRight.y - padding.bottom,
        ),
        onDragEnd = onDragEnd
    ) { change ->
        bottomRight = (bottomRight - change).coerceAtLeast(Offset.Zero)
    }
}

@Composable
private fun CornerHandle(
    color: Color,
    position: Offset,
    onDragEnd: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (position.x - 12.dp.toPx()).roundToInt(),
                    (position.y - 12.dp.toPx()).roundToInt()
                )
            }
            .size(24.dp)
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = onDragEnd) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
            .background(color, shape = CircleShape)
    )
}

private fun FrameOffset.scaleToSize(
    baseSize: Size,
    targetSize: Size,
): FrameOffset {
    val xScale = targetSize.width / baseSize.width
    val yScale = targetSize.height / baseSize.height
    return FrameOffset(
        left = left * xScale,
        top = top * yScale,
        right = right * xScale,
        bottom = bottom * yScale,
    )
}