package org.koitharu.album.ui.picker

import org.koitharu.album.ui.common.AlbumItem

sealed interface PickerEffect {

    data class ReturnSingleItem(
        val item: AlbumItem.Media,
    ) : PickerEffect

    data class ReturnMultipleItems(
        val items: Collection<AlbumItem.Media>,
    ) : PickerEffect

    data class OnError(
        val error: Throwable,
    ) : PickerEffect
}