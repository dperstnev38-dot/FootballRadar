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
    val homeShotsOnTarget: Int = 0,
    val awayShotsOnTarget: Int = 0,
    val events: List<MatchEvent> = emptyList(),
)
