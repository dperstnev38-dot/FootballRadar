package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.model.LiveMatchesResponse
import com.footballradar.app.domain.repository.MatchDataState
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteMatchRepositoryTest {
    @Test
    fun reportsNetworkUnavailableWithoutSubstitutingDemoMatches() = runBlocking {
        val repository = RemoteMatchRepository(
            apiService = object : ApiService {
                override suspend fun getLiveMatches(): Response<LiveMatchesResponse> {
                    throw IOException("offline")
                }
            },
        )

        val state = repository.fetchMatches()

        assertTrue(state is MatchDataState.Error)
        assertEquals("Нет соединения с футбольным сервером", (state as MatchDataState.Error).message)
    }

    @Test
    fun reportsEmptyResponseWithoutSubstitutingDemoMatches() = runBlocking {
        val repository = RemoteMatchRepository(
            apiService = object : ApiService {
                override suspend fun getLiveMatches(): Response<LiveMatchesResponse> =
                    Response.success(LiveMatchesResponse(matches = emptyList()))
            },
        )

        val state = repository.fetchMatches()

        assertTrue(state is MatchDataState.Error)
        assertEquals("Футбольный сервер пока не вернул матчи", (state as MatchDataState.Error).message)
    }

    @Test
    fun reportsHttpFailureAsError() = runBlocking {
        val repository = RemoteMatchRepository(
            apiService = object : ApiService {
                override suspend fun getLiveMatches(): Response<LiveMatchesResponse> =
                    Response.error(503, "Unavailable".toResponseBody())
            },
        )

        val state = repository.fetchMatches()

        assertEquals("Ошибка футбольного сервера (HTTP 503)", (state as MatchDataState.Error).message)
    }
}
