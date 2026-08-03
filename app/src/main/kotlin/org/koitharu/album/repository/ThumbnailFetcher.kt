package org.koitharu.album.repository

import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import coil3.toAndroidUri
import org.koitharu.album.ui.util.runCancellable

@RequiresApi(Build.VERSION_CODES.Q)
class ThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val contentResolver = options.context.contentResolver
        val uri = data.newBuilder()
            .scheme(data.scheme?.removePrefix("thumb+"))
            .build()
        val thumb = runCancellable { signal ->
            contentResolver.loadThumbnail(
                uri.toAndroidUri(),
                Size(
                    options.size.width.pxOrElse { 200 },
                    options.size.height.pxOrElse { 200 },
                ),
                signal,
            )
        }
        return ImageFetchResult(
            image = thumb.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {

        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!isApplicable(data)) return null
            return ThumbnailFetcher(data, options)
        }

        private fun isApplicable(data: Uri): Boolean {
            return data.scheme?.startsWith("thumb+") == true
        }
    }
}