package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receivers.AlarmActionReceiver
import com.example.ui.alarm.AlarmActivity

object NotificationHelper {

    const val CHANNEL_ALARMS = "smartalarm_alarms_channel"
    const val CHANNEL_PRAYER = "smartalarm_prayer_channel"
    const val CHANNEL_IBADAT = "smartalarm_ibadat_channel"
    const val CHANNEL_GENERAL = "smartalarm_general_channel"
    const val CHANNEL_EVENTS = "smartalarm_events_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            // Alarms Channel (Highest importance)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARMS,
                "Alarms & Timers",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarms, wake up calls, and urgent reminders"
                enableLights(true)
                enableVibration(true)
                setSound(defaultAlarmSound, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            // Prayer Times Channel
            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                "Prayer Times & Adhan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily Islamic prayer times and Adhan notifications"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Ibadat & Worship Channel
            val ibadatChannel = NotificationChannel(
                CHANNEL_IBADAT,
                "Ibadat & Spiritual Practices",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tahajjud, Sehri, Iftar, Jummah, and spiritual practice reminders"
                enableLights(true)
                enableVibration(true)
            }

            // General Reminders Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard personal, work, and study reminders"
            }

            // Important Events Channel
            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Important Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Calendar milestones, birthdays, and planned events"
            }

            notificationManager.createNotificationChannels(
                listOf(alarmChannel, prayerChannel, ibadatChannel, generalChannel, eventsChannel)
            )
        }
    }

    fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        title: String,
        description: String,
        category: String,
        isPrayer: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = when {
            isPrayer -> CHANNEL_PRAYER
            category.equals("Ibadat", ignoreCase = true) -> CHANNEL_IBADAT
            category.equals("Event", ignoreCase = true) -> CHANNEL_EVENTS
            else -> CHANNEL_ALARMS
        }

        // Full Screen Intent
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActivity.EXTRA_TITLE, title)
            putExtra(AlarmActivity.EXTRA_DESC, description)
            putExtra(AlarmActivity.EXTRA_CATEGORY, category)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action Intent
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_DISMISS
            putExtra(AlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 1).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10m Action Intent
        val snooze10Intent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmActionReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmActionReceiver.EXTRA_ALARM_TITLE, title)
            putExtra(AlarmActionReceiver.EXTRA_ALARM_CATEGORY, category)
            putExtra(AlarmActionReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snooze10PendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10 + 2).toInt(),
            snooze10Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_smart_alarm_logo)
            .setContentTitle(title)
            .setContentText(if (description.isNotEmpty()) description else "Alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_smart_alarm_logo, "Dismiss", dismissPendingIntent)
            .addAction(R.drawable.ic_smart_alarm_logo, "Snooze 10m", snooze10PendingIntent)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }

    fun cancelNotification(context: Context, alarmId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.toInt())
    }
}
