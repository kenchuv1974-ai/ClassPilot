package com.classpilot.offline.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.classpilot.offline.MainActivity
import com.classpilot.offline.data.database.entities.TimetableEntryEntity
import com.classpilot.offline.data.preferences.AppPreferences
import com.classpilot.offline.receivers.AlarmReceiver
import com.classpilot.offline.receivers.DailyNotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central notification and alarm scheduler.
 *
 * Android platform notes:
 * - On Android 12+ (API 31+), SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM is required.
 *   If the user has revoked this permission, alarms fall back to inexact (setWindow).
 * - Doze mode may delay alarms. Use setExactAndAllowWhileIdle for time-sensitive reminders.
 * - On some OEM devices (MIUI, ColorOS), alarms and notifications may be restricted
 *   by battery optimisation or auto-start restrictions. The diagnostic screen in Settings
 *   guides users to whitelist the app.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ── Notification ID allocation ──────────────────────────────────────────
    // Class reminder: entryId * 1000 + reminderOffset
    // Daily timetable: 9_000_000
    // Evening preview: 9_000_001
    // Attendance alert: 9_000_100 + subjectId

    companion object {
        const val EXTRA_SUBJECT_NAME = "subject_name"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_ROOM = "room"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        const val EXTRA_ENTRY_ID = "entry_id"
        const val DAILY_TIMETABLE_NOTIF_ID = 9_000_000
        const val EVENING_PREVIEW_NOTIF_ID = 9_000_001
    }

    /**
     * Schedule all reminders for a timetable entry on the next occurrence of its weekday.
     */
    fun scheduleRemindersForEntry(
        entry: TimetableEntryEntity,
        subjectName: String,
        room: String,
        prefs: AppPreferences
    ) {
        if (!prefs.notificationsEnabled || !entry.notificationEnabled) return

        val reminderOffsets = entry.reminderMinutes
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }

        val targetDay = nextOccurrenceOf(DayOfWeek.of(entry.dayOfWeek))
        val classStart = LocalTime.of(entry.startMinute / 60, entry.startMinute % 60)

        for (offsetMinutes in reminderOffsets) {
            val fireTime = LocalDateTime.of(targetDay, classStart).minusMinutes(offsetMinutes.toLong())
            if (fireTime.isBefore(LocalDateTime.now())) continue

            val notifId = (entry.id * 1000 + offsetMinutes).toInt()
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_SUBJECT_NAME, subjectName)
                putExtra(EXTRA_START_TIME, classStart.toString())
                putExtra(EXTRA_ROOM, room)
                putExtra(EXTRA_MINUTES_BEFORE, offsetMinutes)
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra("notif_id", notifId)
            }

            val pi = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val epochMillis = fireTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            scheduleExact(epochMillis, pi)
        }
    }

    /**
     * Cancel all reminders associated with a timetable entry.
     */
    fun cancelRemindersForEntry(entryId: Long, reminderOffsetsStr: String) {
        val offsets = reminderOffsetsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        for (offset in offsets) {
            val notifId = (entryId * 1000 + offset).toInt()
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                notifId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pi?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            notificationManager.cancel(notifId)
        }
    }

    /** Schedule the daily morning timetable notification */
    fun scheduleDailyMorningNotification(hour: Int, minute: Int) {
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            putExtra("type", "MORNING")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_TIMETABLE_NOTIF_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fireTime = nextOccurrenceOfTime(hour, minute)
        // Repeat daily using inexact repeating (battery friendly)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            fireTime,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancelDailyMorningNotification() {
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            DAILY_TIMETABLE_NOTIF_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it); it.cancel() }
    }

    /** Schedule the evening preview notification */
    fun scheduleEveningPreviewNotification(hour: Int, minute: Int) {
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            putExtra("type", "EVENING")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            EVENING_PREVIEW_NOTIF_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fireTime = nextOccurrenceOfTime(hour, minute)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            fireTime,
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancelEveningPreviewNotification() {
        val intent = Intent(context, DailyNotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            EVENING_PREVIEW_NOTIF_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it); it.cancel() }
    }

    /** Fire an immediate low-attendance notification */
    fun showLowAttendanceNotification(subjectName: String, percentage: Float, notifId: Int) {
        val tapIntent = Intent(context, MainActivity::class.java)
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_ATTENDANCE_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Attendance: $subjectName")
            .setContentText("Your attendance is ${String.format("%.1f", percentage)}%. Take action to avoid detainment.")
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notifId, notification)
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private fun scheduleExact(epochMillis: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, epochMillis, pi)
            }
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM not granted; fall back to inexact
            alarmManager.set(AlarmManager.RTC_WAKEUP, epochMillis, pi)
        }
    }

    private fun nextOccurrenceOf(dayOfWeek: DayOfWeek): LocalDate {
        var date = LocalDate.now()
        while (date.dayOfWeek != dayOfWeek) {
            date = date.plusDays(1)
        }
        return date
    }

    private fun nextOccurrenceOfTime(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(hour, minute)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
