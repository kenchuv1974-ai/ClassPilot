package com.classpilot.offline.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classpilot.offline.backup.BackupManager
import com.classpilot.offline.backup.BackupResult
import com.classpilot.offline.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val prefsManager: PreferencesManager
) : ViewModel() {

    var statusMessage by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    fun createBackup(uri: Uri) = viewModelScope.launch {
        isLoading = true
        val result = backupManager.createBackup(uri)
        statusMessage = when (result) {
            is BackupResult.Success -> { prefsManager.updateLastBackupTimestamp(System.currentTimeMillis()); result.message }
            is BackupResult.Failure -> result.error
        }
        isLoading = false
    }

    fun restoreBackup(uri: Uri) = viewModelScope.launch {
        isLoading = true
        val result = backupManager.restoreBackup(uri)
        statusMessage = when (result) {
            is BackupResult.Success -> result.message
            is BackupResult.Failure -> result.error
        }
        isLoading = false
    }

    fun clearAllData() = viewModelScope.launch {
        isLoading = true
        val result = backupManager.clearAllData()
        statusMessage = when (result) {
            is BackupResult.Success -> result.message
            is BackupResult.Failure -> result.error
        }
        isLoading = false
    }

    fun clearStatus() { statusMessage = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf<Uri?>(null) }

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showRestoreConfirm = it }
    }

    // Dialogs
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("⚠️ Clear All Data?") },
            text = { Text("This will permanently delete ALL subjects, timetable, attendance records, and events. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData(); showClearConfirm = false }) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }

    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("Restore Backup?") },
            text = { Text("This will replace all current data with the contents of the backup file. Do you want to continue?") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreBackup(uri); showRestoreConfirm = null }) {
                    Text("Restore")
                }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = null }) { Text("Cancel") } }
        )
    }

    // Status snackbar
    viewModel.statusMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            viewModel.statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (viewModel.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Backup card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💾 Create Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Export all your subjects, timetable, attendance records, and calendar events to a JSON file. Store it anywhere — your downloads, Google Drive, or USB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { exportLauncher.launch("classpilot_backup_$timestamp.json") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isLoading
                    ) {
                        Text("Export Backup")
                    }
                }
            }

            // Restore card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📂 Restore Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Import a previously created ClassPilot backup file. All current data will be replaced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isLoading
                    ) {
                        Text("Import Backup")
                    }
                }
            }

            // Clear data card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🗑️ Clear All Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(
                        "Permanently delete all data. Create a backup first!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = !viewModel.isLoading
                    ) {
                        Text("Clear All Data")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            com.classpilot.offline.ui.components.PoweredByWatermark()
            Spacer(Modifier.height(24.dp))
        }
    }
}
