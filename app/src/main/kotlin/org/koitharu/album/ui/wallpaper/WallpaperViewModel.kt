package org.koitharu.album.ui.wallpaper

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.wallpaper.WallpaperEffect.OnError
import org.koitharu.album.ui.wallpaper.WallpaperEffect.OnWallpaperApplied
import org.koitharu.album.ui.wallpaper.WallpaperIntent.ApplyWallpaper
import org.koitharu.album.ui.wallpaper.WallpaperIntent.SetCropRect
import org.koitharu.album.ui.wallpaper.WallpaperIntent.SetTarget
import org.koitharu.album.ui.wallpaper.WallpaperTarget.BOTH
import org.koitharu.album.ui.wallpaper.WallpaperTarget.HOME_SCREEN
import org.koitharu.album.ui.wallpaper.WallpaperTarget.LOCKSCREEN

@HiltViewModel(assistedFactory = WallpaperViewModel.Factory::class)
class WallpaperViewModel @AssistedInject constructor(
    @Assisted imageUri: String,
    @ApplicationContext context: Context,
    private val contentResolver: ContentResolver,
) : MviViewModel<WallpaperState, WallpaperIntent, WallpaperEffect>(WallpaperState(imageUri)) {

    private val wallpaperManager =
        context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
    private var cropRect = Rect.Zero

    init {
        if (!wallpaperManager.isWallpaperSupported && wallpaperManager.isSetWallpaperAllowed) {
            sendEffectAsync(OnError(IllegalStateException("Wallpapers are not supported")))
        }
    }

    override fun handleIntent(intent: WallpaperIntent) {
        when (intent) {
            is SetTarget -> state.update {
                it.copy(target = intent.target)
            }

            is SetCropRect -> cropRect = intent.rect
            ApplyWallpaper -> applyWallpaper()
        }
    }

    private fun applyWallpaper() {
        viewModelScope.launch(Dispatchers.Default) {
            val stateSnapshot = state.updateAndGet { it.copy(isLoading = true) }
            try {
                contentResolver.openInputStream(stateSnapshot.imageUri.toUri())?.use {
                    val which = when (stateSnapshot.target) {
                        HOME_SCREEN -> WallpaperManager.FLAG_SYSTEM
                        LOCKSCREEN -> WallpaperManager.FLAG_LOCK
                        BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }
                    wallpaperManager.setStream(
                        it,
                        cropRect.roundToIntRect().toAndroidRect(),
                        true,
                        which
                    )
                } ?: error("Unable to load image")
                sendEffect(OnWallpaperApplied)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                sendEffect(OnError(e))
            } finally {
                state.update { it.copy(isLoading = false) }
            }
        }

    }

    @AssistedFactory
    interface Factory {

        fun create(imageUri: String): WallpaperViewModel
    }
}