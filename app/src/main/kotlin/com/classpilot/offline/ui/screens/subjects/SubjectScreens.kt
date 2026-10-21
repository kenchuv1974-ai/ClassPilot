package com.classpilot.offline.ui.screens.subjects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.domain.model.SubjectType
import com.classpilot.offline.ui.components.EmptyState
import com.classpilot.offline.viewmodels.SubjectViewModel
import kotlinx.coroutines.launch

// ── Subjects List ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectViewModel = hiltViewModel(),
    onAddSubject: () -> Unit,
    onEditSubject: (Long) -> Unit
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Subjects", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSubject,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Subject") }
            )
        }
    ) { padding ->
        if (subjects.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("📚", "No subjects", "Add your first subject to get started.", action = {
                    Button(onClick = onAddSubject) { Text("Add Subject") }
                })
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
                start = 16.dp, end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subjects) { subject ->
                SubjectListItem(
                    subject = subject,
                    onEdit = { onEditSubject(subject.id) },
                    onDelete = { viewModel.deleteSubject(subject.id) }
                )
            }
        }
    }
}

@Composable
private fun SubjectListItem(subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val subColor = runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }
        .getOrDefault(Color(0xFF7C4DFF))

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${subject.name}?") },
            text = { Text("This will remove the subject and all its attendance records permanently.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = subColor.copy(0.2f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(subject.name.take(2).uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = subColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subject.code.isNotBlank()) Text(subject.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${subject.type.display} • ${subject.attendanceThreshold.toInt()}% target",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit") }
            IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

// ── Add / Edit Subject ────────────────────────────────────────────────────────

val PRESET_COLORS = listOf(
    "#7C4DFF", "#FF6B6B", "#00E676", "#00E5FF", "#FF4081",
    "#FFD600", "#FF6D00", "#AA00FF", "#00BFA5", "#2979FF"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubjectScreen(
    subjectId: Long,
    viewModel: SubjectViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val isEdit = subjectId > 0L
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SubjectType.THEORY) }
    var threshold by remember { mutableFloatStateOf(75f) }
    var selectedColor by remember { mutableStateOf("#7C4DFF") }
    var notes by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    // Initial attendance state (BunkWell feature)
    var attendedText by remember { mutableStateOf("0") }
    var conductedText by remember { mutableStateOf("0") }

    LaunchedEffect(subjectId) {
        if (isEdit) {
            viewModel.getSubjectById(subjectId)?.let { s ->
                name = s.name; code = s.code; faculty = s.facultyName
                classroom = s.classroom; selectedType = s.type
                threshold = s.attendanceThreshold; selectedColor = s.colorHex; notes = s.notes
                attendedText = s.presentClasses.toString()
                conductedText = s.conductedClasses.toString()
            }
        }
    }

    val attendedCount = attendedText.toIntOrNull() ?: 0
    val conductedCount = conductedText.toIntOrNull() ?: 0
    val currentPct = if (conductedCount > 0) (attendedCount * 100f / conductedCount).coerceIn(0f, 100f) else 0f
    val canBunk = if (conductedCount > 0) {
        val maxTotal = (attendedCount * 100.0 / threshold).toInt()
        maxOf(0, maxTotal - conductedCount)
    } else 0
    val needToAttend = if (conductedCount > 0 && currentPct < threshold) {
        val t = threshold / 100.0
        val denom = 1.0 - t
        if (denom <= 0) 0 else Math.ceil((t * conductedCount - attendedCount) / denom).toInt().coerceAtLeast(0)
    } else 0

    val onSaveAction: () -> Unit = {
        if (name.isNotBlank()) {
            val safeAttended = attendedCount.coerceAtLeast(0)
            val safeConducted = maxOf(safeAttended, conductedCount)
            if (isEdit) {
                scope.launch {
                    viewModel.getSubjectById(subjectId)?.let { existing ->
                        viewModel.updateSubject(
                            existing.copy(
                                name = name, code = code, facultyName = faculty,
                                classroom = classroom, type = selectedType,
                                attendanceThreshold = threshold, colorHex = selectedColor, notes = notes
                            ),
                            attended = safeAttended,
                            conducted = safeConducted
                        )
                    }
                    onBack()
                }
            } else {
                viewModel.addSubject(
                    name = name,
                    code = code,
                    faculty = faculty,
                    classroom = classroom,
                    type = selectedType,
                    threshold = threshold,
                    colorHex = selectedColor,
                    notes = notes,
                    initialAttended = safeAttended,
                    initialConducted = safeConducted
                )
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Subject" else "Add Subject", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onSaveAction) { Icon(Icons.Filled.Check, "Save") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Subject Name *") },
                placeholder = { Text("e.g. Data Structures, Physics") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Subject Code (optional)") },
                placeholder = { Text("e.g. CS201, PHY101") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = faculty,
                onValueChange = { faculty = it },
                label = { Text("Faculty Name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = classroom,
                onValueChange = { classroom = it },
                label = { Text("Default Classroom (optional)") },
                placeholder = { Text("e.g. Room 304, Lab 2") },
                modifier = Modifier.fillMaxWidth()
            )

            // ── BunkWell Feature: Initial Attendance Setup ───────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📊 Initial Attendance Setup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Already started the semester? Enter what you've attended so far.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = attendedText,
                            onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Attended *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        OutlinedTextField(
                            value = conductedText,
                            onValueChange = { conductedText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Total Held *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }

                    if (conductedCount > 0) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            color = if (currentPct >= threshold)
                                com.classpilot.offline.ui.theme.Green400.copy(alpha = 0.15f)
                            else
                                com.classpilot.offline.ui.theme.Red400.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current: ${"%.1f".format(currentPct)}%",
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentPct >= threshold)
                                        com.classpilot.offline.ui.theme.Green400
                                    else
                                        com.classpilot.offline.ui.theme.Red400
                                )
                                Text(
                                    text = if (currentPct >= threshold)
                                        "🎉 Can bunk $canBunk classes"
                                    else
                                        "⚠️ Need $needToAttend to reach ${threshold.toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentPct >= threshold)
                                        com.classpilot.offline.ui.theme.Green400
                                    else
                                        com.classpilot.offline.ui.theme.Red400
                                )
                            }
                        }
                    }
                }
            }

            // Type picker
            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedType.display, onValueChange = {}, readOnly = true,
                    label = { Text("Subject Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    SubjectType.values().forEach { type ->
                        DropdownMenuItem(text = { Text(type.display) }, onClick = { selectedType = type; typeMenuExpanded = false })
                    }
                }
            }

            // Threshold
            Text("Attendance Target: ${threshold.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 50f..100f, steps = 49, modifier = Modifier.fillMaxWidth())

            // Color picker
            Text("Subject Color Badge", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_COLORS.forEach { hex ->
                    val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = c,
                        modifier = Modifier.size(36.dp),
                        onClick = { selectedColor = hex },
                        tonalElevation = if (selectedColor == hex) 8.dp else 0.dp,
                        border = if (selectedColor == hex) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null
                    ) {}
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Prominent Save Button
            Button(
                onClick = onSaveAction,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Text(if (isEdit) "Update Subject" else "Save Subject", fontWeight = FontWeight.Bold)
            }

            // Watermark
            com.classpilot.offline.ui.components.PoweredByWatermark()

            Spacer(Modifier.height(32.dp))
        }
    }
}

