package com.classpilot.offline.ui.screens.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.data.preferences.AppPreferences
import com.classpilot.offline.data.preferences.PreferencesManager
import com.classpilot.offline.notifications.NotificationScheduler
import com.classpilot.offline.scheduling.ReminderRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefsManager: PreferencesManager,
    private val scheduler: NotificationScheduler,
    private val rescheduler: ReminderRescheduler
) : ViewModel() {

    val prefs = prefsManager.appPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences())

    fun updateTheme(mode: String) = viewModelScope.launch { prefsManager.updateThemeMode(mode) }
    fun updateName(name: String) = viewModelScope.launch { prefsManager.updateStudentName(name) }
    fun updateThreshold(t: Float) = viewModelScope.launch { prefsManager.updateDefaultThreshold(t) }

    fun updateNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsManager.updateNotificationsEnabled(enabled)
        if (!enabled) rescheduler.cancelAll() else rescheduler.rescheduleAll()
    }

    fun updateMorningNotification(enabled: Boolean, hour: Int, minute: Int) = viewModelScope.launch {
        prefsManager.updateMorningNotification(enabled, hour, minute)
        if (enabled) scheduler.scheduleDailyMorningNotification(hour, minute)
        else scheduler.cancelDailyMorningNotification()
    }

    fun updateEveningNotification(enabled: Boolean, hour: Int, minute: Int) = viewModelScope.launch {
        prefsManager.updateEveningNotification(enabled, hour, minute)
        if (enabled) scheduler.scheduleEveningPreviewNotification(hour, minute)
        else scheduler.cancelEveningPreviewNotification()
    }

    fun canScheduleExact() = scheduler.canScheduleExactAlarms()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBackup: () -> Unit,
    onNavigateNotificationDiagnostic: () -> Unit
) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    var nameText by remember(prefs.studentName) { mutableStateOf(prefs.studentName) }
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Profile
            SettingsSection("Profile") {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.updateName(nameText) }) {
                            Icon(Icons.Filled.Check, "Save")
                        }
                    }
                )
            }

            // Appearance
            SettingsSection("Appearance") {
                ExposedDropdownMenuBox(expanded = themeMenuExpanded, onExpandedChange = { themeMenuExpanded = it }) {
                    OutlinedTextField(
                        value = prefs.themeMode.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(themeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = themeMenuExpanded, onDismissRequest = { themeMenuExpanded = false }) {
                        listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { viewModel.updateTheme(mode); themeMenuExpanded = false }
                            )
                        }
                    }
                }
            }

            // Attendance
            SettingsSection("Attendance") {
                Text("Default Threshold: ${prefs.defaultAttendanceThreshold.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = prefs.defaultAttendanceThreshold,
                    onValueChange = { viewModel.updateThreshold(it) },
                    valueRange = 50f..100f, steps = 49, modifier = Modifier.fillMaxWidth()
                )
            }

            // Notifications
            SettingsSection("Notifications") {
                SettingsToggleRow(
                    title = "Enable Notifications",
                    subtitle = "Global notification switch",
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                )
                if (prefs.notificationsEnabled) {
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Morning Timetable",
                        subtitle = "Daily class summary at ${"%02d:%02d".format(prefs.morningNotificationHour, prefs.morningNotificationMinute)}",
                        checked = prefs.morningNotificationEnabled,
                        onCheckedChange = { viewModel.updateMorningNotification(it, prefs.morningNotificationHour, prefs.morningNotificationMinute) }
                    )
                    HorizontalDivider()
                    SettingsToggleRow(
                        title = "Evening Preview",
                        subtitle = "Tomorrow's classes at ${"%02d:%02d".format(prefs.eveningNotificationHour, prefs.eveningNotificationMinute)}",
                        checked = prefs.eveningNotificationEnabled,
                        onCheckedChange = { viewModel.updateEveningNotification(it, prefs.eveningNotificationHour, prefs.eveningNotificationMinute) }
                    )
                    HorizontalDivider()
                    SettingsActionRow(
                        title = "Notification Diagnostic",
                        subtitle = "Check permission and alarm status",
                        onClick = onNavigateNotificationDiagnostic
                    )
                }
            }

            // Data
            SettingsSection("Data Management") {
                SettingsActionRow(
                    title = "Backup & Restore",
                    subtitle = "Export or import your data offline",
                    onClick = onNavigateBackup
                )
            }

            // About & Watermark
            SettingsSection("About ClassPilot") {
                Text(
                    text = "ClassPilot — Offline Attendance & Smart Schedule Manager",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Version 1.0.0 • Completely Offline • Privacy First",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                com.classpilot.offline.ui.components.PoweredByWatermark()
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClick) { Icon(Icons.Filled.ChevronRight, null) }
    }
}

// ── Notification Diagnostic ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDiagnosticScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val notificationsEnabled = nm.areNotificationsEnabled()
    val exactAlarmsEnabled = viewModel.canScheduleExact()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Diagnostic", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DiagnosticRow(
                title = "Notifications",
                status = if (notificationsEnabled) "✅ Enabled" else "❌ Disabled",
                hint = if (!notificationsEnabled) "Tap to open notification settings" else null,
                onClick = if (!notificationsEnabled) {{
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }} else null
            )
            DiagnosticRow(
                title = "Exact Alarms",
                status = if (exactAlarmsEnabled) "✅ Allowed" else "⚠️ Not allowed (fallback to inexact)",
                hint = if (!exactAlarmsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    "Tap to grant SCHEDULE_EXACT_ALARM permission" else null,
                onClick = if (!exactAlarmsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {{
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }} else null
            )
            HorizontalDivider()
            Text(
                "Platform notes:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "• Android 12+ requires SCHEDULE_EXACT_ALARM permission for precise alarms.\n" +
                "• MIUI, ColorOS, and some OEM Android forks may restrict background alarms even with permissions granted. Whitelist ClassPilot in your battery or startup manager settings.\n" +
                "• Doze mode may delay reminders by several minutes when the phone has been still and unplugged. This is an Android platform limitation and cannot be fully bypassed.\n" +
                "• All reminders are rescheduled automatically after a reboot.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticRow(title: String, status: String, hint: String?, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.bodyMedium)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
