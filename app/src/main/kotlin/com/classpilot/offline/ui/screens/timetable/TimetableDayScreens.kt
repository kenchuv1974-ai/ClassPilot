package com.classpilot.offline.ui.screens.timetable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.domain.model.AttendanceStatus
import com.classpilot.offline.domain.model.ClassPeriod
import com.classpilot.offline.ui.components.ClassPeriodCard
import com.classpilot.offline.ui.components.EmptyState
import com.classpilot.offline.viewmodels.AttendanceViewModel
import com.classpilot.offline.viewmodels.TimetableViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTodayScreen(
    viewModel: TimetableViewModel = hiltViewModel(),
    attendanceViewModel: AttendanceViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val now = LocalTime.now()
    val nowMin = now.hour * 60 + now.minute

    val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today's Classes", fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.todayPeriods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "🎉",
                    title = "No classes today!",
                    subtitle = "Relax — you have a free day today."
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "${state.todayPeriods.size} classes scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(state.todayPeriods) { period ->
                val status = when {
                    period.endMinuteOfDay < nowMin -> AttendanceStatus.COMPLETED
                    period.startMinuteOfDay <= nowMin -> AttendanceStatus.UPCOMING // in progress
                    else -> AttendanceStatus.UPCOMING
                }
                val displayPeriod = period.copy(attendanceStatus = status)

                ClassPeriodCard(
                    period = displayPeriod,
                    onMarkPresent = {
                        attendanceViewModel.markAttendance(
                            subjectId = period.subject.id,
                            date = today,
                            status = AttendanceStatus.PRESENT,
                            startMinute = period.startMinuteOfDay,
                            endMinute = period.endMinuteOfDay,
                            timetableEntryId = period.id
                        )
                    },
                    onMarkAbsent = {
                        attendanceViewModel.markAttendance(
                            subjectId = period.subject.id,
                            date = today,
                            status = AttendanceStatus.ABSENT,
                            startMinute = period.startMinuteOfDay,
                            endMinute = period.endMinuteOfDay,
                            timetableEntryId = period.id
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableTomorrowScreen(
    viewModel: TimetableViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tomorrow = LocalDate.now().plusDays(1)
    val dateStr = tomorrow.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tomorrow's Classes", fontWeight = FontWeight.Bold)
                        Text(dateStr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.tomorrowPeriods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "📭",
                    title = "No classes tomorrow",
                    subtitle = "Nothing scheduled for ${tomorrow.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}."
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "${state.tomorrowPeriods.size} classes tomorrow",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val first = state.tomorrowPeriods.firstOrNull()
                if (first != null) {
                    val fmt = DateTimeFormatter.ofPattern("hh:mm a")
                    Text(
                        text = "First class: ${first.subject.name} at ${first.startTime.format(fmt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.tomorrowPeriods) { period ->
                ClassPeriodCard(period = period)
            }
        }
    }
}
