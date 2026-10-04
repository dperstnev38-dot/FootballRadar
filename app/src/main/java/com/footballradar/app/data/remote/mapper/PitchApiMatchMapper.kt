package com.footballradar.app.data.remote.mapper

import com.footballradar.app.data.remote.model.PitchApiEnvelope
import com.footballradar.app.data.remote.model.PitchApiEventDto
import com.footballradar.app.data.remote.model.PitchApiMatchDto
import com.footballradar.app.data.remote.model.PitchApiMatchesByDateDto
import com.footballradar.app.data.remote.model.PitchApiShotsDto
import com.footballradar.app.data.remote.model.PitchApiTeamStatsDto
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchEvent
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.model.MatchTeam
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class PitchApiMatchMapper {
    fun mapMatches(envelope: PitchApiEnvelope<PitchApiMatchesByDateDto>): List<FootballMatch> {
        val matches = envelope.data?.matches.orEmpty()
        return matches.mapNotNull(::mapMatch)
    }

    fun mapMatch(dto: PitchApiMatchDto): FootballMatch? {
        val status = dto.status.toMatchStatus() ?: return null
        val matchId = dto.id.required("id")
        val home = dto.homeTeam.required("home_team")
        val away = dto.awayTeam.required("away_team")
        return FootballMatch(
            id = matchId,
            competition = dto.league?.name.required("league.name"),
            status = status,
            kickoff = dto.timeUtc.toLocalKickoff(),
            homeTeam = home.name.required("home_team.name"),
            awayTeam = away.name.required("away_team.name"),
            homeScore = dto.scoreHome,
            awayScore = dto.scoreAway,
            homeTeamLogoUrl = home.imageUrl,
            awayTeamLogoUrl = away.imageUrl,
            homeTeamId = home.id,
            awayTeamId = away.id,
        )
    }

    fun mapDetails(
        envelope: PitchApiEnvelope<PitchApiMatchDto>,
        expectedMatchId: String,
    ): FootballMatch {
        val dto = envelope.data
            ?: throw IllegalArgumentException("PitchAPI не вернул данные матча")
        require(dto.id == expectedMatchId) {
            "PitchAPI вернул данные другого матча"
        }
        return mapMatch(dto)
            ?: throw IllegalArgumentException("PitchAPI вернул неизвестный статус матча")
    }

    fun mapEvents(
        envelope: PitchApiEnvelope<com.footballradar.app.data.remote.model.PitchApiEventsDto>,
        match: FootballMatch,
    ): List<MatchEvent> {
        val data = envelope.data
            ?: throw IllegalArgumentException("PitchAPI не вернул события матча")
        require(data.matchId == null || data.matchId == match.id) {
            "PitchAPI вернул события другого матча"
        }
        return data.events.orEmpty()
            .sortedWith(compareBy<PitchApiEventDto> { it.minute ?: Int.MAX_VALUE }
                .thenBy { it.minuteAdded ?: 0 })
            .mapNotNull { event -> event.toDomainEvent(match) }
    }

    fun mapShotsOnTarget(
        envelope: PitchApiEnvelope<PitchApiShotsDto>,
        match: FootballMatch,
    ): ShotCounts? {
        val data = envelope.data ?: return null
        require(data.matchId == null || data.matchId == match.id) {
            "PitchAPI вернул удары другого матча"
        }
        if (match.homeTeamId == null || match.awayTeamId == null) return null
        val periods = data.periods ?: return null
        var home = 0
        var away = 0
        periods.flatMap { it.shots.orEmpty() }.forEach { shot ->
            val isOnTarget = shot.isOnTarget ?: return null
            when (shot.teamId) {
                match.homeTeamId -> if (isOnTarget) home++
                match.awayTeamId -> if (isOnTarget) away++
                else -> return null
            }
        }
        return ShotCounts(home, away)
    }

    fun mapShotsOnTargetFromStats(
        envelope: PitchApiEnvelope<PitchApiTeamStatsDto>,
        expectedMatchId: String,
    ): ShotCounts? {
        val data = envelope.data ?: return null
        require(data.matchId == null || data.matchId == expectedMatchId) {
            "PitchAPI вернул статистику другого матча"
        }
        val allPeriod = data.periods.orEmpty()
            .firstOrNull { it.period.equals("All", ignoreCase = true) }
            ?: return null
        val item = allPeriod.groups.orEmpty()
            .flatMap { it.items.orEmpty() }
            .firstOrNull { it.key.equals(SHOTS_ON_TARGET_KEY, ignoreCase = true) }
            ?: return null
        val home = item.home?.toIntOrNull() ?: return null
        val away = item.away?.toIntOrNull() ?: return null
        return ShotCounts(home, away)
    }

    private fun PitchApiEventDto.toDomainEvent(match: FootballMatch): MatchEvent? {
        val type = eventType.toMatchEventType() ?: return null
        val minute = minute ?: return null
        if (match.homeTeamId == null || match.awayTeamId == null) return null
        val team = when (teamId) {
            match.homeTeamId -> MatchTeam.HOME
            match.awayTeamId -> MatchTeam.AWAY
            else -> return null
        }
        val playerName = player?.name?.takeIf(String::isNotBlank)
        val incomingPlayerName = subInPlayer?.name?.takeIf(String::isNotBlank)
        val description = when (type) {
            MatchEventType.SUBSTITUTION -> listOfNotNull(playerName, incomingPlayerName)
                .joinToString(" → ")
                .ifBlank { "Замена" }
            MatchEventType.GOAL -> buildString {
                append(playerName ?: "Гол")
                if (isOwnGoal == true) append(" (автогол)")
                if (isPenalty == true) append(" (пенальти)")
            }
            MatchEventType.PENALTY -> playerName ?: "Пенальти"
            MatchEventType.YELLOW_CARD -> playerName ?: "Жёлтая карточка"
            MatchEventType.RED_CARD -> playerName ?: "Красная карточка"
        }
        return MatchEvent(
            minute = minute + (minuteAdded ?: 0),
            description = description,
            type = type,
            team = team,
        )
    }

    private fun String?.toMatchStatus(): MatchStatus? =
        when (this?.lowercase(Locale.ROOT)) {
            "live" -> MatchStatus.LIVE
            "not_started" -> MatchStatus.UPCOMING
            "finished" -> MatchStatus.FINISHED
            else -> null
        }

    private fun String?.toMatchEventType(): MatchEventType? =
        when (this?.filter(Char::isLetterOrDigit)?.lowercase(Locale.ROOT)) {
            "goal" -> MatchEventType.GOAL
            "yellowcard" -> MatchEventType.YELLOW_CARD
            "redcard" -> MatchEventType.RED_CARD
            "substitution" -> MatchEventType.SUBSTITUTION
            "penalty" -> MatchEventType.PENALTY
            else -> null
        }

    private fun String?.toLocalKickoff(): String {
        if (this.isNullOrBlank()) return ""
        return try {
            KICKOFF_FORMATTER.format(Instant.parse(this).atZone(ZoneId.systemDefault()))
        } catch (_: DateTimeParseException) {
            this
        }
    }

    private fun String?.required(field: String): String =
        this?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("PitchAPI is missing $field")

    private fun <T : Any> T?.required(field: String): T =
        this ?: throw IllegalArgumentException("PitchAPI is missing $field")

    companion object {
        private const val SHOTS_ON_TARGET_KEY = "shots_on_target"
        private val KICKOFF_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }
}

data class ShotCounts(
    val home: Int,
    val away: Int,
)
