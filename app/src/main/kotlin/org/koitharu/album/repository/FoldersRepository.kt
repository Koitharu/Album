package org.koitharu.album.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import dagger.Reusable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koitharu.album.model.MediaFolder
import javax.inject.Inject

@Reusable
class FoldersRepository @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    fun observeFolders(): Flow<List<MediaFolder>> = flow {
        emit(getFolders())
    }

    suspend fun getFolders(): ImmutableList<MediaFolder> {
        return contentResolver.queryCompat(
            uri = MediaStore.Files.getContentUri("external"),
            projection = arrayOf(
                FileColumns.BUCKET_ID,
                FileColumns.BUCKET_DISPLAY_NAME,
                FileColumns.MEDIA_TYPE,
                FileColumns._ID,
            ),
            selection = "${FileColumns.MEDIA_TYPE} = ? OR ${FileColumns.MEDIA_TYPE} = ?",
            selectionArgs = arrayOf(
                FileColumns.MEDIA_TYPE_IMAGE.toString(),
                FileColumns.MEDIA_TYPE_VIDEO.toString()
            ),
            orderBy = FileColumns.DATE_ADDED,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use persistentListOf()
            }
            val idColumn = cursor.getColumnIndexOrThrow(FileColumns._ID)
            val bucketIdColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            val result = HashMap<String, MediaFolder>()
            while (cursor.moveToNext()) {

                val bucketId = cursor.getString(bucketIdColumn)
                val bucketName = cursor.getString(nameColumn) ?: "Unknown Folder"

                if (result.containsKey(bucketId)) {
                    result[bucketId]?.let { it.size++ }
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
                        thumbnail = contentUri
                            .buildUpon()
                            .scheme("thumb+" + contentUri.scheme)
                            .build(),
                    )
                    result[bucketId] = newFolder
                    result.put(bucketId, newFolder)
                }
            }
            result.values.toImmutableList()
        } ?: persistentListOf()
    }
}