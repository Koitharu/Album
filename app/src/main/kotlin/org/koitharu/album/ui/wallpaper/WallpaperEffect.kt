package org.koitharu.album.ui.wallpaper

sealed interface WallpaperEffect {

    data object OnWallpaperApplied: WallpaperEffect

    data class OnError(
        val error: Throwable
    ): WallpaperEffect
}