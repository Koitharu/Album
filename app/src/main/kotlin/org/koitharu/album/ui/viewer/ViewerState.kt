package org.koitharu.album.ui.viewer

import androidx.compose.runtime.Immutable
import org.koitharu.album.ui.album.AlbumItem

@Immutable
data class ViewerState(
    val currentMedia: AlbumItem.Media,
)