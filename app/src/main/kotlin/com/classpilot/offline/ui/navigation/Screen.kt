package com.classpilot.offline.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Timetable : Screen("timetable")
    object TimetableToday : Screen("timetable/today")
    object TimetableTomorrow : Screen("timetable/tomorrow")
    object TimetableWeekly : Screen("timetable/weekly")
    object AddEditClass : Screen("timetable/add_edit?entryId={entryId}&dayOfWeek={dayOfWeek}") {
        fun createRoute(entryId: Long = -1L, dayOfWeek: Int = 1) =
            "timetable/add_edit?entryId=$entryId&dayOfWeek=$dayOfWeek"
    }
    object Attendance : Screen("attendance")
    object AttendanceDetail : Screen("attendance/detail/{subjectId}") {
        fun createRoute(subjectId: Long) = "attendance/detail/$subjectId"
    }
    object AttendanceCalculator : Screen("attendance/calculator/{subjectId}") {
        fun createRoute(subjectId: Long) = "attendance/calculator/$subjectId"
    }
    object Calendar : Screen("calendar")
    object Subjects : Screen("subjects")
    object AddEditSubject : Screen("subjects/add_edit?subjectId={subjectId}") {
        fun createRoute(subjectId: Long = -1L) = "subjects/add_edit?subjectId=$subjectId"
    }
    object Settings : Screen("settings")
    object Backup : Screen("backup")
    object NotificationDiagnostic : Screen("settings/notifications")
}

enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    HOME(Screen.Home, "Home", Icons.Filled.Home),
    TIMETABLE(Screen.Timetable, "Schedule", Icons.Filled.CalendarMonth),
    ATTENDANCE(Screen.Attendance, "Attendance", Icons.Filled.CheckCircle),
    CALENDAR(Screen.Calendar, "Calendar", Icons.Filled.EventNote),
    SETTINGS(Screen.Settings, "Settings", Icons.Filled.Settings)
}
