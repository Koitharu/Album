package org.koitharu.album.ui.viewer

import androidx.compose.runtime.Immutable
import org.koitharu.album.ui.common.AlbumItem

@Immutable
data class ViewerState(
    val currentMedia: AlbumItem.Media,
    val isFavorite: Boolean,
) {

    constructor(media: AlbumItem.Media) : this(
        currentMedia = media,
        isFavorite = false,
    )
}