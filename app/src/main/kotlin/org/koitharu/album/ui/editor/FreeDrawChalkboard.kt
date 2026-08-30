package org.koitharu.album.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.koitharu.album.model.DrawPrimitive.FreePath
import org.koitharu.album.util.scaled

@Composable
fun FreeDrawChalkboard(
    modifier: Modifier,
    currentColor: Color,
    lineThickness: Dp,
    path: FreePath?,
    onPathDrawn: (FreePath) -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    val boxSize = with(LocalDensity.current) {
        Size(
            width = maxWidth.toPx(),
            height = maxHeight.toPx(),
        )
    }
    var pathBuilder by remember {
        mutableStateOf(FreePathBuilder())
    }
    LaunchedEffect(path, boxSize) {
        pathBuilder = FreePathBuilder(
            path?.scaled(
                scaleX = boxSize.width,
                scaleY = boxSize.height,
            )?.path ?: Path()
        )
    }
    val lineHeight = path?.lineHeight ?: with(LocalDensity.current) {
        lineThickness.toPx()
    }
    val color = path?.let { Color(it.color) } ?: currentColor
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(path) {
                detectDragGestures(
                    onDragStart = { point ->
                        pathBuilder = pathBuilder.moveTo(point.x, point.y)
                    },
                    onDrag = { _, delta ->
                        pathBuilder = pathBuilder.lineBy(delta.x, delta.y)
                    },
                    onDragCancel = {
                        pathBuilder = FreePathBuilder(
                            path?.scaled(
                                scaleX = boxSize.width,
                                scaleY = boxSize.height,
                            )?.path ?: Path()
                        )
                    },
                    onDragEnd = {
                        val result = pathBuilder.toPath().scaled(
                            scaleX = 1f / boxSize.width,
                            scaleY = 1f / boxSize.height,
                        )
                        onPathDrawn(FreePath(result, color.toArgb(), lineHeight))
                    }
                )
            },
    ) {
        drawPath(
            path = pathBuilder.toPath(),
            color = color,
            style = Stroke(
                width = lineHeight,
                cap = StrokeCap.Round,
            ),
        )
    }
}
