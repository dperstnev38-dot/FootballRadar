package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.mapper.PitchApiMatchMapper
import com.footballradar.app.data.remote.mapper.ShotCounts
import com.footballradar.app.data.remote.model.PitchApiEnvelope
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.repository.MatchDataState
import com.footballradar.app.domain.repository.MatchRepository
import com.footballradar.app.domain.repository.MatchStatisticsState
import com.google.gson.JsonParseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

class RemoteMatchRepository(
    private val apiService: ApiService,
    private val mapper: PitchApiMatchMapper = PitchApiMatchMapper(),
    private val config: FootballApiConfig = FootballApiConfig(),
) : MatchRepository {
    private val requestMutex = Mutex()
    private val cachedMatches = linkedMapOf<String, FootballMatch>()

    override fun getMatches(): List<FootballMatch> = synchronized(cachedMatches) {
        cachedMatches.values.toList()
    }

    override suspend fun fetchMatches(): MatchDataState = requestMutex.withLock {
        try {
            val response = apiService.getMatchesByDate(
                date = LocalDate.now(ZoneOffset.UTC).toString(),
                status = FootballApiConfig.DATE_MATCH_STATUS,
            )
            val matches = mapper.mapMatches(response.requireData("список матчей"))
            synchronized(cachedMatches) {
                val previousMatches = cachedMatches.toMap()
                cachedMatches.clear()
                matches.forEach { match ->
                    val previous = previousMatches[match.id]
                    val updated = if (previous != null) {
                        match.copy(
                            minute = previous.minute.takeIf { match.status == MatchStatus.LIVE },
                            homeShotsOnTarget = previous.homeShotsOnTarget,
                            awayShotsOnTarget = previous.awayShotsOnTarget,
                            events = previous.events,
                            statisticsLoaded = previous.statisticsLoaded,
                        )
                    } else {
                        match
                    }
                    cachedMatches[updated.id] = updated
                }
            }
            MatchDataState.Success(getMatches())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            MatchDataState.Error(exception.toDisplayMessage())
        } catch (exception: IOException) {
            MatchDataState.Error(exception.toDisplayMessage())
        } catch (exception: IllegalArgumentException) {
            MatchDataState.Error(exception.toDisplayMessage())
        } catch (exception: JsonParseException) {
            MatchDataState.Error(exception.toDisplayMessage())
        } catch (exception: IllegalStateException) {
            MatchDataState.Error(exception.toDisplayMessage())
        }
    }

    override suspend fun fetchMatchStatistics(matchId: String): MatchStatisticsState {
        if (matchId.isBlank()) {
            return MatchStatisticsState.Error("Некорректный идентификатор матча")
        }
        return try {
            val matchResponse = apiService.getMatch(matchId)
            val match = mapper.mapDetails(
                envelope = matchResponse.requireData("данные матча"),
                expectedMatchId = matchId,
            )
            val events = mapper.mapEvents(
                envelope = apiService.getMatchEvents(matchId).requireData("события матча"),
                match = match,
            )
            val currentMatch = match.copy(events = events)
            val shots = requestShotCounts(currentMatch)
            val previous = synchronized(cachedMatches) { cachedMatches[matchId] }
            val updatedMatch = currentMatch.copy(
                homeShotsOnTarget = shots?.home ?: previous?.homeShotsOnTarget,
                awayShotsOnTarget = shots?.away ?: previous?.awayShotsOnTarget,
                statisticsLoaded = true,
            )
            synchronized(cachedMatches) {
                cachedMatches[matchId] = updatedMatch
            }
            MatchStatisticsState.Success(updatedMatch)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SocketTimeoutException) {
            MatchStatisticsState.Error(exception.toDisplayMessage())
        } catch (exception: IOException) {
            MatchStatisticsState.Error(exception.toDisplayMessage())
        } catch (exception: IllegalArgumentException) {
            MatchStatisticsState.Error(exception.toDisplayMessage())
        } catch (exception: JsonParseException) {
            MatchStatisticsState.Error(exception.toDisplayMessage())
        } catch (exception: IllegalStateException) {
            MatchStatisticsState.Error(exception.toDisplayMessage())
        }
    }

    private suspend fun requestShotCounts(match: FootballMatch): ShotCounts? {
        val shotsEnvelope = try {
            apiService.getMatchShots(match.id).requireData("удары по воротам")
        } catch (exception: PitchApiHttpException) {
            if (exception.statusCode == 404) null else throw exception
        }
        val shotCounts = shotsEnvelope?.let { mapper.mapShotsOnTarget(it, match) }

        val statsEnvelope = try {
            apiService.getMatchStats(match.id).requireData("статистика матча")
        } catch (exception: PitchApiHttpException) {
            if (exception.statusCode == 404) null else throw exception
        }
        val statsCounts = statsEnvelope?.let {
            mapper.mapShotsOnTargetFromStats(it, match.id)
        }
        return shotCounts ?: statsCounts
    }

    private fun <T> Response<PitchApiEnvelope<T>>.requireData(operation: String): PitchApiEnvelope<T> {
        if (!isSuccessful) throw PitchApiHttpException(code())
        val envelope = body() ?: throw IOException("PitchAPI вернул пустой ответ: $operation")
        if (envelope.data == null) {
            throw IOException("PitchAPI не вернул $operation")
        }
        return envelope
    }

    private fun Exception.toDisplayMessage(): String = when (this) {
        is PitchApiHttpException -> message ?: "Ошибка PitchAPI"
        is SocketTimeoutException -> "Время ожидания ответа PitchAPI истекло"
        is IOException -> message?.takeIf(String::isNotBlank) ?: "Нет соединения с PitchAPI"
        is IllegalArgumentException ->
            "Не удалось обработать ответ PitchAPI: ${message ?: "неверный формат данных"}"
        is JsonParseException -> "Не удалось разобрать ответ PitchAPI"
        is IllegalStateException -> message ?: "PitchAPI не настроен"
        else -> "Не удалось загрузить данные PitchAPI"
    }

    private class PitchApiHttpException(
        val statusCode: Int,
    ) : IOException(
        when (statusCode) {
            400 -> "PitchAPI отклонил запрос (HTTP 400)"
            401 -> "Проверьте API-ключ PitchAPI (HTTP 401)"
            403 -> "PitchAPI запретил доступ к данным (HTTP 403)"
            404 -> "PitchAPI не нашёл запрошенные данные (HTTP 404)"
            429 -> "PitchAPI ограничил частоту запросов. Повторите позже (HTTP 429)"
            in 500..599 -> "PitchAPI временно недоступен (HTTP $statusCode)"
            else -> "Ошибка PitchAPI (HTTP $statusCode)"
        },
    )
}
