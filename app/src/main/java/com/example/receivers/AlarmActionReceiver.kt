package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.domain.scheduler.AndroidAlarmScheduler
import com.example.services.NotificationHelper
import com.example.services.SoundPlayer

class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.example.smartalarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.smartalarm.ACTION_SNOOZE"

        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_alarm_title"
        const val EXTRA_ALARM_CATEGORY = "extra_alarm_category"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)
        val title = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "SmartAlarm"
        val category = intent.getStringExtra(EXTRA_ALARM_CATEGORY) ?: "Personal"
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

        // Stop ringing immediately
        SoundPlayer.stop()
        NotificationHelper.cancelNotification(context, alarmId)

        when (intent.action) {
            ACTION_DISMISS -> {
                Log.d("AlarmActionReceiver", "Alarm dismissed: $alarmId")
            }
            ACTION_SNOOZE -> {
                Log.d("AlarmActionReceiver", "Alarm snoozed for $snoozeMinutes min: $alarmId")
                val scheduler = AndroidAlarmScheduler(context)
                scheduler.snooze(alarmId, title, category, snoozeMinutes)
            }
        }
    }
}
