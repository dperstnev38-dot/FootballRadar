package com.footballradar.app.ui.settings

import android.net.Uri
import com.footballradar.app.data.update.AppUpdate
import com.footballradar.app.data.update.ReleaseInfo

sealed interface UpdateUiState {
    data object Idle : UpdateUiState

    data object NotConfigured : UpdateUiState

    data object Checking : UpdateUiState

    data class UpToDate(val releaseInfo: ReleaseInfo) : UpdateUiState

    data class CachedReleaseInfo(val releaseInfo: ReleaseInfo) : UpdateUiState

    data class UpdateAvailable(
        val update: AppUpdate,
        val releaseInfo: ReleaseInfo,
    ) : UpdateUiState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateUiState

    data class ReadyToInstall(
        val uri: Uri,
        val version: String,
    ) : UpdateUiState

    data class Error(val message: String) : UpdateUiState
}

sealed interface InstallRequest {
    data class OpenInstaller(val uri: Uri) : InstallRequest
}
