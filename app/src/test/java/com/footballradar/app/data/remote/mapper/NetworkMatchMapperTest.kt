package com.footballradar.app.data.remote.mapper

import com.footballradar.app.data.remote.model.LiveMatchesResponse
import com.footballradar.app.data.remote.model.NetworkMatchDto
import com.footballradar.app.data.remote.model.NetworkMatchEventDto
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.model.MatchTeam
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkMatchMapperTest {
    @Test
    fun mapsNetworkMatchAndEventsToDomainModels() {
        val response = LiveMatchesResponse(
            matches = listOf(
                NetworkMatchDto(
                    id = "match-1",
                    competition = "Premier League",
                    status = "LIVE",
                    kickoff = "20:00",
                    minute = 67,
                    homeTeam = "Arsenal",
                    awayTeam = "Chelsea",
                    homeScore = 2,
                    awayScore = 1,
                    homeShotsOnTarget = 5,
                    awayShotsOnTarget = 3,
                    events = listOf(
                        NetworkMatchEventDto(
                            id = "goal-1",
                            minute = 54,
                            description = "Saka",
                            type = "GOAL",
                            team = "HOME",
                        ),
                        NetworkMatchEventDto(
                            id = "card-1",
                            minute = 61,
                            description = "Palmer",
                            type = "YELLOW_CARD",
                            team = "Chelsea",
                        ),
                    ),
                ),
            ),
        )

        val match = NetworkMatchMapper().map(response).single()

        assertEquals("match-1", match.id)
        assertEquals("Premier League", match.competition)
        assertEquals(MatchStatus.LIVE, match.status)
        assertEquals("Arsenal", match.homeTeam)
        assertEquals(2, match.homeScore)
        assertEquals(5, match.homeShotsOnTarget)
        assertEquals(MatchEventType.GOAL, match.events[0].type)
        assertEquals(MatchTeam.HOME, match.events[0].team)
        assertEquals("goal-1", match.events[0].eventId)
        assertEquals(MatchTeam.AWAY, match.events[1].team)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownMatchStatusInsteadOfCreatingInvalidDomainState() {
        val response = LiveMatchesResponse(
            matches = listOf(
                NetworkMatchDto(
                    id = "match-1",
                    competition = "Premier League",
                    status = "UNKNOWN",
                    kickoff = "20:00",
                    minute = null,
                    homeTeam = "Arsenal",
                    awayTeam = "Chelsea",
                    homeScore = null,
                    awayScore = null,
                    homeShotsOnTarget = null,
                    awayShotsOnTarget = null,
                    events = emptyList(),
                ),
            ),
        )

        NetworkMatchMapper().map(response)
    }
}
