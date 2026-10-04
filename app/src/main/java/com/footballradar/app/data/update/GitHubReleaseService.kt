package com.footballradar.app.data.update

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubReleaseService(
    private val repository: String,
    private val client: OkHttpClient = httpClient,
    private val gson: Gson = Gson(),
) {
    fun latestRelease(): GitHubReleaseDto {
        require(REPOSITORY_PATTERN.matches(repository)) {
            "GitHub repository must be configured as owner/repository"
        }
        val request = Request.Builder()
            .url("$GITHUB_API/repos/$repository/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Football-Radar-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val apiMessage = errorBody
                    ?.let { body ->
                        runCatching {
                            gson.fromJson(body, JsonObject::class.java)
                                ?.get("message")
                                ?.takeIf { it.isJsonPrimitive }
                                ?.asString
                        }.getOrNull()
                    }
                    ?.takeIf(String::isNotBlank)
                    ?: response.message.ifBlank { "неизвестная причина" }
                throw GitHubApiException(response.code, apiMessage)
            }
            val body = response.body?.string()
                ?: throw IOException("GitHub Releases вернул пустой ответ")
            return try {
                gson.fromJson(body, GitHubReleaseDto::class.java)
                    ?: throw IOException("Не удалось разобрать GitHub Release")
            } catch (exception: JsonParseException) {
                throw IOException("Не удалось разобрать GitHub Release", exception)
            }
        }
    }

    private companion object {
        const val GITHUB_API = "https://api.github.com"
        val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
