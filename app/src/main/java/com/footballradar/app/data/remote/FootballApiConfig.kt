package com.footballradar.app.data.remote

import com.footballradar.app.BuildConfig

data class FootballApiConfig(
    val baseUrl: String = BuildConfig.PITCH_API_BASE_URL,
    val apiKey: String = BuildConfig.PITCH_API_KEY,
) {
    fun configurationError(): String? {
        val hasSupportedPrefix = apiKey.startsWith(LIVE_KEY_PREFIX) ||
            apiKey.startsWith(TEST_KEY_PREFIX)
        val hasKeyMaterial = apiKey.length > LIVE_KEY_PREFIX.length
        if (!hasSupportedPrefix || !hasKeyMaterial || apiKey.any(Char::isWhitespace)) {
            return "PitchAPI не настроен. Укажите API-ключ PitchAPI в footballApiToken в local.properties."
        }
        if (baseUrl != DEFAULT_BASE_URL) {
            return "footballApiBaseUrl должен быть https://api.pitchapi.dev/."
        }
        return null
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.pitchapi.dev/"
        const val DATE_MATCH_STATUS = "all"
        private const val LIVE_KEY_PREFIX = "pk_live_"
        private const val TEST_KEY_PREFIX = "pk_test_"
    }
}
