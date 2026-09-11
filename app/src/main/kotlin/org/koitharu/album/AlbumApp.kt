package org.koitharu.album

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import org.koitharu.album.repository.ThumbnailFetcher
import org.koitharu.album.util.ActivityContextProvider
import javax.inject.Inject

@HiltAndroidApp
class AlbumApp : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var activityContextProvider: ActivityContextProvider

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityContextProvider)
    }


    override fun newImageLoader(
        context: PlatformContext,
    ): ImageLoader = ImageLoader.Builder(applicationContext)
        .crossfade(true)
        .diskCachePolicy(CachePolicy.DISABLED)
        .fetcherCoroutineContext(Dispatchers.Default)
        .interceptorCoroutineContext(Dispatchers.Default)
        .components {
            add(ThumbnailFetcher.Factory())
        }.build()

    companion object {

        const val GITHUB_URL = "https://github.com/koitharu/album"
    }
}