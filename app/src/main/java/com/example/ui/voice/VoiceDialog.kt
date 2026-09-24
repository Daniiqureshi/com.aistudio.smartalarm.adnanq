package com.example.ui.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.parser.NaturalLanguageParser
import com.example.domain.parser.ParsedReminder
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryGreen
import java.util.Locale

@Composable
fun VoiceDialog(
    onDismiss: () -> Unit,
    onConfirmParsed: (ParsedReminder) -> Unit,
    onEditParsed: (ParsedReminder) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedReminder?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
                parsedResult = NaturalLanguageParser.parse(spoken)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Smart & Voice Reminder", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Speak or type natural language, e.g.:\n• \"Tomorrow 8 PM doctor appointment\"\n• \"2 weeks later meeting\"\n• \"In 30 minutes call mom\"",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input field + Mic & Send icons
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        parsedResult = if (it.isNotBlank()) NaturalLanguageParser.parse(it) else null
                    },
                    placeholder = { Text("Type or click mic to speak...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nlp_text_input"),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your reminder...")
                                    }
                                    speechLauncher.launch(intent)
                                },
                                modifier = Modifier.testTag("mic_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = PrimaryGreen)
                            }
                        }
                    }
                )

                // Preview Card if parsed
                parsedResult?.let { parsed ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("I understood:", fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 13.sp)
                                Text(
                                    text = if (parsed.isConfident) "High Confidence" else "Needs Review",
                                    fontSize = 11.sp,
                                    color = if (parsed.isConfident) PrimaryGreen else AccentGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Title: ${parsed.title}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Date: ${parsed.dateDisplay}", fontSize = 13.sp)
                            Text("Time: ${parsed.timeDisplay}", fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onEditParsed(parsed) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                Button(
                                    onClick = { onConfirmParsed(parsed) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    modifier = Modifier.testTag("confirm_nlp_alarm_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirm")
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
