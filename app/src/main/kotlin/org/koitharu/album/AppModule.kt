package org.koitharu.album

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.koitharu.album.repository.LegacyFavoritesRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository
import org.koitharu.album.repository.mediastore.MediaStoreRepository30Impl
import org.koitharu.album.repository.mediastore.MediaStoreRepositoryLegacyImpl
import org.koitharu.album.util.ActivityContextProvider

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideContentResolver(
        @ApplicationContext context: Context,
    ): ContentResolver = context.contentResolver

    @Provides
    @Reusable
    fun provideMediaStoreRepository(
        activityContextProvider: ActivityContextProvider,
        contentResolver: ContentResolver,
        legacyFavoritesRepository: LegacyFavoritesRepository,
    ): MediaStoreRepository = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> MediaStoreRepository30Impl(
            activityContextProvider = activityContextProvider,
            contentResolver = contentResolver,
            legacyFavoritesRepository = legacyFavoritesRepository,
        )

        else -> MediaStoreRepositoryLegacyImpl(
            activityContextProvider = activityContextProvider,
            contentResolver = contentResolver,
            legacyFavoritesRepository = legacyFavoritesRepository,
        )
    }
}