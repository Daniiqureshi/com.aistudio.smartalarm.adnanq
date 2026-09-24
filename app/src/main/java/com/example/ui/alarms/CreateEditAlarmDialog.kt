package com.example.ui.alarms

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Alarm
import com.example.domain.model.AlarmCategory
import com.example.domain.model.RepeatType
import com.example.domain.model.VibrationPattern
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditAlarmDialog(
    initialAlarm: Alarm? = null,
    initialDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val cal = remember {
        Calendar.getInstance().apply {
            if (initialAlarm != null) {
                timeInMillis = initialAlarm.triggerTime
            } else if (initialDateMillis != null) {
                timeInMillis = initialDateMillis
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
    }

    var title by remember { mutableStateOf(initialAlarm?.title ?: "") }
    var description by remember { mutableStateOf(initialAlarm?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialAlarm?.category ?: AlarmCategory.PERSONAL) }
    var selectedRepeat by remember { mutableStateOf(initialAlarm?.repeatType ?: RepeatType.NONE) }
    var intervalDays by remember { mutableIntStateOf(initialAlarm?.repeatIntervalDays ?: 3) }
    val customDaysOfWeek = remember {
        mutableStateListOf<Int>().apply {
            if (initialAlarm != null) addAll(initialAlarm.repeatDaysOfWeek)
        }
    }

    var soundName by remember { mutableStateOf(initialAlarm?.soundName ?: "Default Alarm") }
    var isVibrationEnabled by remember { mutableStateOf(initialAlarm?.isVibrationEnabled ?: true) }
    var vibrationPattern by remember { mutableStateOf(initialAlarm?.vibrationPattern ?: VibrationPattern.NORMAL) }

    var selectedYear by remember { mutableIntStateOf(cal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(cal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }

    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val displayDate = remember(selectedYear, selectedMonth, selectedDay) {
        val temp = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay)
        }
        dateFormat.format(temp.time)
    }

    val displayTime = remember(selectedHour, selectedMinute) {
        val temp = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        timeFormat.format(temp.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        title = "Reminder"
                    }
                    val finalCal = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val updated = Alarm(
                        id = initialAlarm?.id ?: 0L,
                        title = title.trim(),
                        description = description.trim(),
                        category = selectedCategory,
                        triggerTime = finalCal.timeInMillis,
                        repeatType = selectedRepeat,
                        repeatIntervalDays = intervalDays,
                        repeatDaysOfWeek = customDaysOfWeek.toList(),
                        isEnabled = true,
                        soundName = soundName,
                        isVibrationEnabled = isVibrationEnabled,
                        vibrationPattern = vibrationPattern,
                        isPrayerAlarm = initialAlarm?.isPrayerAlarm ?: false,
                        prayerName = initialAlarm?.prayerName
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("save_alarm_button")
            ) {
                Text(if (initialAlarm == null) stringResource(R.string.action_create) else stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        title = {
            Text(
                text = if (initialAlarm == null) stringResource(R.string.create_reminder_title) else stringResource(R.string.edit_reminder_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    placeholder = { Text(stringResource(R.string.field_title_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_title_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.field_description)) },
                    placeholder = { Text(stringResource(R.string.field_description_placeholder)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlarmCategory.entries.forEach { category ->
                        FilterChip(
                            selected = (selectedCategory == category),
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) },
                            modifier = Modifier.testTag("category_${category.name.lowercase()}")
                        )
                    }
                }

                // Date and Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        selectedYear = y
                                        selectedMonth = m
                                        selectedDay = d
                                    },
                                    selectedYear, selectedMonth, selectedDay
                                ).show()
                            }
                            .testTag("date_picker_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(displayDate, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Time card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        selectedHour = h
                                        selectedMinute = m
                                    },
                                    selectedHour, selectedMinute, false
                                ).show()
                            }
                            .testTag("time_picker_button"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(displayTime, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Repeat Selection
                Text(stringResource(R.string.field_repeat), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RepeatType.entries.forEach { repeat ->
                        FilterChip(
                            selected = (selectedRepeat == repeat),
                            onClick = { selectedRepeat = repeat },
                            label = { Text(repeat.label) }
                        )
                    }
                }

                // If CUSTOM_DAYS, show day of week selectors (Mon-Sun)
                if (selectedRepeat == RepeatType.CUSTOM_DAYS) {
                    Text("Select Days of Week", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    val daysLabels = listOf(
                        1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysLabels.forEach { (dayNum, label) ->
                            val isSelected = customDaysOfWeek.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                                    .clickable {
                                        if (isSelected) customDaysOfWeek.remove(dayNum)
                                        else customDaysOfWeek.add(dayNum)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Sound Selection
                Text(stringResource(R.string.field_sound), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                val soundOptions = listOf("Default Alarm", "Notification Chime", "Adhan Melody", "Temple Bell", "Gentle Sunrise Breeze")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    soundOptions.forEach { sound ->
                        FilterChip(
                            selected = (soundName == sound),
                            onClick = { soundName = sound },
                            label = { Text(sound) }
                        )
                    }
                }

                // Vibration Toggle & Pattern
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.field_vibration), fontSize = 14.sp)
                    }
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { isVibrationEnabled = it }
                    )
                }

                if (isVibrationEnabled) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VibrationPattern.entries.forEach { pattern ->
                            FilterChip(
                                selected = (vibrationPattern == pattern),
                                onClick = { vibrationPattern = pattern },
                                label = { Text(pattern.title) }
                            )
                        }
                    }
                }
            }
        }
    )
}
