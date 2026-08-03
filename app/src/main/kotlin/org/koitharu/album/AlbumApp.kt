package org.koitharu.album

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import org.koitharu.album.repository.ThumbnailFetcher

@HiltAndroidApp
class AlbumApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
    }


    override fun newImageLoader(
        context: PlatformContext,
    ): ImageLoader = ImageLoader.Builder(applicationContext)
        .crossfade(true)
        .fetcherCoroutineContext(Dispatchers.Default)
        .interceptorCoroutineContext(Dispatchers.Default)
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(ThumbnailFetcher.Factory())
            }
        }.build()
}