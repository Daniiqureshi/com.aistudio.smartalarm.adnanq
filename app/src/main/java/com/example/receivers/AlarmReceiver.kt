package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.domain.scheduler.AndroidAlarmScheduler
import com.example.services.NotificationHelper
import com.example.services.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_ALARM_DESC = "extra_alarm_desc"
        const val EXTRA_ALARM_CATEGORY = "extra_alarm_category"
        const val EXTRA_ALARM_SOUND = "extra_alarm_sound"
        const val EXTRA_ALARM_VIB = "extra_alarm_vib"
        const val EXTRA_ALARM_IS_PRAYER = "extra_alarm_is_prayer"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmartAlarm::AlarmWakeLock"
        ).apply {
            acquire(30000) // 30 seconds
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "SmartAlarm"
        val desc = intent.getStringExtra(EXTRA_ALARM_DESC) ?: ""
        val category = intent.getStringExtra(EXTRA_ALARM_CATEGORY) ?: "Personal"
        val sound = intent.getStringExtra(EXTRA_ALARM_SOUND) ?: "Default Alarm"
        val vib = intent.getBooleanExtra(EXTRA_ALARM_VIB, true)
        val isPrayer = intent.getBooleanExtra(EXTRA_ALARM_IS_PRAYER, false)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        Log.d("AlarmReceiver", "Alarm triggered: $title (ID: $alarmId, Snooze: $isSnooze)")

        // 1. Play sound & vibration
        SoundPlayer.play(context, sound, vib)

        // 2. Display heads-up full-screen notification
        NotificationHelper.showAlarmNotification(
            context = context,
            alarmId = alarmId,
            title = title,
            description = desc,
            category = category,
            isPrayer = isPrayer
        )

        // 3. For recurring alarms, compute and schedule the next occurrence in database
        if (!isSnooze && alarmId > 0) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarmEntity = db.alarmDao().getAlarmById(alarmId)
                    if (alarmEntity != null) {
                        if (alarmEntity.repeatType != "NONE") {
                            val nextTrigger = AndroidAlarmScheduler.computeNextTriggerTime(
                                alarmEntity.copy(triggerTime = System.currentTimeMillis())
                            )
                            db.alarmDao().updateTriggerTime(alarmId, nextTrigger)
                            val updated = alarmEntity.copy(triggerTime = nextTrigger)
                            AndroidAlarmScheduler(context).schedule(updated)
                            Log.d("AlarmReceiver", "Rescheduled recurring alarm [${alarmEntity.id}] to: $nextTrigger")
                        } else {
                            // Non-recurring: mark disabled
                            db.alarmDao().updateAlarmStatus(alarmId, false)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error processing recurring alarm reschedule", e)
                } finally {
                    try {
                        if (wakeLock.isHeld) wakeLock.release()
                    } catch (ignored: Exception) {}
                    pendingResult.finish()
                }
            }
        } else {
            try {
                if (wakeLock.isHeld) wakeLock.release()
            } catch (ignored: Exception) {}
        }
    }
}
