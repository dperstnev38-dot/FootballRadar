package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.model.PitchApiEnvelope
import com.footballradar.app.data.remote.model.PitchApiEventsDto
import com.footballradar.app.data.remote.model.PitchApiMatchDto
import com.footballradar.app.data.remote.model.PitchApiMatchesByDateDto
import com.footballradar.app.data.remote.model.PitchApiShotsDto
import com.footballradar.app.data.remote.model.PitchApiTeamStatsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("v1/date/{date}")
    suspend fun getMatchesByDate(
        @Path("date") date: String,
        @Query("status") status: String,
    ): Response<PitchApiEnvelope<PitchApiMatchesByDateDto>>

    @GET("v1/matches/{id}")
    suspend fun getMatch(
        @Path("id") matchId: String,
    ): Response<PitchApiEnvelope<PitchApiMatchDto>>

    @GET("v1/matches/{id}/events")
    suspend fun getMatchEvents(
        @Path("id") matchId: String,
    ): Response<PitchApiEnvelope<PitchApiEventsDto>>

    @GET("v1/matches/{id}/shots")
    suspend fun getMatchShots(
        @Path("id") matchId: String,
    ): Response<PitchApiEnvelope<PitchApiShotsDto>>

    @GET("v1/matches/{id}/stats")
    suspend fun getMatchStats(
        @Path("id") matchId: String,
    ): Response<PitchApiEnvelope<PitchApiTeamStatsDto>>
}
