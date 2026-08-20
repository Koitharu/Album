package org.koitharu.album.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.koitharu.album.R
import org.koitharu.album.model.HomeBannerSource
import org.koitharu.album.model.ThemeVariant
import org.koitharu.album.repository.Features
import org.koitharu.album.repository.SettingsRepository.Keys
import org.koitharu.album.ui.common.MviIntentHandler
import org.koitharu.album.ui.settings.SettingsIntent.SetAppTheme
import org.koitharu.album.ui.settings.SettingsIntent.SetHomeBanner
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRecycleBinEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetIsRotationGestureEnabled
import org.koitharu.album.ui.settings.SettingsIntent.SetUseExternalEditor
import org.koitharu.album.ui.settings.SettingsIntent.SetViewerTheme
import org.koitharu.album.ui.settings.preferences.ListPreference
import org.koitharu.album.ui.settings.preferences.PreferenceCategory
import org.koitharu.album.ui.settings.preferences.SwitchPreference
import org.koitharu.album.ui.theme.AlbumTheme
import org.koitharu.album.util.IconButtonWithTooltip

@Composable
fun SettingsScreen(
    state: SettingsState,
    handleIntent: MviIntentHandler<SettingsIntent>,
    onClose: () -> Unit,
) = Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings),
                )
            },
            navigationIcon = {
                IconButtonWithTooltip(
                    tooltip = stringResource(R.string.back),
                    onClick = onClose,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )
    }
) { innerPadding ->
    val themeVariants = rememberThemeVariants()
    LazyColumn(
        contentPadding = innerPadding
    ) {
        item(key = R.string.appearance) {
            PreferenceCategory(stringResource(R.string.appearance))
        }
        item(Keys.appTheme.name) {
            ListPreference(
                title = stringResource(R.string.app_theme),
                entries = themeVariants,
                entryValues = ThemeVariant.entries.toPersistentList(),
                selectedValue = state.appTheme,
                onEntryClick = { handleIntent(SetAppTheme(it)) }
            )
        }
        item(Keys.viewerTheme.name) {
            ListPreference(
                title = stringResource(R.string.viewer_theme),
                entries = themeVariants,
                entryValues = ThemeVariant.entries.toPersistentList(),
                selectedValue = state.viewerTheme,
                onEntryClick = { handleIntent(SetViewerTheme(it)) }
            )
        }
        item(Keys.homeBanner.name) {
            ListPreference(
                title = stringResource(R.string.home_slideshow),
                entries = persistentListOf(
                    stringResource(R.string.random_image),
                    stringResource(R.string.none),
                ),
                entryValues = HomeBannerSource.entries.toPersistentList(),
                selectedValue = state.homeBanner,
                onEntryClick = { handleIntent(SetHomeBanner(it)) }
            )
        }
        item(key = R.string.behavior) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
            )
            PreferenceCategory(stringResource(R.string.behavior))
        }
        if (Features.isRecycleBinSupported) {
            item(Keys.useRecycleBin.name) {
                SwitchPreference(
                    title = stringResource(R.string.use_recycle_bin),
                    summary = stringResource(R.string.use_recycle_bin_summary),
                    isChecked = state.isRecycleBinEnabled,
                    onClick = { handleIntent(SetIsRecycleBinEnabled(!state.isRecycleBinEnabled)) },
                )
            }
        }
        item(Keys.allowRotationGesture.name) {
            SwitchPreference(
                title = stringResource(R.string.allow_rotation_gesture),
                summary = stringResource(R.string.allow_rotation_gesture_summary),
                isChecked = state.isRotationGestureEnabled,
                onClick = { handleIntent(SetIsRotationGestureEnabled(!state.isRotationGestureEnabled)) },
            )
        }
        item(Keys.useExternalEditor.name) {
            SwitchPreference(
                title = stringResource(R.string.use_external_editor),
                summary = stringResource(R.string.use_external_editor_summary),
                isChecked = state.useExternalEditor,
                onClick = { handleIntent(SetUseExternalEditor(!state.useExternalEditor)) },
            )
        }
    }
}

@Composable
private fun rememberThemeVariants(): PersistentList<String> {
    val resources = LocalResources.current
    return remember(resources) {
        persistentListOf(
            resources.getString(R.string.follow_system),
            resources.getString(R.string.light),
            resources.getString(R.string.dark),
        )
    }
}

@Preview
@Composable
private fun PreviewSettingsScreen() = AlbumTheme {
    SettingsScreen(
        state = SettingsState(),
        handleIntent = MviIntentHandler.NoOp,
        onClose = { /* no-op */ },
    )
}