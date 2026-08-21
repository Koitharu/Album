package org.koitharu.album.ui.album

import org.koitharu.album.ui.common.AlbumItem

sealed interface AlbumIntent {

    data class HandleClick(
        val media: AlbumItem.Media,
    ) : AlbumIntent

    data class HandleLongClick(
        val media: AlbumItem.Media,
    ) : AlbumIntent

    data object CloseMedia : AlbumIntent

    data class UpdateScale(
        val factor: Float
    ) : AlbumIntent

    data object CancelSelectionMode : AlbumIntent

    enum class SelectionAlbumIntent : AlbumIntent {

        Share,
        Trash,
        Delete,
        Recover,
        Unfavorite,
        Hide,
        Unhide,
    }
}