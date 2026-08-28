package org.koitharu.album.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.model.DrawPrimitive
import org.koitharu.album.repository.editor.ImageEditOperation

@Immutable
data class ImageEditorState(
    val imageUri: String,
    val imageName: String?,
    val mode: ImageEditorMode?,
    val operations: PersistentList<ImageEditOperation>,
    val undoneOperations: PersistentList<ImageEditOperation>,
    val cropFrame: FrameOffset,
    val currentPrimitive: DrawPrimitive?,
    val currentColor: Color,
    val isSaving: Boolean,
) {

    constructor(
        imageUri: String,
        imageName: String?,
    ) : this(
        imageUri = imageUri,
        imageName = imageName,
        mode = null,
        operations = persistentListOf(),
        undoneOperations = persistentListOf(),
        cropFrame = FrameOffset.Zero,
        currentPrimitive = null,
        currentColor = Color.Red,
        isSaving = false,
    )

    val canApply = when (mode) {
        ImageEditorMode.CROP -> cropFrame != FrameOffset.Zero
        ImageEditorMode.DRAW_ARROW -> currentPrimitive is DrawPrimitive.Arrow
        else -> false
    }

    fun withPendingOperations(): ImageEditorState = withPendingOperations(this)
}

private tailrec fun withPendingOperations(state: ImageEditorState): ImageEditorState {
    val newState = when {
        state.cropFrame != FrameOffset.Zero -> state.copy(
            operations = state.operations.adding(
                ImageEditOperation.Crop(
                    top = state.cropFrame.top,
                    left = state.cropFrame.left,
                    right = state.cropFrame.right,
                    bottom = state.cropFrame.bottom,
                )
            ),
            cropFrame = FrameOffset.Zero,
        )

        state.currentPrimitive != null -> state.copy(
            operations = state.operations.adding(
                ImageEditOperation.Draw(
                    primitive = state.currentPrimitive,
                ),
            ),
            currentPrimitive = null,
        )

        else -> return state
    }
    return withPendingOperations(newState)
}