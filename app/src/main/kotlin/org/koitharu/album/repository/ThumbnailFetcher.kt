package org.koitharu.album.repository

import android.os.Build
import android.provider.MediaStore.Images.Thumbnails
import android.util.Size
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
import org.koitharu.album.util.runCancellable
import android.net.Uri as AndroidUri

class ThumbnailFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val contentResolver = options.context.contentResolver
        val uri = data.newBuilder()
            .scheme(data.scheme?.removePrefix(SCHEME_PREFIX))
            .build()
            .toAndroidUri()
        val thumb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCancellable { signal ->
                contentResolver.loadThumbnail(
                    uri,
                    Size(
                        options.size.width.pxOrElse { 200 },
                        options.size.height.pxOrElse { 200 },
                    ),
                    signal,
                )
            }
        } else {
            @Suppress("DEPRECATION")
            Thumbnails.getThumbnail(
                contentResolver,
                requireNotNull(uri.lastPathSegment?.toLong()),
                Thumbnails.MINI_KIND,
                null
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
            return data.scheme?.startsWith(SCHEME_PREFIX) == true
        }
    }

    companion object {

        private const val SCHEME_PREFIX = "thumb+"

        fun AndroidUri.thumbnailUri(): AndroidUri = buildUpon()
            .scheme(scheme?.let { SCHEME_PREFIX + it })
            .build()
    }
}