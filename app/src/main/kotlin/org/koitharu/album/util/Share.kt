package org.koitharu.album.util

import android.content.Context
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import org.koitharu.album.ui.common.AlbumItem

fun shareMedia(context: Context, media: AlbumItem.Media) {
    ShareCompat.IntentBuilder(context)
        .addStream(media.uri)
        .setType(media.mimeType)
        .startChooser()
}

fun shareImage(context: Context, uri: String) {
    ShareCompat.IntentBuilder(context)
        .addStream(uri.toUri())
        .setType("image/*")
        .startChooser()
}