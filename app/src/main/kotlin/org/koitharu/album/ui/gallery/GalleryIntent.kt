package org.koitharu.album.ui.gallery

import org.koitharu.album.ui.common.AlbumItem

sealed interface GalleryIntent {

    data class HandleClick(
        val media: AlbumItem.Media,
    ) : GalleryIntent

    data class HandleLongClick(
        val media: AlbumItem.Media,
    ) : GalleryIntent

    data object CloseMedia : GalleryIntent

    data class UpdateScale(
        val factor: Float
    ) : GalleryIntent

    data object OpenCamera : GalleryIntent

    data object ClearRecycleBin : GalleryIntent

    data object CancelSelectionMode : GalleryIntent

    enum class SelectedMediaIntent : GalleryIntent {

        Share,
        Trash,
        Delete,
        Recover,
        Unfavorite,
        Hide,
        Unhide,
    }
}