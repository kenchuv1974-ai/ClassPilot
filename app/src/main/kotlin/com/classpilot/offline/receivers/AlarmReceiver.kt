package com.classpilot.offline.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.classpilot.offline.MainActivity
import com.classpilot.offline.notifications.NotificationChannels
import com.classpilot.offline.notifications.NotificationScheduler

/**
 * Receives alarm broadcasts and shows the class reminder notification.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra(NotificationScheduler.EXTRA_SUBJECT_NAME) ?: return
        val startTime = intent.getStringExtra(NotificationScheduler.EXTRA_START_TIME) ?: ""
        val room = intent.getStringExtra(NotificationScheduler.EXTRA_ROOM) ?: ""
        val minutesBefore = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTES_BEFORE, 0)
        val notifId = intent.getIntExtra("notif_id", 0)

        val contentText = when {
            minutesBefore == 0 -> "$subjectName is starting now${if (room.isNotBlank()) " in $room" else ""}."
            minutesBefore < 60 -> "$subjectName starts in $minutesBefore min${if (room.isNotBlank()) " • $room" else ""}."
            else -> "$subjectName starts in ${minutesBefore / 60}h${minutesBefore % 60}m${if (room.isNotBlank()) " • $room" else ""}."
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "timetable")
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_CLASS_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("📚 Upcoming: $subjectName")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(notifId, notification)
    }
}
