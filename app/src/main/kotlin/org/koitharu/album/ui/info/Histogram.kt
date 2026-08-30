package org.koitharu.album.ui.info

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun Histogram(
    histogram: HistogramData,
    color: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = histogram.maxValue.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        if (!histogram.isEmpty) {
            val barWidth = size.width / histogram.size

            for (index in 0 until histogram.size) {
                val count = histogram[index]
                val barHeight = (count.toFloat() / maxVal) * size.height
                drawRect(
                    color = color,
                    topLeft = Offset(index * barWidth, size.height - barHeight),
                    size = Size(barWidth.coerceAtLeast(1f), barHeight)
                )
            }
        }
    }
}