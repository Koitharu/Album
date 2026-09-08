package org.koitharu.album.ui.picker

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.koitharu.album.repository.SettingsRepository.Companion.GRID_SCALE_DEFAULT
import org.koitharu.album.ui.folders.FolderItem

@Immutable
data class PickerState(
    val scale: Float,
    val selectedItems: PersistentSet<Long>,
    val folder: FolderItem.Bucket?,
    val isFavoritesOnly: Boolean,
    val isImagesOnly: Boolean,
    val isVideosOnly: Boolean,
    val availableFolders: ImmutableList<FolderItem.Bucket>,
) {

    constructor() : this(
        scale = GRID_SCALE_DEFAULT,
        selectedItems = persistentSetOf(),
        folder = null,
        isFavoritesOnly = false,
        isImagesOnly = false,
        isVideosOnly = false,
        availableFolders = persistentListOf(),
    )
}
