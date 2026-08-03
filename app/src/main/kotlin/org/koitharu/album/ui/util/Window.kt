package org.koitharu.album.ui.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun rememberWindowInsetsController(): WindowInsetsControllerCompat? {
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    return remember(window) {
        if (window != null) {
            WindowInsetsControllerCompat(window, window.decorView)
        } else {
            null
        }
    }
}