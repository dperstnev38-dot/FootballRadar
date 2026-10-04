package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.mapper.NetworkMatchMapper
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.repository.MatchDataState
import com.footballradar.app.domain.repository.MatchRepository
import com.google.gson.JsonParseException
import java.io.IOException

class RemoteMatchRepository(
    private val apiService: ApiService,
    private val mapper: NetworkMatchMapper = NetworkMatchMapper(),
) : MatchRepository {
    override fun getMatches(): List<FootballMatch> = emptyList()

    override suspend fun fetchMatches(): MatchDataState {
        return try {
            val response = apiService.getLiveMatches()
            if (!response.isSuccessful) {
                return MatchDataState.Error("Ошибка футбольного сервера (HTTP ${response.code()})")
            }

            val body = response.body()
                ?: return MatchDataState.Error("Футбольный сервер вернул пустой ответ")
            val matches = mapper.map(body)
            if (matches.isEmpty()) {
                return MatchDataState.Error("Футбольный сервер пока не вернул матчи")
            }
            MatchDataState.Success(matches)
        } catch (exception: IOException) {
            MatchDataState.Error("Нет соединения с футбольным сервером")
        } catch (exception: IllegalArgumentException) {
            MatchDataState.Error("Не удалось обработать ответ футбольного сервера: ${exception.message}")
        } catch (exception: JsonParseException) {
            MatchDataState.Error("Не удалось разобрать ответ футбольного сервера")
        } catch (exception: IllegalStateException) {
            MatchDataState.Error(exception.message ?: "Источник футбольных данных не настроен")
        }
    }
}
