package org.koitharu.album.ui.common

import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import coil3.toAndroidUri

fun deleteMedia(context: Context, media: AlbumItem.Media) {
    val uri = media.uri.toAndroidUri()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        deleteImages(context, listOf(uri))
    } else {
        deleteImagesLegacy(context, uri)
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun deleteImages(context: Context, imageUris: List<Uri>) {
    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, imageUris)
    context.startIntentSender(
        pendingIntent.intentSender,
        null,
        0,
        0,
        0
    )
}

private fun deleteImagesLegacy(context: Context, imageUri: Uri) {
    try {
        context.contentResolver.delete(imageUri, null, null)
    } catch (securityException: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
            context.startIntentSender(
                securityException.userAction.actionIntent.intentSender,
                null,
                0,
                0,
                0
            )
        } else {
            // TODO
        }
    }
}