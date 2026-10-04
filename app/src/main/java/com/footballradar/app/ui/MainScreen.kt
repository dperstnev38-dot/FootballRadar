package com.footballradar.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footballradar.app.domain.model.FootballMatch
import com.footballradar.app.domain.model.MatchStatus
import com.footballradar.app.domain.radar.RadarEvent
import com.footballradar.app.domain.radar.RadarEventType
import com.footballradar.app.ui.theme.TextPrimary
import com.footballradar.app.ui.theme.TextSecondary
import com.footballradar.app.ui.theme.LocalAppThemeColors
import com.footballradar.app.ui.theme.FootballRadarTheme
import com.footballradar.app.ui.theme.toAppThemeColors
import com.footballradar.app.ui.settings.SettingsViewModel
import com.footballradar.app.ui.settings.SettingsScreen
import com.footballradar.app.ui.settings.UpdateViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: FootballRadarViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    FootballRadarTheme(colors = settings.toAppThemeColors()) {
        MainScreenContent(
            uiState = uiState,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            updateViewModel = updateViewModel,
            modifier = modifier,
        )
    }
}

@Composable
private fun MainScreenContent(
    uiState: FootballRadarUiState,
    viewModel: FootballRadarViewModel,
    settingsViewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier,
) {
    val colors = LocalAppThemeColors.current
    val filteredMatches = uiState.matches
        .filter { match ->
            when (uiState.selectedFilter) {
                MatchFilter.ALL -> true
                MatchFilter.LIVE -> match.status == MatchStatus.LIVE
                MatchFilter.UPCOMING -> match.status == MatchStatus.UPCOMING
                MatchFilter.FINISHED -> match.status == MatchStatus.FINISHED
            }
        }
        .filter { match ->
            uiState.searchQuery.isBlank() ||
                match.competition.contains(uiState.searchQuery, ignoreCase = true) ||
                match.homeTeam.contains(uiState.searchQuery, ignoreCase = true) ||
                match.awayTeam.contains(uiState.searchQuery, ignoreCase = true)
        }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 12.dp)
                    .widthIn(max = 720.dp),
            ) {
                TopBar(
                    liveCount = uiState.matches.count { it.status == MatchStatus.LIVE },
                    onSearchClick = viewModel::toggleSearch,
                )
                if (uiState.isSearchVisible && uiState.destination != MainDestination.SETTINGS) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        singleLine = true,
                        placeholder = { Text("Команда или турнир") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = colors.live,
                            unfocusedBorderColor = colors.matchCard,
                            focusedContainerColor = colors.matchCard,
                            unfocusedContainerColor = colors.matchCard,
                        ),
                    )
                }
                DestinationBar(
                    selectedDestination = uiState.destination,
                    radarCount = uiState.radarEvents.size,
                    onDestinationSelected = viewModel::selectDestination,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            if (uiState.destination == MainDestination.MATCHES) {
                FilterBar(
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = viewModel::selectFilter,
                    modifier = Modifier.widthIn(max = 720.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                when {
                    uiState.destination == MainDestination.SETTINGS ->
                        SettingsScreen(
                            settingsViewModel = settingsViewModel,
                            updateViewModel = updateViewModel,
                            modifier = Modifier.widthIn(max = 720.dp),
                        )
                    uiState.loadStatus == MatchLoadStatus.LOADING -> LoadingState()
                    uiState.loadStatus == MatchLoadStatus.ERROR ->
                        MessageState(
                            title = if (
                                uiState.errorMessage?.contains(
                                    "Футбольный API не настроен",
                                    ignoreCase = true,
                                ) == true ||
                                uiState.errorMessage?.contains(
                                    "футбольного API",
                                    ignoreCase = true,
                                ) == true
                                || uiState.errorMessage?.contains(
                                    "PitchAPI не настроен",
                                    ignoreCase = true,
                                ) == true
                            ) {
                                "PitchAPI не настроен"
                            } else {
                                "Не удалось загрузить матчи"
                            },
                            message = uiState.errorMessage ?: "Попробуйте ещё раз позже",
                        )
                    uiState.destination == MainDestination.RADAR -> {
                        val filteredEvents = uiState.radarEvents.filter { event ->
                            uiState.searchQuery.isBlank() ||
                                event.team.contains(uiState.searchQuery, ignoreCase = true) ||
                                event.competition.contains(uiState.searchQuery, ignoreCase = true) ||
                                event.playerName.orEmpty()
                                    .contains(uiState.searchQuery, ignoreCase = true)
                        }
                        if (filteredEvents.isEmpty()) {
                            MessageState(
                                title = "Событий пока нет",
                                message = "Здесь появятся важные моменты матчей",
                            )
                        } else {
                            RadarEventList(
                                events = filteredEvents,
                                modifier = Modifier.widthIn(max = 720.dp),
                            )
                        }
                    }
                    filteredMatches.isEmpty() -> {
                        val isLiveFilter = uiState.selectedFilter == MatchFilter.ALL ||
                            uiState.selectedFilter == MatchFilter.LIVE
                        MessageState(
                            title = if (isLiveFilter) {
                                "Сейчас нет матчей в эфире"
                            } else {
                                "Матчей по этому фильтру нет"
                            },
                            message = if (isLiveFilter) {
                                "Новые LIVE-матчи появятся здесь автоматически"
                            } else {
                                "Попробуйте выбрать другой фильтр"
                            },
                        )
                    }
                    else -> MatchList(
                        matches = filteredMatches,
                        statisticsLoadingMatchIds = uiState.statisticsLoadingMatchIds,
                        statisticsErrorsByMatchId = uiState.statisticsErrorsByMatchId,
                        onLoadStatistics = viewModel::loadMatchStatistics,
                        modifier = Modifier.widthIn(max = 720.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationBar(
    selectedDestination: MainDestination,
    radarCount: Int,
    onDestinationSelected: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppThemeColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(colors.matchCard)
            .padding(4.dp),
    ) {
        MainDestination.entries.forEach { destination ->
            val selected = destination == selectedDestination
            val label = when (destination) {
                MainDestination.MATCHES -> "МАТЧИ"
                MainDestination.RADAR -> "РАДАР · $radarCount"
                MainDestination.SETTINGS -> "НАСТРОЙКИ"
            }
            Surface(
                onClick = { onDestinationSelected(destination) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) colors.accent.copy(alpha = 0.18f) else Color.Transparent,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 11.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) TextPrimary else colors.accent,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    liveCount: Int,
    onSearchClick: () -> Unit,
) {
    val colors = LocalAppThemeColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Football Radar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.live),
                )
                Text(
                    text = "  $liveCount МАТЧА В ЭФИРЕ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }
        }
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.matchCard),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Поиск матчей",
                tint = TextPrimary,
            )
        }
    }
}

@Composable
private fun RadarEventList(
    events: List<RadarEvent>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(events, key = { it.id }) { event ->
            RadarEventCard(event = event)
        }
    }
}

@Composable
private fun FilterBar(
    selectedFilter: MatchFilter,
    onFilterSelected: (MatchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppThemeColors.current
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        items(MatchFilter.entries) { filter ->
            val selected = filter == selectedFilter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(50),
                color = if (selected) colors.live else colors.matchCard,
            ) {
                Text(
                    text = filter.label,
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MatchList(
    matches: List<FootballMatch>,
    statisticsLoadingMatchIds: Set<String>,
    statisticsErrorsByMatchId: Map<String, String>,
    onLoadStatistics: (FootballMatch) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(matches, key = { it.id }) { match ->
            MatchCard(
                match = match,
                onLoadStatistics = { onLoadStatistics(match) },
                statisticsLoading = match.id in statisticsLoadingMatchIds,
                statisticsError = statisticsErrorsByMatchId[match.id],
            )
        }
    }
}

@Composable
private fun LoadingState() {
    val colors = LocalAppThemeColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = colors.accent)
        Text(
            text = "Загружаем матчи...",
            modifier = Modifier.padding(top = 16.dp),
            color = TextSecondary,
        )
    }
}

@Composable
private fun MessageState(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = TextSecondary,
        )
    }
}
