package org.koitharu.album.ui.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.settings.SettingsIntent.SetAppTheme
import org.koitharu.album.ui.settings.SettingsIntent.SetHomeBanner
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRecycleBinEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRotationGestureEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetViewerTheme
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent, Nothing>(SettingsState()) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine(
                repository.useRecycleBin,
                repository.isRotationGestureEnabled,
                repository.appTheme,
                repository.viewerTheme,
                repository.homeBannerSource,
            ) { useRecycleBin, isRotationGestureEnabled, appTheme, viewerTheme, homeBanner ->
                SettingsState(
                    isRecycleBinEnabled = useRecycleBin,
                    isRotationGestureEnabled = isRotationGestureEnabled,
                    appTheme = appTheme,
                    viewerTheme = viewerTheme,
                    homeBanner = homeBanner,
                )
            }.collect {
                state.value = it
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (intent) {
                is SetAppTheme -> repository.setAppTheme(intent.value)
                is SetIsRecycleBinEnabled -> repository.setUseRecycleBin(intent.value)
                is SetIsRotationGestureEnabled -> repository.setRotationGestureEnabled(intent.value)
                is SetViewerTheme -> repository.setViewerTheme(intent.value)
                is SetHomeBanner -> repository.setHomeBannerSource(intent.value)
            }
        }
    }
}