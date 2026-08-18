package org.koitharu.album.util

import android.app.Activity
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun rememberWindowInsetsController(): WindowInsetsControllerCompat? {
    val view = LocalView.current
    return remember(view) {
        val window = view.findCurrentWindow()
        if (window != null) {
            WindowInsetsControllerCompat(window, window.decorView)
        } else {
            null
        }
    }
}

@Composable
@NonRestartableComposable
fun SetSystemBarsColorsEffect(
    insetsController: WindowInsetsControllerCompat? = rememberWindowInsetsController(),
    isLightStatusBar: Boolean = insetsController?.isAppearanceLightStatusBars ?: false,
    isLightNavigationBar: Boolean = insetsController?.isAppearanceLightNavigationBars ?: false,
) = if (insetsController != null) {
    DisposableEffect(Unit) {
        val wasLightStatusBar = insetsController.isAppearanceLightStatusBars
        val wasLightNavBar = insetsController.isAppearanceLightNavigationBars
        insetsController.isAppearanceLightStatusBars = isLightStatusBar
        insetsController.isAppearanceLightNavigationBars = isLightNavigationBar
        onDispose {
            insetsController.isAppearanceLightStatusBars = wasLightStatusBar
            insetsController.isAppearanceLightNavigationBars = wasLightNavBar
        }
    }
} else {
    Unit
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


context(drawScope: DrawScope)
fun WindowInsets.toRect() = Rect(
    top = getTop(drawScope).toFloat(),
    left = getLeft(drawScope, drawScope.layoutDirection).toFloat(),
    bottom = getBottom(drawScope).toFloat(),
    right = getRight(drawScope, drawScope.layoutDirection).toFloat(),
)