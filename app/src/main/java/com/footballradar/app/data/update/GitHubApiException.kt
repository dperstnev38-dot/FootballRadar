package com.footballradar.app.data.update

import java.io.IOException

class GitHubApiException(
    val statusCode: Int,
    val apiMessage: String,
) : IOException(
    when (statusCode) {
        404 -> "GitHub API вернул HTTP 404: опубликованный Release не найден " +
            "или репозиторий недоступен. Проверьте githubReleasesRepository и наличие Release."
        401 -> "GitHub API вернул HTTP 401: запрос не авторизован."
        403 -> "GitHub API вернул HTTP 403: GitHub ограничил запрос. $apiMessage"
        else -> "GitHub API вернул HTTP $statusCode: $apiMessage"
    },
)
