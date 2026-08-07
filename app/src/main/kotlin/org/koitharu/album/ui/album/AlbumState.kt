package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable
import org.koitharu.album.ui.common.AlbumItem

@Immutable
data class AlbumState(
    val scale: Float,
    val openedItem: AlbumItem.Media?,
    val banner: AlbumItem.Image?,
) {

    constructor() : this(
        scale = 2f,
        openedItem = null,
        banner = null,
    )
}
