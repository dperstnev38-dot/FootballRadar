package com.footballradar.app.data.remote.mapper

import com.footballradar.app.data.remote.model.PitchApiEnvelope
import com.footballradar.app.data.remote.model.PitchApiEventsDto
import com.footballradar.app.data.remote.model.PitchApiMatchesByDateDto
import com.footballradar.app.data.remote.model.PitchApiShotsDto
import com.footballradar.app.data.remote.model.PitchApiTeamStatsDto
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.model.MatchTeam
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchApiMatchMapperTest {
    private val gson = Gson()
    private val mapper = PitchApiMatchMapper()

    @Test
    fun mapsOfficialDateMatchFieldsAndHandlesEmptyMatchList() {
        val matches = mapper.mapMatches(
            envelope<PitchApiMatchesByDateDto>(
                """
                    {
                      "data": {
                        "date": "2026-10-04",
                        "matches": [{
                          "id": "m_match01",
                          "league": {"id": "l_league", "name": "Premier League"},
                          "home_team": {
                            "id": "t_home",
                            "name": "Home FC",
                            "image_url": "https://cdn.pitchapi.dev/home.webp"
                          },
                          "away_team": {
                            "id": "t_away",
                            "name": "Away FC",
                            "image_url": "https://cdn.pitchapi.dev/away.webp"
                          },
                          "time_utc": "2026-10-04T16:30:00Z",
                          "status": "live",
                          "score_home": 2,
                          "score_away": 1
                        }]
                      }
                    }
                """.trimIndent(),
            ),
        )

        val match = matches.single()
        assertEquals("m_match01", match.id)
        assertEquals("Premier League", match.competition)
        assertEquals(MatchStatus.LIVE, match.status)
        assertEquals("Home FC", match.homeTeam)
        assertEquals("Away FC", match.awayTeam)
        assertEquals(2, match.homeScore)
        assertEquals(1, match.awayScore)
        assertEquals("t_home", match.homeTeamId)
        assertEquals("https://cdn.pitchapi.dev/away.webp", match.awayTeamLogoUrl)
        assertNull(match.minute)
        assertTrue(match.kickoff.isNotBlank())

        assertTrue(
            mapper.mapMatches(
                envelope(
                    """{"data":{"date":"2026-10-04","matches":[]}}""",
                ),
            ).isEmpty(),
        )
    }

    @Test
    fun mapsUpcomingAndFinishedStatusesAndSkipsUnknownStatus() {
        val matches = mapper.mapMatches(
            envelope<PitchApiMatchesByDateDto>(
                """
                    {"data":{"matches":[
                      ${matchJson("m_upcoming", "not_started")},
                      ${matchJson("m_finished", "finished")},
                      ${matchJson("m_unknown", "postponed")}
                    ]}}
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(MatchStatus.UPCOMING, MatchStatus.FINISHED), matches.map { it.status })
        assertEquals(null, matches.first().homeScore)
    }

    @Test
    fun mapsLiveStatusCaseInsensitively() {
        val matches = mapper.mapMatches(
            envelope<PitchApiMatchesByDateDto>(
                """{"data":{"matches":[${matchJson("m_live", "LIVE")}]}}""",
            ),
        )

        assertEquals(MatchStatus.LIVE, matches.single().status)
    }

    @Test
    fun mapsDocumentedEventsAndIgnoresUnknownEventTypes() {
        val match = sampleMatch()
        val events = mapper.mapEvents(
            envelope<PitchApiEventsDto>(
                """
                    {
                      "data": {
                        "match_id": "m_match01",
                        "events": [
                          {
                            "event_type": "Goal", "minute": 34, "minute_added": 2,
                            "team_id": "t_home", "player": {"id": "p_1", "name": "Striker"},
                            "score_home": 1, "score_away": 0, "is_penalty": true
                          },
                          {
                            "event_type": "redcard", "minute": 56,
                            "team_id": "t_away", "player": {"name": "Defender"}
                          },
                          {
                            "event_type": "yellowcard", "minute": 60,
                            "team_id": "t_home", "player": {"name": "Midfielder"}
                          },
                          {
                            "event_type": "substitution", "minute": 70,
                            "team_id": "t_away", "player": {"name": "Outgoing"},
                            "sub_in_player": {"name": "Incoming"}
                          },
                          {
                            "event_type": "penalty", "minute": 75,
                            "team_id": "t_home", "player": {"name": "Shooter"}
                          },
                          {"event_type": "unknown", "minute": 80, "team_id": "t_home"}
                        ]
                      }
                    }
                """.trimIndent(),
            ),
            match,
        )

        assertEquals(
            listOf(
                MatchEventType.GOAL,
                MatchEventType.RED_CARD,
                MatchEventType.YELLOW_CARD,
                MatchEventType.SUBSTITUTION,
                MatchEventType.PENALTY,
            ),
            events.map { it.type },
        )
        assertEquals(36, events[0].minute)
        assertEquals(MatchTeam.HOME, events[0].team)
        assertEquals("Striker (пенальти)", events[0].description)
        assertEquals(MatchTeam.AWAY, events[1].team)
        assertEquals("Outgoing → Incoming", events[3].description)
    }

    @Test
    fun countsOnlyShotsWhoseIsOnTargetIsTrue() {
        val match = sampleMatch()
        val counts = mapper.mapShotsOnTarget(
            envelope<PitchApiShotsDto>(
                """
                    {
                      "data": {
                        "match_id": "m_match01",
                        "periods": [{
                          "period": "FirstHalf",
                          "shots": [
                            {"id":"s_1","team_id":"t_home","is_on_target":true},
                            {"id":"s_2","team_id":"t_home","is_on_target":false},
                            {"id":"s_3","team_id":"t_away","is_on_target":true}
                          ]
                        }]
                      }
                    }
                """.trimIndent(),
            ),
            match,
        )

        assertEquals(ShotCounts(home = 1, away = 1), counts)
        assertNull(
            mapper.mapShotsOnTarget(
                envelope(
                    """{"data":{"match_id":"m_match01","periods":[{"shots":[{"team_id":"t_home"}]}]}}""",
                ),
                match,
            ),
        )
    }

    @Test
    fun eventIdentityIsStableAcrossRepeatedEventSnapshots() {
        val match = sampleMatch()
        val envelope = envelope<PitchApiEventsDto>(
            """
                {
                  "data": {
                    "match_id":"m_match01",
                    "events":[{
                      "event_type":"redcard","minute":56,"team_id":"t_away",
                      "player":{"name":"Defender"}
                    }]
                  }
                }
            """.trimIndent(),
        )
        val first = mapper.mapEvents(envelope, match).single()
        val second = mapper.mapEvents(envelope, match).single()

        assertEquals(first.identity(match.id), second.identity(match.id))
    }

    @Test
    fun mapsStatsShotsOnTargetAndDoesNotUseOtherShotMetrics() {
        val counts = mapper.mapShotsOnTargetFromStats(
            envelope<PitchApiTeamStatsDto>(
                """
                    {
                      "data": {
                        "match_id": "m_match01",
                        "periods": [{
                          "period": "All",
                          "groups": [{
                            "group_name": "Shots",
                            "items": [
                              {"key":"shots","home":"14","away":"9","format_type":"integer"},
                              {
                                "key":"shots_on_target","home":"6","away":"3",
                                "format_type":"integer"
                              }
                            ]
                          }]
                        }]
                      }
                    }
                """.trimIndent(),
            ),
            "m_match01",
        )

        assertEquals(ShotCounts(home = 6, away = 3), counts)
        assertNull(
            mapper.mapShotsOnTargetFromStats(
                envelope(
                    """{"data":{"match_id":"m_match01","periods":[]}}""",
                ),
                "m_match01",
            ),
        )
    }

    @Test
    fun rejectsResponsesForAnotherMatch() {
        try {
            mapper.mapShotsOnTargetFromStats(
                envelope(
                    """{"data":{"match_id":"m_other","periods":[]}}""",
                ),
                "m_match01",
            )
            throw AssertionError("Mismatched match IDs must be rejected")
        } catch (exception: IllegalArgumentException) {
            assertEquals("PitchAPI вернул статистику другого матча", exception.message)
        }
    }

    private fun sampleMatch() = mapper.mapMatch(
        gson.fromJson(
            """
                {
                  "id":"m_match01",
                  "league":{"name":"League"},
                  "home_team":{"id":"t_home","name":"Home FC"},
                  "away_team":{"id":"t_away","name":"Away FC"},
                  "status":"live",
                  "time_utc":"2026-10-04T16:30:00Z"
                }
            """.trimIndent(),
            com.footballradar.app.data.remote.model.PitchApiMatchDto::class.java,
        ),
    )!!

    private inline fun <reified T> envelope(json: String): PitchApiEnvelope<T> =
        gson.fromJson(json, object : TypeToken<PitchApiEnvelope<T>>() {}.type)

    private fun matchJson(id: String, status: String): String =
        """
            {
              "id":"$id",
              "league":{"name":"League"},
              "home_team":{"id":"${id}_home","name":"Home"},
              "away_team":{"id":"${id}_away","name":"Away"},
              "status":"$status",
              "time_utc":"2026-10-04T16:30:00Z"
            }
        """.trimIndent()
}
