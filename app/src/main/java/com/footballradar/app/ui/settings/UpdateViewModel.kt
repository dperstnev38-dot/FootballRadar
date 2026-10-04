package com.footballradar.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footballradar.app.BuildConfig
import com.footballradar.app.data.settings.settingsDataStore
import com.footballradar.app.data.update.GitHubReleaseConfig
import com.footballradar.app.data.update.ReleaseInfo
import com.footballradar.app.data.update.ReleaseUpdateRepository
import com.footballradar.app.domain.update.UpdateCheckErrorMessage
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
                if (GitHubReleaseConfig.isConfigured) {
                    if (automaticCheckIsDue()) {
                        checkForUpdates()
                    } else {
                        readCachedReleaseInfo()?.let { releaseInfo ->
                            _state.value = UpdateUiState.CachedReleaseInfo(releaseInfo)
                        }
                    }
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
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = try {
                val result = withContext(Dispatchers.IO) {
                    repository.checkForUpdate(currentVersion)
                }
                result.update?.let { update ->
                    UpdateUiState.UpdateAvailable(update, result.releaseInfo)
                } ?: UpdateUiState.UpToDate(result.releaseInfo)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                UpdateUiState.Error(UpdateCheckErrorMessage.from(exception))
            }
            _state.value = result
            try {
                val releaseInfo = when (result) {
                    is UpdateUiState.UpToDate -> result.releaseInfo
                    is UpdateUiState.UpdateAvailable -> result.releaseInfo
                    else -> null
                }
                saveLastCheckTime(releaseInfo)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                if (result is UpdateUiState.UpToDate || result is UpdateUiState.UpdateAvailable) {
                    _state.value = UpdateUiState.Error(
                        "Результат проверки получен, но не удалось сохранить дату проверки. " +
                            UpdateCheckErrorMessage.from(exception),
                    )
                }
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

    private suspend fun saveLastCheckTime(releaseInfo: ReleaseInfo?) {
        getApplication<Application>().settingsDataStore.edit { preferences ->
            preferences[LAST_UPDATE_CHECK_KEY] = System.currentTimeMillis()
            if (releaseInfo != null) {
                preferences[CACHED_RELEASE_VERSION_KEY] = releaseInfo.version
                preferences[CACHED_RELEASE_TITLE_KEY] = releaseInfo.title
                releaseInfo.notes?.let { preferences[CACHED_RELEASE_NOTES_KEY] = it }
                    ?: preferences.remove(CACHED_RELEASE_NOTES_KEY)
                releaseInfo.publishedAt?.let { preferences[CACHED_RELEASE_DATE_KEY] = it }
                    ?: preferences.remove(CACHED_RELEASE_DATE_KEY)
            }
        }
    }

    private suspend fun readCachedReleaseInfo(): ReleaseInfo? {
        val preferences = getApplication<Application>().settingsDataStore.data.first()
        val version = preferences[CACHED_RELEASE_VERSION_KEY] ?: return null
        return ReleaseInfo(
            version = version,
            title = preferences[CACHED_RELEASE_TITLE_KEY] ?: version,
            notes = preferences[CACHED_RELEASE_NOTES_KEY],
            publishedAt = preferences[CACHED_RELEASE_DATE_KEY],
        )
    }

    private fun safeFileVersion(version: String): String =
        version.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check_time")
        val CACHED_RELEASE_VERSION_KEY = stringPreferencesKey("cached_release_version")
        val CACHED_RELEASE_TITLE_KEY = stringPreferencesKey("cached_release_title")
        val CACHED_RELEASE_NOTES_KEY = stringPreferencesKey("cached_release_notes")
        val CACHED_RELEASE_DATE_KEY = stringPreferencesKey("cached_release_date")
        const val AUTO_CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
    }
}
