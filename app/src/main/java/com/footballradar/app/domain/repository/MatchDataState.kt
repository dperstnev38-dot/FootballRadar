package com.footballradar.app.domain.repository

import com.footballradar.app.domain.model.FootballMatch

sealed interface MatchDataState {
    data object Loading : MatchDataState

    data class Success(
        val matches: List<FootballMatch>,
    ) : MatchDataState

    data class Error(
        val message: String,
    ) : MatchDataState

}
