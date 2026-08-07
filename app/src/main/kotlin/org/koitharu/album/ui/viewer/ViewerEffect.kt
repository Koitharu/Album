package org.koitharu.album.ui.viewer

sealed interface ViewerEffect {

    data class OnError(
        val error: Throwable,
    ): ViewerEffect
}
