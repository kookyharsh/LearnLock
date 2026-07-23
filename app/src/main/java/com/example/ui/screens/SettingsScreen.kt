package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferencesManager
import com.example.service.GeminiConceptGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefsManager = remember { AppPreferencesManager(context) }

    var apiKeyInput by remember { mutableStateOf(prefsManager.getApiKey()) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingKey by remember { mutableStateOf(false) }

    var windowEnabled by remember { mutableStateOf(prefsManager.isLearningWindowEnabled()) }
    var startTimeInput by remember { mutableStateOf(prefsManager.getLearningWindowStart()) }
    var endTimeInput by remember { mutableStateOf(prefsManager.getLearningWindowEnd()) }
    var skipDuringActive by remember { mutableStateOf(prefsManager.isSkipDuringActiveWindow()) }

    var selectedTopics by remember { mutableStateOf(prefsManager.getSelectedTopics().toMutableSet()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = ElegantPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Settings & Credentials",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Gemini API Credentials Card
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Api,
                        contentDescription = null,
                        tint = ElegantPrimary
                    )
                    Text(
                        text = "LLM API Credentials",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Encrypted key used to pre-generate adaptive learning concepts and code questions beforehand.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        prefsManager.setApiKey(it)
                    },
                    label = { Text("API Token / Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Visibility",
                                tint = TextMuted
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElegantPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedLabelColor = ElegantPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            isTestingKey = true
                            val generator = GeminiConceptGenerator(context, prefsManager)
                            val testResult = generator.generateBatchConcepts(setOf("React"), count = 1)
                            if (testResult.isNotEmpty()) {
                                Toast.makeText(context, "API Key Verified & Connected Successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Connection failed. Please verify Key.", Toast.LENGTH_SHORT).show()
                            }
                            isTestingKey = false
                        }
                    },
                    enabled = !isTestingKey && apiKeyInput.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElegantPrimary),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElegantPrimary)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingKey) {
                        CircularProgressIndicator(color = ElegantPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing Connection...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test API Key Connection", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Learning Schedule Window Preferences
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CodeBlue
                        )
                        Text(
                            text = "Learning Window Schedule",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = windowEnabled,
                        onCheckedChange = {
                            windowEnabled = it
                            prefsManager.setLearningWindowEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElegantOnPrimary,
                            checkedTrackColor = ElegantPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeInput,
                        onValueChange = {
                            startTimeInput = it
                            prefsManager.setLearningWindowStart(it)
                        },
                        label = { Text("Start Time") },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endTimeInput,
                        onValueChange = {
                            endTimeInput = it
                            prefsManager.setLearningWindowEnd(it)
                        },
                        label = { Text("End Time") },
                        placeholder = { Text("21:00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Allow optional skip during window",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Checkbox(
                        checked = skipDuringActive,
                        onCheckedChange = {
                            skipDuringActive = it
                            prefsManager.setSkipDuringActiveWindow(it)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ElegantPrimary,
                            checkmarkColor = ElegantOnPrimary
                        )
                    )
                }
            }
        }

        // My Topics Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Subject,
                        contentDescription = null,
                        tint = ElegantPrimary
                    )
                    Text(
                        text = "My Learning Topics",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add the subjects you want the LLM to generate questions for.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                var newTopicInput by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTopicInput,
                        onValueChange = { newTopicInput = it },
                        label = { Text("Add Custom Topic") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElegantPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (newTopicInput.isNotBlank()) {
                                val updated = selectedTopics.toMutableSet()
                                updated.add(newTopicInput.trim())
                                selectedTopics = updated
                                prefsManager.setSelectedTopics(updated)
                                newTopicInput = ""
                            }
                        },
                        modifier = Modifier.background(ElegantPrimary, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Topic", tint = ElegantOnPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedTopics.forEach { topic ->
                        AssistChip(
                            onClick = {
                                val updated = selectedTopics.toMutableSet()
                                updated.remove(topic)
                                selectedTopics = updated
                                prefsManager.setSelectedTopics(updated)
                            },
                            label = { Text(topic, fontSize = 12.sp) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove $topic", modifier = Modifier.size(16.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkBackground,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}
