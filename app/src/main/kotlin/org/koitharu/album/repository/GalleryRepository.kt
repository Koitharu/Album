package org.koitharu.album.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.provider.MediaStore.Images.ImageColumns
import coil3.toCoilUri
import dagger.Reusable
import org.koitharu.album.model.MediaItem
import javax.inject.Inject

@Reusable
class GalleryRepository @Inject constructor(
    private val contentResolver: ContentResolver,
) {

    suspend fun getRandomImage() = contentResolver.queryCompat(
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection = arrayOf(
            ImageColumns._ID,
            ImageColumns.DISPLAY_NAME,
            ImageColumns.MIME_TYPE,
            ImageColumns.DATE_ADDED,
        ),
        selection = "${ImageColumns.IS_TRASHED} = ?",
        selectionArgs = arrayOf("0"),
        orderBy = "RANDOM()",
        limit = 1,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(ImageColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(ImageColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(ImageColumns.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(ImageColumns.DATE_ADDED)
            val id = cursor.getLong(idColumn)
            MediaItem(
                index = -1,
                id = id,
                name = cursor.getString(nameColumn),
                mimeType = cursor.getString(mimeTypeColumn),
                uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    .toCoilUri(),
                dateAdded = cursor.getLong(dateAddedColumn),
                isVideo = false,
            )
        } else {
            null
        }
    }
}