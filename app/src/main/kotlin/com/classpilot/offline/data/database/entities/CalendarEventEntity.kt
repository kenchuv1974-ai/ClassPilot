package com.classpilot.offline.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * An academic calendar event (holiday, exam, assignment deadline, etc.)
 */
@Serializable
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    /** Days since epoch (LocalDate.toEpochDay()) */
    val dateEpochDay: Long,
    /** "HOLIDAY", "EXAM", "ASSIGNMENT", "TEST", "EXTRA_CLASS", "CANCELLED_CLASS", "PERSONAL", "OTHER" */
    val eventType: String = "PERSONAL",
    val colorHex: String = "#FF6B6B",
    val hasReminder: Boolean = false,
    /** Epoch millis for reminder, -1 if none */
    val reminderAt: Long = -1L,
    val isAllDay: Boolean = true,
    /** Minutes since midnight for start (if not all-day) */
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
