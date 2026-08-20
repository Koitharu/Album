package org.koitharu.album.ui.editor

sealed interface ImageEditorIntent {

    data class ToggleMode(
        val mode: ImageEditorMode,
    ) : ImageEditorIntent

    data class Crop(
        val frame: FrameOffset,
    ): ImageEditorIntent

    data object FlipHorizontal: ImageEditorIntent

    data object FlipVertical: ImageEditorIntent

    data object Apply: ImageEditorIntent

    data object Undo: ImageEditorIntent

    data object Redo: ImageEditorIntent

    data object SaveCopy: ImageEditorIntent

    data object SaveReplacing: ImageEditorIntent

    data object Share: ImageEditorIntent

    data class ImageLoadFailed(
        val error: Throwable
    ): ImageEditorIntent
}