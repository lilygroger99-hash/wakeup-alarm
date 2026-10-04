package com.wakeup.alarm

import android.app.Application
import android.content.Context
import com.wakeup.alarm.alarm.AlarmNotifications
import com.wakeup.alarm.di.AppContainer

class WakeUpApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Idempotent: creating an existing channel is a no-op.
        AlarmNotifications.createChannels(this)
    }
}

/** Access the app's dependency container from any Context (Activity, Service, BroadcastReceiver). */
val Context.container: AppContainer
    get() = (applicationContext as WakeUpApp).container
