package org.koitharu.album.ui.viewer

import org.koitharu.album.ui.album.AlbumItem

sealed interface ViewerIntent {

    data class OnMediaChanged(
        val media: AlbumItem.Media,
    ): ViewerIntent
}