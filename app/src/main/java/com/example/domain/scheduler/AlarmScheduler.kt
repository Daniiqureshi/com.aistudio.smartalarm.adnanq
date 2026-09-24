package com.example.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AlarmEntity
import com.example.receivers.AlarmReceiver
import java.util.Calendar

interface AlarmScheduler {
    fun schedule(alarm: AlarmEntity)
    fun cancel(alarmId: Long)
    fun snooze(alarmId: Long, title: String, category: String, snoozeMinutes: Int)
    fun canScheduleExactAlarms(): Boolean
}

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    override fun schedule(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        val triggerTime = computeNextTriggerTime(alarm)
        if (triggerTime <= System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Alarm trigger time is in the past: ${alarm.title}")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_DESC, alarm.description)
            putExtra(AlarmReceiver.EXTRA_ALARM_CATEGORY, alarm.category)
            putExtra(AlarmReceiver.EXTRA_ALARM_SOUND, alarm.soundName)
            putExtra(AlarmReceiver.EXTRA_ALARM_VIB, alarm.isVibrationEnabled)
            putExtra(AlarmReceiver.EXTRA_ALARM_IS_PRAYER, alarm.isPrayerAlarm)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Successfully scheduled exact alarm [${alarm.id}] for: $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Permission denied for exact alarm", e)
            // Fallback to inexact set() if exact alarm permission is missing
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    override fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm: $alarmId")
    }

    override fun snooze(alarmId: Long, title: String, category: String, snoozeMinutes: Int) {
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_ALARM_DESC, "Snoozed for $snoozeMinutes minutes")
            putExtra(AlarmReceiver.EXTRA_ALARM_CATEGORY, category)
            putExtra(AlarmReceiver.EXTRA_ALARM_SOUND, "Default Alarm")
            putExtra(AlarmReceiver.EXTRA_ALARM_VIB, true)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + 999900).toInt(), // unique snooze requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Snoozed alarm [$alarmId] for $snoozeMinutes min")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to set snooze alarm", e)
        }
    }

    companion object {
        fun computeNextTriggerTime(alarm: AlarmEntity): Long {
            val now = System.currentTimeMillis()
            var trigger = alarm.triggerTime

            if (trigger > now) {
                return trigger
            }

            // If it's a recurring alarm and trigger is in past, compute next occurrence
            val cal = Calendar.getInstance().apply { timeInMillis = trigger }
            val nowCal = Calendar.getInstance().apply { timeInMillis = now }

            when (alarm.repeatType) {
                "DAILY" -> {
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return cal.timeInMillis
                }
                "WEEKDAYS" -> {
                    while (cal.timeInMillis <= now || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return cal.timeInMillis
                }
                "WEEKLY" -> {
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    return cal.timeInMillis
                }
                "BIWEEKLY" -> {
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.WEEK_OF_YEAR, 2)
                    }
                    return cal.timeInMillis
                }
                "MONTHLY" -> {
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.MONTH, 1)
                    }
                    return cal.timeInMillis
                }
                "YEARLY" -> {
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.YEAR, 1)
                    }
                    return cal.timeInMillis
                }
                "INTERVAL_DAYS" -> {
                    val step = if (alarm.repeatIntervalDays > 0) alarm.repeatIntervalDays else 1
                    while (cal.timeInMillis <= now) {
                        cal.add(Calendar.DAY_OF_YEAR, step)
                    }
                    return cal.timeInMillis
                }
                "CUSTOM_DAYS" -> {
                    val dayNumbers = alarm.repeatDaysOfWeek.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                    if (dayNumbers.isNotEmpty()) {
                        // day numbers: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
                        // Calendar: Sunday=1, Monday=2, ..., Saturday=7
                        val calDays = dayNumbers.map { toCalendarDay(it) }
                        while (cal.timeInMillis <= now || !calDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        return cal.timeInMillis
                    }
                }
            }

            return trigger
        }

        private fun toCalendarDay(isoDay: Int): Int {
            return when (isoDay) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
        }
    }
}
