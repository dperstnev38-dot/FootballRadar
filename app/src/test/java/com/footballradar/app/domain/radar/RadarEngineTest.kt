package com.footballradar.app.domain.radar

import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchEvent
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.model.MatchTeam
import org.junit.Assert.assertEquals
import org.junit.Test

class RadarEngineTest {
    @Test
    fun detectsNewEventsInPriorityOrderAndDoesNotRepeatThem() {
        val engine = RadarEngine()
        engine.seed(previousMatches)
        val events = engine.update(currentMatches)

        assertEquals(
            listOf(
                RadarEventType.RED_CARD,
                RadarEventType.GOAL,
                RadarEventType.SHOTS_ON_TARGET,
                RadarEventType.SHOTS_ON_TARGET,
                RadarEventType.SHOTS_ON_TARGET,
            ),
            events.map { it.type },
        )
        assertEquals(listOf(100, 80, 60, 40, 40), events.map { it.priority })
        assertEquals("ПСЖ", events.first().team)
        assertEquals(66, events.first().minute)
        assertEquals("Арсенал", events[2].team)
        assertEquals(3, events[2].shotsOnTarget)
        assertEquals(2, events[3].shotsOnTarget)
        assertEquals("2:1", events[1].score.toString())
        assertEquals(events, engine.update(currentMatches))
    }

    @Test
    fun detectsOnlyEventsAddedAfterThePreviousSnapshot() {
        val engine = RadarEngine()
        engine.seed(previousMatches)
        engine.update(currentMatches)

        val arsenal = currentMatches.first { it.id == "arsenal-city" }
        val updatedArsenal = arsenal.copy(
            minute = 71,
            homeScore = 3,
            events = arsenal.events + MatchEvent(
                minute = 71,
                description = "Жезус",
                type = MatchEventType.GOAL,
                team = MatchTeam.HOME,
                eventId = "arsenal-goal-71",
            ),
        )

        val updatedMatches = currentMatches.filterNot { it.id == arsenal.id } + updatedArsenal
        val events = engine.update(updatedMatches)
        val newGoal = events.single { it.id.endsWith("arsenal-goal-71") }
        assertEquals(3, newGoal.score?.home)
        assertEquals(events, engine.update(updatedMatches))
    }

    @Test
    fun doesNotTreatUnavailableShotCountsAsZeroOnInitialStatisticsLoad() {
        val engine = RadarEngine()
        val withoutStatistics = match(
            id = "live-match",
            home = "Home",
            away = "Away",
            homeScore = 0,
            awayScore = 0,
            homeShots = null,
        ).copy(awayShotsOnTarget = null)
        engine.seed(listOf(withoutStatistics))

        val firstStatistics = withoutStatistics.copy(
            homeShotsOnTarget = 3,
            awayShotsOnTarget = 2,
            statisticsLoaded = true,
        )
        assertEquals(emptyList<RadarEvent>(), engine.update(listOf(firstStatistics)))

        val laterStatistics = firstStatistics.copy(
            homeShotsOnTarget = 4,
            awayShotsOnTarget = 3,
        )
        val events = engine.update(listOf(laterStatistics))
        assertEquals(listOf(3), events.mapNotNull { it.shotsOnTarget })
    }

    private val previousMatches = listOf(
        match(
            id = "arsenal-city",
            home = "Арсенал",
            away = "Манчестер Сити",
            homeScore = 1,
            awayScore = 1,
            homeShots = 1,
            events = listOf(MatchEvent(32, "Хаверц", MatchEventType.GOAL, MatchTeam.HOME)),
        ),
        match(
            id = "psg-marseille",
            home = "ПСЖ",
            away = "Марсель",
            homeScore = 0,
            awayScore = 0,
            homeShots = 0,
        ),
    )

    private val currentMatches = listOf(
        previousMatches[0].copy(
            minute = 63,
            homeScore = 2,
            awayScore = 1,
            homeShotsOnTarget = 3,
            events = previousMatches[0].events + MatchEvent(
                63,
                "Эдегор",
                MatchEventType.GOAL,
                MatchTeam.HOME,
                eventId = "arsenal-goal-63",
            ),
        ),
        previousMatches[1].copy(
            minute = 66,
            awayScore = 1,
            awayShotsOnTarget = 2,
            events = listOf(
                MatchEvent(
                    66,
                    "Хакими",
                    MatchEventType.RED_CARD,
                    MatchTeam.HOME,
                    eventId = "psg-red-card",
                ),
            ),
        ),
    )

    private fun match(
        id: String,
        home: String,
        away: String,
        homeScore: Int,
        awayScore: Int,
        homeShots: Int?,
        awayShots: Int? = 0,
        events: List<MatchEvent> = emptyList(),
    ) = FootballMatch(
        id = id,
        competition = "Тестовая лига",
        status = MatchStatus.LIVE,
        kickoff = "20:00",
        minute = 60,
        homeTeam = home,
        awayTeam = away,
        homeScore = homeScore,
        awayScore = awayScore,
        homeShotsOnTarget = homeShots,
        awayShotsOnTarget = awayShots,
        events = events,
    )
}
