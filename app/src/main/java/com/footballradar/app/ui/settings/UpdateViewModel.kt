package com.footballradar.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footballradar.app.BuildConfig
import com.footballradar.app.data.settings.settingsDataStore
import com.footballradar.app.data.update.GitHubReleaseConfig
import com.footballradar.app.data.update.ReleaseUpdateRepository
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = ReleaseUpdateRepository()
    private val _state = MutableStateFlow<UpdateUiState>(
        if (GitHubReleaseConfig.isConfigured) UpdateUiState.Idle else UpdateUiState.NotConfigured,
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private val installRequestChannel = Channel<InstallRequest>(Channel.BUFFERED)
    val installRequests = installRequestChannel.receiveAsFlow()
    private var preparedApkUri: Uri? = null

    val currentVersion: String = BuildConfig.VERSION_NAME

    init {
        viewModelScope.launch {
            try {
                if (GitHubReleaseConfig.isConfigured && automaticCheckIsDue()) {
                    checkForUpdates()
                }
            } catch (exception: IOException) {
                _state.value = UpdateUiState.Error(
                    "Не удалось прочитать настройки автоматической проверки обновлений",
                )
            }
        }
    }

    fun checkForUpdates() {
        if (_state.value == UpdateUiState.Checking) return
        if (!GitHubReleaseConfig.isConfigured) {
            _state.value = UpdateUiState.NotConfigured
            return
        }
        viewModelScope.launch {
            _state.value = UpdateUiState.Checking
            val result = try {
                val result = withContext(Dispatchers.IO) {
                    repository.checkForUpdate(currentVersion)
                }
                result?.let(UpdateUiState::UpdateAvailable)
                    ?: UpdateUiState.UpToDate
            } catch (exception: IOException) {
                UpdateUiState.Error(
                    exception.message ?: "Не удалось проверить обновления",
                )
            } catch (exception: IllegalArgumentException) {
                UpdateUiState.Error(
                    exception.message ?: "Версия приложения в релизе некорректна",
                )
            } catch (exception: IllegalStateException) {
                UpdateUiState.Error(
                    exception.message ?: "Не удалось проверить обновления",
                )
            }
            try {
                saveLastCheckTime()
                _state.value = result
            } catch (exception: IOException) {
                _state.value = UpdateUiState.Error(
                    "Не удалось сохранить дату проверки обновлений",
                )
            }
        }
    }

    fun downloadUpdate(update: com.footballradar.app.data.update.AppUpdate) {
        viewModelScope.launch {
            try {
                val apk = File(
                    getApplication<Application>().cacheDir,
                    "updates/football-radar-${safeFileVersion(update.newVersion)}.apk",
                )
                val downloadedApk = withContext(Dispatchers.IO) {
                    repository.downloadApk(update, apk) { downloaded, total ->
                        _state.value = UpdateUiState.Downloading(downloaded, total)
                    }
                }
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    downloadedApk,
                )
                preparedApkUri = uri
                _state.value = UpdateUiState.ReadyToInstall(uri, update.newVersion)
                installRequestChannel.send(InstallRequest.OpenInstaller(uri))
            } catch (exception: IOException) {
                _state.value = UpdateUiState.Error(
                    exception.message ?: "Не удалось скачать обновление",
                )
            } catch (exception: IllegalArgumentException) {
                _state.value = UpdateUiState.Error(
                    exception.message ?: "Не удалось подготовить APK к установке",
                )
            } catch (exception: SecurityException) {
                _state.value = UpdateUiState.Error(
                    exception.message ?: "Нет разрешения на подготовку обновления",
                )
            }
        }
    }

    fun requestInstallerAfterPermission() {
        preparedApkUri?.let { uri ->
            viewModelScope.launch {
                installRequestChannel.send(InstallRequest.OpenInstaller(uri))
            }
        }
    }

    fun onInstallerLaunchFailed(message: String) {
        _state.value = UpdateUiState.Error(message)
    }

    private suspend fun automaticCheckIsDue(): Boolean {
        val preferences = getApplication<Application>().settingsDataStore.data.first()
        val lastCheck = preferences[LAST_UPDATE_CHECK_KEY] ?: 0L
        return System.currentTimeMillis() - lastCheck >= AUTO_CHECK_INTERVAL_MILLIS
    }

    private suspend fun saveLastCheckTime() {
        getApplication<Application>().settingsDataStore.edit { preferences ->
            preferences[LAST_UPDATE_CHECK_KEY] = System.currentTimeMillis()
        }
    }

    private fun safeFileVersion(version: String): String =
        version.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check_time")
        const val AUTO_CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
    }
}
