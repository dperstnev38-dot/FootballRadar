package com.footballradar.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class LiveMatchesResponse(
    @SerializedName("matches")
    val matches: List<NetworkMatchDto>?,
)

data class NetworkMatchDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("competition")
    val competition: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("kickoff")
    val kickoff: String?,
    @SerializedName("minute")
    val minute: Int?,
    @SerializedName("homeTeam")
    val homeTeam: String?,
    @SerializedName("awayTeam")
    val awayTeam: String?,
    @SerializedName("homeScore")
    val homeScore: Int?,
    @SerializedName("awayScore")
    val awayScore: Int?,
    @SerializedName("homeShotsOnTarget")
    val homeShotsOnTarget: Int?,
    @SerializedName("awayShotsOnTarget")
    val awayShotsOnTarget: Int?,
    @SerializedName("events")
    val events: List<NetworkMatchEventDto>?,
)

data class NetworkMatchEventDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("minute")
    val minute: Int?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("team")
    val team: String?,
)
