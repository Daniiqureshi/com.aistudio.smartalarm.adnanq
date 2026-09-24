package com.example.ui.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.parser.NaturalLanguageParser
import com.example.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomRelativeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("2") }
    var selectedUnit by remember { mutableStateOf("Hours") }
    var title by remember { mutableStateOf("") }

    val units = listOf("Minutes", "Hours", "Days", "Weeks", "Months", "Years")

    val amount = amountText.toIntOrNull() ?: 1
    val previewTime = remember(amount, selectedUnit) {
        val millis = NaturalLanguageParser.calculateRelative(amount, selectedUnit)
        val format = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
        format.format(Date(millis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Relative Reminder", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Label") },
                    placeholder = { Text("e.g. Call client, Dentist, Meeting") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text("Select Time Unit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    units.forEach { unit ->
                        FilterChip(
                            selected = (selectedUnit == unit),
                            onClick = { selectedUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scheduled for:\n$previewTime",
                    fontSize = 12.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(amount, selectedUnit, title)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Set Alarm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
