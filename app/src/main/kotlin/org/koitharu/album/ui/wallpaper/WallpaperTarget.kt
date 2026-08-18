package org.koitharu.album.ui.wallpaper

import androidx.annotation.StringRes
import org.koitharu.album.R

enum class WallpaperTarget(
    @StringRes val label: Int,
) {

    HOME_SCREEN(R.string.home_screen),
    LOCKSCREEN(R.string.lockscreen),
    BOTH(R.string.both),
}