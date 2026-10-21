package com.classpilot.offline.ui.screens.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import com.classpilot.offline.domain.model.ClassType
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.domain.model.SubjectType
import com.classpilot.offline.ui.components.PoweredByWatermark
import com.classpilot.offline.ui.theme.Green400
import com.classpilot.offline.ui.theme.Indigo400
import com.classpilot.offline.ui.theme.IndigoLight
import com.classpilot.offline.ui.theme.Red400
import com.classpilot.offline.viewmodels.SubjectViewModel
import com.classpilot.offline.viewmodels.TimetableViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClassScreen(
    entryId: Long,
    initialDayOfWeek: Int,
    viewModel: TimetableViewModel = hiltViewModel(),
    subjectViewModel: SubjectViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val subjects by subjectViewModel.subjects.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isEdit = entryId > 0L

    var existingEntry by remember { mutableStateOf<TimetableEntryEntity?>(null) }

    LaunchedEffect(entryId) {
        if (isEdit) existingEntry = viewModel.getEntryById(entryId)
    }

    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedDay by remember { mutableIntStateOf(initialDayOfWeek.coerceIn(1, 7)) }
    var startHour by remember { mutableIntStateOf(9) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(10) }
    var endMin by remember { mutableIntStateOf(0) }
    var room by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ClassType.THEORY) }
    var reminder15 by remember { mutableStateOf(true) }
    var reminder5 by remember { mutableStateOf(true) }
    var reminderAtStart by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var allowOverlap by remember { mutableStateOf(false) }
    var showNewSubjectDialog by remember { mutableStateOf(false) }

    // Auto-select first subject if only 1 exists
    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty() && !isEdit) {
            selectedSubject = subjects.first()
        }
    }

    // Populate fields when editing
    LaunchedEffect(existingEntry, subjects) {
        existingEntry?.let { e ->
            selectedSubject = subjects.firstOrNull { it.id == e.subjectId }
            selectedDay = e.dayOfWeek
            startHour = e.startMinute / 60
            startMin = e.startMinute % 60
            endHour = e.endMinute / 60
            endMin = e.endMinute % 60
            room = e.room
            faculty = e.faculty
            selectedType = ClassType.values().firstOrNull { it.name == e.classType } ?: ClassType.THEORY
            val offsets = e.reminderMinutes.split(",").mapNotNull { it.trim().toIntOrNull() }
            reminder5 = 5 in offsets
            reminder15 = 15 in offsets
            reminderAtStart = 0 in offsets
        }
    }

    fun buildReminderStr(): String = buildList {
        if (reminderAtStart) add("0")
        if (reminder5) add("5")
        if (reminder15) add("15")
    }.joinToString(",").ifEmpty { "15" }

    val onSaveAction = {
        val sub = selectedSubject
        if (sub == null) {
            errorMsg = "Please select or add a subject first"
        } else {
            val startMinTotal = startHour * 60 + startMin
            val endMinTotal = endHour * 60 + endMin
            if (endMinTotal <= startMinTotal) {
                errorMsg = "End time must be after start time"
            } else {
                errorMsg = null
                scope.launch {
                    val entry = TimetableEntryEntity(
                        id = if (isEdit) entryId else 0L,
                        subjectId = sub.id,
                        dayOfWeek = selectedDay,
                        startMinute = startMinTotal,
                        endMinute = endMinTotal,
                        room = room.ifBlank { sub.classroom },
                        faculty = faculty.ifBlank { sub.facultyName },
                        classType = selectedType.name,
                        reminderMinutes = buildReminderStr()
                    )
                    val result = viewModel.addOrUpdateEntry(entry, allowOverlap = allowOverlap)
                    if (result.isSuccess) {
                        onBack()
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Unable to save class"
                        errorMsg = msg
                    }
                }
            }
        }
    }

    if (showNewSubjectDialog) {
        QuickAddSubjectDialog(
            onDismiss = { showNewSubjectDialog = false },
            onCreated = { newName, newCode, newType, newColor, att, cond ->
                subjectViewModel.addSubject(
                    name = newName,
                    code = newCode,
                    type = newType,
                    colorHex = newColor,
                    initialAttended = att,
                    initialConducted = cond
                )
                showNewSubjectDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Class" else "Add Scheduled Class", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { onSaveAction() }) {
                        Icon(Icons.Filled.Check, "Save")
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Error or overlap alert
            errorMsg?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ $msg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (msg.contains("overlap", ignoreCase = true)) {
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = allowOverlap,
                                    onCheckedChange = { allowOverlap = it }
                                )
                                Text(
                                    "Save anyway (allow overlap)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 1: Subject Selection ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Subject *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showNewSubjectDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("+ New Subject", fontWeight = FontWeight.Bold)
                }
            }

            if (subjects.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Indigo400.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📚 No Subjects Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a subject first to attach this schedule to.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showNewSubjectDialog = true }) {
                            Text("Create Subject Now")
                        }
                    }
                }
            } else {
                // Horizontal scrolling subject chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.forEach { sub ->
                        val isSelected = selectedSubject?.id == sub.id
                        val subColor = try {
                            Color(android.graphics.Color.parseColor(sub.colorHex))
                        } catch (e: Exception) {
                            Indigo400
                        }
                        Surface(
                            onClick = { selectedSubject = sub; errorMsg = null },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) subColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(2.dp, subColor) else null,
                            tonalElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(subColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) subColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (sub.code.isNotBlank()) {
                                        Text(
                                            text = sub.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 2: Day of Week ───────────────────────────────────────
            Text(
                text = "Day of Week",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..7).forEach { dayNum ->
                    val isDaySelected = selectedDay == dayNum
                    val dayNameShort = DayOfWeek.of(dayNum).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    FilterChip(
                        selected = isDaySelected,
                        onClick = { selectedDay = dayNum },
                        label = {
                            Text(
                                text = dayNameShort,
                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ── Section 3: Class Time ─────────────────────────────────────────
            Text(
                text = "Class Timing",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Preset popular time slots
            Text(
                text = "Quick Presets:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair(Pair(9, 0), Pair(10, 0)),
                    Pair(Pair(10, 0), Pair(11, 0)),
                    Pair(Pair(11, 15), Pair(12, 15)),
                    Pair(Pair(13, 0), Pair(14, 0)),
                    Pair(Pair(14, 0), Pair(15, 0)),
                    Pair(Pair(15, 0), Pair(16, 0))
                ).forEach { slot ->
                    val label = "%02d:%02d - %02d:%02d".format(
                        slot.first.first, slot.first.second,
                        slot.second.first, slot.second.second
                    )
                    val isSlotActive = startHour == slot.first.first &&
                            startMin == slot.first.second &&
                            endHour == slot.second.first &&
                            endMin == slot.second.second

                    SuggestionChip(
                        onClick = {
                            startHour = slot.first.first
                            startMin = slot.first.second
                            endHour = slot.second.first
                            endMin = slot.second.second
                        },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSlotActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        border = if (isSlotActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    )
                }
            }

            // Custom Time Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeStepperCard(
                    title = "Start Time",
                    hour = startHour,
                    minute = startMin,
                    onHourChange = { startHour = it },
                    onMinChange = { startMin = it },
                    modifier = Modifier.weight(1f)
                )
                TimeStepperCard(
                    title = "End Time",
                    hour = endHour,
                    minute = endMin,
                    onHourChange = { endHour = it },
                    onMinChange = { endMin = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Section 4: Details & Reminders ───────────────────────────────
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room / Lab No. (optional)") },
                placeholder = { Text(selectedSubject?.classroom?.ifBlank { "e.g. 301" } ?: "e.g. 301") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = faculty,
                onValueChange = { faculty = it },
                label = { Text("Faculty / Lecturer (optional)") },
                placeholder = { Text(selectedSubject?.facultyName?.ifBlank { "e.g. Dr. Sharma" } ?: "e.g. Dr. Sharma") },
                modifier = Modifier.fillMaxWidth()
            )

            // Class Type Chips
            Text(
                text = "Class Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClassType.values().forEach { type ->
                    val isTypeSelected = selectedType == type
                    FilterChip(
                        selected = isTypeSelected,
                        onClick = { selectedType = type },
                        label = { Text(type.display) }
                    )
                }
            }

            // Reminder options
            Text(
                text = "Reminders Before Class",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = reminder15,
                    onClick = { reminder15 = !reminder15 },
                    label = { Text("15 min before") }
                )
                FilterChip(
                    selected = reminder5,
                    onClick = { reminder5 = !reminder5 },
                    label = { Text("5 min before") }
                )
                FilterChip(
                    selected = reminderAtStart,
                    onClick = { reminderAtStart = !reminderAtStart },
                    label = { Text("At start") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Prominent Save Button ────────────────────────────────────────
            Button(
                onClick = { onSaveAction() },
                enabled = selectedSubject != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isEdit) "Update Scheduled Class" else "Save Class to Schedule",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Watermark
            PoweredByWatermark()

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Time Stepper Component ───────────────────────────────────────────────────

@Composable
private fun TimeStepperCard(
    title: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            // Display time formatted as hh:mm AM/PM
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            Text(
                text = "%02d:%02d %s".format(displayHour, minute, amPm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Hour and Minute adjusters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Hour adjuster
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hour", style = MaterialTheme.typography.labelSmall)
                    Row {
                        IconButton(
                            onClick = { onHourChange((hour - 1 + 24) % 24) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { onHourChange((hour + 1) % 24) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                // Minute adjuster
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Min", style = MaterialTheme.typography.labelSmall)
                    Row {
                        IconButton(
                            onClick = { onMinChange((minute - 5 + 60) % 60) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { onMinChange((minute + 5) % 60) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Quick Add Subject Dialog ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSubjectDialog(
    onDismiss: () -> Unit,
    onCreated: (String, String, SubjectType, String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(SubjectType.THEORY) }
    var attendedText by remember { mutableStateOf("0") }
    var conductedText by remember { mutableStateOf("0") }
    val presetColors = listOf("#7C4DFF", "#FF6B6B", "#00E676", "#00E5FF", "#FFD600", "#FF6D00")
    var selectedColor by remember { mutableStateOf("#7C4DFF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Subject", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Mathematics, OS") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attendedText,
                        onValueChange = { attendedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Attended") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = conductedText,
                        onValueChange = { conductedText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Total Held") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetColors.forEach { hex ->
                        val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        Surface(
                            onClick = { selectedColor = hex },
                            shape = CircleShape,
                            color = c,
                            modifier = Modifier.size(28.dp),
                            border = if (selectedColor == hex) BorderStroke(2.dp, Color.White) else null
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val att = attendedText.toIntOrNull() ?: 0
                        val cond = conductedText.toIntOrNull() ?: 0
                        onCreated(name, code, selectedType, selectedColor, att, cond)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
