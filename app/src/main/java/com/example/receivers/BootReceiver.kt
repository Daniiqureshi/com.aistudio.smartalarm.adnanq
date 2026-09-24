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

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            Log.d("BootReceiver", "Device reboot detected. Rescheduling all enabled future alarms...")
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val enabledAlarms = db.alarmDao().getAllEnabledAlarms()
                    val scheduler = AndroidAlarmScheduler(context)

                    for (alarm in enabledAlarms) {
                        scheduler.schedule(alarm)
                    }
                    Log.d("BootReceiver", "Successfully rescheduled ${enabledAlarms.size} alarms after boot")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
