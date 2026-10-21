package com.classpilot.offline.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.classpilot.offline.MainActivity
import com.classpilot.offline.notifications.NotificationChannels
import com.classpilot.offline.notifications.NotificationScheduler.Companion.DAILY_TIMETABLE_NOTIF_ID
import com.classpilot.offline.notifications.NotificationScheduler.Companion.EVENING_PREVIEW_NOTIF_ID

/**
 * Fires for the daily morning timetable summary and evening tomorrow preview.
 */
class DailyNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: "MORNING"
        val isMorning = type == "MORNING"

        val title = if (isMorning) "📅 Today's Classes" else "🌙 Tomorrow's Classes"
        val text = if (isMorning)
            "Check your schedule for today. Tap to see all classes."
        else
            "Preview tomorrow's timetable and set reminders."

        val notifId = if (isMorning) DAILY_TIMETABLE_NOTIF_ID else EVENING_PREVIEW_NOTIF_ID

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", if (isMorning) "timetable_today" else "timetable_tomorrow")
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_TIMETABLE)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(notifId, notification)
    }
}
