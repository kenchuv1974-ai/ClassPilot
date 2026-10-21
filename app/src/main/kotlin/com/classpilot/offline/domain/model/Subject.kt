package com.classpilot.offline.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Domain model for a Subject — used by the UI layer.
 */
data class Subject(
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val facultyName: String = "",
    val classroom: String = "",
    val type: SubjectType = SubjectType.THEORY,
    val attendanceThreshold: Float = 75f,
    val colorHex: String = "#7C4DFF",
    val notes: String = "",
    val separateLabTracking: Boolean = false,
    val isActive: Boolean = true,
    // Computed attendance statistics
    val conductedClasses: Int = 0,
    val presentClasses: Int = 0,
    val absentClasses: Int = 0
) {
    val attendancePercentage: Float
        get() = if (conductedClasses == 0) 0f
                else (presentClasses.toFloat() / conductedClasses) * 100f

    val isBelowThreshold: Boolean
        get() = conductedClasses > 0 && attendancePercentage < attendanceThreshold

    /** How many more classes can be missed while staying above threshold */
    val classesCanMiss: Int
        get() {
            if (conductedClasses == 0) return 0
            // present / (conducted + x) >= threshold/100
            // Solving: x <= present/threshold*100 - conducted
            val maxTotal = (presentClasses * 100.0 / attendanceThreshold).toInt()
            return maxOf(0, maxTotal - conductedClasses)
        }

    /** How many consecutive classes must be attended to reach threshold */
    val classesNeededToReachThreshold: Int
        get() {
            if (attendancePercentage >= attendanceThreshold) return 0
            // (present + x) / (conducted + x) >= threshold/100
            // x >= (threshold*conducted - 100*present) / (100 - threshold)
            val t = attendanceThreshold / 100.0
            val denominator = 1.0 - t
            if (denominator <= 0) return Int.MAX_VALUE
            val needed = Math.ceil((t * conductedClasses - presentClasses) / denominator).toInt()
            return maxOf(0, needed)
        }
}

enum class SubjectType(val display: String) {
    THEORY("Theory"),
    LAB("Laboratory"),
    TUTORIAL("Tutorial"),
    SEMINAR("Seminar"),
    OTHER("Other")
}
