package com.footballradar.app.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiServiceFactory {
    fun createOrUnconfigured(baseUrl: String): ApiService {
        if (baseUrl.isBlank()) {
            return object : ApiService {
                override suspend fun getLiveMatches() =
                    throw IllegalStateException("Источник футбольных данных ещё не настроен")
            }
        }
        return create(baseUrl)
    }

    fun create(baseUrl: String): ApiService {
        require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
            "Base URL must use HTTP or HTTPS"
        }
        require(baseUrl.endsWith("/")) { "Base URL must end with '/'" }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 15L
    private const val WRITE_TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 20L
}
