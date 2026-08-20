package org.koitharu.album.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import org.koitharu.album.ui.editor.FrameOffset

operator fun Rect.times(factor: Float): Rect {
    val c = center
    val hw = width / 2f * factor
    val hh = height / 2f * factor
    return Rect(c.x - hw, c.y - hh, c.x + hw, c.y + hh)
}

operator fun Rect.div(factor: Float): Rect {
    val c = center
    val hw = width / 2f / factor
    val hh = height / 2f / factor
    return Rect(c.x - hw, c.y - hh, c.x + hw, c.y + hh)
}

@Composable
fun PaddingValues.toFrameOffset(): FrameOffset {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    return remember(layoutDirection, density, this) {
        with(density) {
            FrameOffset(
                top = calculateTopPadding().toPx(),
                left = calculateLeftPadding(layoutDirection).toPx(),
                right = calculateRightPadding(layoutDirection).toPx(),
                bottom = calculateBottomPadding().toPx(),
            )
        }
    }
}

fun Offset.coerceAtLeast(minimumValue: Offset) = if (
    x >= minimumValue.x && y >= minimumValue.y
) {
    this
} else {
    Offset(
        x = this.x.coerceAtLeast(minimumValue.x),
        y = this.y.coerceAtLeast(minimumValue.y),
    )
}
