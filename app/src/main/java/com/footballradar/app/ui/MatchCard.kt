package com.footballradar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchEventType
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.ui.theme.LocalAppThemeColors
import com.footballradar.app.ui.theme.TextMuted
import com.footballradar.app.ui.theme.TextPrimary
import com.footballradar.app.ui.theme.TextSecondary

@Composable
fun MatchCard(
    match: FootballMatch,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppThemeColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.matchCard,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = match.competition,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                )
                MatchTime(match)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TeamRow(name = match.homeTeam)
                    TeamRow(name = match.awayTeam)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScoreText(match.homeScore, match.status)
                    ScoreText(match.awayScore, match.status)
                }
            }

            if (match.events.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.035f))
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    match.events.take(3).forEach { event ->
                        EventChip(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTime(match: FootballMatch) {
    val colors = LocalAppThemeColors.current
    when (match.status) {
        MatchStatus.LIVE -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.live),
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.live,
            )
            Text(
                text = "${match.minute ?: 0}′",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.live,
            )
        }
        MatchStatus.UPCOMING -> Text(
            text = match.kickoff,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        MatchStatus.FINISHED -> Text(
            text = "ЗАВЕРШЁН",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
        )
    }
}

@Composable
private fun TeamRow(name: String) {
    val colors = LocalAppThemeColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "•",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.accent,
        )
    }
}

@Composable
private fun ScoreText(
    score: Int?,
    status: MatchStatus,
) {
    val colors = LocalAppThemeColors.current
    Text(
        text = score?.toString() ?: "—",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (status == MatchStatus.LIVE) TextPrimary else TextSecondary,
    )
}

@Composable
private fun EventChip(event: com.footballradar.app.domain.model.MatchEvent) {
    val colors = LocalAppThemeColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        when (event.type) {
            MatchEventType.GOAL -> Text("⚽", style = MaterialTheme.typography.labelSmall)
            MatchEventType.YELLOW_CARD -> CardMarker(Color(0xFFFFC857))
            MatchEventType.RED_CARD -> CardMarker(colors.live)
            MatchEventType.SUBSTITUTION -> Text("↔", color = TextSecondary)
        }
        Text(
            text = buildString {
                if (event.minute > 0) append("${event.minute}′ ")
                append(event.description)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (event.type == MatchEventType.RED_CARD) colors.live else TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun CardMarker(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 8.dp, height = 11.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}
