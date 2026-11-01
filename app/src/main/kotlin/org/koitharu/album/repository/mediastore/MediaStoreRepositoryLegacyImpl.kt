package org.koitharu.album.repository.mediastore

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.core.database.getStringOrNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import org.koitharu.album.model.MediaFolder
import org.koitharu.album.model.MediaItem
import org.koitharu.album.model.MediaQuery
import org.koitharu.album.model.MediaQuery.Companion.PATTERN_DCIM
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.HiddenMediaRepository
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.MediaStoreConfirmationDialogs
import org.koitharu.album.repository.OrderDirection.DESC
import org.koitharu.album.repository.ThumbnailFetcher.Companion.thumbnailUri
import org.koitharu.album.repository.observeChanges
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.ActivityContextProvider
import org.koitharu.album.util.commonPrefix
import org.koitharu.album.util.resolve

open class MediaStoreRepositoryLegacyImpl(
    protected val activityContextProvider: ActivityContextProvider,
    protected val contentResolver: ContentResolver,
    protected val hiddenMediaRepository: HiddenMediaRepository,
    private val legacyFavoritesRepository: LegacyFavoritesRepository,
    private val confirmationDialogs: MediaStoreConfirmationDialogs,
    private val queryBuilder: QueryBuilder,
) : MediaStoreRepository {

    protected val baseUri: Uri = MediaStore.Files.getContentUri("external")

    override suspend fun deleteMedia(media: Collection<Uri>, useRecycleBin: Boolean) {
        if (useRecycleBin) {
            throw UnsupportedOperationException("Recycle Bin is not supported")
        }
        if (!confirmationDialogs.confirmDeletion(media)) {
            return
        }
        for (uri in media) {
            try {
                contentResolver.delete(uri, null, null)
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                    if (securityException.resolve(activityContextProvider.get())) {
                        contentResolver.delete(uri, null, null)
                    }
                } else {
                    throw securityException
                }
            }
        }
    }

    override suspend fun recoverMedia(media: Collection<Uri>): Unit =
        throw UnsupportedOperationException("Recycle Bin is not supported")

    override suspend fun clearRecycleBin(): Unit =
        throw UnsupportedOperationException("Recycle Bin is not supported")

    override suspend fun isFavorite(id: Long): Boolean = withContext(Dispatchers.IO) {
        legacyFavoritesRepository.isFavorite(id)
    }

    override suspend fun setIsFavorite(media: Collection<Uri>, isFavorite: Boolean) {
        for (uri in media) {
            val id = uri.lastPathSegment?.toLongOrNull() ?: continue
            legacyFavoritesRepository.setIsFavorite(id, isFavorite)
        }
    }

    override suspend fun getFavoritesSize(): Int = withContext(Dispatchers.IO) {
        legacyFavoritesRepository.getFavoritesCount()
    }

    override suspend fun getRecycleBinSize(): Int = 0

    override suspend fun getFolders(): List<MediaFolder> {
        val queryParams = queryBuilder(MediaQuery.DEFAULT)
        return contentResolver.queryCompat(
            uri = MediaStore.Files.getContentUri("external"),
            projection = arrayOf(
                FileColumns.BUCKET_ID,
                FileColumns.BUCKET_DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                FileColumns._ID,
                if (Features.isPathColumnSupported) {
                    FileColumns.RELATIVE_PATH
                } else {
                    FileColumns.DATA
                }
            ),
            selection = queryParams.selection,
            selectionArgs = queryParams.selectionArgs.toTypedArray(),
            orderBy = FileColumns.DATE_ADDED,
            orderDirection = DESC,
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use persistentListOf()
            }
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val bucketIdColumn =
                cursor.getColumnIndexOrThrow(FileColumns.BUCKET_ID)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val nameColumn =
                cursor.getColumnIndexOrThrow(FileColumns.BUCKET_DISPLAY_NAME)
            val pathColumn = if (Features.isPathColumnSupported) {
                cursor.getColumnIndex(FileColumns.RELATIVE_PATH)
            } else {
                cursor.getColumnIndex(FileColumns.DATA)
            }

            val result = HashMap<String, MediaFolder>()
            while (cursor.moveToNext()) {

                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getStringOrNull(nameColumn) ?: "Unknown Folder"
                val path = cursor.getStringOrNull(pathColumn)

                if (result.containsKey(bucketId)) {
                    result[bucketId]?.let {
                        it.size++
                        it.path = commonPrefix(it.path, path)
                    }
                } else {
                    val id = cursor.getLong(idColumn)
                    val isVideo = cursor.getInt(mediaTypeColumn) == FileColumns.MEDIA_TYPE_VIDEO
                    val contentUri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    val newFolder = MediaFolder(
                        id = bucketId,
                        name = bucketName,
                        size = 1,
                        thumbnail = contentUri.thumbnailUri(),
                        path = path,
                    )
                    result[bucketId] = newFolder
                }
            }
            result.values.toList()
        }
    }

    override fun observeFolders(): Flow<List<MediaFolder>> = contentResolver.observeChanges(
        uri = baseUri,
        kickstart = true
    ).mapLatest {
        getFolders()
    }.distinctUntilChanged()

    override fun observeIsFavorite(id: Long): Flow<Boolean> {
        return legacyFavoritesRepository.observeIsFavorite(id)
    }

    override fun observeFavoritesSize(): Flow<Int> {
        return legacyFavoritesRepository.observeCount()
    }

    override suspend fun getPhotosCount(): Int = queryCount(
        MediaQuery(
            includeVideos = false,
            pathPattern = PATTERN_DCIM,
        )
    )

    override suspend fun getVideosCount(): Int = queryCount(
        MediaQuery(
            includeImages = false,
        )
    )

    override suspend fun getMediaList(query: MediaQuery, limit: Int): List<MediaItem> {
        val params = queryBuilder(query)
        return contentResolver.queryCompat(
            uri = baseUri,
            projection = defaultProjection(),
            selection = params.selection,
            selectionArgs = params.selectionArgs.toTypedArray(),
            orderBy = FileColumns.DATE_ADDED,
            orderDirection = DESC,
            limit = limit,
        ).use { cursor ->
            cursor.parseMediaList(withIndices = true)
        }
    }

    override suspend fun getMedia(id: Long): MediaItem = contentResolver.queryCompat(
        uri = baseUri,
        projection = defaultProjection(),
        selection = "${FileColumns._ID} = ?",
        selectionArgs = arrayOf(id.toString()),
        limit = 1,
    ).use { cursor ->
        cursor.parseMediaList(withIndices = false).firstOrNull()
    } ?: error("Unable to load media $id")

    override fun observeMedia(
        id: Long,
        withStartValue: Boolean,
    ): Flow<MediaItem> = contentResolver.observeChanges(
        uri = ContentUris.withAppendedId(baseUri, id),
        kickstart = withStartValue,
    ).mapLatest {
        getMedia(id)
    }.distinctUntilChanged()

    override suspend fun getRandomMedia(
        query: MediaQuery,
    ): MediaItem? {
        val params = queryBuilder(query)
        return contentResolver.queryCompat(
            uri = baseUri,
            projection = defaultProjection(),
            selection = params.selection,
            selectionArgs = params.selectionArgs.toTypedArray(),
            orderBy = "RANDOM()",
            limit = 1,
        ).use { cursor ->
            cursor.parseMediaList(withIndices = false).firstOrNull()
        }
    }

    protected fun Cursor.parseMediaList(
        withIndices: Boolean,
    ): List<MediaItem> {
        if (!moveToFirst()) {
            return emptyList()
        }
        val idColumn = getColumnIndexOrThrow(FileColumns._ID)
        val nameColumn = getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val mimeTypeColumn = getColumnIndexOrThrow(FileColumns.MIME_TYPE)
        val dateAddedColumn = getColumnIndexOrThrow(FileColumns.DATE_ADDED)
        val dateModifiedColumn = getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
        val mediaTypeColumn = getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val favoriteColumn = if (Features.isNativeFavoritesSupported) {
            getColumnIndex(FileColumns.IS_FAVORITE)
        } else {
            -1
        }
        val trashedColumn = if (Features.isRecycleBinSupported) {
            getColumnIndex(FileColumns.IS_TRASHED)
        } else {
            -1
        }
        val pathColumn = if (Features.isPathColumnSupported) {
            getColumnIndex(FileColumns.RELATIVE_PATH)
        } else {
            getColumnIndex(FileColumns.DATA)
        }
        val result = ArrayList<MediaItem>(count)
        do {
            val id = getLong(idColumn)
            val isVideo = getInt(mediaTypeColumn) == FileColumns.MEDIA_TYPE_VIDEO
            val contentUri = if (isVideo) {
                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            } else {
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            }
            result += MediaItem(
                index = if (withIndices) result.size else -1,
                id = id,
                name = getString(nameColumn),
                mimeType = getString(mimeTypeColumn),
                uri = contentUri,
                dateAdded = getLong(dateAddedColumn),
                dateModified = getLong(dateModifiedColumn),
                isFavorite = if (favoriteColumn >= 0) {
                    getInt(favoriteColumn) > 0
                } else {
                    legacyFavoritesRepository.isFavorite(id)
                },
                isVideo = isVideo,
                isTrashed = trashedColumn >= 0 && getInt(trashedColumn) > 0,
                path = if (pathColumn >= 0) {
                    getStringOrNull(pathColumn)
                } else {
                    null
                },
            )
        } while (moveToNext())
        return result
    }

    protected suspend fun queryCount(mediaQuery: MediaQuery): Int {
        val queryParams = queryBuilder(mediaQuery)
        return contentResolver.queryCompat(
            uri = baseUri,
            projection = arrayOf(FileColumns._ID),
            selection = queryParams.selection,
            selectionArgs = queryParams.selectionArgs.toTypedArray(),
        ).use {
            it.count
        }
    }

    private fun defaultProjection() = buildList(10) {
        add(FileColumns._ID)
        add(FileColumns.DISPLAY_NAME)
        add(FileColumns.MIME_TYPE)
        add(FileColumns.DATE_ADDED)
        add(FileColumns.DATE_MODIFIED)
        add(FileColumns.MEDIA_TYPE)
        if (Features.isNativeFavoritesSupported) {
            add(FileColumns.IS_FAVORITE)
        }
        if (Features.isPathColumnSupported) {
            add(FileColumns.RELATIVE_PATH)
        } else {
            add(FileColumns.DATA)
        }
    }.toTypedArray()
}