package org.koitharu.album.ui.viewer

import org.koitharu.album.ui.common.AlbumItem

sealed interface ViewerIntent {

    data class OnMediaChanged(
        val media: AlbumItem.Media,
    ) : ViewerIntent

    data class Favorite(
        val media: AlbumItem.Media,
        val isFavorite: Boolean,
    ) : ViewerIntent

    data class Delete(
        val media: AlbumItem.Media,
    ) : ViewerIntent

    data class Recover(
        val media: AlbumItem.Media,
    ) : ViewerIntent

    data class Rotate(
        val image: AlbumItem.Image,
        val angle: Int,
    ) : ViewerIntent

    data class Share(
        val media: AlbumItem.Media,
    ) : ViewerIntent

    data class Print(
        val image: AlbumItem.Image,
    ) : ViewerIntent

    data class UseAs(
        val image: AlbumItem.Image,
    ) : ViewerIntent

    data class Edit(
        val image: AlbumItem.Image,
    ) : ViewerIntent

    data class OpenInfo(
        val image: AlbumItem.Image,
    ) : ViewerIntent

    data object CloseInfo : ViewerIntent
}