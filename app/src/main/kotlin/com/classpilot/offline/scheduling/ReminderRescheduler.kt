package com.classpilot.offline.scheduling

import com.classpilot.offline.data.preferences.PreferencesManager
import com.classpilot.offline.data.repository.SubjectRepository
import com.classpilot.offline.data.repository.TimetableRepository
import com.classpilot.offline.notifications.NotificationScheduler
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reschedules all alarms — called after boot or timetable changes.
 */
@Singleton
class ReminderRescheduler @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduler: NotificationScheduler,
    private val preferencesManager: PreferencesManager
) {

    suspend fun rescheduleAll() {
        val prefs = preferencesManager.appPreferences.first()
        val subjects = subjectRepository.allSubjectsWithStats.first()
        val subjectMap = subjects.associateBy { it.id }

        // Reschedule per-class reminders for the next 7 days
        for (dayValue in 1..7) {
            val entries = timetableRepository.getPeriodsForDaySync(DayOfWeek.of(dayValue))
            for (entry in entries) {
                if (!entry.isActive || !entry.notificationEnabled) continue
                val subject = subjectMap[entry.subjectId] ?: continue
                scheduler.scheduleRemindersForEntry(
                    entry = entry,
                    subjectName = subject.name,
                    room = entry.room.ifBlank { subject.classroom },
                    prefs = prefs
                )
            }
        }

        // Reschedule daily timetable notifications
        if (prefs.notificationsEnabled && prefs.morningNotificationEnabled) {
            scheduler.scheduleDailyMorningNotification(
                prefs.morningNotificationHour,
                prefs.morningNotificationMinute
            )
        }
        if (prefs.notificationsEnabled && prefs.eveningNotificationEnabled) {
            scheduler.scheduleEveningPreviewNotification(
                prefs.eveningNotificationHour,
                prefs.eveningNotificationMinute
            )
        }
    }

    suspend fun cancelAll() {
        val subjects = subjectRepository.allSubjectsWithStats.first()
        val subjectMap = subjects.associateBy { it.id }
        for (dayValue in 1..7) {
            val entries = timetableRepository.getPeriodsForDaySync(DayOfWeek.of(dayValue))
            for (entry in entries) {
                scheduler.cancelRemindersForEntry(entry.id, entry.reminderMinutes)
            }
        }
        scheduler.cancelDailyMorningNotification()
        scheduler.cancelEveningPreviewNotification()
    }
}
