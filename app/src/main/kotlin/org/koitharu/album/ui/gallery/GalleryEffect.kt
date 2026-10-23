package org.koitharu.album.ui.gallery

import org.koitharu.album.ui.common.AlbumItem

sealed interface GalleryEffect {

    data class ShowError(
        val error: Throwable,
    ) : GalleryEffect

    data class OpenMedia(
        val media: AlbumItem.Media,
    ) : GalleryEffect
}