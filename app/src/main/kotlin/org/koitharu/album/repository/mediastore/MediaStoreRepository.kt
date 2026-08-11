package org.koitharu.album.repository.mediastore

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import org.koitharu.album.model.MediaFolder
import org.koitharu.album.model.MediaItem

interface MediaStoreRepository {

    suspend fun deleteMedia(media: Collection<Uri>)

    suspend fun recoverMedia(media: Collection<Uri>)

    suspend fun isFavorite(id: Long): Boolean

    suspend fun setIsFavorite(media: Collection<Uri>, isFavorite: Boolean)

    suspend fun getFavoritesSize(): Int

    suspend fun getRecycleBinSize(): Int

    suspend fun getFolders(): List<MediaFolder>

    fun observeFolders(): Flow<List<MediaFolder>>

    fun observeIsFavorite(id: Long): Flow<Boolean>

    suspend fun getMedia(id: Long): MediaItem

    fun observeMedia(id: Long, withStartValue: Boolean): Flow<MediaItem>

    suspend fun getRandomMedia(
        isImageOnly: Boolean,
        isFavoriteOnly: Boolean,
    ): MediaItem?
}