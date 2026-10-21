package com.classpilot.offline.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classpilot_prefs")

data class AppPreferences(
    val studentName: String = "",
    val themeMode: String = "SYSTEM",          // "DARK", "LIGHT", "SYSTEM"
    val defaultAttendanceThreshold: Float = 75f,
    val notificationsEnabled: Boolean = true,
    val morningNotificationEnabled: Boolean = true,
    val morningNotificationHour: Int = 7,
    val morningNotificationMinute: Int = 30,
    val eveningNotificationEnabled: Boolean = true,
    val eveningNotificationHour: Int = 20,
    val eveningNotificationMinute: Int = 0,
    val firstClassReminderEnabled: Boolean = true,
    val pendingAttendanceReminderEnabled: Boolean = true,
    val semesterStartEpochDay: Long = -1L,
    val semesterEndEpochDay: Long = -1L,
    val lastBackupTimestamp: Long = -1L,
    val onboardingCompleted: Boolean = false
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_THRESHOLD = floatPreferencesKey("default_threshold")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MORNING_NOTIF_ENABLED = booleanPreferencesKey("morning_notif_enabled")
        val MORNING_NOTIF_HOUR = intPreferencesKey("morning_notif_hour")
        val MORNING_NOTIF_MINUTE = intPreferencesKey("morning_notif_minute")
        val EVENING_NOTIF_ENABLED = booleanPreferencesKey("evening_notif_enabled")
        val EVENING_NOTIF_HOUR = intPreferencesKey("evening_notif_hour")
        val EVENING_NOTIF_MINUTE = intPreferencesKey("evening_notif_minute")
        val FIRST_CLASS_REMINDER = booleanPreferencesKey("first_class_reminder")
        val PENDING_ATTENDANCE_REMINDER = booleanPreferencesKey("pending_attendance_reminder")
        val SEMESTER_START = longPreferencesKey("semester_start")
        val SEMESTER_END = longPreferencesKey("semester_end")
        val LAST_BACKUP = longPreferencesKey("last_backup")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val appPreferences: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs ->
            AppPreferences(
                studentName = prefs[Keys.STUDENT_NAME] ?: "",
                themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
                defaultAttendanceThreshold = prefs[Keys.DEFAULT_THRESHOLD] ?: 75f,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                morningNotificationEnabled = prefs[Keys.MORNING_NOTIF_ENABLED] ?: true,
                morningNotificationHour = prefs[Keys.MORNING_NOTIF_HOUR] ?: 7,
                morningNotificationMinute = prefs[Keys.MORNING_NOTIF_MINUTE] ?: 30,
                eveningNotificationEnabled = prefs[Keys.EVENING_NOTIF_ENABLED] ?: true,
                eveningNotificationHour = prefs[Keys.EVENING_NOTIF_HOUR] ?: 20,
                eveningNotificationMinute = prefs[Keys.EVENING_NOTIF_MINUTE] ?: 0,
                firstClassReminderEnabled = prefs[Keys.FIRST_CLASS_REMINDER] ?: true,
                pendingAttendanceReminderEnabled = prefs[Keys.PENDING_ATTENDANCE_REMINDER] ?: true,
                semesterStartEpochDay = prefs[Keys.SEMESTER_START] ?: -1L,
                semesterEndEpochDay = prefs[Keys.SEMESTER_END] ?: -1L,
                lastBackupTimestamp = prefs[Keys.LAST_BACKUP] ?: -1L,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            )
        }

    suspend fun updateStudentName(name: String) {
        context.dataStore.edit { it[Keys.STUDENT_NAME] = name }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun updateDefaultThreshold(threshold: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_THRESHOLD] = threshold }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateMorningNotification(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.MORNING_NOTIF_ENABLED] = enabled
            it[Keys.MORNING_NOTIF_HOUR] = hour
            it[Keys.MORNING_NOTIF_MINUTE] = minute
        }
    }

    suspend fun updateEveningNotification(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.EVENING_NOTIF_ENABLED] = enabled
            it[Keys.EVENING_NOTIF_HOUR] = hour
            it[Keys.EVENING_NOTIF_MINUTE] = minute
        }
    }

    suspend fun updateFirstClassReminder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FIRST_CLASS_REMINDER] = enabled }
    }

    suspend fun updatePendingAttendanceReminder(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PENDING_ATTENDANCE_REMINDER] = enabled }
    }

    suspend fun updateSemesterDates(startDay: Long, endDay: Long) {
        context.dataStore.edit {
            it[Keys.SEMESTER_START] = startDay
            it[Keys.SEMESTER_END] = endDay
        }
    }

    suspend fun updateLastBackupTimestamp(ts: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP] = ts }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }
}
