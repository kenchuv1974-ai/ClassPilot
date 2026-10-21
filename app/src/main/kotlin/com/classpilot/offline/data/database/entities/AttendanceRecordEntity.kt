package com.classpilot.offline.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single attendance record for one class on one date.
 * [status]: "PRESENT", "ABSENT", "CANCELLED", "PENDING", "EXTRA"
 */
@Serializable
@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("dateEpochDay")]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    /** Days since epoch (LocalDate.toEpochDay()) */
    val dateEpochDay: Long,
    /** "PRESENT", "ABSENT", "CANCELLED", "PENDING", "EXTRA" */
    val status: String,
    /** Optional link to which timetable entry this attendance belongs to */
    val timetableEntryId: Long? = null,
    /** Minute-of-day the class started */
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val notes: String = "",
    val isExtraClass: Boolean = false,
    val isCancelled: Boolean = false,
    val markedAt: Long = System.currentTimeMillis()
)
