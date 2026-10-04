package com.footballradar.app.data.update

import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReleaseUpdateRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun discoversVersionNewerThanTheBuildAndSelectsAnApkReleaseAsset() {
        val capturedRequest = AtomicReference<Request?>()
        val repositoryName = "dperstnev38-dot/FootballRadar"
        val client = fakeClient(RELEASE_JSON.toByteArray(), capturedRequest)
        val service = GitHubReleaseService(repositoryName, client)
        val repository = ReleaseUpdateRepository(
            repositoryName = repositoryName,
            releaseService = service,
        )

        val result = repository.checkForUpdate(currentVersion = "1.0.0")

        assertEquals("v1.1.0", result.update?.newVersion)
        assertEquals(
            "https://github.com/dperstnev38-dot/FootballRadar/releases/download/" +
                "v1.1.0/football-radar-v1.1.0.apk",
            result.update?.apkUrl,
        )
        assertEquals(
            "https://api.github.com/repos/$repositoryName/releases/latest",
            capturedRequest.get()?.url.toString(),
        )
        assertFalse(capturedRequest.get()?.header("Authorization") != null)
    }

    @Test
    fun reportsMissingPublicReleaseAsHttp404() {
        val request = AtomicReference<Request?>()
        val service = GitHubReleaseService(
            "dperstnev38-dot/FootballRadar",
            fakeClient("""{"message":"Not Found"}""".toByteArray(), request, code = 404),
        )

        try {
            service.latestRelease()
            throw AssertionError("A missing latest release must not be treated as success")
        } catch (exception: GitHubApiException) {
            assertEquals(404, exception.statusCode)
            assertTrue(exception.message.orEmpty().contains("HTTP 404"))
        }
    }

    @Test
    fun downloadsOnlyPublicReleaseAssetsReportsProgressAndVerifiesApkBytes() {
        val apkBytes = byteArrayOf(0x50, 0x4b, 0x03, 0x04, 1, 2, 3, 4)
        val downloadRequest = AtomicReference<Request?>()
        val repository = ReleaseUpdateRepository(
            repositoryName = "dperstnev38-dot/FootballRadar",
            downloadClient = fakeClient(apkBytes, downloadRequest),
        )
        val destination = File(temporaryFolder.root, "updates/app.apk")
        val progress = mutableListOf<Pair<Long, Long>>()

        val downloaded = repository.downloadApk(
            update(apkUrl = ASSET_URL),
            destination,
        ) { bytes, total -> progress += bytes to total }

        assertEquals(destination, downloaded)
        assertTrue(destination.readBytes().contentEquals(apkBytes))
        assertEquals(ASSET_URL, downloadRequest.get()?.url.toString())
        assertTrue(progress.isNotEmpty())
        assertEquals(apkBytes.size.toLong(), progress.last().first)
        assertEquals(apkBytes.size.toLong(), progress.last().second)
    }

    @Test
    fun rejectsNonReleaseUrlsAndInvalidOrTruncatedApkDownloads() {
        val request = AtomicReference<Request?>()
        val client = fakeClient(byteArrayOf(1, 2, 3, 4), request)
        val repository = ReleaseUpdateRepository(
            repositoryName = "dperstnev38-dot/FootballRadar",
            downloadClient = client,
        )
        val destination = File(temporaryFolder.root, "updates/app.apk")

        try {
            repository.downloadApk(
                update(apkUrl = "https://example.com/app.apk"),
                destination,
            ) { _, _ -> }
            throw AssertionError("Assets outside the configured public release must be rejected")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("неизвестный сервер"))
        }
        assertEquals(null, request.get())

        try {
            repository.downloadApk(update(ASSET_URL), destination) { _, _ -> }
            throw AssertionError("A non-APK response must not be made installable")
        } catch (exception: IOException) {
            assertEquals("GitHub Release asset не является APK", exception.message)
        }
        assertFalse(destination.exists())
    }

    private fun update(apkUrl: String) = AppUpdate(
        currentVersion = "1.0.0",
        newVersion = "v1.1.0",
        releasePageUrl = "https://github.com/dperstnev38-dot/FootballRadar/releases/tag/v1.1.0",
        apkUrl = apkUrl,
        releaseTitle = "Football Radar 1.1.0",
        releaseNotes = null,
        publishedAt = null,
    )

    private fun fakeClient(
        body: ByteArray,
        capturedRequest: AtomicReference<Request?>,
        code: Int = 200,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            capturedRequest.set(request)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Not Found")
                .body(body.toResponseBody("application/octet-stream".toMediaType()))
                .build()
        }
        .build()

    private companion object {
        const val ASSET_URL =
            "https://github.com/dperstnev38-dot/FootballRadar/releases/download/" +
                "v1.1.0/football-radar-v1.1.0.apk"
        const val RELEASE_JSON = """
            {
              "tag_name":"v1.1.0",
              "name":"Football Radar 1.1.0",
              "body":"Release notes",
              "published_at":"2026-10-04T12:00:00Z",
              "html_url":"https://github.com/dperstnev38-dot/FootballRadar/releases/tag/v1.1.0",
              "assets":[{
                "name":"football-radar-v1.1.0.apk",
                "browser_download_url":"https://github.com/dperstnev38-dot/FootballRadar/releases/download/v1.1.0/football-radar-v1.1.0.apk"
              }]
            }
        """
    }
}
