package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppTheme
import com.example.domain.model.Alarm
import com.example.ui.alarms.AlarmsScreen
import com.example.ui.alarms.CreateEditAlarmDialog
import com.example.ui.alarms.CustomRelativeDialog
import com.example.ui.calendar.CalendarScreen
import com.example.ui.home.HomeScreen
import com.example.ui.ibadat.IbadatScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SmartAlarmTheme
import com.example.ui.viewmodel.AlarmViewModel
import com.example.ui.viewmodel.PrayerViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.voice.VoiceDialog

sealed class Screen(val titleRes: Int, val icon: ImageVector, val tag: String) {
    object Home : Screen(R.string.nav_home, Icons.Default.Home, "nav_home")
    object Alarms : Screen(R.string.nav_alarms, Icons.Default.Alarm, "nav_alarms")
    object Ibadat : Screen(R.string.nav_ibadat, Icons.Default.Mosque, "nav_ibadat")
    object Calendar : Screen(R.string.nav_calendar, Icons.Default.CalendarMonth, "nav_calendar")
    object Settings : Screen(R.string.nav_settings, Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val prayerViewModel: PrayerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userSettings by settingsViewModel.settings.collectAsState()
            val isDarkTheme = when (userSettings.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            SmartAlarmTheme(darkTheme = isDarkTheme) {
                // Request Notification permission on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                MainAppContent(
                    alarmViewModel = alarmViewModel,
                    prayerViewModel = prayerViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}

@Composable
fun MainAppContent(
    alarmViewModel: AlarmViewModel,
    prayerViewModel: PrayerViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    val screens = listOf(Screen.Home, Screen.Alarms, Screen.Ibadat, Screen.Calendar, Screen.Settings)

    val snackbarHostState = remember { SnackbarHostState() }
    val alarmStatusMessage by alarmViewModel.statusMessage.collectAsState()
    val prayerUserMessage by prayerViewModel.userMessage.collectAsState()

    LaunchedEffect(alarmStatusMessage) {
        alarmStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            alarmViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(prayerUserMessage) {
        prayerUserMessage?.let {
            snackbarHostState.showSnackbar(it)
            prayerViewModel.clearMessage()
        }
    }

    // Dialog states
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var preselectedDateMillis by remember { mutableStateOf<Long?>(null) }

    var showVoiceDialog by remember { mutableStateOf(false) }
    var showCustomRelativeDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = (selectedScreenIndex == index),
                        onClick = { selectedScreenIndex = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            indicatorColor = PrimaryGreen
                        ),
                        modifier = Modifier.testTag(screen.tag)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedScreenIndex != 4) { // Don't show FAB on Settings
                FloatingActionButton(
                    onClick = {
                        editingAlarm = null
                        preselectedDateMillis = null
                        showCreateEditDialog = true
                    },
                    containerColor = PrimaryGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_alarm")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_reminder))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreenIndex) {
                0 -> HomeScreen(
                    alarmViewModel = alarmViewModel,
                    prayerViewModel = prayerViewModel,
                    onAddNewAlarm = {
                        editingAlarm = null
                        preselectedDateMillis = null
                        showCreateEditDialog = true
                    },
                    onOpenVoiceDialog = { showVoiceDialog = true },
                    onOpenIbadatTab = { selectedScreenIndex = 2 },
                    onOpenCustomRelativeDialog = { showCustomRelativeDialog = true }
                )
                1 -> AlarmsScreen(
                    alarmViewModel = alarmViewModel,
                    onAddNewAlarm = {
                        editingAlarm = null
                        preselectedDateMillis = null
                        showCreateEditDialog = true
                    },
                    onEditAlarm = { alarm ->
                        editingAlarm = alarm
                        preselectedDateMillis = null
                        showCreateEditDialog = true
                    }
                )
                2 -> IbadatScreen(
                    prayerViewModel = prayerViewModel,
                    alarmViewModel = alarmViewModel,
                    settingsViewModel = settingsViewModel
                )
                3 -> CalendarScreen(
                    alarmViewModel = alarmViewModel,
                    onAddNewAlarmForDate = { dateMillis ->
                        editingAlarm = null
                        preselectedDateMillis = dateMillis
                        showCreateEditDialog = true
                    },
                    onEditAlarm = { alarm ->
                        editingAlarm = alarm
                        preselectedDateMillis = null
                        showCreateEditDialog = true
                    }
                )
                4 -> SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onPrayerSettingsChanged = { prayerViewModel.loadPrayerTimes() }
                )
            }
        }
    }

    // Create / Edit Alarm Dialog
    if (showCreateEditDialog) {
        CreateEditAlarmDialog(
            initialAlarm = editingAlarm,
            initialDateMillis = preselectedDateMillis,
            onDismiss = { showCreateEditDialog = false },
            onSave = { alarm ->
                alarmViewModel.saveAlarm(alarm)
                showCreateEditDialog = false
            }
        )
    }

    // Voice / NLP Dialog
    if (showVoiceDialog) {
        VoiceDialog(
            onDismiss = { showVoiceDialog = false },
            onConfirmParsed = { parsed ->
                alarmViewModel.saveParsedReminder(parsed)
                showVoiceDialog = false
            },
            onEditParsed = { parsed ->
                showVoiceDialog = false
                editingAlarm = Alarm(
                    title = parsed.title,
                    triggerTime = parsed.triggerTimeMillis
                )
                showCreateEditDialog = true
            }
        )
    }

    // Custom Relative Dialog
    if (showCustomRelativeDialog) {
        CustomRelativeDialog(
            onDismiss = { showCustomRelativeDialog = false },
            onConfirm = { amount, unit, title ->
                alarmViewModel.createCustomRelativeAlarm(amount, unit, title)
                showCustomRelativeDialog = false
            }
        )
    }
}
