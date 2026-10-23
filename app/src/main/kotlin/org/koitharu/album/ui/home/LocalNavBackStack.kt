package org.koitharu.album.ui.home

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack

val LocalNavBackStack: ProvidableCompositionLocal<NavBackStack<NavDestination>> =
    compositionLocalOf {
        throw NotImplementedError("No default back stack has been provided")
    }