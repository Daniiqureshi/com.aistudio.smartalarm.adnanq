package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.domain.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED) {

            Log.d("TimeChangeReceiver", "Time or timezone alteration detected ($action). Rescheduling alarms...")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val enabledAlarms = db.alarmDao().getAllEnabledAlarms()
                    val scheduler = AndroidAlarmScheduler(context)

                    for (alarm in enabledAlarms) {
                        scheduler.schedule(alarm)
                    }
                    Log.d("TimeChangeReceiver", "Successfully synchronized ${enabledAlarms.size} alarms after time/timezone update")
                } catch (e: Exception) {
                    Log.e("TimeChangeReceiver", "Error synchronizing alarms after time adjustment", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
