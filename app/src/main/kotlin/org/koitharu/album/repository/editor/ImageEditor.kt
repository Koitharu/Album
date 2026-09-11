package org.koitharu.album.repository.editor

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.database.getStringOrNull
import coil3.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.size.Size
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.album.R
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.queryCompat
import org.koitharu.album.util.ActivityContextProvider
import org.koitharu.album.util.IntentSenderLauncher
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ImageEditor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    private val activityContextProvider: ActivityContextProvider,
) {

    private val imageLoader: ImageLoader
        get() = SingletonImageLoader.get(context)

    private val fileIndexRegex = Regex("\\(([0-9]+)\\)$")

    suspend fun editAndReplace(
        sourceUri: Uri,
        operations: List<ImageEditOperation>,
        targetUri: Uri,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestWritePermission(targetUri)
        }
        val type = getImageType(sourceUri) ?: ImageType.PNG
        val image = prepareImage(sourceUri, operations)
        try {
            checkNotNull(contentResolver.openOutputStream(targetUri)) {
                "Unable to open image for writing"
            }.use { output ->
                image.compress(type.compressFormat, 100, output)
            }

            val timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_MODIFIED, timestamp)
            }
            contentResolver.update(targetUri, contentValues, null, null)
            contentResolver.notifyChange(targetUri, null)
        } finally {
            image.recycle()
        }
    }

    suspend fun editAndShare(
        sourceUri: Uri,
        operations: List<ImageEditOperation>,
    ) {
        val type = getImageType(sourceUri) ?: ImageType.PNG
        val name = sourceUri.lastPathSegment + "." + type.extension
        val targetDir = File(context.externalCacheDir ?: context.cacheDir, "edited")
        targetDir.mkdirs()
        val tempFile = File(targetDir, name)
        val image = prepareImage(sourceUri, operations)
        try {
            tempFile.outputStream().use { output ->
                image.compress(type.compressFormat, 100, output)
            }
        } finally {
            image.recycle()
        }
        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.files", tempFile)
        ShareCompat.IntentBuilder(activityContextProvider.get())
            .setStream(fileUri)
            .setChooserTitle(R.string.share)
            .setType(type.mimeType)
            .startChooser()
    }

    suspend fun editAndSaveCopy(
        sourceUri: Uri,
        operations: List<ImageEditOperation>,
    ) {
        val targetDir = findPath(sourceUri)
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        val name =
            getImageName(sourceUri) ?: sourceUri.lastPathSegment ?: sourceUri.hashCode().toString()
        val image = prepareImage(sourceUri, operations)
        val type = getImageType(sourceUri) ?: ImageType.PNG
        val targetName = if (name.endsWith(type.extension, ignoreCase = true)) {
            name
        } else {
            name + "." + type.extension
        }.nextFileName()
        try {
            val newUri = writeNewImage(
                targetDir = targetDir,
                name = targetName,
                type = type,
                bitmap = image,
            )
            contentResolver.notifyChange(newUri, null)
        } finally {
            image.recycle()
        }
    }

    private suspend fun prepareImage(
        uri: Uri,
        operations: List<ImageEditOperation>,
    ): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .transformations(operations)
            .size(Size.ORIGINAL)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
        return when (val result = imageLoader.execute(request)) {
            is ErrorResult -> throw result.throwable
            is SuccessResult -> result.image.toBitmap()
        }
    }

    private suspend fun writeNewImage(
        targetDir: String,
        name: String,
        type: ImageType,
        bitmap: Bitmap,
    ): Uri = runInterruptible(Dispatchers.IO) {
        val timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, type.mimeType)
            put(MediaStore.Images.Media.DATE_ADDED, timestamp)
            put(MediaStore.Images.Media.DATE_MODIFIED, timestamp)

            if (Features.isPathColumnSupported) {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, targetDir)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val targetFile = File(targetDir, name)
                put(MediaStore.Images.Media.TITLE, name)
                put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
            }
        }

        // Insert entry and write bytes
        val targetUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("Cannot create a new MediaStore entry for $targetDir/$name")
        try {
            contentResolver.openOutputStream(targetUri)?.use { stream ->
                bitmap.compress(type.compressFormat, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(targetUri, contentValues, null, null)
            }
            targetUri
        } catch (e: Exception) {
            contentResolver.delete(targetUri, null, null)
            throw e
        }
    }

    private suspend fun findPath(sourceUri: Uri): String? {
        return if (Features.isPathColumnSupported) {
            val projection = arrayOf(MediaStore.Images.Media.RELATIVE_PATH)
            contentResolver.queryCompat(
                uri = sourceUri,
                projection = projection,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                    if (index != -1) {
                        cursor.getString(index)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } else {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentResolver.queryCompat(
                uri = sourceUri,
                projection = projection,
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (index != -1) {
                        val absolutePath = cursor.getString(index)
                        val sourceFile = File(absolutePath)
                        sourceFile.parentFile?.absolutePath
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestWritePermission(target: Uri): Boolean {
        val intent = MediaStore.createWriteRequest(contentResolver, listOf(target)).intentSender
        return IntentSenderLauncher.from(activityContextProvider.get())
            ?.awaitIntentSenderForResult(intent) != null
    }

    private suspend fun getImageType(uri: Uri) = contentResolver.queryCompat(
        uri = uri,
        projection = arrayOf(FileColumns.MIME_TYPE),
    ).let { cursor ->
        if (cursor.moveToFirst()) {
            val column = cursor.getColumnIndex(FileColumns.MIME_TYPE)
            if (column >= 0) {
                cursor.getStringOrNull(column)
            } else {
                null
            }
        } else {
            null
        }
    }?.let { mime ->
        ImageType.entries.find { x -> x.mimeType == mime }
    }

    private suspend fun getImageName(uri: Uri) = contentResolver.queryCompat(
        uri = uri,
        projection = arrayOf(FileColumns.DISPLAY_NAME),
    ).let { cursor ->
        if (cursor.moveToFirst()) {
            val column = cursor.getColumnIndex(FileColumns.DISPLAY_NAME)
            if (column >= 0) {
                cursor.getStringOrNull(column)
            } else {
                null
            }
        } else {
            null
        }
    }

    private enum class ImageType(
        val mimeType: String,
        val compressFormat: CompressFormat,
        val extension: String,
    ) {
        JPEG(
            mimeType = "image/jpeg",
            compressFormat = CompressFormat.JPEG,
            extension = "jpg",
        ),
        PNG(
            mimeType = "image/png",
            compressFormat = CompressFormat.PNG,
            extension = "png",
        ),
    }

    private fun String.nextFileName(): String {
        val baseName = substringBeforeLast('.')
        val ext = substringBeforeLast('.', "")
        val newName = if (baseName.contains(fileIndexRegex)) {
            runCatching {
                baseName.replace(fileIndexRegex) {
                    it.value.toInt().plus(1).toString()
                }
            }.getOrElse {
                "$baseName(0)"
            }
        } else {
            "$baseName(0)"
        }
        return "$newName.$ext"
    }
}