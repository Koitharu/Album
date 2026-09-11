package org.koitharu.album.ui.viewer

import android.net.Uri

sealed interface ViewerEffect {

    data class OnError(
        val error: Throwable,
    ): ViewerEffect

    data class OpenImageEditor(
        val uri: Uri,
        val name: String?,
    ): ViewerEffect

    data object CloseViewer : ViewerEffect
}
