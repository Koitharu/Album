package org.koitharu.album.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.koitharu.album.util.ActivityContextProvider
import org.koitharu.album.util.IntentSenderLauncher
import org.koitharu.album.util.resolve

class ExifEditor @AssistedInject constructor(
    @Assisted private val uri: Uri,
    private val contentResolver: ContentResolver,
    private val activityContextProvider: ActivityContextProvider,
) {

    private var rotation: Int = 0

    fun rotate(degrees: Int) = apply {
        rotation = degrees
    }

    suspend fun commit() = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestWritePermission()
        }
        commitImpl()
    } catch (e: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            if (e.resolve(activityContextProvider.get())) {
                commitImpl()
            } else {
                throw e
            }
        } else {
            throw e
        }
    }

    private suspend fun commitImpl() = runInterruptible(Dispatchers.IO) {
        contentResolver.openFileDescriptor(uri, "rw")?.use { fd ->
            val exif = ExifInterface(fd.fileDescriptor)
            if (rotation != 0) {
                exif.setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    mapOrientation(exif, rotation).toString()
                )
            }
            exif.saveAttributes()
            updateModificationDate()
        } ?: error("Cannot open $uri fd")
    }

    private fun updateModificationDate() {
        val cv = ContentValues(1)
        cv.put(MediaStore.Files.FileColumns.DATE_MODIFIED, System.currentTimeMillis())
        contentResolver.update(uri, cv, null, null)
    }

    private fun mapOrientation(exif: ExifInterface, rotationDegrees: Int): Int {
        return when (val rotation = (exif.rotationDegrees + rotationDegrees + 360) % 360) {
            0 -> ExifInterface.ORIENTATION_NORMAL
            90 -> ExifInterface.ORIENTATION_ROTATE_90
            180 -> ExifInterface.ORIENTATION_ROTATE_180
            270 -> ExifInterface.ORIENTATION_ROTATE_270
            else -> throw IllegalArgumentException("Wrong rotation $rotation")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestWritePermission() {
        val launcher = IntentSenderLauncher.from(activityContextProvider.get()) ?: return
        val request = MediaStore.createWriteRequest(contentResolver, listOf(uri))
        checkNotNull(launcher.awaitIntentSenderForResult(request.intentSender)) {
            "No permissions"
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(uri: Uri): ExifEditor
    }
}