package org.koitharu.album.ui.util

import android.content.Context
import androidx.core.app.ShareCompat
import coil3.toAndroidUri
import org.koitharu.album.ui.album.AlbumItem

fun shareMedia(context: Context, media: AlbumItem.Media) {
    ShareCompat.IntentBuilder(context)
        .addStream(media.uri.toAndroidUri())
        .setType(media.mimeType)
        .startChooser()
}