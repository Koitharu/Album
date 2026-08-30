package org.koitharu.album.ui.editor

import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koitharu.album.model.DrawPrimitive
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.repository.editor.ImageEditOperation
import org.koitharu.album.repository.editor.ImageEditor
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.editor.ImageEditorEffect.OnError
import org.koitharu.album.ui.editor.ImageEditorEffect.OnImageSaved
import org.koitharu.album.ui.editor.ImageEditorIntent.Apply
import org.koitharu.album.ui.editor.ImageEditorIntent.Crop
import org.koitharu.album.ui.editor.ImageEditorIntent.Draw
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipHorizontal
import org.koitharu.album.ui.editor.ImageEditorIntent.FlipVertical
import org.koitharu.album.ui.editor.ImageEditorIntent.ImageLoadFailed
import org.koitharu.album.ui.editor.ImageEditorIntent.Redo
import org.koitharu.album.ui.editor.ImageEditorIntent.Reset
import org.koitharu.album.ui.editor.ImageEditorIntent.Rotate
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveCopy
import org.koitharu.album.ui.editor.ImageEditorIntent.SaveReplacing
import org.koitharu.album.ui.editor.ImageEditorIntent.SetColor
import org.koitharu.album.ui.editor.ImageEditorIntent.SetCropAspectRatio
import org.koitharu.album.ui.editor.ImageEditorIntent.SetLineThickness
import org.koitharu.album.ui.editor.ImageEditorIntent.SetMode
import org.koitharu.album.ui.editor.ImageEditorIntent.Share
import org.koitharu.album.ui.editor.ImageEditorIntent.Undo
import org.koitharu.album.util.runCatchingCancellable

@HiltViewModel(assistedFactory = ImageEditorViewModel.Factory::class)
class ImageEditorViewModel @AssistedInject constructor(
    @Assisted("uri") imageUri: String,
    @Assisted("name") imageName: String?,
    private val imageEditorProvider: Lazy<ImageEditor>,
    private val settingsRepository: SettingsRepository,
) : MviViewModel<ImageEditorState, ImageEditorIntent, ImageEditorEffect>(
    ImageEditorState(
        imageUri = imageUri,
        imageName = imageName,
    )
) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val color = settingsRepository.editorColor.first()
            val lineThickness = settingsRepository.editorLineThickness.first()
            state.update {
                it.copy(
                    currentColor = color,
                    lineThickness = lineThickness,
                )
            }
        }
    }

    override fun handleIntent(intent: ImageEditorIntent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (intent) {
                is SetMode -> state.update {
                    it.copy(mode = intent.mode)
                }

                is Crop -> state.update {
                    it.copy(cropFrame = intent.frame)
                }

                is Draw -> state.update {
                    it.copy(
                        currentArrow = intent.primitive as? DrawPrimitive.Arrow ?: it.currentArrow,
                        currentPath = intent.primitive as? DrawPrimitive.FreePath ?: it.currentPath,
                    )
                }

                FlipHorizontal -> state.update {
                    it.copy(operations = it.operations.adding(ImageEditOperation.FlipHorizontal))
                }

                FlipVertical -> state.update {
                    it.copy(operations = it.operations.adding(ImageEditOperation.FlipVertical))
                }

                Apply -> state.update {
                    it.withPendingOperation()?.copy(
                        mode = null,
                        undoneOperations = persistentListOf(),
                    ) ?: it
                }

                Undo -> state.update {
                    val lastOperation = it.operations.lastOrNull() ?: return@update it
                    it.copy(
                        operations = it.operations.removingAt(it.operations.lastIndex),
                        undoneOperations = it.undoneOperations.adding(lastOperation),
                    )
                }

                Redo -> state.update {
                    val lastOperation = it.undoneOperations.lastOrNull() ?: return@update it
                    it.copy(
                        operations = it.operations.adding(lastOperation),
                        undoneOperations = it.undoneOperations.removingAt(it.undoneOperations.lastIndex),
                    )
                }

                SaveCopy -> runCatchingCancellable {
                    val snapshot = state.value
                    imageEditorProvider.get()
                        .editAndSaveCopy(
                            sourceUri = snapshot.imageUri.toUri(),
                            operations = snapshot.operations,
                        )
                }.onFailure {
                    sendEffect(OnError(it))
                }.onSuccess {
                    sendEffect(OnImageSaved)
                }

                SaveReplacing -> runCatchingCancellable {
                    val snapshot = state.value
                    imageEditorProvider.get()
                        .editAndReplace(
                            sourceUri = snapshot.imageUri.toUri(),
                            operations = snapshot.operations,
                            targetUri = snapshot.imageUri.toUri(),
                        )
                }.onFailure {
                    sendEffect(OnError(it))
                }.onSuccess {
                    sendEffect(OnImageSaved)
                }

                Share -> runCatchingCancellable {
                    val snapshot = state.value
                    imageEditorProvider.get()
                        .editAndShare(
                            sourceUri = snapshot.imageUri.toUri(),
                            operations = snapshot.operations,
                        )
                }.onFailure {
                    sendEffect(OnError(it))
                }

                is ImageLoadFailed -> sendEffect(OnError(intent.error))
                is SetColor -> {
                    state.update {
                        it.copy(
                            currentColor = intent.color,
                            currentArrow = it.currentArrow?.colored(intent.color.toArgb()),
                            currentPath = it.currentPath?.colored(intent.color.toArgb()),
                        )
                    }
                    settingsRepository.setEditorColor(intent.color)
                }

                Reset -> state.update {
                    it.copy(
                        currentArrow = null,
                        cropFrame = FrameOffset.Zero,
                    )
                }

                is Rotate -> state.update {
                    it.copy(
                        operations = it.operations.adding(
                            ImageEditOperation.Rotate(intent.degrees)
                        )
                    )
                }

                is SetCropAspectRatio -> state.update {
                    it.copy(
                        cropAspectRatio = intent.fraction,
                    )
                }

                is SetLineThickness -> {
                    state.update {
                        it.copy(
                            lineThickness = intent.thickness,
                            currentArrow = it.currentArrow?.copy(
                                lineHeight = intent.thicknessPx,
                            ),
                            currentPath = it.currentPath?.copy(
                                lineHeight = intent.thicknessPx,
                            ),
                        )
                    }
                    settingsRepository.setEditorLineThickness(intent.thickness)
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            @Assisted("uri") imageUri: String,
            @Assisted("name") imageName: String?,
        ): ImageEditorViewModel
    }
}