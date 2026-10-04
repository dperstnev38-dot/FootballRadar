package com.footballradar.app.domain.model

enum class MatchStatus {
    LIVE,
    UPCOMING,
    FINISHED,
}

enum class MatchEventType {
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION,
    PENALTY,
}

enum class MatchTeam {
    HOME,
    AWAY,
}

data class MatchEvent(
    val minute: Int,
    val description: String,
    val type: MatchEventType,
    val team: MatchTeam,
    val eventId: String? = null,
) {
    fun identity(matchId: String): String = eventId?.let { "$matchId:$it" }
        ?: listOf(
            matchId,
            type.name,
            minute.toString(),
            team?.name.orEmpty(),
            description,
        ).joinToString(":")
}

data class FootballMatch(
    val id: String,
    val competition: String,
    val status: MatchStatus,
    val kickoff: String,
    val minute: Int? = null,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val homeShotsOnTarget: Int? = null,
    val awayShotsOnTarget: Int? = null,
    val events: List<MatchEvent> = emptyList(),
    val homeTeamLogoUrl: String? = null,
    val awayTeamLogoUrl: String? = null,
    val statisticsLoaded: Boolean = false,
    val homeTeamId: String? = null,
    val awayTeamId: String? = null,
)
