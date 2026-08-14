package org.koitharu.album.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LifecycleCoroutineScope
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.album.model.ThemeVariant
import javax.inject.Inject

@Reusable
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    coroutineScope: LifecycleCoroutineScope,
) {

    val gridScale: Flow<Float>
        get() = context.dataStore.data.map { prefs ->
            prefs[Keys.gridScale] ?: GRID_SCALE_DEFAULT
        }

    val isRotationGestureEnabled: Flow<Boolean>
        get() = context.dataStore.data.map { prefs ->
            prefs[Keys.allowRotationGesture] ?: false
        }

    val useRecycleBin: Flow<Boolean>
        get() = context.dataStore.data.map { prefs ->
            prefs[Keys.useRecycleBin] ?: Features.isRecycleBinSupported
        }

    val appTheme: StateFlow<ThemeVariant> = context.dataStore.data.map { prefs ->
        prefs[Keys.appTheme, ThemeVariant.SYSTEM]
    }.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Eagerly, ThemeVariant.SYSTEM)

    val viewerTheme: StateFlow<ThemeVariant> = context.dataStore.data.map { prefs ->
        prefs[Keys.viewerTheme, ThemeVariant.DARK]
    }.stateIn(coroutineScope + Dispatchers.Default, SharingStarted.Eagerly, ThemeVariant.DARK)

    suspend fun setGridScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.gridScale] = scale
        }
    }

    suspend fun setUseRecycleBin(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.useRecycleBin] = value
        }
    }

    suspend fun setRotationGestureEnabled(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.allowRotationGesture] = value
        }
    }

    suspend fun setAppTheme(theme: ThemeVariant) {
        context.dataStore.edit { prefs ->
            prefs[Keys.appTheme] = theme.name
        }
    }

    suspend fun setViewerTheme(theme: ThemeVariant) {
        context.dataStore.edit { prefs ->
            prefs[Keys.viewerTheme] = theme.name
        }
    }

    object Keys {

        val useRecycleBin = booleanPreferencesKey("recycle_bin")
        val allowRotationGesture = booleanPreferencesKey("rotation_gesture")
        val gridScale = floatPreferencesKey("grid_scale")
        val appTheme = stringPreferencesKey("app_theme")
        val viewerTheme = stringPreferencesKey("viewer_theme")
    }

    companion object {

        const val GRID_SCALE_DEFAULT = 2f

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        private operator fun <E : Enum<E>> Preferences.get(
            key: Preferences.Key<String>,
            fallback: E,
        ): E {
            val stringValue = this[key] ?: return fallback
            val values = fallback.javaClass.enumConstants
            return values?.find { it.name == stringValue } ?: fallback
        }
    }
}