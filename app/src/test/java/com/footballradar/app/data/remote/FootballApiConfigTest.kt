package com.footballradar.app.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FootballApiConfigTest {
    @Test
    fun reportsMissingOrPlaceholderPitchApiKeyWithoutRevealingIt() {
        listOf("", "YOUR_TOKEN", "not-a-pitch-key", "pk_live_").forEach { key ->
            assertEquals(
                "PitchAPI не настроен. Укажите API-ключ PitchAPI в footballApiToken в local.properties.",
                FootballApiConfig(apiKey = key).configurationError(),
            )
        }
    }

    @Test
    fun acceptsDocumentedKeyPrefixesAndPitchApiBaseUrl() {
        assertNull(FootballApiConfig(apiKey = "pk_live_test-value").configurationError())
        assertNull(FootballApiConfig(apiKey = "pk_test_test-value").configurationError())
        assertEquals(
            "https://api.pitchapi.dev/",
            FootballApiConfig(apiKey = "pk_test_test-value").baseUrl,
        )
    }

    @Test
    fun rejectsUnexpectedBaseUrl() {
        assertEquals(
            "footballApiBaseUrl должен быть https://api.pitchapi.dev/.",
            FootballApiConfig(
                baseUrl = "https://api.example.test/",
                apiKey = "pk_test_test-value",
            ).configurationError(),
        )
    }

    @Test
    fun unconfiguredServiceFailsBeforeMakingARequest() = runBlocking {
        val apiService = ApiServiceFactory.createOrUnconfigured(FootballApiConfig(apiKey = ""))

        try {
            apiService.getMatchesByDate("2026-10-04", "all")
            throw AssertionError("An unconfigured API must not send a request")
        } catch (exception: IllegalStateException) {
            assertEquals(
                "PitchAPI не настроен. Укажите API-ключ PitchAPI в footballApiToken в local.properties.",
                exception.message,
            )
        }
    }
}
