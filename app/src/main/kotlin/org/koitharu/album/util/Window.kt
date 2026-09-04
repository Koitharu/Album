package org.koitharu.album.util

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun rememberWindowInsetsController(): WindowInsetsControllerCompat? {
    val view = LocalView.current
    return remember(view) {
        val window = view.findCurrentWindow()
        if (window != null) {
            WindowCompat.getInsetsController(window, view)
        } else {
            null
        }
    }
}

private fun View.findCurrentWindow(): Window? {

    // 1. Check if the view is hosted inside a Compose Dialog or Popup
    val dialogWindow = (parent as? DialogWindowProvider)?.window
    if (dialogWindow != null) return dialogWindow

    // 2. Fallback to extracting the Window from the Activity context
    var context = context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context.window
        }
        context = context.baseContext
    }

    return null
}
