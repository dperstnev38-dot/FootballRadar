package com.footballradar.app.ui

import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.radar.RadarEvent

enum class MainDestination {
    MATCHES,
    RADAR,
    SETTINGS,
}

enum class MatchFilter(val label: String) {
    ALL("Все"),
    LIVE("LIVE"),
    UPCOMING("Предстоящие"),
    FINISHED("Завершённые"),
}

enum class MatchLoadStatus {
    LOADING,
    CONTENT,
    ERROR,
}

data class FootballRadarUiState(
    val matches: List<FootballMatch> = emptyList(),
    val radarEvents: List<RadarEvent> = emptyList(),
    val destination: MainDestination = MainDestination.MATCHES,
    val selectedFilter: MatchFilter = MatchFilter.ALL,
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val loadStatus: MatchLoadStatus = MatchLoadStatus.CONTENT,
    val errorMessage: String? = null,
    val statisticsLoadingMatchIds: Set<String> = emptySet(),
    val statisticsErrorsByMatchId: Map<String, String> = emptyMap(),
)
