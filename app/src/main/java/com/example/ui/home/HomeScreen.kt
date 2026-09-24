package com.example.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Alarm
import com.example.domain.model.NextPrayerResult
import com.example.domain.model.PrayerTimes
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryGreen
import com.example.ui.viewmodel.AlarmViewModel
import com.example.ui.viewmodel.PrayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    alarmViewModel: AlarmViewModel,
    prayerViewModel: PrayerViewModel,
    onAddNewAlarm: () -> Unit,
    onOpenVoiceDialog: () -> Unit,
    onOpenIbadatTab: () -> Unit,
    onOpenCustomRelativeDialog: () -> Unit
) {
    val context = LocalContext.current
    val nextAlarm by alarmViewModel.nextUpcomingAlarm.collectAsState()
    val prayerTimes by prayerViewModel.prayerTimes.collectAsState()
    val nextPrayer by prayerViewModel.nextPrayer.collectAsState()
    val isLoadingPrayer by prayerViewModel.isLoading.collectAsState()

    val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Branding Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_smart_alarm_logo),
                    contentDescription = "SmartAlarm Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // WhatsApp Icon visible right in front (user requested WhatsApp icon in front, no phone number)
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/923109340486"))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                        .testTag("home_whatsapp_icon")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Next Alarm Hero Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("next_alarm_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    PrimaryGreen.copy(alpha = 0.08f),
                                    AccentGold.copy(alpha = 0.04f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.home_next_alarm).uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (nextAlarm != null) {
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryGreen, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = nextAlarm!!.category.displayName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (nextAlarm != null) {
                            val alarm = nextAlarm!!
                            val remaining = formatRemaining(alarm.triggerTime)

                            Text(
                                text = alarm.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (alarm.description.isNotEmpty()) {
                                Text(
                                    text = alarm.description,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = timeFormat.format(Date(alarm.triggerTime)),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${dateFormat.format(Date(alarm.triggerTime))} • $remaining",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AccentGold
                                    )
                                }

                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { alarmViewModel.toggleAlarm(alarm.id, it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.home_no_upcoming_alarms),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.home_set_alarm_now),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onAddNewAlarm,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_add_reminder))
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Relative Alarms Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Quick Relative Reminder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Custom +",
                            color = PrimaryGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenCustomRelativeDialog() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickChip(label = "+5m") { alarmViewModel.createQuickRelativeAlarm("5 min reminder", 5) }
                        QuickChip(label = "+15m") { alarmViewModel.createQuickRelativeAlarm("15 min reminder", 15) }
                        QuickChip(label = "+30m") { alarmViewModel.createQuickRelativeAlarm("30 min reminder", 30) }
                        QuickChip(label = "+1h") { alarmViewModel.createQuickRelativeAlarm("1 hour reminder", 60) }
                        QuickChip(label = "+2h") { alarmViewModel.createQuickRelativeAlarm("2 hours reminder", 120) }
                        QuickChip(label = "Tomorrow") { alarmViewModel.createCustomRelativeAlarm(1, "days", "Tomorrow reminder") }
                        QuickChip(label = "1 Week") { alarmViewModel.createCustomRelativeAlarm(1, "weeks", "1 week reminder") }
                        QuickChip(label = "1 Month") { alarmViewModel.createCustomRelativeAlarm(1, "months", "1 month reminder") }
                    }
                }
            }
        }

        // 4. Natural Language & Voice Assistant Prompt Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVoiceDialog() }
                    .testTag("voice_assistant_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(PrimaryGreen.copy(alpha = 0.4f), AccentGold.copy(alpha = 0.4f)))
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(listOf(PrimaryGreen, SecondaryGreen)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart & Voice Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Say: \"2 weeks later meeting\" or tap to speak",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 5. Today's Prayer Times Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.home_todays_prayer_times),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        nextPrayer?.let { np ->
                            Text(
                                text = "⏳ ${np.countdownText}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGold
                            )
                        }
                    }

                    Text(
                        text = "View All →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen,
                        modifier = Modifier.clickable { onOpenIbadatTab() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoadingPrayer && prayerTimes == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                } else {
                    prayerTimes?.let { pt ->
                        PrayerTimesGrid(
                            prayerTimes = pt,
                            nextPrayerName = nextPrayer?.prayerName ?: "Fajr",
                            onPrayerClick = { name, time ->
                                prayerViewModel.schedulePrayerAlarm(name, time)
                            }
                        )
                    }
                }
            }
        }

        // 6. Developer Information Card (Adnan Maqbool Qureshi)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_developer_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(PrimaryGreen, SecondaryGreen)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AQ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.developer_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.developer_title),
                            fontSize = 12.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/923109340486"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("home_developer_whatsapp_button")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.developer_chat_whatsapp_short),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(PrimaryGreen.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = PrimaryGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PrayerTimesGrid(
    prayerTimes: PrayerTimes,
    nextPrayerName: String,
    onPrayerClick: (String, String) -> Unit
) {
    val prayers = listOf(
        Triple("Fajr", prayerTimes.fajr, Icons.Default.Bedtime),
        Triple("Sunrise", prayerTimes.sunrise, Icons.Default.WbSunny),
        Triple("Dhuhr", prayerTimes.dhuhr, Icons.Default.WbSunny),
        Triple("Asr", prayerTimes.asr, Icons.Default.WbSunny),
        Triple("Maghrib", prayerTimes.maghrib, Icons.Default.Bedtime),
        Triple("Isha", prayerTimes.isha, Icons.Default.Bedtime)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prayers.forEach { (name, time, icon) ->
            val isNext = name.equals(nextPrayerName, ignoreCase = true)
            Card(
                modifier = Modifier
                    .width(86.dp)
                    .clickable { onPrayerClick(name, time) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNext) PrimaryGreen else MaterialTheme.colorScheme.surface
                ),
                border = if (isNext) null else CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (isNext) AccentGold else PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                        color = if (isNext) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = time.take(5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNext) AccentGold else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatRemaining(triggerTime: Long): String {
    val remaining = triggerTime - System.currentTimeMillis()
    if (remaining <= 0) return "Due now"
    val hours = remaining / (1000 * 60 * 60)
    val minutes = (remaining / (1000 * 60)) % 60
    val days = hours / 24

    return when {
        days > 1 -> "${days}d remaining"
        hours > 0 -> "${hours}h ${minutes}m remaining"
        else -> "${minutes}m remaining"
    }
}
