package org.koitharu.album.ui.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koitharu.album.model.HomeBannerSource
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.repository.SettingsRepository
import org.koitharu.album.ui.common.MviViewModel
import org.koitharu.album.ui.settings.SettingsIntent.SetAppTheme
import org.koitharu.album.ui.settings.SettingsIntent.SetHomeBanner
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRecycleBinEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRotationGestureEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetUseExternalEditor
import org.koitharu.album.ui.settings.SettingsIntent.SetViewerTheme
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent, Nothing>(SettingsState()) {

    init {
        viewModelScope.launch(Dispatchers.Default) {
            combine<Any, SettingsState>(
                repository.useRecycleBin,
                repository.isRotationGestureEnabled,
                repository.appTheme,
                repository.viewerTheme,
                repository.homeBannerSource,
                repository.useExternalEditor,
            ) { data ->
                SettingsState(
                    isRecycleBinEnabled = data[0] as Boolean,
                    isRotationGestureEnabled = data[1] as Boolean,
                    appTheme = data[2] as ThemeVariant,
                    viewerTheme = data[3] as ThemeVariant,
                    homeBanner = data[4] as HomeBannerSource,
                    useExternalEditor = data[5] as Boolean,
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
                is SetUseExternalEditor -> repository.setUseExternalEditor(intent.value)
            }
        }
    }
}