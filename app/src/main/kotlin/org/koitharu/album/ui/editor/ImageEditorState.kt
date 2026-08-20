package org.koitharu.album.ui.editor

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.repository.editor.ImageEditOperation

@Immutable
data class ImageEditorState(
    val imageUri: String,
    val imageName: String?,
    val mode: ImageEditorMode?,
    val operations: PersistentList<ImageEditOperation>,
    val undoneOperations: PersistentList<ImageEditOperation>,
    val cropFrame: FrameOffset,
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
        isSaving = false,
    )

    val canApply = when (mode) {
        ImageEditorMode.CROP -> cropFrame != FrameOffset.Zero
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

        else -> return state
    }
    return withPendingOperations(newState)
}