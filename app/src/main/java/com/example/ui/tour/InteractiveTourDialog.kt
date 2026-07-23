package com.example.ui.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.NavTab
import com.example.data.preferences.AppPreferencesManager
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)
@Composable
fun InteractiveTourDialog(
    onDismiss: () -> Unit,
    onNavigateTab: (NavTab) -> Unit
) {
    val context = LocalContext.current
    val prefsManager = remember { AppPreferencesManager(context) }

    var currentStep by remember { mutableIntStateOf(1) } // Steps 1 to 4
    var selectedTopics by remember { mutableStateOf(prefsManager.getSelectedTopics().toMutableSet()) }
    var newTopicText by remember { mutableStateOf("") }

    val presetTopics = remember {
        listOf(
            "Computer Science", "Data Structures", "React", "Python", "SQL",
            "World History", "Geography", "Biology", "Astronomy",
            "Literature", "Psychology", "Economics", "Android Dev"
        )
    }

    Dialog(
        onDismissRequest = {
            prefsManager.setTourCompleted(true)
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = DarkSurface,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(ElegantPrimary)
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Tour Top Header & Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = ElegantPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "INTERACTIVE TOUR",
                            color = ElegantPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            prefsManager.setTourCompleted(true)
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tour",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Step Dots / Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (step <= currentStep) ElegantPrimary else DarkBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Step Content
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    },
                    label = "TourStepAnimation"
                ) { step ->
                    when (step) {
                        1 -> Step1ChooseConcepts(
                            selectedTopics = selectedTopics,
                            presetTopics = presetTopics,
                            newTopicText = newTopicText,
                            onNewTopicChange = { newTopicText = it },
                            onAddCustomTopic = {
                                if (newTopicText.isNotBlank()) {
                                    val updated = selectedTopics.toMutableSet()
                                    updated.add(newTopicText.trim())
                                    selectedTopics = updated
                                    prefsManager.setSelectedTopics(updated)
                                    newTopicText = ""
                                }
                            },
                            onToggleTopic = { topic ->
                                val updated = selectedTopics.toMutableSet()
                                if (updated.contains(topic)) {
                                    if (updated.size > 1) updated.remove(topic)
                                } else {
                                    updated.add(topic)
                                }
                                selectedTopics = updated
                                prefsManager.setSelectedTopics(updated)
                            }
                        )

                        2 -> Step2ShowHistory(
                            onPreviewTab = { onNavigateTab(NavTab.HISTORY) }
                        )

                        3 -> Step3ShowSettings(
                            onPreviewTab = { onNavigateTab(NavTab.SETTINGS) }
                        )

                        4 -> Step4TourComplete()
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tour Navigation Buttons Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        TextButton(
                            onClick = { currentStep-- },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Back", fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < 4) {
                                currentStep++
                            } else {
                                prefsManager.setTourCompleted(true)
                                onNavigateTab(NavTab.LEARN)
                                onDismiss()
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantPrimary,
                            contentColor = ElegantOnPrimary
                        )
                    ) {
                        Text(
                            text = if (currentStep < 4) "Next Step" else "Get Started!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep < 4) Icons.Default.NavigateNext else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1ChooseConcepts(
    selectedTopics: Set<String>,
    presetTopics: List<String>,
    newTopicText: String,
    onNewTopicChange: (String) -> Unit,
    onAddCustomTopic: () -> Unit,
    onToggleTopic: (String) -> Unit
) {
    Column {
        Text(
            text = "Step 1: Choose Your Learning Topics",
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select what concepts you want to learn. Our AI automatically generates detailed summaries and quizzes based on your active subjects whenever you unlock your phone.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Topic Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newTopicText,
                onValueChange = onNewTopicChange,
                placeholder = { Text("Add custom topic (e.g. Quantum Physics)", fontSize = 13.sp) },
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
                onClick = onAddCustomTopic,
                modifier = Modifier.background(ElegantPrimary, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic", tint = ElegantOnPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tap to toggle topics:",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preset and Selected Topic Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Combine presets and custom selected topics
            val allTopics = (presetTopics + selectedTopics).distinct()

            allTopics.forEach { topic ->
                val isSelected = selectedTopics.contains(topic)

                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleTopic(topic) },
                    label = {
                        Text(
                            text = topic,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElegantPrimary,
                        selectedLabelColor = ElegantOnPrimary,
                        containerColor = DarkBackground,
                        labelColor = TextSecondary
                    ),
                    shape = CircleShape
                )
            }
        }
    }
}

@Composable
private fun Step2ShowHistory(
    onPreviewTab: () -> Unit
) {
    Column {
        Text(
            text = "Step 2: Where Is Your History?",
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "You can review every concept and quiz question you've ever answered right in the History tab at the bottom navigation bar.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // History Tab Highlight Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkBackground,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(CodeBlue)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CodeBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CodeBlue
                        )
                    }

                    Column {
                        Text(
                            text = "History Tab (Bottom Bar)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Access via the 🕒 icon anytime",
                            color = CodeBlue,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TourFeatureItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Attempt Records",
                    description = "Stores all passed and missed questions with timestamp log."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TourFeatureItem(
                    icon = Icons.Default.Subject,
                    title = "Filter by Subject or Status",
                    description = "Quickly isolate questions by topic or retry pending status."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TourFeatureItem(
                    icon = Icons.Default.Quiz,
                    title = "One-Tap Question Retry",
                    description = "Practice missed questions on demand to reinforce long-term memory."
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onPreviewTab,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CodeBlue),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CodeBlue)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch & Preview History Tab Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Step3ShowSettings(
    onPreviewTab: () -> Unit
) {
    Column {
        Text(
            text = "Step 3: What Settings Are Available?",
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Customize your learning active hours, Gemini API token, and screen lock overlay preferences in the API & Setup screen.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Tab Highlight Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkBackground,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(ElegantPrimary)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ElegantPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = ElegantPrimary
                        )
                    }

                    Column {
                        Text(
                            text = "API & Setup Tab (Bottom Bar)",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Access via the 🔑 icon anytime",
                            color = ElegantPrimary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TourFeatureItem(
                    icon = Icons.Default.Schedule,
                    title = "Learning Schedule Window",
                    description = "Set start and end times (e.g. 09:00 - 21:00) so quizzes only trigger during active study hours."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TourFeatureItem(
                    icon = Icons.Default.Key,
                    title = "Gemini API Token Key",
                    description = "Add or test your custom key to generate infinite concepts."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TourFeatureItem(
                    icon = Icons.Default.Subject,
                    title = "Subject Management",
                    description = "Add, edit, or remove subjects anytime from Settings."
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onPreviewTab,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElegantPrimary),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ElegantPrimary)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Switch & Preview Settings Tab Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Step4TourComplete() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ElegantPrimary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ElegantPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "You're All Set!",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your personalized learning setup is active. Every time you unlock your device, UnlockLearn will present an engaging 200-300 word concept and quick practice question to level up your knowledge daily!",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💡 Quick Tip:",
                    color = ElegantPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You can re-take this Interactive Tour anytime by tapping 'Take Interactive Tour' in the Settings tab.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun TourFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
