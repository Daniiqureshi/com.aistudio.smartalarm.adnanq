package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.AlarmRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.scheduler.AndroidAlarmScheduler
import com.example.services.NotificationHelper

class SmartAlarmApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var alarmScheduler: AndroidAlarmScheduler
        private set
    lateinit var alarmRepository: AlarmRepository
        private set
    lateinit var prayerRepository: PrayerRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Create notification channels immediately
        NotificationHelper.createNotificationChannels(this)

        // 2. Safely initialize Firebase if configured
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Throwable) {
            android.util.Log.i("SmartAlarmApp", "Firebase not configured or google-services.json missing; operating in local-first mode")
        }

        // 3. Initialize local database and components
        database = AppDatabase.getDatabase(this)
        alarmScheduler = AndroidAlarmScheduler(this)
        alarmRepository = AlarmRepository(this, database.alarmDao(), alarmScheduler)
        prayerRepository = PrayerRepository(this, database.prayerDao())
        settingsRepository = SettingsRepository(this)
    }

    companion object {
        lateinit var instance: SmartAlarmApp
            private set
    }
}
