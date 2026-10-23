package org.koitharu.album.ui.gallery

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.SettingsRepository.Companion.GRID_SCALE_DEFAULT
import org.koitharu.album.ui.common.AlbumItem

@Immutable
data class GalleryState(
    val scale: Float,
    val banner: AlbumItem.Image?,
    val isBannerDark: Boolean,
    val selectedItems: PersistentSet<Long>,
    val query: MediaQuery,
    val isRecycleBinEnabled: Boolean,
) {

    constructor(
        query: MediaQuery,
    ) : this(
        scale = GRID_SCALE_DEFAULT,
        banner = null,
        isBannerDark = false,
        query = query,
        selectedItems = persistentSetOf(),
        isRecycleBinEnabled = Features.isRecycleBinSupported,
    )
}
