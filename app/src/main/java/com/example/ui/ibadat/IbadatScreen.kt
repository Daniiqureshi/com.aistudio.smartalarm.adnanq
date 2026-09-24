package com.example.ui.ibadat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Alarm
import com.example.domain.model.AlarmCategory
import com.example.domain.model.SpiritualPractice
import com.example.domain.model.TraditionType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryGreen
import com.example.ui.viewmodel.AlarmViewModel
import com.example.ui.viewmodel.PrayerViewModel
import com.example.ui.viewmodel.SettingsViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IbadatScreen(
    prayerViewModel: PrayerViewModel,
    alarmViewModel: AlarmViewModel,
    settingsViewModel: SettingsViewModel
) {
    val prayerTimes by prayerViewModel.prayerTimes.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val userMessage by prayerViewModel.userMessage.collectAsState()

    var selectedTraditionTab by remember { mutableStateOf(TraditionType.ISLAM) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ibadat_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tradition Category Tabs (Islam, Christianity, Hinduism, Sikhism, Buddhism, Judaism, Custom)
        item {
            ScrollableTabRow(
                selectedTabIndex = TraditionType.entries.indexOf(selectedTraditionTab),
                edgePadding = 0.dp,
                divider = {}
            ) {
                TraditionType.entries.forEach { tradition ->
                    Tab(
                        selected = (selectedTraditionTab == tradition),
                        onClick = { selectedTraditionTab = tradition },
                        text = {
                            Text(
                                text = tradition.displayName,
                                fontWeight = if (selectedTraditionTab == tradition) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        if (selectedTraditionTab == TraditionType.ISLAM) {
            // Next Prayer Banner Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(PrimaryGreen, SecondaryGreen)))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mosque, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Daily Prayer Schedule",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = settings.prayerCity,
                                    color = AccentGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            nextPrayer?.let { np ->
                                Text(
                                    text = "Upcoming: ${np.prayerName} at ${np.time}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "⏳ ${np.countdownText}",
                                    color = AccentGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Daily 5 Prayers list with Adhan Alarms
            prayerTimes?.let { pt ->
                val prayerList = listOf(
                    Triple("Fajr", pt.fajr, settings.isFajrAdhanEnabled),
                    Triple("Dhuhr", pt.dhuhr, settings.isDhuhrAdhanEnabled),
                    Triple("Asr", pt.asr, settings.isAsrAdhanEnabled),
                    Triple("Maghrib", pt.maghrib, settings.isMaghribAdhanEnabled),
                    Triple("Isha", pt.isha, settings.isIshaAdhanEnabled)
                )

                item {
                    Text(
                        text = "Five Daily Prayers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(prayerList) { (name, time, isEnabled) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Azan: $time", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { prayerViewModel.schedulePrayerAlarm(name, time) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Set Alarm", fontSize = 12.sp)
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { settingsViewModel.toggleAdhan(name, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                                )
                            }
                        }
                    }
                }

                // Ramadan Specials: Sehri & Iftar Card
                item {
                    Text(
                        text = "🌙 Ramadan & Fasting Reminders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Sehri / Imsak
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sehri Cutoff", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text(pt.imsak, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGreen)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { prayerViewModel.schedulePrayerAlarm("Sehri", pt.imsak) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Set Sehri Alarm", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Iftar / Maghrib
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Iftar Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text(pt.maghrib, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AccentGold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { prayerViewModel.schedulePrayerAlarm("Iftar", pt.maghrib) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Set Iftar Alarm", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tahajjud Special Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tahajjud (Night Vigil)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Recommended: ${pt.tahajjud} (Last third of night)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Button(
                                    onClick = { prayerViewModel.scheduleTahajjudAlarm(pt.tahajjud) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Set Alarm", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Jummah Mubarak Reminder Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Jummah Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Every Friday: Preparation & Surah Al-Kahf", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { prayerViewModel.scheduleJummahAlarm() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Set Friday", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Other Traditions & Custom Mindfulness
            val practices = SpiritualPractice.getPredefinedPractices(selectedTraditionTab)

            item {
                Text(
                    text = "${selectedTraditionTab.displayName} Reminders & Practices",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(practices) { practice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(practice.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(practice.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Suggested: ${practice.suggestedTime} • ${practice.repeatType.label}", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = {
                                    val parts = practice.suggestedTime.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                    }
                                    if (cal.timeInMillis <= System.currentTimeMillis()) {
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    val alarm = Alarm(
                                        title = practice.title,
                                        description = practice.description,
                                        category = AlarmCategory.IBADAT,
                                        triggerTime = cal.timeInMillis,
                                        repeatType = practice.repeatType,
                                        soundName = "Temple Bell",
                                        isEnabled = true
                                    )
                                    alarmViewModel.saveAlarm(alarm)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Set Reminder", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
