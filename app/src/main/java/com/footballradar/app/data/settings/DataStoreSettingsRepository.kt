package com.footballradar.app.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.footballradar.app.domain.settings.AppSettings
import com.footballradar.app.domain.settings.SettingsRepository
import com.footballradar.app.domain.settings.ThemeColorSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            colors = ThemeColorSetting.entries.associateWith { setting ->
                preferences[stringPreferencesKey(setting.name)] ?: setting.defaultHex
            },
        )
    }

    override suspend fun setColor(setting: ThemeColorSetting, hex: String) {
        require(HEX_COLOR.matches(hex)) { "Color must use #RRGGBB format" }
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(setting.name)] = hex.uppercase()
        }
    }

    private companion object {
        val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
