package com.wakeup.alarm.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = true)
abstract class AlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        private const val DB_NAME = "wakeup.db"

        /**
         * The database lives in DEVICE-PROTECTED storage so that alarms can be re-armed and can ring after a reboot
         * even before the user has unlocked the phone for the first time (Direct Boot).
         * Nothing sensitive is stored here: only alarm times, labels and settings.
         */
        fun create(appContext: Context): AlarmDatabase {
            val deviceContext = appContext.createDeviceProtectedStorageContext()
            // Passing an absolute path makes Room open the file inside device-protected storage.
            val path = deviceContext.getDatabasePath(DB_NAME).absolutePath
            return Room.databaseBuilder(appContext, AlarmDatabase::class.java, path).build()
        }
    }
}
