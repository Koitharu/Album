package org.koitharu.album.ui.editor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.koitharu.album.model.DrawPrimitive
import org.koitharu.album.model.ImmutableError
import org.koitharu.album.repository.editor.ImageEditOperation
import org.koitharu.album.ui.common.Fraction

@Immutable
data class ImageEditorState(
    val imageUri: String,
    val imageName: String?,
    val mode: ImageEditorMode?,
    val operations: PersistentList<ImageEditOperation>,
    val undoneOperations: PersistentList<ImageEditOperation>,
    val cropFrame: FrameOffset,
    val cropAspectRatio: Fraction,
    val currentArrow: DrawPrimitive.Arrow?,
    val currentPath: DrawPrimitive.FreePath?,
    val currentColor: Color,
    val lineThickness: Dp,
    val isSaving: Boolean,
    val error: ImmutableError?,
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
        cropAspectRatio = Fraction.Unspecified,
        currentArrow = null,
        currentPath = null,
        lineThickness = 2.dp,
        currentColor = Color.Red,
        isSaving = false,
        error = null,
    )

    val canApply = when (mode) {
        ImageEditorMode.CROP -> cropFrame != FrameOffset.Zero
        ImageEditorMode.DRAW_ARROW -> currentArrow != null
        ImageEditorMode.DRAW_FREE -> currentPath != null
        else -> false
    }

    fun withPendingOperation(): ImageEditorState? = when (mode) {
        ImageEditorMode.CROP -> if (cropFrame != FrameOffset.Zero) {
            copy(
                operations = operations.adding(
                    ImageEditOperation.Crop(
                        top = cropFrame.top.coerceAtLeast(0f),
                        left = cropFrame.left.coerceAtLeast(0f),
                        right = cropFrame.right.coerceAtLeast(0f),
                        bottom = cropFrame.bottom.coerceAtLeast(0f),
                    )
                ),
                cropFrame = FrameOffset.Zero,
            )
        } else {
            null
        }

        ImageEditorMode.ROTATE, ImageEditorMode.MIRROR, null -> null

        ImageEditorMode.DRAW_ARROW -> if (currentArrow != null) {
            copy(
                operations = operations.adding(
                    ImageEditOperation.Draw(
                        primitive = currentArrow,
                    ),
                ),
                currentArrow = null,
            )
        } else {
            null
        }

        ImageEditorMode.DRAW_FREE -> if (currentPath != null) {
            copy(
                operations = operations.adding(
                    ImageEditOperation.Draw(
                        primitive = currentPath,
                    ),
                ),
                currentPath = null,
            )
        } else {
            null
        }

        ImageEditorMode.COLOR_CORRECTION -> null // TODO
    }
}
