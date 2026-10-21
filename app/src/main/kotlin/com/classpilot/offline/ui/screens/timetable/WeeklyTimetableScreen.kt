package com.classpilot.offline.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classpilot.offline.domain.model.ClassPeriod
import com.classpilot.offline.ui.components.ClassPeriodCard
import com.classpilot.offline.ui.components.EmptyState
import com.classpilot.offline.viewmodels.TimetableViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyTimetableScreen(
    viewModel: TimetableViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onAddClass: (Int) -> Unit,
    onEditEntry: (Long) -> Unit
) {
    var selectedDay by remember { mutableStateOf(DayOfWeek.MONDAY) }
    val periodsForDay by viewModel.getPeriodsForDay(selectedDay)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddClass(selectedDay.value) }) {
                Icon(Icons.Filled.Add, "Add class")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Day selector tabs
            ScrollableTabRow(
                selectedTabIndex = selectedDay.value - 1,
                edgePadding = 16.dp
            ) {
                DayOfWeek.values().forEach { day ->
                    Tab(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        text = {
                            Text(
                                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (periodsForDay.isEmpty()) {
                EmptyState(
                    emoji = "📭",
                    title = "No classes on ${selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                    subtitle = "Tap + to add a class",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 100.dp + padding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "${periodsForDay.size} class${if (periodsForDay.size != 1) "es" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(periodsForDay) { period ->
                        ClassPeriodCard(
                            period = period,
                            modifier = Modifier
                        )
                    }
                    item {
                        com.classpilot.offline.ui.components.PoweredByWatermark()
                    }
                }
            }
        }
    }
}

