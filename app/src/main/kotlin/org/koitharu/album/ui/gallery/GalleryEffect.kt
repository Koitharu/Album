package org.koitharu.album.ui.gallery

sealed interface GalleryEffect {

    data class ShowError(
        val error: Throwable,
    ) : GalleryEffect
}