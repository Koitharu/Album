package org.koitharu.album.ui.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.repository.SettingsRepository

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ThemeResolverEntryPoint {
    val settingsRepository: SettingsRepository
}

@Composable
fun resolveThemeVariant(
    isForViewer: Boolean,
): ThemeVariant {
    val activity = LocalActivity.current ?: return ThemeVariant.SYSTEM
    val settings = remember(activity) {
        EntryPointAccessors.fromActivity<ThemeResolverEntryPoint>(activity).settingsRepository
    }
    val result by if (isForViewer) {
        settings.viewerTheme
    } else {
        settings.appTheme
    }.collectAsState()
    return result
}