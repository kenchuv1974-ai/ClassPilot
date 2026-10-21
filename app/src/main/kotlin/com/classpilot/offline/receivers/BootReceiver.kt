package com.classpilot.offline.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.classpilot.offline.scheduling.ReminderRescheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fired after device reboots. Reschedules all future class reminders
 * and daily timetable notifications because AlarmManager alarms don't
 * survive a reboot.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRescheduler: ReminderRescheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        // Use goAsync to keep the receiver alive while we do async work
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderRescheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
