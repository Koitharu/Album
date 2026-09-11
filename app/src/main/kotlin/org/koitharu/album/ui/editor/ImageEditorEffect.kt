package org.koitharu.album.ui.editor

sealed interface ImageEditorEffect {

    @Deprecated("")
    data class OnError(
        val error: Throwable
    ): ImageEditorEffect

    data object OnImageSaved: ImageEditorEffect
}