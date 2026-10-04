package com.footballradar.app.domain.update

import com.footballradar.app.data.update.GitHubApiException
import java.net.UnknownHostException
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckErrorMessageTest {
    @Test
    fun mapsNoInternetToClearLocalizedMessage() {
        assertTrue(
            UpdateCheckErrorMessage.from(UnknownHostException("api.github.com"))
                .startsWith("Нет подключения к интернету."),
        )
    }

    @Test
    fun explainsMissingLatestReleaseAndIncludesHttpStatus() {
        val message = UpdateCheckErrorMessage.from(
            GitHubApiException(404, "Not Found"),
        )

        assertTrue(message.contains("HTTP 404"))
        assertTrue(message.contains("опубликованный Release"))
        assertTrue(message.contains("githubReleasesRepository"))
    }

    @Test
    fun includesGitHubApiReasonForRateLimitErrors() {
        val message = UpdateCheckErrorMessage.from(
            GitHubApiException(403, "API rate limit exceeded"),
        )

        assertTrue(message.contains("HTTP 403"))
        assertTrue(message.contains("API rate limit exceeded"))
    }
}
