package com.example.ui.alarms

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Alarm
import com.example.domain.model.RepeatType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.AlarmFilterTab
import com.example.ui.viewmodel.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmsScreen(
    alarmViewModel: AlarmViewModel,
    onEditAlarm: (Alarm) -> Unit,
    onAddNewAlarm: () -> Unit
) {
    val selectedTab by alarmViewModel.selectedTab.collectAsState()
    val searchQuery by alarmViewModel.searchQuery.collectAsState()
    val alarms by alarmViewModel.filteredAlarms.collectAsState()

    val tabs = listOf(
        AlarmFilterTab.UPCOMING to stringResource(R.string.tab_upcoming),
        AlarmFilterTab.TODAY to stringResource(R.string.tab_today),
        AlarmFilterTab.PAST to stringResource(R.string.tab_past),
        AlarmFilterTab.DISABLED to stringResource(R.string.tab_disabled)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alarms_screen")
    ) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { alarmViewModel.updateSearchQuery(it) },
            placeholder = { Text(stringResource(R.string.search_alarms_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { alarmViewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_alarms_input")
        )

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEach { (tab, label) ->
                Tab(
                    selected = (selectedTab == tab),
                    onClick = { alarmViewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Alarms List
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_alarms_found),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
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
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    val date = Date(alarm.triggerTime)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("alarm_item_${alarm.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & AM/PM
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(date),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmFormat.format(date),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Switch + More Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen),
                        modifier = Modifier.testTag("toggle_alarm_${alarm.id}")
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate)) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title & Description
            Text(
                text = alarm.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (alarm.description.isNotEmpty()) {
                Text(
                    text = alarm.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags row: Date, Category, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date tag
                Text(
                    text = dateFormat.format(date),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category badge
                Box(
                    modifier = Modifier
                        .background(PrimaryGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = alarm.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }

                // Repeat tag
                if (alarm.repeatType != RepeatType.NONE) {
                    Row(
                        modifier = Modifier
                            .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = alarm.repeatType.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentGold
                        )
                    }
                }
            }
        }
    }
}
