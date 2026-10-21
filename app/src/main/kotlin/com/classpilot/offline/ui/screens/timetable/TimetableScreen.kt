package com.classpilot.offline.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.ui.components.ClassPeriodCard
import com.classpilot.offline.ui.components.EmptyState
import com.classpilot.offline.viewmodels.TimetableViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel = hiltViewModel(),
    onNavigateToday: () -> Unit,
    onNavigateTomorrow: () -> Unit,
    onNavigateWeekly: () -> Unit,
    onAddClass: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Schedule", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddClass(state.today.dayOfWeek.value) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Class") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick access cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickNavCard(
                        title = "Today",
                        subtitle = "${state.todayPeriods.size} classes",
                        icon = { Icon(Icons.Filled.Today, null) },
                        onClick = onNavigateToday,
                        modifier = Modifier.weight(1f)
                    )
                    QuickNavCard(
                        title = "Tomorrow",
                        subtitle = "${state.tomorrowPeriods.size} classes",
                        icon = { Icon(Icons.Filled.CalendarMonth, null) },
                        onClick = onNavigateTomorrow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = onNavigateWeekly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Full Weekly Timetable")
                }
            }

            // Today's classes preview
            item {
                Text(
                    text = "Today's Classes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (state.todayPeriods.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "📭",
                        title = "No classes today",
                        subtitle = "Enjoy your free day! Tap '+ Add Class' to set up your weekly schedule.",
                        action = {
                            Button(onClick = { onAddClass(state.today.dayOfWeek.value) }) {
                                Text("+ Add Scheduled Class")
                            }
                        }
                    )
                }
            } else {
                items(state.todayPeriods) { period ->
                    ClassPeriodCard(period = period)
                }
            }

            // Watermark
            item {
                com.classpilot.offline.ui.components.PoweredByWatermark()
            }
        }
    }
}

@Composable
private fun QuickNavCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
