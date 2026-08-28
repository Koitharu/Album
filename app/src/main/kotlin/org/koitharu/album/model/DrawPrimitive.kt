package org.koitharu.album.model

import android.graphics.Path
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
sealed interface DrawPrimitive {

    @get:ColorInt
    val color: Int

    @get:Px
    val lineHeight: Float

    fun toPath(
        width: Float,
        height: Float,
    ): Path

    fun scaled(scaleX: Float, scaleY: Float): DrawPrimitive

    fun colored(color: Int) : DrawPrimitive

    @Immutable
    data class Arrow(
        val start: Offset,
        val end: Offset,
        override val color: Int,
        override val lineHeight: Float,
    ) : DrawPrimitive {

        override fun scaled(
            scaleX: Float,
            scaleY: Float
        ) = Arrow(
            start = Offset(
                x = start.x * scaleX,
                y = start.y * scaleY
            ),
            end = Offset(
                x = end.x * scaleX,
                y = end.y * scaleY,
            ),
            color = color,
            lineHeight = lineHeight,
        )

        override fun colored(color: Int) = copy(
            color = color,
        )

        fun moveEnd(delta: Offset, maxX: Float, maxY: Float) = copy(
            end = Offset(
                x = (end.x + delta.x).coerceIn(0f, maxX),
                y = (end.y + delta.y).coerceIn(0f, maxY),
            ),
        )

        override fun toPath(width: Float, height: Float): Path {
            val x1 = start.x * width
            val x2 = end.x * width
            val y1 = start.y * height
            val y2 = end.y * height

            val length = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
            val angle = atan2(y2 - y1, x2 - x1)
            val arrowLength = maxOf(lineHeight * 4f, length * 0.1f)
            val arrowAngle = Math.toRadians(45.0).toFloat()

            val x3 = x2 - arrowLength * cos(angle - arrowAngle)
            val y3 = y2 - arrowLength * sin(angle - arrowAngle)
            val x4 = x2 - arrowLength * cos(angle + arrowAngle)
            val y4 = y2 - arrowLength * sin(angle + arrowAngle)

            // 3. Draw the arrowhead path
            return Path().apply {
                moveTo(x2, y2)
                lineTo(x3, y3)
                moveTo(x2, y2)
                lineTo(x4, y4)
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
        }
    }
}