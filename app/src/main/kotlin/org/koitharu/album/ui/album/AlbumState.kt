package org.koitharu.album.ui.album

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.SettingsRepository.Companion.GRID_SCALE_DEFAULT
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.folders.FolderItem

@Immutable
data class AlbumState(
    val scale: Float,
    val openedItem: AlbumItem.Media?,
    val banner: AlbumItem.Image?,
    val selectedItems: PersistentSet<Long>,
    val folder: FolderItem?,
    val isRecycleBinEnabled: Boolean,
) {

    constructor(
        folder: FolderItem?,
    ) : this(
        scale = GRID_SCALE_DEFAULT,
        openedItem = null,
        banner = null,
        folder = folder,
        selectedItems = persistentSetOf(),
        isRecycleBinEnabled = Features.isRecycleBinSupported,
    )
}
