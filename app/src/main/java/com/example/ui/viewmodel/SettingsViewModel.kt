package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartAlarmApp
import com.example.data.repository.AlarmRepository
import com.example.data.repository.AppLanguage
import com.example.data.repository.AppTheme
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = SmartAlarmApp.instance.settingsRepository,
    private val alarmRepository: AlarmRepository = SmartAlarmApp.instance.alarmRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _jsonBackup = MutableStateFlow<String?>(null)
    val jsonBackup: StateFlow<String?> = _jsonBackup.asStateFlow()

    init {
        refreshCurrentUser()
    }

    fun refreshCurrentUser() {
        _currentUser.value = try {
            if (com.google.firebase.FirebaseApp.getApps(SmartAlarmApp.instance).isNotEmpty()) {
                Firebase.auth.currentUser
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun updateTheme(theme: AppTheme) {
        settingsRepository.updateTheme(theme)
    }

    fun updateLanguage(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
    }

    fun updateCity(city: String, country: String, lat: Double, lng: Double) {
        settingsRepository.updateCity(city, country, lat, lng)
    }

    fun updateGpsLocation(lat: Double, lng: Double) {
        settingsRepository.updateGpsLocation(lat, lng)
    }

    fun updateCalculationMethod(method: Int, madhab: Int) {
        settingsRepository.updateCalculationMethod(method, madhab)
    }

    fun toggleAdhan(prayer: String, enabled: Boolean) {
        settingsRepository.updateAdhanToggle(prayer, enabled)
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _syncStatus.value = "Synchronizing with Cloud Firestore..."
            val result = alarmRepository.syncWithCloud()
            if (result.isSuccess) {
                _syncStatus.value = "Synced ${result.getOrNull()} items successfully"
            } else {
                _syncStatus.value = "Cloud sync failed: ${result.exceptionOrNull()?.message ?: "Check connection"}"
            }
        }
    }

    fun generateJsonExport() {
        viewModelScope.launch {
            val json = alarmRepository.exportToJson()
            _jsonBackup.value = json
        }
    }

    fun clearJsonBackup() {
        _jsonBackup.value = null
    }

    fun importJsonBackup(json: String) {
        viewModelScope.launch {
            try {
                val count = alarmRepository.importFromJson(json)
                _syncStatus.value = "Successfully imported $count alarms from JSON"
            } catch (e: Exception) {
                _syncStatus.value = "Failed to import JSON: ${e.message}"
            }
        }
    }

    fun onAuthChanged() {
        refreshCurrentUser()
    }

    fun signOut() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(SmartAlarmApp.instance).isNotEmpty()) {
                Firebase.auth.signOut()
            }
        } catch (e: Throwable) {
            // Ignore if Firebase is not active
        }
        _currentUser.value = null
        _syncStatus.value = "Signed out. Operating in local guest mode."
    }
}
