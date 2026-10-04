package com.footballradar.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballradar.app.BuildConfig
import com.footballradar.app.data.remote.ApiServiceFactory
import com.footballradar.app.data.remote.RemoteMatchRepository
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.radar.RadarEngine
import com.footballradar.app.domain.repository.MatchDataState
import com.footballradar.app.domain.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FootballRadarViewModel(
    private val repository: MatchRepository = RemoteMatchRepository(
        apiService = ApiServiceFactory.createOrUnconfigured(BuildConfig.API_BASE_URL),
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
        refreshMatches()
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
            _uiState.update {
                it.copy(loadStatus = MatchLoadStatus.LOADING, errorMessage = null)
            }
            when (val result = repository.fetchMatches()) {
                MatchDataState.Loading -> _uiState.update {
                    it.copy(loadStatus = MatchLoadStatus.LOADING)
                }
                is MatchDataState.Success -> showMatches(
                    matches = result.matches,
                    status = MatchLoadStatus.CONTENT,
                )
                is MatchDataState.Error -> _uiState.update {
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
}
