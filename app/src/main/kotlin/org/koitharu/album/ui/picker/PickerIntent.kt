package org.koitharu.album.ui.picker

import org.koitharu.album.ui.common.AlbumItem

sealed interface PickerIntent {

    data class OnItemClick(
        val item: AlbumItem.Media,
    ) : PickerIntent

    data object OnDoneClick : PickerIntent

    data class UpdateScale(
        val factor: Float
    ) : PickerIntent

    data object ToggleFavoriteOnly : PickerIntent

    data object ToggleVideosOnly : PickerIntent

    data object ToggleImagesOnly : PickerIntent

    data class SetBucketId(
        val bucketId: String?,
    ) : PickerIntent
}