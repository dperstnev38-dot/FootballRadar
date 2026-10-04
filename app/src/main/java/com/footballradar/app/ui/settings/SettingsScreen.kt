package com.footballradar.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footballradar.app.data.update.AppUpdate
import com.footballradar.app.data.update.ReleaseInfo
import com.footballradar.app.domain.settings.ThemeColorSetting
import com.footballradar.app.ui.theme.LocalAppThemeColors
import com.footballradar.app.ui.theme.TextPrimary
import com.footballradar.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

private val ColorOptions = listOf(
    "#38D39F",
    "#64B5F6",
    "#FFB74D",
    "#FF5263",
    "#BA86FC",
    "#F4F7F5",
    "#101512",
    "#1A211E",
    "#25344A",
    "#3D2940",
)

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val appColors = LocalAppThemeColors.current
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (context.packageManager.canRequestPackageInstalls()) {
            updateViewModel.requestInstallerAfterPermission()
        } else {
            updateViewModel.onInstallerLaunchFailed(
                "Разрешите установку приложений для Football Radar и повторите попытку",
            )
        }
    }

    LaunchedEffect(updateViewModel) {
        updateViewModel.installRequests.collectLatest { request ->
            when (request) {
                is InstallRequest.OpenInstaller -> {
                    if (context.packageManager.canRequestPackageInstalls()) {
                        openPackageInstaller(context, request.uri, updateViewModel)
                    } else {
                        permissionLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 10.dp,
            end = 20.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Оформление",
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }
        items(ThemeColorSetting.entries) { setting ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = appColors.matchCard,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = setting.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text = settings.color(setting),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorOptions.forEach { hex ->
                            val selected = settings.color(setting).equals(hex, ignoreCase = true)
                            Surface(
                                onClick = { settingsViewModel.setColor(setting, hex) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex))),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = if (selected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, TextPrimary)
                                } else {
                                    null
                                },
                            ) {}
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Обновления",
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            UpdateSection(
                state = updateState,
                currentVersion = updateViewModel.currentVersion,
                onCheckUpdates = updateViewModel::checkForUpdates,
                onDownload = updateViewModel::downloadUpdate,
                onInstall = updateViewModel::requestInstallerAfterPermission,
            )
        }
    }
}

@Composable
private fun UpdateSection(
    state: UpdateUiState,
    currentVersion: String,
    onCheckUpdates: () -> Unit,
    onDownload: (AppUpdate) -> Unit,
    onInstall: () -> Unit,
) {
    val colors = LocalAppThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.matchCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Текущая версия: $currentVersion",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        when (state) {
            UpdateUiState.Idle -> Text(
                text = "Что нового\nОписание изменений отсутствует\n\n" +
                    "Проверка обновлений выполняется автоматически не чаще раза в сутки.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            UpdateUiState.NotConfigured -> Text(
                text = "Укажите GitHub-репозиторий в Gradle-свойстве githubReleasesRepository.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            UpdateUiState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("  Проверка обновлений…", color = TextSecondary)
            }
            is UpdateUiState.UpToDate -> {
                Text(
                    text = "У вас установлена последняя версия.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                ReleaseDetails(state.releaseInfo)
            }
            is UpdateUiState.CachedReleaseInfo -> {
                Text(
                    text = "Информация о последнем GitHub Release",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                ReleaseDetails(state.releaseInfo)
            }
            is UpdateUiState.UpdateAvailable -> {
                Text(
                    text = "Доступно обновление: ${state.update.newVersion}",
                    color = colors.radarEvent,
                    style = MaterialTheme.typography.bodyMedium,
                )
                ReleaseDetails(state.releaseInfo)
                Button(onClick = { onDownload(state.update) }) {
                    Text("Обновить")
                }
            }
            is UpdateUiState.Downloading -> {
                val progress = if (state.totalBytes > 0L) {
                    (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f)
                } else {
                    null
                }
                Text(
                    text = if (progress == null) {
                        "Скачиваем обновление…"
                    } else {
                        "Скачиваем обновление… ${(progress * 100).toInt()}%"
                    },
                    color = TextSecondary,
                )
                if (progress == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            is UpdateUiState.ReadyToInstall -> {
                Text(
                    text = "APK версии ${state.version} загружен.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onInstall) {
                    Text("Открыть установщик")
                }
            }
            is UpdateUiState.Error -> Text(
                text = state.message,
                color = colors.live,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = onCheckUpdates,
            enabled = state != UpdateUiState.Checking &&
                state !is UpdateUiState.Downloading,
        ) {
            Text("Проверить обновления")
        }
    }
}

@Composable
private fun ReleaseDetails(releaseInfo: ReleaseInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = releaseInfo.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
        Text(
            text = "Версия релиза: ${releaseInfo.version}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Text(
            text = "Дата релиза: ${formatReleaseDate(releaseInfo.publishedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Text(
            text = "Что нового",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
        )
        Text(
            text = releaseInfo.notes ?: "Описание изменений отсутствует",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

private fun formatReleaseDate(value: String?): String {
    if (value.isNullOrBlank()) return "Не указана"
    return runCatching {
        Instant.parse(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru")))
    }.getOrDefault(value)
}

private fun openPackageInstaller(
    context: Context,
    uri: Uri,
    updateViewModel: UpdateViewModel,
) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, APK_MIME_TYPE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.clipData = ClipData.newRawUri("Football Radar update", uri)
    try {
        context.startActivity(intent)
    } catch (exception: ActivityNotFoundException) {
        updateViewModel.onInstallerLaunchFailed("На устройстве не найден установщик APK")
    } catch (exception: SecurityException) {
        updateViewModel.onInstallerLaunchFailed("Не удалось передать APK системному установщику")
    }
}

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
