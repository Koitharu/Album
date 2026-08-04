package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable

@Immutable
data class AlbumState(
    val scale: Float,
    val openedItem: AlbumItem.Media?,
) {

    constructor() : this(
        scale = 2f,
        openedItem = null,
    )
}
