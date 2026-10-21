package com.classpilot.offline.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.ui.components.EmptyState
import com.classpilot.offline.ui.components.SubjectAttendanceCard
import com.classpilot.offline.viewmodels.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel = hiltViewModel(),
    subjectViewModel: com.classpilot.offline.viewmodels.SubjectViewModel = hiltViewModel(),
    onSubjectClick: (Long) -> Unit,
    onAddSubject: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Attendance Manager", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSubject,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Subject") }
            )
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "📚",
                    title = "No subjects yet",
                    subtitle = "Add your subjects with previous attended counts to start tracking.",
                    action = {
                        Button(onClick = onAddSubject) { Text("Add Subject") }
                    }
                )
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Summary row
                val totalConducted = subjects.sumOf { it.conductedClasses }
                val totalPresent = subjects.sumOf { it.presentClasses }
                val overallPct = if (totalConducted == 0) 0f
                    else totalPresent * 100f / totalConducted
                val avgThreshold = if (subjects.isNotEmpty())
                    subjects.map { it.attendanceThreshold }.average().toFloat()
                else 75f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Overall College Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("$totalPresent of $totalConducted total classes attended", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "%.1f%%".format(overallPct),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (overallPct >= avgThreshold) com.classpilot.offline.ui.theme.Green400
                                        else com.classpilot.offline.ui.theme.Red400
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        // BunkWell summary advice pill
                        val totalCanMiss = subjects.sumOf { it.classesCanMiss }
                        val totalNeeded = subjects.sumOf { it.classesNeededToReachThreshold }
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            color = if (overallPct >= avgThreshold)
                                com.classpilot.offline.ui.theme.Green400.copy(alpha = 0.15f)
                            else
                                com.classpilot.offline.ui.theme.Amber400.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (overallPct >= avgThreshold)
                                        "🎉 Total Safe Bunks: $totalCanMiss across subjects"
                                    else
                                        "⚠️ Catch-up: Attend $totalNeeded more classes total",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallPct >= avgThreshold)
                                        com.classpilot.offline.ui.theme.Green400
                                    else
                                        com.classpilot.offline.ui.theme.Amber400
                                )
                            }
                        }
                    }
                }
            }

            items(subjects) { subject ->
                SubjectAttendanceCard(
                    subject = subject,
                    onClick = { onSubjectClick(subject.id) },
                    onQuickPresent = { subjectViewModel.quickMarkSubject(subject.id, true) },
                    onQuickAbsent = { subjectViewModel.quickMarkSubject(subject.id, false) }
                )
            }

            // Watermark
            item {
                com.classpilot.offline.ui.components.PoweredByWatermark()
            }
        }
    }
}

