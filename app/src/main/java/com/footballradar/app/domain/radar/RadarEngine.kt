package com.footballradar.app.domain.radar

import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchEvent
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchTeam

class RadarEngine(
    private val rules: List<RadarRule> = listOf(
        RedCardRule(),
        GoalRule(),
        ShotsOnTargetRule(),
    ),
) {
    private val previousMatches = mutableMapOf<String, FootballMatch>()
    private val detectedEvents = linkedMapOf<String, RadarEvent>()

    @Synchronized
    fun seed(matches: List<FootballMatch>) {
        matches.forEach { match -> previousMatches[match.id] = match }
    }

    @Synchronized
    fun update(matches: List<FootballMatch>): List<RadarEvent> {
        matches.forEach { current ->
            val previous = previousMatches[current.id]
            rules
                .flatMap { rule -> rule.evaluate(previous, current) }
                .forEach { event -> detectedEvents.putIfAbsent(event.id, event) }
            previousMatches[current.id] = current
        }
        return detectedEvents.values
            .sortedWith(compareByDescending<RadarEvent> { it.priority }.thenBy { it.minute })
    }
}

private class RedCardRule : RadarRule {
    override fun evaluate(previous: FootballMatch?, current: FootballMatch): List<RadarEvent> =
        current.events
            .filter { it.type == MatchEventType.RED_CARD && it.isNewSince(previous, current.id) }
            .map { event ->
                RadarEvent(
                    id = "red-card:${event.identity(current.id)}",
                    matchId = current.id,
                    competition = current.competition,
                    type = RadarEventType.RED_CARD,
                    priority = RED_CARD_PRIORITY,
                    minute = event.minute,
                    team = current.teamName(event.team),
                    playerName = event.description,
                    score = current.score(),
                )
            }
}

private class GoalRule : RadarRule {
    override fun evaluate(previous: FootballMatch?, current: FootballMatch): List<RadarEvent> =
        current.events
            .filter { it.type == MatchEventType.GOAL && it.isNewSince(previous, current.id) }
            .map { event ->
                RadarEvent(
                    id = "goal:${event.identity(current.id)}",
                    matchId = current.id,
                    competition = current.competition,
                    type = RadarEventType.GOAL,
                    priority = GOAL_PRIORITY,
                    minute = event.minute,
                    team = current.teamName(event.team),
                    playerName = event.description,
                    score = current.score(),
                )
            }
}

private class ShotsOnTargetRule : RadarRule {
    override fun evaluate(previous: FootballMatch?, current: FootballMatch): List<RadarEvent> =
        buildList {
            addThresholdEvents(
                match = current,
                team = MatchTeam.HOME,
                previousCount = previous?.homeShotsOnTarget,
                currentCount = current.homeShotsOnTarget,
            )
            addThresholdEvents(
                match = current,
                team = MatchTeam.AWAY,
                previousCount = previous?.awayShotsOnTarget,
                currentCount = current.awayShotsOnTarget,
            )
        }

    private fun MutableList<RadarEvent>.addThresholdEvents(
        match: FootballMatch,
        team: MatchTeam,
        previousCount: Int?,
        currentCount: Int?,
    ) {
        if (previousCount == null || currentCount == null) return
        for (threshold in SHOT_THRESHOLDS) {
            if (previousCount < threshold && currentCount >= threshold) {
                add(
                    RadarEvent(
                        id = "shots:${match.id}:${team.name}:$threshold",
                        matchId = match.id,
                        competition = match.competition,
                        type = RadarEventType.SHOTS_ON_TARGET,
                        priority = if (threshold == 3) THREE_SHOTS_PRIORITY else TWO_SHOTS_PRIORITY,
                        minute = match.minute ?: 0,
                        team = match.teamName(team),
                        score = match.score(),
                        shotsOnTarget = threshold,
                    ),
                )
            }
        }
    }
}

private fun MatchEvent.isNewSince(previous: FootballMatch?, matchId: String): Boolean {
    val previousEventIds = previous?.events.orEmpty().mapTo(mutableSetOf()) { it.identity(matchId) }
    return identity(matchId) !in previousEventIds
}

private fun FootballMatch.teamName(team: MatchTeam): String = when (team) {
    MatchTeam.HOME -> homeTeam
    MatchTeam.AWAY -> awayTeam
}

private fun FootballMatch.score(): RadarScore? {
    val home = homeScore ?: return null
    val away = awayScore ?: return null
    return RadarScore(home, away)
}

private const val RED_CARD_PRIORITY = 100
private const val GOAL_PRIORITY = 80
private const val THREE_SHOTS_PRIORITY = 60
private const val TWO_SHOTS_PRIORITY = 40
private val SHOT_THRESHOLDS = listOf(2, 3)
