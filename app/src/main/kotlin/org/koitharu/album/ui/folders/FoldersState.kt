package org.koitharu.album.ui.folders

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class FoldersState(
    val items: ImmutableList<FolderItem>,
    val error: Throwable?,
) {

    constructor(): this(
        items = persistentListOf(),
        error = null,
    )
}