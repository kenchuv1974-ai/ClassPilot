package com.classpilot.offline.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.classpilot.offline.domain.model.AttendanceStatus
import com.classpilot.offline.domain.model.ClassPeriod
import com.classpilot.offline.domain.model.Subject
import com.classpilot.offline.ui.theme.*
import java.time.format.DateTimeFormatter

// ── Attendance percentage arc ─────────────────────────────────────────────────

@Composable
fun AttendanceCircle(
    percentage: Float,
    threshold: Float,
    modifier: Modifier = Modifier,
    size: Int = 80
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "attendance_progress"
    )
    val color by animateColorAsState(
        targetValue = when {
            percentage >= threshold -> Green400
            percentage >= threshold * 0.9f -> Amber400
            else -> Red400
        },
        animationSpec = tween(600),
        label = "attendance_color"
    )

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 5.dp,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = (size / 5).sp
            )
        }
    }
}

// ── Subject attendance card ───────────────────────────────────────────────────

@Composable
fun SubjectAttendanceCard(
    subject: Subject,
    onClick: () -> Unit = {},
    onQuickPresent: (() -> Unit)? = null,
    onQuickAbsent: (() -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colour indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            try { Color(android.graphics.Color.parseColor(subject.colorHex)) }
                            catch (e: Exception) { Indigo400 }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (subject.code.isNotBlank()) {
                        Text(
                            text = subject.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${subject.presentClasses}/${subject.conductedClasses} attended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject.isBelowThreshold) {
                        Text(
                            text = "⚠️ Attend next ${subject.classesNeededToReachThreshold} to reach ${subject.attendanceThreshold.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber400,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (subject.conductedClasses > 0) {
                        Text(
                            text = "🎉 Can bunk ${subject.classesCanMiss} class${if (subject.classesCanMiss != 1) "es" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Green400,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Target: ${subject.attendanceThreshold.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                AttendanceCircle(
                    percentage = subject.attendancePercentage,
                    threshold = subject.attendanceThreshold,
                    size = 64
                )
            }

            // Quick action buttons for BunkWell-style quick attendance marking
            if (onQuickPresent != null || onQuickAbsent != null) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onQuickPresent != null) {
                        FilledTonalButton(
                            onClick = onQuickPresent,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Green400.copy(alpha = 0.18f),
                                contentColor = Green400
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("+ Attended", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (onQuickAbsent != null) {
                        FilledTonalButton(
                            onClick = onQuickAbsent,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Red400.copy(alpha = 0.18f),
                                contentColor = Red400
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text("+ Missed", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// ── Class period card ─────────────────────────────────────────────────────────

@Composable
fun ClassPeriodCard(
    period: ClassPeriod,
    showDate: Boolean = false,
    onMarkPresent: (() -> Unit)? = null,
    onMarkAbsent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val statusColor = when (period.attendanceStatus) {
        AttendanceStatus.PRESENT -> Green400
        AttendanceStatus.ABSENT -> Red400
        AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        AttendanceStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
        AttendanceStatus.UPCOMING -> Indigo400
        AttendanceStatus.PENDING -> Amber400
    }
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(period.subject.colorHex))
    } catch (e: Exception) { Indigo400 }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left colour accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(subjectColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = period.subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (period.subject.code.isNotBlank()) {
                            Text(
                                text = period.subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Status chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = period.attendanceStatus.display,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🕐 ${period.startTime.format(timeFormatter)} – ${period.endTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (period.room.isNotBlank()) {
                        Text(
                            text = "  📍 ${period.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (period.faculty.isNotBlank()) {
                    Text(
                        text = "👤 ${period.faculty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Mark attendance buttons (only for upcoming/pending)
                if (onMarkPresent != null || onMarkAbsent != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onMarkPresent != null) {
                            FilledTonalButton(
                                onClick = onMarkPresent,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Green400.copy(alpha = 0.2f),
                                    contentColor = Green400
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("✓ Present", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (onMarkAbsent != null) {
                            FilledTonalButton(
                                onClick = onMarkAbsent,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Red400.copy(alpha = 0.2f),
                                    contentColor = Red400
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("✗ Absent", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        action?.invoke()
    }
}

// ── Watermark ─────────────────────────────────────────────────────────────────

@Composable
fun PoweredByWatermark(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Indigo400.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡",
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Powered By Harshith",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndigoLight,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

