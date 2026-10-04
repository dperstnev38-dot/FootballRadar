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
import com.footballradar.app.domain.radar.RadarEvent
import com.footballradar.app.domain.radar.RadarEventType
import com.footballradar.app.ui.theme.LocalAppThemeColors
import com.footballradar.app.ui.theme.TextMuted
import com.footballradar.app.ui.theme.TextPrimary
import com.footballradar.app.ui.theme.TextSecondary

@Composable
fun RadarEventCard(
    event: RadarEvent,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppThemeColors.current
    val (icon, title, accent) = when (event.type) {
        RadarEventType.RED_CARD -> Triple("🟥", "КРАСНАЯ КАРТОЧКА", colors.live)
        RadarEventType.GOAL -> Triple("⚽", "ГОЛ", colors.radarEvent)
        RadarEventType.SHOTS_ON_TARGET ->
            Triple("🎯", "${event.shotsOnTarget} УДАРА В СТВОР", colors.radarEvent)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.matchCard,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                    Text(
                        text = "${event.minute}′",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                    )
                }
                Text(
                    text = event.team + (event.playerName?.let { " · $it" } ?: ""),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = event.competition,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    event.score?.let { score ->
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}
