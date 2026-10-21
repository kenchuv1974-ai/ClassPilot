package com.classpilot.offline.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.data.database.entities.AttendanceRecordEntity
import com.classpilot.offline.domain.model.AttendanceStatus
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.ui.components.AttendanceCircle
import com.classpilot.offline.ui.theme.*
import com.classpilot.offline.viewmodels.AttendanceViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDetailScreen(
    subjectId: Long,
    viewModel: AttendanceViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenCalculator: () -> Unit
) {
    val history by viewModel.getAttendanceHistory(subjectId).collectAsStateWithLifecycle(emptyList())
    var subject by remember { mutableStateOf<Subject?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(subjectId) {
        subject = viewModel.getSubjectById(subjectId)
    }

    val sub = subject ?: return

    var showMarkDialog by remember { mutableStateOf(false) }

    if (showMarkDialog) {
        var dateStr by remember { mutableStateOf(LocalDate.now().toString()) }
        var selectedStatus by remember { mutableStateOf(AttendanceStatus.PRESENT) }
        AlertDialog(
            onDismissRequest = { showMarkDialog = false },
            title = { Text("Mark Attendance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AttendanceStatus.values().filter { it != AttendanceStatus.COMPLETED && it != AttendanceStatus.UPCOMING }.forEach { status ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status }
                            )
                            Text(status.display)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: LocalDate.now()
                    viewModel.markAttendance(subjectId, date, selectedStatus)
                    showMarkDialog = false
                }) { Text("Mark") }
            },
            dismissButton = {
                TextButton(onClick = { showMarkDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sub.name, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = onOpenCalculator) {
                        Icon(Icons.Filled.Calculate, "Calculator")
                    }
                    IconButton(onClick = { showMarkDialog = true }) {
                        Icon(Icons.Filled.Add, "Mark attendance")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats card
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (sub.code.isNotBlank()) Text(sub.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            StatRow("Conducted", "${sub.conductedClasses}")
                            StatRow("Present", "${sub.presentClasses}", Green400)
                            StatRow("Absent", "${sub.absentClasses}", Red400)
                            StatRow("Target", "${sub.attendanceThreshold.toInt()}%")
                            if (sub.conductedClasses > 0) {
                                if (sub.isBelowThreshold) {
                                    StatRow("Need to attend", "${sub.classesNeededToReachThreshold} more", Amber400)
                                } else {
                                    StatRow("Can miss", "${sub.classesCanMiss} more", Green400)
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        AttendanceCircle(
                            percentage = sub.attendancePercentage,
                            threshold = sub.attendanceThreshold,
                            size = 100
                        )
                    }
                }
            }

            item {
                Text("Attendance History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        "No records yet. Mark attendance using the + button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                items(history) { record ->
                    AttendanceRecordItem(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record) },
                        onChangeStatus = { newStatus -> viewModel.updateRecord(record, newStatus) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun AttendanceRecordItem(
    record: AttendanceRecordEntity,
    onDelete: () -> Unit,
    onChangeStatus: (String) -> Unit
) {
    val date = LocalDate.ofEpochDay(record.dateEpochDay)
    val fmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
    val statusColor = when (record.status) {
        "PRESENT" -> Green400
        "ABSENT" -> Red400
        "CANCELLED" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Amber400
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date.format(fmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (record.notes.isNotBlank()) {
                    Text(record.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = record.status.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Mark Present") }, onClick = { onChangeStatus("PRESENT"); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Mark Absent") }, onClick = { onChangeStatus("ABSENT"); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Mark Cancelled") }, onClick = { onChangeStatus("CANCELLED"); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); menuExpanded = false }
                    )
                }
            }
        }
    }
}
