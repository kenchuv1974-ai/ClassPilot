package com.classpilot.offline.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classpilot.offline.data.database.dao.CalendarEventDao
import com.classpilot.offline.data.database.entities.CalendarEventEntity
import com.classpilot.offline.ui.theme.Indigo400
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarEventDao: CalendarEventDao
) : ViewModel() {

    val allEvents = calendarEventDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEvent(title: String, dateEpochDay: Long, type: String) {
        viewModelScope.launch {
            calendarEventDao.insertEvent(
                CalendarEventEntity(title = title, dateEpochDay = dateEpochDay, eventType = type)
            )
        }
    }

    fun deleteEvent(event: CalendarEventEntity) {
        viewModelScope.launch { calendarEventDao.deleteEvent(event) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val eventsByDate = remember(events) {
        events.groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
    }

    val selectedDateEvents = remember(selectedDate, events) {
        events.filter { it.dateEpochDay == selectedDate.toEpochDay() }
    }

    if (showAddDialog) {
        var eventTitle by remember { mutableStateOf("") }
        var eventType by remember { mutableStateOf("PERSONAL") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Event on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
                    listOf("HOLIDAY", "EXAM", "ASSIGNMENT", "TEST", "PERSONAL", "OTHER").forEach { type ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = eventType == type, onClick = { eventType = type })
                            Text(type.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (eventTitle.isNotBlank()) {
                        viewModel.addEvent(eventTitle, selectedDate.toEpochDay(), eventType)
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Add event")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                // Month header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, null)
                    }
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }
            item {
                CalendarGrid(
                    yearMonth = currentMonth,
                    selectedDate = selectedDate,
                    eventDates = eventsByDate.keys,
                    onDateClick = { selectedDate = it }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (selectedDateEvents.isEmpty()) {
                item {
                    Text(
                        "No events on this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(selectedDateEvents) { event ->
                    EventItem(event = event, onDelete = { viewModel.deleteEvent(event) })
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                com.classpilot.offline.ui.components.PoweredByWatermark()
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    eventDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = (firstDay.dayOfWeek.value % 7) // 0=Sun, 1=Mon...
    val today = LocalDate.now()

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Su","Mo","Tu","We","Th","Fr","Sa").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // Days grid
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - startOffset + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayNum)
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val hasEvent = date in eventDates
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Indigo400
                                        isToday -> Indigo400.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> Indigo400
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasEvent) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Indigo400)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEventEntity, onDelete: () -> Unit) {
    val typeColor = when (event.eventType) {
        "HOLIDAY" -> Color(0xFF00E676)
        "EXAM" -> Color(0xFFFF1744)
        "ASSIGNMENT" -> Color(0xFFFFAB00)
        "TEST" -> Color(0xFFFF6D00)
        else -> Indigo400
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(typeColor))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(event.eventType.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall, color = typeColor)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
