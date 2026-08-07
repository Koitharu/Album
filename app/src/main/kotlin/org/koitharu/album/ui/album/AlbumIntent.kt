package org.koitharu.album.ui.album

import org.koitharu.album.ui.common.AlbumItem

sealed interface AlbumIntent {

    data class OpenMedia(
        val media: AlbumItem.Media,
    ): AlbumIntent

    data object CloseMedia: AlbumIntent

    data class UpdateScale(
        val factor: Float
    ): AlbumIntent
}