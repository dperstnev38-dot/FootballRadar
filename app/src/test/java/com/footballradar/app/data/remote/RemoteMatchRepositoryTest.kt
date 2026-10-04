package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.model.PitchApiEnvelope
import com.footballradar.app.data.remote.model.PitchApiEventsDto
import com.footballradar.app.data.remote.model.PitchApiMatchDto
import com.footballradar.app.data.remote.model.PitchApiMatchesByDateDto
import com.footballradar.app.data.remote.model.PitchApiShotsDto
import com.footballradar.app.data.remote.model.PitchApiTeamStatsDto
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.repository.MatchDataState
import com.footballradar.app.domain.repository.MatchStatisticsState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteMatchRepositoryTest {
    @Test
    fun requestsTodaysAllMatchesAndTreatsAnEmptyListAsSuccess() = runBlocking {
        val service = FakePitchApiService()
        val repository = RemoteMatchRepository(service, config = configuredApi)

        val state = repository.fetchMatches()

        assertEquals(MatchDataState.Success(emptyList()), state)
        assertTrue(service.requestedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertEquals("all", service.requestedStatus)
        assertEquals(1, service.dateCalls)
    }

    @Test
    fun fetchesDetailsEventsShotsAndStatsWhileUsingShotFlagsWithoutAddingStats() = runBlocking {
        val service = FakePitchApiService(
            dateResponse = success(
                """
                    {"data":{"matches":[${MATCH_JSON}]}}
                """,
            ),
            detailsResponse = success("""{"data":$MATCH_JSON}"""),
            eventsResponse = success(
                """
                    {
                      "data": {
                        "match_id":"m_match01",
                        "events":[{
                          "event_type":"redcard","minute":42,"team_id":"t_away",
                          "player":{"name":"Defender"}
                        }]
                      }
                    }
                """,
            ),
            shotsResponse = success(
                """
                    {
                      "data": {
                        "match_id":"m_match01",
                        "periods":[{
                          "period":"FirstHalf",
                          "shots":[
                            {"team_id":"t_home","is_on_target":true},
                            {"team_id":"t_home","is_on_target":false},
                            {"team_id":"t_away","is_on_target":true}
                          ]
                        }]
                      }
                    }
                """,
            ),
            statsResponse = success(
                """
                    {
                      "data": {
                        "match_id":"m_match01",
                        "periods":[{
                          "period":"All",
                          "groups":[{
                            "group_name":"Shots",
                            "items":[{
                              "key":"shots_on_target","home":"9","away":"8",
                              "format_type":"integer"
                            }]
                          }]
                        }]
                      }
                    }
                """,
            ),
        )
        val repository = RemoteMatchRepository(service, config = configuredApi)
        val initialMatches = (repository.fetchMatches() as MatchDataState.Success).matches
        assertEquals(MatchStatus.LIVE, initialMatches.single().status)

        val details = repository.fetchMatchStatistics("m_match01") as MatchStatisticsState.Success

        assertEquals("m_match01", service.requestedDetailsId)
        assertEquals("m_match01", service.requestedEventsId)
        assertEquals("m_match01", service.requestedShotsId)
        assertTrue(service.statsRequested)
        assertEquals(1, details.match.homeShotsOnTarget)
        assertEquals(1, details.match.awayShotsOnTarget)
        assertEquals(MatchEventType.RED_CARD, details.match.events.single().type)
        assertEquals("Defender", details.match.events.single().description)
        assertTrue(details.match.statisticsLoaded)
    }

    @Test
    fun fallsBackToTheDocumentedStatsShotsOnTargetKeyWhenShotsAreUnavailable() = runBlocking {
        val service = FakePitchApiService(
            dateResponse = success("""{"data":{"matches":[${MATCH_JSON}]}}"""),
            detailsResponse = success("""{"data":$MATCH_JSON}"""),
            eventsResponse = success("""{"data":{"match_id":"m_match01","events":[]}}"""),
            shotsResponse = success("""{"data":{"match_id":"m_match01"}}"""),
            statsResponse = success(
                """
                    {
                      "data": {
                        "match_id":"m_match01",
                        "periods":[{
                          "period":"All",
                          "groups":[{
                            "group_name":"Shots",
                            "items":[
                              {"key":"shots","home":"15","away":"8","format_type":"integer"},
                              {
                                "key":"shots_on_target","home":"5","away":"2",
                                "format_type":"integer"
                              }
                            ]
                          }]
                        }]
                      }
                    }
                """,
            ),
        )
        val repository = RemoteMatchRepository(service, config = configuredApi)

        val details = repository.fetchMatchStatistics("m_match01") as MatchStatisticsState.Success

        assertTrue(service.statsRequested)
        assertEquals(5, details.match.homeShotsOnTarget)
        assertEquals(2, details.match.awayShotsOnTarget)
    }

    @Test
    fun keepsUnavailableShotStatisticsNullInsteadOfReportingZero() = runBlocking {
        val service = FakePitchApiService(
            detailsResponse = success("""{"data":$MATCH_JSON}"""),
            eventsResponse = success("""{"data":{"match_id":"m_match01","events":[]}}"""),
            shotsResponse = success("""{"data":{"match_id":"m_match01","periods":null}}"""),
            statsResponse = success(
                """{"data":{"match_id":"m_match01","periods":[]}}""",
            ),
        )
        val repository = RemoteMatchRepository(service, config = configuredApi)

        val details = repository.fetchMatchStatistics("m_match01") as MatchStatisticsState.Success

        assertEquals(null, details.match.homeShotsOnTarget)
        assertEquals(null, details.match.awayShotsOnTarget)
        assertTrue(details.match.statisticsLoaded)
    }

    @Test
    fun preservesEnrichedMatchDataDuringDayListRefresh() = runBlocking {
        val service = FakePitchApiService(
            dateResponse = success("""{"data":{"matches":[${MATCH_JSON}]}}"""),
            detailsResponse = success("""{"data":$MATCH_JSON}"""),
            eventsResponse = success(
                """
                    {"data":{"match_id":"m_match01","events":[{
                      "event_type":"goal","minute":31,"team_id":"t_home",
                      "player":{"name":"Forward"}
                    }]}}
                """,
            ),
            shotsResponse = success(
                """
                    {"data":{"match_id":"m_match01","periods":[{
                      "period":"FirstHalf",
                      "shots":[
                        {"team_id":"t_home","is_on_target":true},
                        {"team_id":"t_home","is_on_target":true},
                        {"team_id":"t_away","is_on_target":false}
                      ]
                    }]}}
                """,
            ),
        )
        val repository = RemoteMatchRepository(service, config = configuredApi)
        repository.fetchMatches()
        repository.fetchMatchStatistics("m_match01")

        val refreshed = repository.fetchMatches() as MatchDataState.Success

        assertEquals(2, refreshed.matches.single().homeShotsOnTarget)
        assertEquals("Forward", refreshed.matches.single().events.single().description)
        assertTrue(refreshed.matches.single().statisticsLoaded)
    }

    @Test
    fun reportsActionableUnauthorizedRateLimitNetworkAndTimeoutErrors() = runBlocking {
        val unauthorized = RemoteMatchRepository(
            FakePitchApiService(
                dateResponse = Response.error(401, "unauthorized".toResponseBody()),
            ),
        ).fetchMatches() as MatchDataState.Error
        assertEquals("Проверьте API-ключ PitchAPI (HTTP 401)", unauthorized.message)

        val limited = RemoteMatchRepository(
            FakePitchApiService(
                dateResponse = Response.error(429, "limited".toResponseBody()),
            ),
        ).fetchMatches() as MatchDataState.Error
        assertEquals(
            "PitchAPI ограничил частоту запросов. Повторите позже (HTTP 429)",
            limited.message,
        )

        val forbidden = RemoteMatchRepository(
            FakePitchApiService(
                dateResponse = Response.error(403, "forbidden".toResponseBody()),
            ),
        ).fetchMatches() as MatchDataState.Error
        assertEquals("PitchAPI запретил доступ к данным (HTTP 403)", forbidden.message)

        val missing = RemoteMatchRepository(
            FakePitchApiService(
                dateResponse = Response.error(404, "missing".toResponseBody()),
            ),
        ).fetchMatches() as MatchDataState.Error
        assertEquals("PitchAPI не нашёл запрошенные данные (HTTP 404)", missing.message)

        val offline = RemoteMatchRepository(
            FakePitchApiService(dateException = IOException("offline")),
        ).fetchMatches() as MatchDataState.Error
        assertEquals("offline", offline.message)

        val timeout = RemoteMatchRepository(
            FakePitchApiService(dateException = SocketTimeoutException()),
        ).fetchMatches() as MatchDataState.Error
        assertEquals("Время ожидания ответа PitchAPI истекло", timeout.message)
    }

    private class FakePitchApiService(
        var dateResponse: Response<PitchApiEnvelope<PitchApiMatchesByDateDto>> =
            success("""{"data":{"date":"2026-10-04","matches":[]}}"""),
        var detailsResponse: Response<PitchApiEnvelope<PitchApiMatchDto>> =
            success("""{"data":$MATCH_JSON}"""),
        var eventsResponse: Response<PitchApiEnvelope<PitchApiEventsDto>> =
            success("""{"data":{"match_id":"m_match01","events":[]}}"""),
        var shotsResponse: Response<PitchApiEnvelope<PitchApiShotsDto>> =
            success("""{"data":{"match_id":"m_match01","periods":[]}}"""),
        var statsResponse: Response<PitchApiEnvelope<PitchApiTeamStatsDto>> =
            success("""{"data":{"match_id":"m_match01","periods":[]}}"""),
        private val dateException: IOException? = null,
    ) : ApiService {
        var dateCalls = 0
        var requestedDate = ""
        var requestedStatus = ""
        var requestedDetailsId: String? = null
        var requestedEventsId: String? = null
        var requestedShotsId: String? = null
        var statsRequested = false

        override suspend fun getMatchesByDate(
            date: String,
            status: String,
        ): Response<PitchApiEnvelope<PitchApiMatchesByDateDto>> {
            dateCalls++
            requestedDate = date
            requestedStatus = status
            dateException?.let { throw it }
            return dateResponse
        }

        override suspend fun getMatch(
            matchId: String,
        ): Response<PitchApiEnvelope<PitchApiMatchDto>> {
            requestedDetailsId = matchId
            return detailsResponse
        }

        override suspend fun getMatchEvents(
            matchId: String,
        ): Response<PitchApiEnvelope<PitchApiEventsDto>> {
            requestedEventsId = matchId
            return eventsResponse
        }

        override suspend fun getMatchShots(
            matchId: String,
        ): Response<PitchApiEnvelope<PitchApiShotsDto>> {
            requestedShotsId = matchId
            return shotsResponse
        }

        override suspend fun getMatchStats(
            matchId: String,
        ): Response<PitchApiEnvelope<PitchApiTeamStatsDto>> {
            statsRequested = true
            return statsResponse
        }
    }

    private companion object {
        val gson = Gson()
        val configuredApi = FootballApiConfig(
            baseUrl = FootballApiConfig.DEFAULT_BASE_URL,
            apiKey = "pk_test_unit-test",
        )
        const val MATCH_JSON = """
            {
              "id":"m_match01",
              "league":{"id":"l_league","name":"Premier League"},
              "home_team":{"id":"t_home","name":"Home FC"},
              "away_team":{"id":"t_away","name":"Away FC"},
              "date":"2026-10-04",
              "time_utc":"2026-10-04T16:30:00Z",
              "status":"live",
              "score_home":1,
              "score_away":0
            }
        """

        inline fun <reified T> success(json: String): Response<PitchApiEnvelope<T>> =
            Response.success(
                gson.fromJson(json, object : TypeToken<PitchApiEnvelope<T>>() {}.type),
            )
    }
}
