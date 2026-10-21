package com.classpilot.offline.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.classpilot.offline.ui.screens.attendance.AttendanceCalculatorScreen
import com.classpilot.offline.ui.screens.attendance.AttendanceDetailScreen
import com.classpilot.offline.ui.screens.attendance.AttendanceScreen
import com.classpilot.offline.ui.screens.backup.BackupScreen
import com.classpilot.offline.ui.screens.calendar.CalendarScreen
import com.classpilot.offline.ui.screens.dashboard.HomeScreen
import com.classpilot.offline.ui.screens.settings.NotificationDiagnosticScreen
import com.classpilot.offline.ui.screens.settings.SettingsScreen
import com.classpilot.offline.ui.screens.subjects.AddEditSubjectScreen
import com.classpilot.offline.ui.screens.subjects.SubjectsScreen
import com.classpilot.offline.ui.screens.timetable.*

@Composable
fun ClassPilotNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTimetableToday = { navController.navigate(Screen.TimetableToday.route) },
                onNavigateToTimetableTomorrow = { navController.navigate(Screen.TimetableTomorrow.route) },
                onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                onNavigateToSubjectDetail = { id ->
                    navController.navigate(Screen.AttendanceDetail.createRoute(id))
                }
            )
        }

        composable(Screen.Timetable.route) {
            TimetableScreen(
                onNavigateToday = { navController.navigate(Screen.TimetableToday.route) },
                onNavigateTomorrow = { navController.navigate(Screen.TimetableTomorrow.route) },
                onNavigateWeekly = { navController.navigate(Screen.TimetableWeekly.route) },
                onAddClass = { day -> navController.navigate(Screen.AddEditClass.createRoute(dayOfWeek = day)) }
            )
        }

        composable(Screen.TimetableToday.route) {
            TimetableTodayScreen(
                onBack = { navController.popBackStack() },
                onEditEntry = { id -> navController.navigate(Screen.AddEditClass.createRoute(entryId = id)) }
            )
        }

        composable(Screen.TimetableTomorrow.route) {
            TimetableTomorrowScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TimetableWeekly.route) {
            WeeklyTimetableScreen(
                onBack = { navController.popBackStack() },
                onAddClass = { day -> navController.navigate(Screen.AddEditClass.createRoute(dayOfWeek = day)) },
                onEditEntry = { id -> navController.navigate(Screen.AddEditClass.createRoute(entryId = id)) }
            )
        }

        composable(
            route = Screen.AddEditClass.route,
            arguments = listOf(
                navArgument("entryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("dayOfWeek") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val dayOfWeek = backStackEntry.arguments?.getInt("dayOfWeek") ?: 1
            AddEditClassScreen(
                entryId = entryId,
                initialDayOfWeek = dayOfWeek,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Attendance.route) {
            AttendanceScreen(
                onSubjectClick = { id -> navController.navigate(Screen.AttendanceDetail.createRoute(id)) },
                onAddSubject = { navController.navigate(Screen.AddEditSubject.route) }
            )
        }

        composable(
            route = Screen.AttendanceDetail.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStack ->
            val subjectId = backStack.arguments?.getLong("subjectId") ?: return@composable
            AttendanceDetailScreen(
                subjectId = subjectId,
                onBack = { navController.popBackStack() },
                onOpenCalculator = { navController.navigate(Screen.AttendanceCalculator.createRoute(subjectId)) }
            )
        }

        composable(
            route = Screen.AttendanceCalculator.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStack ->
            val subjectId = backStack.arguments?.getLong("subjectId") ?: return@composable
            AttendanceCalculatorScreen(
                subjectId = subjectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Subjects.route) {
            SubjectsScreen(
                onAddSubject = { navController.navigate(Screen.AddEditSubject.route) },
                onEditSubject = { id -> navController.navigate(Screen.AddEditSubject.createRoute(id)) }
            )
        }

        composable(
            route = Screen.AddEditSubject.route,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStack ->
            val subjectId = backStack.arguments?.getLong("subjectId") ?: -1L
            AddEditSubjectScreen(
                subjectId = subjectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateNotificationDiagnostic = { navController.navigate(Screen.NotificationDiagnostic.route) }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.NotificationDiagnostic.route) {
            NotificationDiagnosticScreen(onBack = { navController.popBackStack() })
        }
    }
}
