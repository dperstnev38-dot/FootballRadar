package com.footballradar.app.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiServiceFactory {
    fun createOrUnconfigured(config: FootballApiConfig): ApiService {
        val configurationError = config.configurationError()
        if (configurationError != null) {
            return object : ApiService {
                override suspend fun getMatchesByDate(date: String, status: String) =
                    throw IllegalStateException(configurationError)

                override suspend fun getMatch(matchId: String) =
                    throw IllegalStateException(configurationError)

                override suspend fun getMatchEvents(matchId: String) =
                    throw IllegalStateException(configurationError)

                override suspend fun getMatchShots(matchId: String) =
                    throw IllegalStateException(configurationError)

                override suspend fun getMatchStats(matchId: String) =
                    throw IllegalStateException(configurationError)
            }
        }
        return create(config)
    }

    fun create(config: FootballApiConfig): ApiService {
        require(config.configurationError() == null) {
            config.configurationError() ?: "Неверная конфигурация PitchAPI"
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request()
                        .newBuilder()
                        .header("Accept", "application/json")
                        .header("X-API-KEY", config.apiKey)
                        .build(),
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 30L
}
