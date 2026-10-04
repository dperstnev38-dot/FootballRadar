package com.footballradar.app.domain.repository

import com.footballradar.app.domain.model.FootballMatch

interface MatchRepository {
    fun getPreviousMatches(): List<FootballMatch> = emptyList()

    fun getMatches(): List<FootballMatch>

    suspend fun fetchMatches(): MatchDataState = MatchDataState.Success(getMatches())

    suspend fun fetchMatchStatistics(matchId: String): MatchStatisticsState =
        MatchStatisticsState.Error("Статистика матча недоступна")
}
