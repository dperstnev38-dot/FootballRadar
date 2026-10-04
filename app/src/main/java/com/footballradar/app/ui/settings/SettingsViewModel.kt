package com.footballradar.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footballradar.app.data.settings.DataStoreSettingsRepository
import com.footballradar.app.data.settings.settingsDataStore
import com.footballradar.app.domain.settings.AppSettings
import com.footballradar.app.domain.settings.ThemeColorSetting
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataStoreSettingsRepository(application.settingsDataStore)

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    fun setColor(setting: ThemeColorSetting, hex: String) {
        viewModelScope.launch {
            repository.setColor(setting, hex)
        }
    }
}
