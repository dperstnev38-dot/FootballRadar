package com.footballradar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.footballradar.app.domain.settings.AppSettings
import com.footballradar.app.domain.settings.ThemeColorSetting

data class AppThemeColors(
    val accent: Color = GreenLight,
    val background: Color = BackgroundDark,
    val matchCard: Color = CardBackground,
    val live: Color = LiveRed,
    val radarEvent: Color = RadarEventBlue,
)

val LocalAppThemeColors = staticCompositionLocalOf { AppThemeColors() }

fun AppSettings.toAppThemeColors(): AppThemeColors = AppThemeColors(
    accent = color(ThemeColorSetting.ACCENT).toComposeColor(),
    background = color(ThemeColorSetting.BACKGROUND).toComposeColor(),
    matchCard = color(ThemeColorSetting.MATCH_CARD).toComposeColor(),
    live = color(ThemeColorSetting.LIVE).toComposeColor(),
    radarEvent = color(ThemeColorSetting.RADAR_EVENT).toComposeColor(),
)

private fun String.toComposeColor(): Color =
    Color(android.graphics.Color.parseColor(this))

@Composable
fun FootballRadarTheme(
    colors: AppThemeColors = AppThemeColors(),
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = colors.accent,
        background = colors.background,
        surface = colors.matchCard,
        error = colors.live,
    )
    CompositionLocalProvider(LocalAppThemeColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
