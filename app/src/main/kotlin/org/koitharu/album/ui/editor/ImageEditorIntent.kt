package org.koitharu.album.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.koitharu.album.model.DrawPrimitive
import org.koitharu.album.ui.common.Fraction

sealed interface ImageEditorIntent {

    data class SetMode(
        val mode: ImageEditorMode?,
    ) : ImageEditorIntent

    data class Crop(
        val frame: FrameOffset,
    ) : ImageEditorIntent

    data class Rotate(
        val degrees: Int,
    ) : ImageEditorIntent

    data class Draw(
        val primitive: DrawPrimitive,
    ) : ImageEditorIntent

    data object Reset : ImageEditorIntent

    data object FlipHorizontal : ImageEditorIntent

    data object FlipVertical : ImageEditorIntent

    data object Apply : ImageEditorIntent

    data object Undo : ImageEditorIntent

    data object Redo : ImageEditorIntent

    data object SaveCopy : ImageEditorIntent

    data object SaveReplacing : ImageEditorIntent

    data object Share : ImageEditorIntent

    data class ImageLoadFailed(
        val error: Throwable,
    ) : ImageEditorIntent

    data class SetColor(
        val color: Color,
    ) : ImageEditorIntent

    data class SetLineThickness(
        val thickness: Dp,
        val thicknessPx: Float,
    ) : ImageEditorIntent

    data class SetCropAspectRatio(
        val fraction: Fraction
    ) : ImageEditorIntent
}