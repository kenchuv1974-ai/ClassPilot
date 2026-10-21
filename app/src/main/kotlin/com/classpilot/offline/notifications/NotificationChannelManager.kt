package com.classpilot.offline.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object NotificationChannels {
    const val CHANNEL_CLASS_REMINDERS = "class_reminders"
    const val CHANNEL_DAILY_TIMETABLE = "daily_timetable"
    const val CHANNEL_ATTENDANCE_ALERTS = "attendance_alerts"
}

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun createAllChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Class reminders — high importance so they pop up as heads-up
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.CHANNEL_CLASS_REMINDERS,
                "Class Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before upcoming classes"
                enableVibration(true)
                enableLights(true)
            }
        )

        // Daily timetable summary — default importance
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.CHANNEL_DAILY_TIMETABLE,
                "Daily Timetable",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning summary of today's classes"
            }
        )

        // Attendance alerts — low importance
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.CHANNEL_ATTENDANCE_ALERTS,
                "Attendance Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low attendance warnings and pending entries"
            }
        )
    }
}
