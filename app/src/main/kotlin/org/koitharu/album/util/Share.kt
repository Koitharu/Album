package org.koitharu.album.util

import android.content.Context
import androidx.core.app.ShareCompat
import coil3.toAndroidUri
import org.koitharu.album.ui.common.AlbumItem

fun shareMedia(context: Context, media: AlbumItem.Media) {
    ShareCompat.IntentBuilder(context)
        .addStream(media.uri.toAndroidUri())
        .setType(media.mimeType)
        .startChooser()
}