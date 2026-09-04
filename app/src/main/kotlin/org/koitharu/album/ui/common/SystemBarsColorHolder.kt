package org.koitharu.album.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.core.view.WindowInsetsControllerCompat
import org.koitharu.album.util.rememberWindowInsetsController

class SystemBarsColorHolder(
    var currentEffect: Any? = null
)

val LocalSystemBarsColorHolder = compositionLocalOf { SystemBarsColorHolder() }

@Composable
@NonRestartableComposable
fun SetSystemBarsColorsEffect(
    insetsController: WindowInsetsControllerCompat? = rememberWindowInsetsController(),
    isLightStatusBar: Boolean = !LocalDarkMode.current,
    isLightNavigationBar: Boolean = !LocalDarkMode.current,
) = if (insetsController != null) {
    val systemBarsColorHolder = LocalSystemBarsColorHolder.current
    DisposableEffect(isLightStatusBar, isLightNavigationBar) {
        val wasLightStatusBar = insetsController.isAppearanceLightStatusBars
        val wasLightNavBar = insetsController.isAppearanceLightNavigationBars
        val effectToken = Any()
        systemBarsColorHolder.currentEffect = effectToken
        insetsController.isAppearanceLightStatusBars = isLightStatusBar
        insetsController.isAppearanceLightNavigationBars = isLightNavigationBar
        onDispose {
            if (systemBarsColorHolder.currentEffect === effectToken) {
                insetsController.isAppearanceLightStatusBars = wasLightStatusBar
                insetsController.isAppearanceLightNavigationBars = wasLightNavBar
            }
        }
    }
} else {
    Unit
}
