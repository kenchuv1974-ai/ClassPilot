package com.classpilot.offline.ui.screens.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.ui.theme.Amber400
import com.classpilot.offline.ui.theme.Green400
import com.classpilot.offline.ui.theme.Red400
import com.classpilot.offline.viewmodels.AttendanceViewModel
import com.classpilot.offline.viewmodels.AttendanceCalculation
import com.classpilot.offline.viewmodels.calculateAttendance
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalculatorScreen(
    subjectId: Long,
    viewModel: AttendanceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var subject by remember { mutableStateOf<Subject?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(subjectId) {
        subject = viewModel.getSubjectById(subjectId)
    }

    val sub = subject ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val calc = remember(sub) { calculateAttendance(sub) }
    var customTarget by remember { mutableFloatStateOf(sub.attendanceThreshold) }

    // Custom target calculation
    val customNeedToAttend = remember(customTarget, sub) {
        if (sub.attendancePercentage >= customTarget) 0
        else {
            val t = customTarget / 100.0
            val denom = 1.0 - t
            if (denom <= 0) Int.MAX_VALUE
            else ceil((t * sub.conductedClasses - sub.presentClasses) / denom).toInt().coerceAtLeast(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculator: ${sub.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
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
            Spacer(Modifier.height(8.dp))

            // Current status card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    CalcRow("Total conducted", "${calc.conducted}")
                    CalcRow("Classes attended", "${calc.present}")
                    CalcRow("Threshold", "${calc.threshold.toInt()}%")
                    CalcRow(
                        "Current attendance",
                        "%.1f%%".format(calc.currentPercentage),
                        if (calc.isAboveThreshold) Green400 else Red400
                    )
                }
            }

            // Predictions card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Predictions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (calc.isAboveThreshold) {
                        CalcRow("Classes you can miss", "${calc.canMiss}", Green400)
                        CalcRow(
                            "If you miss next class",
                            "%.1f%%".format(calc.percentageIfMissNext),
                            if (calc.percentageIfMissNext >= calc.threshold) Green400 else Red400
                        )
                    } else {
                        CalcRow("Classes needed to recover", "${calc.needToAttend}", Amber400)
                    }
                    CalcRow(
                        "If you attend next 5 classes",
                        "%.1f%%".format(calc.percentageIfAttendNext5),
                        if (calc.percentageIfAttendNext5 >= calc.threshold) Green400 else Amber400
                    )
                }
            }

            // Custom target card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Custom Target Calculator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Target: ${customTarget.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = customTarget,
                        onValueChange = { customTarget = it },
                        valueRange = 50f..100f,
                        steps = 49,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val customCurrent = if (sub.conductedClasses == 0) 0f
                        else sub.presentClasses * 100f / sub.conductedClasses
                    if (customCurrent >= customTarget) {
                        val canMissCustom = run {
                            val maxTotal = (sub.presentClasses * 100.0 / customTarget).toInt()
                            max(0, maxTotal - sub.conductedClasses)
                        }
                        CalcRow("Classes you can miss", "$canMissCustom", Green400)
                    } else {
                        CalcRow("Classes needed", "$customNeedToAttend", Amber400)
                    }
                    Text(
                        "Formula: (present + x) / (conducted + x) ≥ target/100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            com.classpilot.offline.ui.components.PoweredByWatermark()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CalcRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
