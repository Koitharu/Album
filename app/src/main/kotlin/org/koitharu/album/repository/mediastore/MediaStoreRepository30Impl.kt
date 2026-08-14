package org.koitharu.album.repository.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.observeChanges
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.ActivityContextProvider

@RequiresApi(Build.VERSION_CODES.R)
class MediaStoreRepository30Impl(
    activityContextProvider: ActivityContextProvider,
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
) : MediaStoreRepositoryLegacyImpl(
    activityContextProvider = activityContextProvider,
    contentResolver = contentResolver,
    legacyFavoritesRepository = legacyFavoritesRepository,
) {

    override suspend fun deleteMedia(media: Collection<Uri>, useRecycleBin: Boolean) {
        val intent = if (useRecycleBin) {
            MediaStore.createTrashRequest(contentResolver, media, true)
        } else {
            MediaStore.createDeleteRequest(contentResolver, media)
        }
        activityContextProvider.get().startIntentSender(
            intent.intentSender,
            null,
            0,
            0,
            0
        )
    }

    override suspend fun recoverMedia(media: Collection<Uri>) {
        val intent = MediaStore.createTrashRequest(contentResolver, media, false)
        activityContextProvider.get().startIntentSender(
            intent.intentSender,
            null,
            0,
            0,
            0
        )
    }

    override suspend fun isFavorite(id: Long): Boolean = contentResolver.queryCompat(
        uri = baseUri,
        projection = arrayOf(FileColumns.IS_FAVORITE),
        selection = "${FileColumns._ID} = ?",
        selectionArgs = arrayOf(id.toString()),
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val column = cursor.getColumnIndex(FileColumns.IS_FAVORITE)
            if (column < 0) {
                false
            } else {
                cursor.getInt(column) > 0
            }
        } else {
            false
        }
    } ?: false

    override suspend fun setIsFavorite(media: Collection<Uri>, isFavorite: Boolean) {
        val intent = MediaStore.createFavoriteRequest(contentResolver, media, isFavorite)
        activityContextProvider.get().startIntentSender(
            intent.intentSender,
            null,
            0,
            0,
            0
        )
    }

    private suspend fun RecoverableSecurityException.resolve() {
        activityContextProvider.get().startIntentSender(
            userAction.actionIntent.intentSender,
            null,
            0,
            0,
            0
        )
    }

    override suspend fun getFavoritesSize(): Int = withContext(Dispatchers.IO) {
        contentResolver.queryCompat(
            uri = baseUri,
            projection = arrayOf(FileColumns._ID),
            selection = "(${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?) AND ${FileColumns.IS_FAVORITE} = ?",
            selectionArgs = arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString(),
                "1"
            )
        )?.use {
            it.count
        } ?: 0
    }

    override suspend fun getRecycleBinSize() = withContext(Dispatchers.IO) {
        contentResolver.queryCompat(
            uri = baseUri,
            projection = arrayOf(FileColumns._ID),
            selection = "(${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?) AND ${FileColumns.IS_TRASHED} = ?",
            selectionArgs = arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString(),
                "1"
            )
        )?.use {
            it.count
        } ?: 0
    }

    override fun observeIsFavorite(id: Long): Flow<Boolean> = contentResolver.observeChanges(
        uri = baseUri,
        kickstart = true
    ).mapLatest {
        isFavorite(id)
    }.distinctUntilChanged()
}