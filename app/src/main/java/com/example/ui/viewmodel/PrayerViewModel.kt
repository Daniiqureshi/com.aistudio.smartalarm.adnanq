package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartAlarmApp
import com.example.data.repository.AlarmRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.Alarm
import com.example.domain.model.AlarmCategory
import com.example.domain.model.NextPrayerResult
import com.example.domain.model.PrayerTimes
import com.example.domain.model.RepeatType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class PrayerViewModel(
    private val prayerRepository: PrayerRepository = SmartAlarmApp.instance.prayerRepository,
    private val alarmRepository: AlarmRepository = SmartAlarmApp.instance.alarmRepository,
    private val settingsRepository: SettingsRepository = SmartAlarmApp.instance.settingsRepository
) : ViewModel() {

    private val _prayerTimes = MutableStateFlow<PrayerTimes?>(null)
    val prayerTimes: StateFlow<PrayerTimes?> = _prayerTimes.asStateFlow()

    private val _nextPrayer = MutableStateFlow<NextPrayerResult?>(null)
    val nextPrayer: StateFlow<NextPrayerResult?> = _nextPrayer.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        loadPrayerTimes()
        startCountdownTicker()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _isLoading.value = true
            val settings = settingsRepository.settingsFlow.value
            val times = prayerRepository.getPrayerTimes(
                city = settings.prayerCity,
                country = settings.prayerCountry,
                latitude = settings.latitude,
                longitude = settings.longitude,
                useGps = settings.useGps,
                method = settings.calculationMethod,
                school = settings.madhab
            )
            _prayerTimes.value = times
            _nextPrayer.value = times.getNextPrayerInfo()
            _isLoading.value = false
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                _prayerTimes.value?.let { times ->
                    _nextPrayer.value = times.getNextPrayerInfo()
                }
                delay(30000) // update every 30 seconds
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    /**
     * Creates an alarm for a specific prayer time (e.g. Fajr at 05:15)
     */
    fun schedulePrayerAlarm(prayerName: String, timeString: String) {
        viewModelScope.launch {
            val parts = timeString.trim().take(5).split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: 5
                val minute = parts[1].toIntOrNull() ?: 0

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val alarm = Alarm(
                    title = "$prayerName Prayer",
                    description = "Daily $prayerName Ibadat reminder",
                    category = AlarmCategory.IBADAT,
                    triggerTime = cal.timeInMillis,
                    repeatType = RepeatType.DAILY,
                    soundName = "Adhan Melody",
                    isPrayerAlarm = true,
                    prayerName = prayerName,
                    isEnabled = true
                )

                alarmRepository.insertAlarm(alarm)
                _userMessage.value = "Scheduled daily $prayerName reminder at ${timeString.take(5)}"
            }
        }
    }

    fun scheduleTahajjudAlarm(tahajjudTime: String, offsetMinutes: Int = 0) {
        viewModelScope.launch {
            val parts = tahajjudTime.trim().take(5).split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: 3
                val minute = parts[1].toIntOrNull() ?: 45

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -offsetMinutes)
                }

                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val alarm = Alarm(
                    title = "Tahajjud Reminder",
                    description = "Last third of the night prayer",
                    category = AlarmCategory.IBADAT,
                    triggerTime = cal.timeInMillis,
                    repeatType = RepeatType.DAILY,
                    soundName = "Gentle Sunrise Breeze",
                    isPrayerAlarm = true,
                    prayerName = "Tahajjud",
                    isEnabled = true
                )

                alarmRepository.insertAlarm(alarm)
                _userMessage.value = "Tahajjud alarm set for tomorrow morning"
            }
        }
    }

    fun scheduleJummahAlarm(hour: Int = 12, minute: Int = 30) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (cal.timeInMillis <= System.currentTimeMillis() || cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val alarm = Alarm(
                title = "Jummah Mubarak",
                description = "Friday Prayer Preparation & Surah Al-Kahf",
                category = AlarmCategory.IBADAT,
                triggerTime = cal.timeInMillis,
                repeatType = RepeatType.WEEKLY,
                soundName = "Adhan Melody",
                isEnabled = true
            )

            alarmRepository.insertAlarm(alarm)
            _userMessage.value = "Weekly Jummah reminder scheduled for Friday"
        }
    }
}
