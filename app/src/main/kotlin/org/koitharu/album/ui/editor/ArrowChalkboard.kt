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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import org.koitharu.album.model.DrawPrimitive.Arrow

@Composable
fun ArrowChalkboard(
    modifier: Modifier,
    currentColor: Color,
    lineThickness: Dp,
    arrow: Arrow?,
    onArrowDrawn: (Arrow) -> Unit,
) = BoxWithConstraints(modifier = modifier) {
    val boxSize = with(LocalDensity.current) {
        Size(
            width = maxWidth.toPx(),
            height = maxHeight.toPx(),
        )
    }
    var currentArrow by remember {
        mutableStateOf<Arrow?>(null)
    }
    LaunchedEffect(arrow, boxSize) {
        currentArrow = arrow?.scaled(
            scaleX = boxSize.width,
            scaleY = boxSize.height,
        )
    }
    val lineHeight = arrow?.lineHeight ?: with(LocalDensity.current) {
        lineThickness.toPx()
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(arrow, currentColor) {
                detectDragGestures(
                    onDragStart = { point ->
                        currentArrow = Arrow(
                            start = point,
                            end = point,
                            color = currentColor.toArgb(),
                            lineHeight = lineHeight,
                        )
                    },
                    onDrag = { _, delta ->
                        currentArrow = currentArrow?.moveEnd(
                            delta = delta,
                            maxX = boxSize.width,
                            maxY = boxSize.height,
                        )
                    },
                    onDragCancel = {
                        currentArrow = arrow?.scaled(
                            scaleX = boxSize.width,
                            scaleY = boxSize.height,
                        )
                    },
                    onDragEnd = {
                        currentArrow?.let {
                            onArrowDrawn(
                                it.scaled(
                                    scaleX = 1f / boxSize.width,
                                    scaleY = 1f / boxSize.height,
                                )
                            )
                        }
                    }
                )
            },
    ) {
        currentArrow?.let {
            val path = it.toPath(1f, 1f)
                .asComposePath()
            drawPath(
                path = path,
                color = Color(it.color),
                style = Stroke(
                    width = it.lineHeight,
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}
