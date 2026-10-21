package com.classpilot.offline.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents an academic subject (course) in the database.
 */
@Serializable
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val facultyName: String = "",
    val classroom: String = "",
    /** "THEORY", "LAB", "TUTORIAL", "SEMINAR", "OTHER" */
    val type: String = "THEORY",
    /** Minimum required attendance percentage, e.g. 75.0 */
    val attendanceThreshold: Float = 75f,
    /** Hex colour string like "#7C4DFF" */
    val colorHex: String = "#7C4DFF",
    val notes: String = "",
    /** Whether theory and lab are tracked separately (set false for combined) */
    val separateLabTracking: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
