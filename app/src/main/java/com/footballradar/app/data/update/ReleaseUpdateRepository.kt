package com.footballradar.app.data.update

import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AppUpdate(
    val currentVersion: String,
    val newVersion: String,
    val releasePageUrl: String,
    val apkUrl: String,
    val releaseTitle: String,
    val releaseNotes: String?,
    val publishedAt: String?,
)

data class UpdateCheckResult(
    val releaseInfo: ReleaseInfo,
    val update: AppUpdate?,
)

data class ReleaseInfo(
    val version: String,
    val title: String,
    val notes: String?,
    val publishedAt: String?,
)

class ReleaseUpdateRepository(
    private val repositoryName: String = GitHubReleaseConfig.repository,
    private val releaseService: GitHubReleaseService = GitHubReleaseService(repositoryName),
    private val downloadClient: OkHttpClient = downloadHttpClient,
) {
    fun checkForUpdate(currentVersion: String): UpdateCheckResult {
        check(REPOSITORY_PATTERN.matches(repositoryName)) {
            "Репозиторий обновлений не настроен (Gradle: githubReleasesRepository)"
        }
        val release = releaseService.latestRelease()
        val latestVersion = release.tagName
            ?.takeIf(String::isNotBlank)
            ?: throw IOException("В GitHub Release отсутствует номер версии")
        val versionComparison = com.footballradar.app.domain.update.VersionComparator.compare(
                latestVersion,
                currentVersion,
            )
        if (versionComparison <= 0) {
            val installedReleaseInfo = if (versionComparison == 0) {
                release.toReleaseInfo()
            } else {
                ReleaseInfo(
                    version = currentVersion,
                    title = currentVersion,
                    notes = null,
                    publishedAt = null,
                )
            }
            return UpdateCheckResult(installedReleaseInfo, update = null)
        }
        val releaseInfo = release.toReleaseInfo()
        val apkUrl = release.assets.orEmpty()
            .firstOrNull { it.name.orEmpty().endsWith(".apk", ignoreCase = true) }
            ?.downloadUrl
            ?.takeIf(String::isNotBlank)
            ?: throw IOException("В новой версии GitHub Release отсутствует APK")
        val pageUrl = release.pageUrl?.takeIf(String::isNotBlank)
            ?: throw IOException("В GitHub Release отсутствует ссылка на страницу")
        val update = AppUpdate(
            currentVersion = currentVersion,
            newVersion = latestVersion,
            releasePageUrl = pageUrl,
            apkUrl = apkUrl,
            releaseTitle = releaseInfo.title,
            releaseNotes = releaseInfo.notes,
            publishedAt = releaseInfo.publishedAt,
        )
        return UpdateCheckResult(releaseInfo, update)
    }

    fun downloadApk(
        update: AppUpdate,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File {
        require(isTrustedGitHubAsset(update.apkUrl)) {
            "Ссылка на APK ведёт на неизвестный сервер"
        }
        destination.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) {
                throw IOException("Не удалось создать каталог для обновления")
            }
        }
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "Football-Radar-Android")
            .build()
        val temporaryFile = File(destination.parentFile, "${destination.name}.download")
        var completed = false
        try {
            downloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Ошибка скачивания APK (HTTP ${response.code})")
                }
                val body = response.body ?: throw IOException("GitHub вернул пустой APK")
                val totalBytes = body.contentLength()
                body.byteStream().use { input ->
                    temporaryFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloadedBytes = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            onProgress(downloadedBytes, totalBytes)
                        }
                        output.flush()
                    }
                }
                validateDownloadedApk(temporaryFile, totalBytes)
            }
            if (destination.exists() && !destination.delete()) {
                throw IOException("Не удалось заменить предыдущий APK")
            }
            if (!temporaryFile.renameTo(destination)) {
                throw IOException("Не удалось сохранить скачанный APK")
            }
            completed = true
            return destination
        } finally {
            if (!completed) temporaryFile.delete()
        }
    }

    private fun isTrustedGitHubAsset(url: String): Boolean {
        val parsed = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        val host = parsed.host?.lowercase() ?: return false
        return parsed.scheme == "https" &&
            host == "github.com" &&
            parsed.path.orEmpty().startsWith("/$repositoryName/releases/download/")
    }

    private fun validateDownloadedApk(file: File, expectedLength: Long) {
        val actualLength = file.length()
        if (actualLength == 0L) {
            throw IOException("Скачанный APK пуст")
        }
        if (expectedLength >= 0L && actualLength != expectedLength) {
            throw IOException("Скачанный APK неполный")
        }
        val signature = ByteArray(APK_ZIP_SIGNATURE.size)
        file.inputStream().use { input ->
            if (input.read(signature) != signature.size ||
                !signature.contentEquals(APK_ZIP_SIGNATURE)
            ) {
                throw IOException("GitHub Release asset не является APK")
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 16 * 1024
        val APK_ZIP_SIGNATURE = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
        val downloadHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }
}
