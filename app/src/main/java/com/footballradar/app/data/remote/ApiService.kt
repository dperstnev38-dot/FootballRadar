package com.footballradar.app.data.remote

import com.footballradar.app.data.remote.model.LiveMatchesResponse
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("matches/live")
    suspend fun getLiveMatches(): Response<LiveMatchesResponse>
}
