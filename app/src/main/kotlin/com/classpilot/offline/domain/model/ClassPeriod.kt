package com.classpilot.offline.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Domain model for a timetable class period.
 */
data class ClassPeriod(
    val id: Long = 0,
    val subject: Subject,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String = "",
    val faculty: String = "",
    val classType: ClassType = ClassType.THEORY,
    val isRepeatingWeekly: Boolean = true,
    val notificationEnabled: Boolean = true,
    /** List of minutes-before-class for reminders, e.g. [30, 5] */
    val reminderMinutes: List<Int> = listOf(15, 5),
    val isActive: Boolean = true,
    /** Optional: attendance status for a specific date */
    val attendanceStatus: AttendanceStatus = AttendanceStatus.UPCOMING
) {
    val durationMinutes: Int
        get() = endTime.toSecondOfDay() / 60 - startTime.toSecondOfDay() / 60

    val startMinuteOfDay: Int
        get() = startTime.hour * 60 + startTime.minute

    val endMinuteOfDay: Int
        get() = endTime.hour * 60 + endTime.minute
}

enum class ClassType(val display: String) {
    THEORY("Theory"),
    LAB("Lab"),
    TUTORIAL("Tutorial"),
    SEMINAR("Seminar"),
    OTHER("Other")
}

enum class AttendanceStatus(val display: String) {
    UPCOMING("Upcoming"),
    PRESENT("Present"),
    ABSENT("Absent"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
    PENDING("Pending")
}

data class DaySchedule(
    val date: LocalDate,
    val periods: List<ClassPeriod>
) {
    val totalClasses: Int get() = periods.size
    val hasClasses: Boolean get() = periods.isNotEmpty()
    val firstClass: ClassPeriod? get() = periods.firstOrNull()
    val lastClass: ClassPeriod? get() = periods.lastOrNull()
}
