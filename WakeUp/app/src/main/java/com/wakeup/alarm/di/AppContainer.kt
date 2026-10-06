package com.wakeup.alarm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wakeup.alarm.alarm.AlarmScheduler
import com.wakeup.alarm.data.AlarmRepository
import com.wakeup.alarm.data.local.AlarmDatabase
import com.wakeup.alarm.data.settings.SettingsRepository
import com.wakeup.alarm.data.stopwatch.StopwatchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Tiny manual dependency container (no DI framework needed for an app this size, which keeps the APK small and the
 * build simple). Created once in [com.wakeup.alarm.WakeUpApp]; everything is lazy so receivers that fire during
 * Direct Boot only pay for what they use.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    /** Process-wide scope for work that must outlive a single screen (DataStore). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val scheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }

    private val database: AlarmDatabase by lazy { AlarmDatabase.create(appContext) }

    val alarmRepository: AlarmRepository by lazy { AlarmRepository(database.alarmDao(), scheduler) }

    private val settingsDataStore: DataStore<Preferences> by lazy {
        val deviceContext = appContext.createDeviceProtectedStorageContext()
        PreferenceDataStoreFactory.create(scope = appScope) {
            deviceContext.preferencesDataStoreFile("settings")
        }
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(settingsDataStore) }

    val stopwatch: StopwatchController by lazy { StopwatchController(appContext) }
}
