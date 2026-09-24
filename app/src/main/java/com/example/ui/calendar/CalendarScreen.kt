package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Alarm
import com.example.ui.alarms.AlarmItemCard
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    alarmViewModel: AlarmViewModel,
    onAddNewAlarmForDate: (Long) -> Unit,
    onEditAlarm: (Alarm) -> Unit
) {
    val allAlarms by alarmViewModel.allAlarms.collectAsState()

    var currentCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    var selectedDay by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    }

    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayHeaderFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    val selectedDateCalendar = remember(currentCalendar, selectedDay) {
        (currentCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, selectedDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
    }

    // Filter alarms for selected date
    val alarmsOnSelectedDate = remember(allAlarms, selectedDateCalendar) {
        val startOfDay = (selectedDateCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = (selectedDateCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        allAlarms.filter { it.triggerTime in startOfDay..endOfDay }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Navigation Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Text(
                            text = monthYearFormat.format(currentCalendar.time),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = {
                            val newCal = currentCalendar.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Days of week header (Sun - Sat)
                    val daysHeader = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        daysHeader.forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month days grid
                    val firstDayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                    val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val leadingBlanks = firstDayOfWeek - 1

                    val totalCells = leadingBlanks + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNumber = cellIndex - leadingBlanks + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = (dayNumber == selectedDay)
                                    // Check if any alarms on this day
                                    val hasAlarm = allAlarms.any {
                                        val cal = Calendar.getInstance().apply { timeInMillis = it.triggerTime }
                                        cal.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                                        cal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                                        cal.get(Calendar.DAY_OF_MONTH) == dayNumber
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .background(
                                                if (isSelected) PrimaryGreen else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable { selectedDay = dayNumber },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNumber",
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasAlarm) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(if (isSelected) AccentGold else PrimaryGreen, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Details & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dayHeaderFormat.format(selectedDateCalendar.time),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${alarmsOnSelectedDate.size} reminders scheduled",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onAddNewAlarmForDate(selectedDateCalendar.timeInMillis) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add for Date", fontSize = 12.sp)
                }
            }
        }

        // List of alarms on selected date
        if (alarmsOnSelectedDate.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reminders scheduled on this day.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(alarmsOnSelectedDate, key = { it.id }) { alarm ->
                AlarmItemCard(
                    alarm = alarm,
                    onToggle = { alarmViewModel.toggleAlarm(alarm.id, it) },
                    onEdit = { onEditAlarm(alarm) },
                    onDuplicate = { alarmViewModel.duplicateAlarm(alarm.id) },
                    onDelete = { alarmViewModel.deleteAlarm(alarm.id) }
                )
            }
        }
    }
}
