package com.footballradar.app.domain.update

import com.footballradar.app.data.update.GitHubApiException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object UpdateCheckErrorMessage {
    fun from(throwable: Throwable): String = when (throwable) {
        is UnknownHostException, is ConnectException ->
            "Нет подключения к интернету. Проверьте сеть и повторите проверку."
        is SocketTimeoutException ->
            "Нет подключения к интернету или GitHub не отвечает. Повторите проверку."
        is GitHubApiException -> throwable.message
            ?: "GitHub API вернул HTTP ${throwable.statusCode}"
        is IOException -> throwable.message
            ?.takeIf(String::isNotBlank)
            ?: "Ошибка сети при проверке обновлений."
        is IllegalArgumentException ->
            "Некорректная версия в GitHub Release: ${throwable.message.orEmpty()}"
        is IllegalStateException -> throwable.message
            ?.takeIf(String::isNotBlank)
            ?: "Репозиторий обновлений не настроен."
        else -> throwable.message
            ?.takeIf(String::isNotBlank)
            ?.let { "Не удалось проверить обновления: $it" }
            ?: "Не удалось проверить обновления из-за непредвиденной ошибки."
    }
}
