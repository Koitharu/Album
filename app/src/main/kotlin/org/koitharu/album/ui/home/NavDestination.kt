package org.koitharu.album.ui.home

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.ui.common.AlbumItem
import org.koitharu.album.ui.folders.FolderItem

@Immutable
@Serializable
sealed interface NavDestination : NavKey {

    @Serializable
    enum class HomeTab : NavDestination {

        Album, Folders
    }

    @Immutable
    @Serializable
    data class FolderContent(
        val folder: FolderItem
    ) : NavDestination

    @Immutable
    @Serializable
    data class MediaViewer(
        val query: MediaQuery,
        val media: AlbumItem.Media,
    ) : NavDestination
}