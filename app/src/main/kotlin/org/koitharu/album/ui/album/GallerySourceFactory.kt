package org.koitharu.album.ui.album

import org.koitharu.album.repository.pagingsource.AlbumSource
import org.koitharu.album.repository.pagingsource.FavoritesSource
import org.koitharu.album.repository.pagingsource.GallerySource
import org.koitharu.album.repository.pagingsource.PhotosSource
import org.koitharu.album.repository.pagingsource.RecycleBinSource
import org.koitharu.album.repository.pagingsource.VideosSource
import org.koitharu.album.ui.folders.FolderItem
import javax.inject.Inject
import javax.inject.Provider

class GallerySourceFactory @Inject constructor(
    private val albumSourceFactory: AlbumSource.Factory,
    private val recycleBinSourceFactory: Provider<RecycleBinSource>,
    private val favoritesSourceFactory: Provider<FavoritesSource>,
    private val photosSourceFactory: Provider<PhotosSource>,
    private val videosSourceFactory: Provider<VideosSource>,
) {

    fun create(folderItem: FolderItem?): GallerySource = when (folderItem) {
        is FolderItem.Bucket -> albumSourceFactory.create(folderItem.id)
        is FolderItem.Favorites -> favoritesSourceFactory.get()
        is FolderItem.RecycleBin -> recycleBinSourceFactory.get()
        is FolderItem.Photos -> photosSourceFactory.get()
        is FolderItem.Videos -> videosSourceFactory.get()
        null -> albumSourceFactory.create(null)
    }
}