package com.classpilot.offline.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.ui.components.*
import com.classpilot.offline.ui.theme.*
import com.classpilot.offline.viewmodels.DashboardViewModel
import com.classpilot.offline.viewmodels.SubjectViewModel
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    subjectViewModel: SubjectViewModel = hiltViewModel(),
    onNavigateToTimetableToday: () -> Unit,
    onNavigateToTimetableTomorrow: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSubjectDetail: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh time every minute
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshTime()
            delay(60_000L)
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Indigo400)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Hero header ───────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Surface700, MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column {
                    val dayName = state.today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val dateStr = state.today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    Text(
                        text = "$dayName • $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    val name = state.studentName.ifBlank { "" }
                    Text(
                        text = "${state.greeting}${if (name.isNotBlank()) ", $name" else ""} 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    // Stats row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatChip(
                            label = "Today",
                            value = "${state.todayPeriods.size}",
                            unit = "classes",
                            color = Indigo400
                        )
                        StatChip(
                            label = "Subjects",
                            value = "${state.subjects.size}",
                            unit = "active",
                            color = Cyan400
                        )
                        if (state.lowAttendanceSubjects.isNotEmpty()) {
                            StatChip(
                                label = "Warning",
                                value = "${state.lowAttendanceSubjects.size}",
                                unit = "low",
                                color = Amber400
                            )
                        }
                    }
                }
            }
        }

        // ── Next class card ───────────────────────────────────────────────────
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                if (state.nextClass != null) {
                    val period = state.nextClass!!
                    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Indigo400.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                text = "⏰ NEXT CLASS",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoLight,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = period.subject.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${period.startTime.format(timeFormatter)} – ${period.endTime.format(timeFormatter)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (period.room.isNotBlank()) {
                                Text(
                                    text = "📍 ${period.room}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.minutesToNextClass >= 0) {
                                Spacer(Modifier.height(8.dp))
                                val mins = state.minutesToNextClass
                                val countdownText = when {
                                    mins == 0L -> "Starting now!"
                                    mins < 60 -> "in ${mins}m"
                                    else -> "in ${mins / 60}h ${mins % 60}m"
                                }
                                Text(
                                    text = "⏱️ $countdownText",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Indigo400,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                } else if (state.todayPeriods.isEmpty()) {
                    EmptyState(
                        emoji = "🎉",
                        title = "No classes today!",
                        subtitle = "Enjoy your free day",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }

        // ── Quick action buttons ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToTimetableToday,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Today, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Today's Schedule")
                }
                FilledTonalButton(
                    onClick = onNavigateToTimetableTomorrow,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.NavigateNext, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tomorrow")
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Low attendance warnings ───────────────────────────────────────────
        if (state.lowAttendanceSubjects.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "⚠️ Attendance Warnings",
                        action = {
                            TextButton(onClick = onNavigateToAttendance) {
                                Text("View All", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }
            }
            items(state.lowAttendanceSubjects) { subject ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SubjectAttendanceCard(
                        subject = subject,
                        onClick = { onNavigateToSubjectDetail(subject.id) },
                        onQuickPresent = { subjectViewModel.quickMarkSubject(subject.id, true) },
                        onQuickAbsent = { subjectViewModel.quickMarkSubject(subject.id, false) }
                    )
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Today's classes section ───────────────────────────────────────────
        if (state.todayPeriods.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "📅 Today's Classes",
                        action = {
                            TextButton(onClick = onNavigateToTimetableToday) {
                                Text("See All", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }
            }
            items(state.todayPeriods.take(3)) { period ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    ClassPeriodCard(period = period)
                }
            }
            if (state.todayPeriods.size > 3) {
                item {
                    TextButton(
                        onClick = onNavigateToTimetableToday,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text("+ ${state.todayPeriods.size - 3} more classes")
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Attendance summary ────────────────────────────────────────────────
        if (state.subjects.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SectionHeader(
                        title = "📊 Quick Attendance Marking",
                        action = {
                            TextButton(onClick = onNavigateToAttendance) {
                                Text("Details", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }
            }
            items(state.subjects) { subject ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    SubjectAttendanceCard(
                        subject = subject,
                        onClick = { onNavigateToSubjectDetail(subject.id) },
                        onQuickPresent = { subjectViewModel.quickMarkSubject(subject.id, true) },
                        onQuickAbsent = { subjectViewModel.quickMarkSubject(subject.id, false) }
                    )
                }
            }
        }

        // ── Watermark ─────────────────────────────────────────────────────────
        item {
            PoweredByWatermark()
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, unit: String, color: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "$label $unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
