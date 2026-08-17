package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.koitharu.album.repository.SettingsRepository.Companion.GRID_SCALE_DEFAULT
import org.koitharu.album.ui.common.AlbumItem

@Immutable
data class AlbumState(
    val scale: Float,
    val openedItem: AlbumItem.Media?,
    val banner: AlbumItem.Image?,
    val selectedItems: PersistentSet<Long>,
) {

    constructor() : this(
        scale = GRID_SCALE_DEFAULT,
        openedItem = null,
        banner = null,
        selectedItems = persistentSetOf(),
    )
}
