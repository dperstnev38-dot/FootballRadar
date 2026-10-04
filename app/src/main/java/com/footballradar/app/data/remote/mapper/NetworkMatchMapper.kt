package com.footballradar.app.data.remote.mapper

import com.footballradar.app.data.remote.model.LiveMatchesResponse
import com.footballradar.app.data.remote.model.NetworkMatchDto
import com.footballradar.app.data.remote.model.NetworkMatchEventDto
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchEvent
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.model.MatchTeam
import java.util.Locale

class NetworkMatchMapper {
    fun map(response: LiveMatchesResponse): List<FootballMatch> =
        response.matches.orEmpty().map(::mapMatch)

    private fun mapMatch(dto: NetworkMatchDto): FootballMatch {
        val id = dto.id.required("match id")
        val competition = dto.competition.required("competition")
        val homeTeam = dto.homeTeam.required("home team")
        val awayTeam = dto.awayTeam.required("away team")
        val status = dto.status.required("match status").uppercase(Locale.ROOT)

        return FootballMatch(
            id = id,
            competition = competition,
            status = when (status) {
                "LIVE", "IN_PLAY" -> MatchStatus.LIVE
                "UPCOMING", "NOT_STARTED" -> MatchStatus.UPCOMING
                "FINISHED", "COMPLETED" -> MatchStatus.FINISHED
                else -> throw IllegalArgumentException("Unsupported match status: $status")
            },
            kickoff = dto.kickoff.orEmpty(),
            minute = dto.minute,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            homeScore = dto.homeScore,
            awayScore = dto.awayScore,
            homeShotsOnTarget = dto.homeShotsOnTarget ?: 0,
            awayShotsOnTarget = dto.awayShotsOnTarget ?: 0,
            events = dto.events.orEmpty().mapNotNull { event ->
                mapEvent(event, homeTeam, awayTeam)
            },
        )
    }

    private fun mapEvent(
        dto: NetworkMatchEventDto,
        homeTeam: String,
        awayTeam: String,
    ): MatchEvent? {
        val type = when (dto.type?.uppercase(Locale.ROOT)) {
            "GOAL" -> MatchEventType.GOAL
            "YELLOW_CARD" -> MatchEventType.YELLOW_CARD
            "RED_CARD" -> MatchEventType.RED_CARD
            "SUBSTITUTION" -> MatchEventType.SUBSTITUTION
            else -> return null
        }
        val minute = dto.minute ?: return null
        val description = dto.description.required("event description")
        val normalizedTeam = dto.team?.uppercase(Locale.ROOT)
        val team = when (normalizedTeam) {
            "HOME" -> MatchTeam.HOME
            "AWAY" -> MatchTeam.AWAY
            homeTeam.uppercase(Locale.ROOT) -> MatchTeam.HOME
            awayTeam.uppercase(Locale.ROOT) -> MatchTeam.AWAY
            else -> throw IllegalArgumentException("Unknown event team: ${dto.team}")
        }

        return MatchEvent(
            minute = minute,
            description = description,
            type = type,
            team = team,
            eventId = dto.id,
        )
    }

    private fun String?.required(field: String): String =
        this?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Missing $field")
}
