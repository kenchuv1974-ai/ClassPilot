package com.classpilot.offline

import android.app.Application
import com.classpilot.offline.notifications.NotificationChannelManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ClassPilotApp : Application() {

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createAllChannels()
    }
}
