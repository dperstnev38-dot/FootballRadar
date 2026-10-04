package com.footballradar.app.domain.radar

enum class RadarEventType {
    RED_CARD,
    GOAL,
    SHOTS_ON_TARGET,
}

data class RadarScore(
    val home: Int,
    val away: Int,
) {
    override fun toString(): String = "$home:$away"
}

data class RadarEvent(
    val id: String,
    val matchId: String,
    val competition: String,
    val type: RadarEventType,
    val priority: Int,
    val minute: Int,
    val team: String,
    val playerName: String? = null,
    val score: RadarScore? = null,
    val shotsOnTarget: Int? = null,
)
