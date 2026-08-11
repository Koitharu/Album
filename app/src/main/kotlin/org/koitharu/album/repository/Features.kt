package org.koitharu.album.repository

import android.os.Build

object Features {

    val isNativeFavoritesSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val isRecycleBinSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val isPathColumnSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}