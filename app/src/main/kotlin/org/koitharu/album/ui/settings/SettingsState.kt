package org.koitharu.album.ui.settings

import androidx.compose.runtime.Immutable
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.repository.Features

@Immutable
data class SettingsState(
    val isRecycleBinEnabled: Boolean,
    val isRotationGestureEnabled: Boolean,
    val appTheme: ThemeVariant,
    val viewerTheme: ThemeVariant,
) {

    constructor() : this(
        isRecycleBinEnabled = Features.isRecycleBinSupported,
        isRotationGestureEnabled = false,
        appTheme = ThemeVariant.SYSTEM,
        viewerTheme = ThemeVariant.DARK,
    )
}