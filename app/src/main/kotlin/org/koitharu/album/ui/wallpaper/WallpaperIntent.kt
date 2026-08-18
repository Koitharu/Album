package org.koitharu.album.ui.wallpaper

import androidx.compose.ui.geometry.Rect

sealed interface WallpaperIntent {

    data class SetTarget(
        val target: WallpaperTarget,
    ): WallpaperIntent

    data class SetCropRect(
        val rect: Rect,
    ): WallpaperIntent

    data object  ApplyWallpaper: WallpaperIntent
}