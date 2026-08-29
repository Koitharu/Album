package org.koitharu.album.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import org.koitharu.album.util.div
import kotlin.math.roundToInt

@Composable
fun CropGrid(
    modifier: Modifier,
    aspectRatio: Float,
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
    val scope = rememberCoroutineScope()
    val currentFrame = remember {
        Animatable(
            initialValue = FrameOffset.Zero,
            typeConverter = FrameOffset.vectorConverter,
        )
    }
    LaunchedEffect(frame, boxSize, aspectRatio) {
        currentFrame.animateTo(
            frame.scaleToSize(Size(1f, 1f), boxSize)
                .withAspectRatio(boxSize, aspectRatio)
        )
    }
    val onDragEnd = {
        onFrameChanged(
            currentFrame.targetValue.scaleToSize(boxSize, Size(1f, 1f)),
        )
    }
    val bounds = remember(currentFrame.value, boxSize) {
        Rect(
            top = currentFrame.value.top,
            left = currentFrame.value.left,
            right = boxSize.width - currentFrame.value.right,
            bottom = boxSize.height - currentFrame.value.bottom,
        )
    }
    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
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
            strokeWidth = strokeWidth,
        )
        // left
        drawLine(
            color = lineColor,
            start = bounds.topLeft,
            end = bounds.bottomLeft,
            strokeWidth = strokeWidth,
        )
        // right
        drawLine(
            color = lineColor,
            start = bounds.topRight,
            end = bounds.bottomRight,
            strokeWidth = strokeWidth,
        )
        // bottom
        drawLine(
            color = lineColor,
            start = bounds.bottomLeft,
            end = bounds.bottomRight,
            strokeWidth = strokeWidth,
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
            strokeWidth = strokeWidth,
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
            strokeWidth = strokeWidth,
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
            strokeWidth = strokeWidth,
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
            strokeWidth = strokeWidth,
        )
    }
    // top left
    CornerHandle(
        color = lineColor,
        position = bounds.topLeft,
        onDragEnd = onDragEnd,
    ) { change ->
        scope.launch {
            currentFrame.snapTo(
                currentFrame.value.moveTopLeftConstrained(
                    delta = change,
                    bounds = boxSize,
                    aspectRatio = aspectRatio,
                )
            )
        }
    }
    // top right
    CornerHandle(
        color = lineColor,
        position = bounds.topRight,
        onDragEnd = onDragEnd,
    ) { change ->
        scope.launch {
            currentFrame.snapTo(
                currentFrame.value.moveTopRightConstrained(
                    delta = change,
                    bounds = boxSize,
                    aspectRatio = aspectRatio,
                )
            )
        }
    }
    // bottom left
    CornerHandle(
        color = lineColor,
        position = bounds.bottomLeft,
        onDragEnd = onDragEnd,
    ) { change ->
        scope.launch {
            currentFrame.snapTo(
                currentFrame.value.moveBottomLeftConstrained(
                    delta = change,
                    bounds = boxSize,
                    aspectRatio = aspectRatio,
                )
            )
        }
    }
    // bottom right
    CornerHandle(
        color = lineColor,
        position = bounds.bottomRight,
        onDragEnd = onDragEnd,
    ) { change ->
        scope.launch {
            currentFrame.snapTo(
                currentFrame.value.moveBottomRightConstrained(
                    delta = change,
                    bounds = boxSize,
                    aspectRatio = aspectRatio,
                )
            )
        }
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