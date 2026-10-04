package com.footballradar.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class PitchApiEnvelope<T>(
    @SerializedName("data")
    val data: T?,
    @SerializedName("error")
    val error: PitchApiErrorDto?,
)

data class PitchApiErrorDto(
    @SerializedName("code")
    val code: String?,
    @SerializedName("message")
    val message: String?,
)

data class PitchApiMatchesByDateDto(
    @SerializedName("date")
    val date: String?,
    @SerializedName("matches")
    val matches: List<PitchApiMatchDto>?,
)

data class PitchApiMatchDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("league")
    val league: PitchApiLeagueDto?,
    @SerializedName("home_team")
    val homeTeam: PitchApiTeamDto?,
    @SerializedName("away_team")
    val awayTeam: PitchApiTeamDto?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("time_utc")
    val timeUtc: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("score_home")
    val scoreHome: Int?,
    @SerializedName("score_away")
    val scoreAway: Int?,
)

data class PitchApiLeagueDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
)

data class PitchApiTeamDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
)

data class PitchApiEventsDto(
    @SerializedName("match_id")
    val matchId: String?,
    @SerializedName("events")
    val events: List<PitchApiEventDto>?,
)

data class PitchApiEventDto(
    @SerializedName("event_type")
    val eventType: String?,
    @SerializedName("minute")
    val minute: Int?,
    @SerializedName("minute_added")
    val minuteAdded: Int?,
    @SerializedName("team_id")
    val teamId: String?,
    @SerializedName("player")
    val player: PitchApiPlayerDto?,
    @SerializedName("sub_in_player")
    val subInPlayer: PitchApiPlayerDto?,
    @SerializedName("score_home")
    val scoreHome: Int?,
    @SerializedName("score_away")
    val scoreAway: Int?,
    @SerializedName("is_own_goal")
    val isOwnGoal: Boolean?,
    @SerializedName("is_penalty")
    val isPenalty: Boolean?,
)

data class PitchApiPlayerDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
)

data class PitchApiShotsDto(
    @SerializedName("match_id")
    val matchId: String?,
    @SerializedName("periods")
    val periods: List<PitchApiShotsPeriodDto>?,
)

data class PitchApiShotsPeriodDto(
    @SerializedName("period")
    val period: String?,
    @SerializedName("shots")
    val shots: List<PitchApiShotDto>?,
)

data class PitchApiShotDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("team_id")
    val teamId: String?,
    @SerializedName("is_on_target")
    val isOnTarget: Boolean?,
)

data class PitchApiTeamStatsDto(
    @SerializedName("match_id")
    val matchId: String?,
    @SerializedName("periods")
    val periods: List<PitchApiStatsPeriodDto>?,
)

data class PitchApiStatsPeriodDto(
    @SerializedName("period")
    val period: String?,
    @SerializedName("groups")
    val groups: List<PitchApiStatsGroupDto>?,
)

data class PitchApiStatsGroupDto(
    @SerializedName("group_name")
    val groupName: String?,
    @SerializedName("items")
    val items: List<PitchApiStatsItemDto>?,
)

data class PitchApiStatsItemDto(
    @SerializedName("key")
    val key: String?,
    @SerializedName("home")
    val home: String?,
    @SerializedName("away")
    val away: String?,
    @SerializedName("format_type")
    val formatType: String?,
)
