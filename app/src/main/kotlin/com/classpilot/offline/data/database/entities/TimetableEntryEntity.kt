package com.classpilot.offline.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A scheduled class period in the weekly timetable.
 * [dayOfWeek] uses java.time.DayOfWeek ordinal: 1=Monday … 7=Sunday.
 */
@Serializable
@Entity(
    tableName = "timetable_entries",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("dayOfWeek")]
)
data class TimetableEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    /** 1=Monday … 7=Sunday */
    val dayOfWeek: Int,
    /** Minutes since midnight, e.g. 9*60+30 = 570 for 09:30 */
    val startMinute: Int,
    val endMinute: Int,
    val room: String = "",
    val faculty: String = "",
    /** "THEORY", "LAB", "TUTORIAL", "SEMINAR", "OTHER" */
    val classType: String = "THEORY",
    val isRepeatingWeekly: Boolean = true,
    val notificationEnabled: Boolean = true,
    /** Comma-separated reminder offsets in minutes before class, e.g. "30,5" */
    val reminderMinutes: String = "15,5",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
