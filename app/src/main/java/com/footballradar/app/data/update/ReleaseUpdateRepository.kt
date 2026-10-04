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
)

class ReleaseUpdateRepository(
    private val repositoryName: String = GitHubReleaseConfig.repository,
    private val releaseService: GitHubReleaseService = GitHubReleaseService(repositoryName),
    private val downloadClient: OkHttpClient = downloadHttpClient,
) {
    fun checkForUpdate(currentVersion: String): AppUpdate? {
        check(REPOSITORY_PATTERN.matches(repositoryName)) {
            "Репозиторий обновлений не настроен (Gradle: githubReleasesRepository)"
        }
        val release = releaseService.latestRelease()
        val latestVersion = release.tagName
            ?.takeIf(String::isNotBlank)
            ?: throw IOException("В GitHub Release отсутствует номер версии")
        if (com.footballradar.app.domain.update.VersionComparator.compare(
                latestVersion,
                currentVersion,
            ) <= 0
        ) {
            return null
        }
        val apkUrl = release.assets.orEmpty()
            .firstOrNull { it.name.orEmpty().endsWith(".apk", ignoreCase = true) }
            ?.downloadUrl
            ?.takeIf(String::isNotBlank)
            ?: throw IOException("В новой версии GitHub Release отсутствует APK")
        val pageUrl = release.pageUrl?.takeIf(String::isNotBlank)
            ?: throw IOException("В GitHub Release отсутствует ссылка на страницу")
        return AppUpdate(
            currentVersion = currentVersion,
            newVersion = latestVersion,
            releasePageUrl = pageUrl,
            apkUrl = apkUrl,
        )
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
                if (temporaryFile.length() == 0L) {
                    throw IOException("Скачанный APK пуст")
                }
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
            (host == "github.com" || host.endsWith(".githubusercontent.com"))
    }

    private companion object {
        const val BUFFER_SIZE = 16 * 1024
        val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
        val downloadHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.MINUTES)
            .followRedirects(true)
            .build()
    }
}
