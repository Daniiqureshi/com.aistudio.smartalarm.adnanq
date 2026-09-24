package com.example.services

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object SoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun play(context: Context, soundName: String = "Default Alarm", enableVibration: Boolean = true, pattern: String = "NORMAL") {
        stop()

        try {
            val soundUri: Uri = when (soundName) {
                "Notification Chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Failed to start media player", e)
        }

        if (enableVibration) {
            startVibration(context, pattern)
        }
    }

    private fun startVibration(context: Context, pattern: String) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = vib

        val timings: LongArray = when (pattern) {
            "SHORT" -> longArrayOf(0, 200, 200, 200)
            "STRONG" -> longArrayOf(0, 800, 200, 800)
            else -> longArrayOf(0, 500, 300, 500)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib?.vibrate(VibrationEffect.createWaveform(timings, 0)) // 0 = repeat from index 0
        } else {
            @Suppress("DEPRECATION")
            vib?.vibrate(timings, 0)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Error stopping mediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Error cancelling vibrator", e)
        } finally {
            vibrator = null
        }
    }
}
