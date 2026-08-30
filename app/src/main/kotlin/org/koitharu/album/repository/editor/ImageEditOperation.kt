package org.koitharu.album.repository.editor

import android.graphics.Bitmap.Config
import android.graphics.Bitmap.createBitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.runtime.Immutable
import androidx.core.graphics.applyCanvas
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.koitharu.album.model.DrawPrimitive
import kotlin.math.roundToInt

@Immutable
sealed class ImageEditOperation(
    override val cacheKey: String,
) : Transformation() {

    @Immutable
    data class Crop(
        val top: Float,
        val left: Float,
        var right: Float,
        val bottom: Float,
    ) : ImageEditOperation("crop[$top,$left,$right,$bottom]") {

        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val topPx = input.height * top
            val leftPx = input.width * left
            val rightPx = input.width * right
            val bottomPx = input.height * bottom
            val targetWidth = input.width - leftPx - rightPx
            val targetHeight = input.height - topPx - bottomPx
            return createBitmap(
                input,
                leftPx.roundToInt(),
                topPx.roundToInt(),
                targetWidth.roundToInt(),
                targetHeight.roundToInt()
            )
        }
    }

    @Immutable
    data object FlipHorizontal : ImageEditOperation("flip_h") {

        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val matrix = Matrix().apply {
                preScale(-1.0f, 1.0f)
            }
            return createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        }
    }

    @Immutable
    data object FlipVertical : ImageEditOperation("flip_v") {

        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val matrix = Matrix().apply {
                preScale(1.0f, -1.0f)
            }
            return createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        }
    }

    @Immutable
    data class Rotate(
        val degrees: Int,
    ) : ImageEditOperation("rotate_$degrees") {

        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val matrix = Matrix().apply {
                preRotate(degrees.toFloat())
            }
            return createBitmap(input, 0, 0, input.width, input.height, matrix, true)
        }
    }

    @Immutable
    data class Draw(
        val primitive: DrawPrimitive,
    ) : ImageEditOperation("draw_$primitive") {

        override suspend fun transform(input: Bitmap, size: Size): Bitmap {
            val paint = Paint()
            return input.copy(input.config ?: Config.ARGB_8888, true).applyCanvas {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = primitive.lineHeight
                paint.strokeCap = Paint.Cap.ROUND
                paint.setColor(primitive.color)
                drawPath(primitive.toPath(width.toFloat(), height.toFloat()), paint)
            }
        }
    }
}
