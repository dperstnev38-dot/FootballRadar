package com.footballradar.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballradar.app.data.remote.ApiServiceFactory
import com.footballradar.app.data.remote.FootballApiConfig
import com.footballradar.app.data.remote.RemoteMatchRepository
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.radar.RadarEngine
import com.footballradar.app.domain.repository.MatchDataState
import com.footballradar.app.domain.repository.MatchRepository
import com.footballradar.app.domain.repository.MatchStatisticsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FootballRadarViewModel(
    private val repository: MatchRepository = RemoteMatchRepository(
        apiService = ApiServiceFactory.createOrUnconfigured(FootballApiConfig()),
    ),
) : ViewModel() {
    private val radarEngine = RadarEngine()

    init {
        radarEngine.seed(repository.getPreviousMatches())
    }

    private val _uiState = MutableStateFlow(
        FootballRadarUiState(
            loadStatus = MatchLoadStatus.LOADING,
        ),
    )
    val uiState: StateFlow<FootballRadarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            refreshMatchesNow(showLoading = true)
            while (isActive) {
                delay(LIVESCORES_POLL_INTERVAL_MILLIS)
                refreshMatchesNow(showLoading = false)
            }
        }
    }

    fun selectFilter(filter: MatchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(
                isSearchVisible = !it.isSearchVisible,
                searchQuery = if (it.isSearchVisible) "" else it.searchQuery,
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectDestination(destination: MainDestination) {
        _uiState.update { it.copy(destination = destination) }
    }

    fun refreshMatches() {
        viewModelScope.launch {
            refreshMatchesNow(showLoading = _uiState.value.matches.isEmpty())
        }
    }

    fun loadMatchStatistics(match: FootballMatch) {
        if (match.id in _uiState.value.statisticsLoadingMatchIds) return
        viewModelScope.launch {
            loadMatchDetailsNow(match.id)
        }
    }

    private suspend fun refreshMatchesNow(showLoading: Boolean) {
        _uiState.update {
            it.copy(
                loadStatus = if (showLoading) MatchLoadStatus.LOADING else it.loadStatus,
                errorMessage = null,
            )
        }
        when (val result = repository.fetchMatches()) {
            MatchDataState.Loading -> _uiState.update {
                it.copy(loadStatus = MatchLoadStatus.LOADING)
            }
            is MatchDataState.Success -> {
                showMatches(
                    matches = result.matches,
                    status = MatchLoadStatus.CONTENT,
                )
                refreshSelectedLiveDetails()
            }
            is MatchDataState.Error -> _uiState.update {
                if (it.matches.isNotEmpty()) {
                    it.copy(errorMessage = result.message)
                } else {
                    it.copy(
                        loadStatus = MatchLoadStatus.ERROR,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private fun showMatches(
        matches: List<FootballMatch>,
        status: MatchLoadStatus,
    ) {
        val events = radarEngine.update(matches)
        _uiState.update {
            it.copy(
                matches = matches,
                radarEvents = events,
                loadStatus = status,
                errorMessage = null,
            )
        }
    }

    private suspend fun refreshSelectedLiveDetails() {
        val liveMatches = _uiState.value.matches.filter { it.status == MatchStatus.LIVE }
        liveMatches.forEach { match ->
            if (match.id !in _uiState.value.statisticsLoadingMatchIds) {
                loadMatchDetailsNow(match.id)
            }
        }
    }

    private suspend fun loadMatchDetailsNow(matchId: String) {
        _uiState.update {
            it.copy(
                statisticsLoadingMatchIds = it.statisticsLoadingMatchIds + matchId,
                statisticsErrorsByMatchId = it.statisticsErrorsByMatchId - matchId,
            )
        }
        when (val result = repository.fetchMatchStatistics(matchId)) {
            is MatchStatisticsState.Success -> _uiState.update { state ->
                val updatedMatches = state.matches.map { current ->
                    if (current.id == matchId) result.match else current
                }
                state.copy(
                    matches = updatedMatches,
                    radarEvents = radarEngine.update(updatedMatches),
                    statisticsLoadingMatchIds = state.statisticsLoadingMatchIds - matchId,
                    statisticsErrorsByMatchId = state.statisticsErrorsByMatchId - matchId,
                )
            }
            is MatchStatisticsState.Error -> _uiState.update {
                it.copy(
                    statisticsLoadingMatchIds = it.statisticsLoadingMatchIds - matchId,
                    statisticsErrorsByMatchId =
                        it.statisticsErrorsByMatchId + (matchId to result.message),
                )
            }
        }
    }

    private companion object {
        const val LIVESCORES_POLL_INTERVAL_MILLIS = 60_000L
    }
}
