package org.koitharu.album.ui.viewer

import org.koitharu.album.ui.common.AlbumItem

sealed interface ViewerIntent {

    data class OnMediaChanged(
        val media: AlbumItem.Media,
    ): ViewerIntent

    data class Favorite(
        val media: AlbumItem.Media,
        val isFavorite: Boolean,
    ): ViewerIntent

    data class Delete(
        val media: AlbumItem.Media,
    ): ViewerIntent

    data class Recover(
        val media: AlbumItem.Media,
    ): ViewerIntent

    data class Rotate(
        val media: AlbumItem.Media,
        val angle: Int,
    ): ViewerIntent
}