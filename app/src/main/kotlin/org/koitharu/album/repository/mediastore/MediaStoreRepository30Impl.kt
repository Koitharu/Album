package org.koitharu.album.repository.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.ArraySet
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.MediaStoreConfirmationDialogs
import org.koitharu.album.repository.observeChanges
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.ActivityContextProvider

@RequiresApi(Build.VERSION_CODES.R)
class MediaStoreRepository30Impl(
    activityContextProvider: ActivityContextProvider,
    contentResolver: ContentResolver,
    legacyFavoritesRepository: LegacyFavoritesRepository,
    confirmationDialogs: MediaStoreConfirmationDialogs,
    queryBuilder: QueryBuilder,
    hiddenMediaRepository: HiddenMediaRepository,
) : MediaStoreRepositoryLegacyImpl(
    activityContextProvider = activityContextProvider,
    contentResolver = contentResolver,
    hiddenMediaRepository = hiddenMediaRepository,
    legacyFavoritesRepository = legacyFavoritesRepository,
    confirmationDialogs = confirmationDialogs,
    queryBuilder = queryBuilder,
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

    override suspend fun clearRecycleBin() {
        val trashedMedia = contentResolver.queryCompat(
            uri = baseUri,
            projection = arrayOf(FileColumns._ID, FileColumns.MEDIA_TYPE),
            selection = "(${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?) AND ${FileColumns.IS_TRASHED} = ?",
            selectionArgs = arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString(),
                "1",
            )
        ).use { cursor ->
            val result = ArraySet<Uri>(cursor.count)
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
                val typeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
                do {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        when (cursor.getInt(typeColumn)) {
                            FileColumns.MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            FileColumns.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            else -> continue
                        }, id
                    )
                    result.add(uri)
                } while (cursor.moveToNext())
            }
            result
        }
        deleteMedia(trashedMedia, useRecycleBin = false)
    }

    override suspend fun isFavorite(id: Long): Boolean = contentResolver.queryCompat(
        uri = baseUri,
        projection = arrayOf(FileColumns.IS_FAVORITE),
        selection = "${FileColumns._ID} = ?",
        selectionArgs = arrayOf(id.toString()),
    ).use { cursor ->
        if (cursor.moveToFirst()) {
            val column = cursor.getColumnIndex(FileColumns.IS_FAVORITE)
            column >= 0 && cursor.getInt(column) > 0
        } else {
            false
        }
    }

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

    override suspend fun getFavoritesSize(): Int = queryCount(
        MediaQuery(
            isFavorites = true,
        )
    )

    override suspend fun getRecycleBinSize() = queryCount(
        MediaQuery(
            isTrashed = true,
        )
    )

    override fun observeIsFavorite(id: Long): Flow<Boolean> = contentResolver.observeChanges(
        uri = baseUri,
        kickstart = true
    ).mapLatest {
        isFavorite(id)
    }.distinctUntilChanged()

    override fun observeFavoritesSize(): Flow<Int> = contentResolver.observeChanges(
        uri = baseUri,
        kickstart = true
    ).mapLatest {
        getFavoritesSize()
    }.distinctUntilChanged()
}