package org.koitharu.album.ui.editor

import androidx.annotation.CheckResult
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

@Immutable
data class FrameOffset(
    @Stable val left: Float,
    @Stable val top: Float,
    @Stable val right: Float,
    @Stable val bottom: Float,
) {

    constructor(
        topLeft: Offset,
        bottomRight: Offset,
    ) : this(
        left = topLeft.x,
        top = topLeft.y,
        right = bottomRight.x,
        bottom = bottomRight.y,
    )

    @Stable
    val topLeft: Offset
        get() = Offset(left, top)

    @Stable
    val bottomRight: Offset
        get() = Offset(right, bottom)

    @Stable
    val bottomLeft: Offset
        get() = Offset(left, bottom)

    @Stable
    val topRight: Offset
        get() = Offset(right, top)

    override fun toString(): String {
        return "→$left ↓$top ←$right ↑$bottom"
    }

    @CheckResult
    fun withAspectRatio(
        bounds: Size,
        aspectRatio: Float // w/h
    ): FrameOffset {
        if (aspectRatio.isNaN()) {
            return this
        }
        val rect = Rect(
            left = left,
            top = top,
            right = bounds.width - right,
            bottom = bounds.height - bottom,
        )
        val boundsRatio = bounds.width / bounds.height
        val c = rect.center
        val w = rect.width
        val h = rect.height
        val hw: Float
        val hh: Float
        if (boundsRatio > aspectRatio) {
            hw = (if (h < w) w else h * aspectRatio) / 2f
            hh = (if (h < w) w / aspectRatio else h) / 2f
        } else {
            hw = (if (h > w) w else h * aspectRatio) / 2f
            hh = (if (h > w) w / aspectRatio else h) / 2f
        }
        return FrameOffset(
            left = c.x - hw,
            top = c.y - hh,
            right = bounds.width - (c.x + hw),
            bottom = bounds.height - (c.y + hh),
        )
    }

    @CheckResult
    fun moveTopLeftConstrained(
        delta: Offset,
        bounds: Size,
    ) = copy(
        top = (top + delta.y).coerceIn(0f, bounds.height - bottom - MIN_GAP),
        left = (left + delta.x).coerceIn(0f, bounds.width - right - MIN_GAP),
    )

    @CheckResult
    fun moveTopRightConstrained(
        delta: Offset,
        bounds: Size,
    ) = copy(
        top = (top + delta.y).coerceIn(0f, bounds.height - bottom - MIN_GAP),
        right = (right - delta.x).coerceIn(0f, bounds.width - left - MIN_GAP),
    )

    @CheckResult
    fun moveBottomLeftConstrained(
        delta: Offset,
        bounds: Size,
    ) = copy(
        bottom = (bottom - delta.y).coerceIn(0f, bounds.height - top - MIN_GAP),
        left = (left + delta.x).coerceIn(0f, bounds.width - right - MIN_GAP),
    )

    @CheckResult
    fun moveBottomRightConstrained(
        delta: Offset,
        bounds: Size,
    ) = copy(
        bottom = (bottom - delta.y).coerceIn(0f, bounds.height - top - MIN_GAP),
        right = (right - delta.x).coerceIn(0f, bounds.width - left - MIN_GAP),
    )

    @CheckResult
    fun moveAllConstrained(delta: Offset): FrameOffset {
        var top = top + delta.y
        var left = left + delta.x
        var bottom = bottom - delta.y
        var right = right - delta.x
        if (top < 0f) {
            val dy = -top
            top = 0f
            bottom -= dy
        } else if (bottom < 0f) {
            val dy = -bottom
            top -= dy
            bottom = 0f
        }
        if (left < 0f) {
            val dx = -left
            left = 0f
            right -= dx
        } else if (right < 0) {
            val dy = -right
            left -= dy
            right = 0f
        }
        return FrameOffset(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    fun hasNegativePoints(): Boolean = top < 0f || left < 0f || right < 0f || bottom < 0f

    @CheckResult
    fun coerceAtLeast(minValue: FrameOffset) = FrameOffset(
        left = left.coerceAtLeast(minValue.left),
        top = top.coerceAtLeast(minValue.top),
        right = right.coerceAtLeast(minValue.right),
        bottom = bottom.coerceAtLeast(minValue.bottom),
    )

    operator fun plus(delta: Float) = FrameOffset(
        top = top + delta,
        left = left + delta,
        right = right + delta,
        bottom = bottom + delta,
    )

    companion object {

        val Zero = FrameOffset(0f, 0f, 0f, 0f)

        private const val MIN_GAP = 20f // 20 px is minimal distance between points

        val vectorConverter = TwoWayConverter<FrameOffset, AnimationVector4D>(
            convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
            convertFromVector = { FrameOffset(it.v1, it.v2, it.v3, it.v4) },
        )
    }
}
