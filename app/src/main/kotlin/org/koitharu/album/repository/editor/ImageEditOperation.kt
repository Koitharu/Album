package org.koitharu.album.repository.editor

import android.graphics.Bitmap.createBitmap
import android.graphics.Matrix
import androidx.compose.runtime.Immutable
import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
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
}
