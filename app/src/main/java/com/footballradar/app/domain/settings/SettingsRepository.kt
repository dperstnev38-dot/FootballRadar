package com.footballradar.app.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setColor(setting: ThemeColorSetting, hex: String)
}
