package org.koitharu.album.ui.settings

import org.koitharu.album.model.ThemeVariant

sealed interface SettingsIntent {

    data class SetIsRecycleBinEnabled(val value: Boolean) : SettingsIntent

    data class SetIsRotationGestureEnabled(val value: Boolean) : SettingsIntent

    data class SetAppTheme(val value: ThemeVariant) : SettingsIntent

    data class SetViewerTheme(val value: ThemeVariant) : SettingsIntent
}