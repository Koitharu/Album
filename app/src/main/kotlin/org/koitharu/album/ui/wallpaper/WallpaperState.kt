package org.koitharu.album.ui.wallpaper

import androidx.compose.runtime.Immutable

@Immutable
data class WallpaperState(
    val target: WallpaperTarget,
    val imageUri: String,
    val isLoading: Boolean,
) {

    constructor(imageUri: String) : this(
        target = WallpaperTarget.HOME_SCREEN,
        imageUri = imageUri,
        isLoading = false,
    )
}
