package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartAlarmApp
import com.example.data.repository.AlarmRepository
import com.example.domain.model.Alarm
import com.example.domain.model.AlarmCategory
import com.example.domain.model.RepeatType
import com.example.domain.model.VibrationPattern
import com.example.domain.parser.NaturalLanguageParser
import com.example.domain.parser.ParsedReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AlarmFilterTab {
    UPCOMING, TODAY, PAST, DISABLED
}

class AlarmViewModel(
    private val repository: AlarmRepository = SmartAlarmApp.instance.alarmRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(AlarmFilterTab.UPCOMING)
    val selectedTab: StateFlow<AlarmFilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val nextUpcomingAlarm: StateFlow<Alarm?> = repository.getNextUpcomingAlarmFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allAlarms: StateFlow<List<Alarm>> = repository.getAllAlarmsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered list based on tab and search
    val filteredAlarms: StateFlow<List<Alarm>> = combine(
        repository.getAllAlarmsFlow(),
        _selectedTab,
        _searchQuery
    ) { alarms, tab, query ->
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val tabFiltered = when (tab) {
            AlarmFilterTab.UPCOMING -> alarms.filter { it.isEnabled && it.triggerTime >= now }
            AlarmFilterTab.TODAY -> alarms.filter { it.triggerTime in startOfToday..endOfToday }
            AlarmFilterTab.PAST -> alarms.filter { it.triggerTime < now && it.repeatType == RepeatType.NONE }
            AlarmFilterTab.DISABLED -> alarms.filter { !it.isEnabled }
        }

        if (query.isBlank()) {
            tabFiltered
        } else {
            tabFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.displayName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: AlarmFilterTab) {
        _selectedTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            if (alarm.id == 0L) {
                repository.insertAlarm(alarm)
                _statusMessage.value = "Alarm created successfully"
            } else {
                repository.updateAlarm(alarm)
                _statusMessage.value = "Alarm updated"
            }
        }
    }

    fun toggleAlarm(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(id, isEnabled)
        }
    }

    fun deleteAlarm(id: Long) {
        viewModelScope.launch {
            repository.deleteAlarm(id)
            _statusMessage.value = "Alarm removed"
        }
    }

    fun duplicateAlarm(id: Long) {
        viewModelScope.launch {
            repository.duplicateAlarm(id)
            _statusMessage.value = "Alarm duplicated"
        }
    }

    // Quick relative alarms (e.g. 5 min, 10 min, 1 hour, tomorrow, etc.)
    fun createQuickRelativeAlarm(title: String, durationMinutes: Int) {
        val trigger = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val alarm = Alarm(
            title = title,
            description = "Quick $durationMinutes minute reminder",
            category = AlarmCategory.PERSONAL,
            triggerTime = trigger,
            repeatType = RepeatType.NONE,
            isEnabled = true
        )
        saveAlarm(alarm)
    }

    fun createCustomRelativeAlarm(amount: Int, unit: String, title: String) {
        val trigger = NaturalLanguageParser.calculateRelative(amount, unit)
        val alarm = Alarm(
            title = if (title.isBlank()) "Quick Reminder" else title,
            description = "Scheduled for $amount $unit later",
            category = AlarmCategory.PERSONAL,
            triggerTime = trigger,
            repeatType = RepeatType.NONE,
            isEnabled = true
        )
        saveAlarm(alarm)
    }

    fun saveParsedReminder(parsed: ParsedReminder) {
        val alarm = Alarm(
            title = parsed.title,
            description = "Created via Smart Reminder",
            category = AlarmCategory.PERSONAL,
            triggerTime = parsed.triggerTimeMillis,
            repeatType = RepeatType.NONE,
            isEnabled = true
        )
        saveAlarm(alarm)
    }
}
