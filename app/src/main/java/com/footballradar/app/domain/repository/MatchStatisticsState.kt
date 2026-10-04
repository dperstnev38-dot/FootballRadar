package com.footballradar.app.domain.repository

import com.footballradar.app.domain.model.FootballMatch

sealed interface MatchStatisticsState {
    data class Success(
        val match: FootballMatch,
    ) : MatchStatisticsState

    data class Error(val message: String) : MatchStatisticsState
}
