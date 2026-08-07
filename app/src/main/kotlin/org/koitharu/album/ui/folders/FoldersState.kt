package org.koitharu.album.ui.folders

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class FoldersState(
    val items: ImmutableList<FolderItem>,
) {

    constructor(): this(
        items = persistentListOf(),
    )
}