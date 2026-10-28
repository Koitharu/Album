package org.koitharu.album.ui.settings

import androidx.compose.runtime.Immutable
import org.koitharu.album.model.HomeBannerSource
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.model.VideosAvailability
import org.koitharu.album.model.VideosAvailability.ALL
import org.koitharu.album.repository.Features

@Immutable
data class SettingsState(
    val isRecycleBinEnabled: Boolean,
    val isRotationGestureEnabled: Boolean,
    val isCloseGestureEnabled: Boolean,
    val appTheme: ThemeVariant,
    val viewerTheme: ThemeVariant,
    val homeBanner: HomeBannerSource,
    val useExternalEditor: Boolean,
    val showVideos: VideosAvailability,
    val isVideosMutedOnStart: Boolean,
    val showAllDCIMFolders: Boolean,
) {

    constructor() : this(
        isRecycleBinEnabled = Features.isRecycleBinSupported,
        isRotationGestureEnabled = false,
        isCloseGestureEnabled = true,
        appTheme = ThemeVariant.SYSTEM,
        viewerTheme = ThemeVariant.DARK,
        homeBanner = HomeBannerSource.RANDOM,
        useExternalEditor = false,
        showVideos = ALL,
        isVideosMutedOnStart = false,
        showAllDCIMFolders = false,
    )
}