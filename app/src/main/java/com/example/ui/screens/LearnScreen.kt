package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.entity.QuestionHistory
import com.example.data.preferences.AppPreferencesManager
import com.example.service.GeminiConceptGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var isServiceEnabled by remember { mutableStateOf(prefsManager.isUnlockServiceEnabled()) }
    var isGeneratingConcepts by remember { mutableStateOf(value = false) }
    val recentHistory by db.historyDao().getRecentlyViewedHistory(10).collectAsState(initial = emptyList())
    var selectedDetailConcept by remember { mutableStateOf<QuestionHistory?>(null) }

    val windowStartRaw = prefsManager.getLearningWindowStart()
    val windowEndRaw = prefsManager.getLearningWindowEnd()

    val formatTime = { time: String ->
        try {
            if (time.contains("AM") || time.contains("PM")) {
                time
            } else {
                val sdf24 = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val sdf12 = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val date = sdf24.parse(time)
                if (date != null) sdf12.format(date) else time
            }
        } catch (_: Exception) {
            time
        }
    }

    val windowStart = formatTime(windowStartRaw)
    val windowEnd = formatTime(windowEndRaw)

    val triggerConceptGeneration: () -> Unit = {
        if (!isGeneratingConcepts) {
            val apiKey = prefsManager.getApiKey()
            if (apiKey.isBlank()) {
                Toast.makeText(context, "Please configure an API Key in Settings first!", Toast.LENGTH_LONG).show()
            } else {
                coroutineScope.launch {
                    isGeneratingConcepts = true
                    Toast.makeText(context, "Generating new concepts...", Toast.LENGTH_SHORT).show()
                    val generator = GeminiConceptGenerator(prefsManager)
                    val newConcepts = generator.generateBatchConcepts(
                        topics = prefsManager.getSelectedTopics(),
                        count = 3
                    )
                    if (newConcepts.isNotEmpty()) {
                        db.conceptDao().insertConcepts(newConcepts)
                        Toast.makeText(context, "Generated ${newConcepts.size} new concepts!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Generation failed. Verify API Key / Network in Settings.", Toast.LENGTH_LONG).show()
                    }
                    isGeneratingConcepts = false
                }
            }
        }
    }

    if (selectedDetailConcept != null) {
        ConceptDetailScreen(
            item = selectedDetailConcept!!,
            onBack = { selectedDetailConcept = null },
            onStarToggled = { newStarred ->
                val updated = selectedDetailConcept!!.copy(isStarred = newStarred)
                selectedDetailConcept = updated
                coroutineScope.launch {
                    db.historyDao().updateStarStatus(updated.id, newStarred)
                    db.conceptDao().updateStarStatusByTitle(updated.conceptTitle, newStarred)
                }
            }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "UNLOCK & LEARN",
                                color = ElegantPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Phone Unlock Tutor",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isServiceEnabled,
                            onCheckedChange = { checked ->
                                isServiceEnabled = checked
                                prefsManager.setUnlockServiceEnabled(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElegantOnPrimary,
                                checkedTrackColor = ElegantPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isServiceEnabled)
                            "Active: Shows a learning concept card automatically when unlocking your device."
                        else
                            "Paused: Enable to receive micro-quizzes on screen unlock.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual Concept Generation Button
                    Button(
                        onClick = triggerConceptGeneration,
                        enabled = !isGeneratingConcepts,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantPrimary,
                            disabledContainerColor = ElegantPrimary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingConcepts) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ElegantOnPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating Concepts...", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = ElegantOnPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate New Concepts Now", color = ElegantOnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Learning Window Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ElegantPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Active Learning Window",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "$windowStart - $windowEnd",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Recently Viewed Concepts Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently Viewed Concepts",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface
                ) {
                    Text(
                        text = "${recentHistory.size} viewed",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (recentHistory.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No concepts viewed yet",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "As you unlock your device and practice quizzes, your viewed concepts will automatically appear here for quick review.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                recentHistory.forEach { historyItem ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDetailConcept = historyItem }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElegantPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = historyItem.topic,
                                        color = ElegantPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val newStar = !historyItem.isStarred
                                        coroutineScope.launch {
                                            db.historyDao().updateStarStatus(historyItem.id, newStar)
                                            db.conceptDao().updateStarStatusByTitle(historyItem.conceptTitle, newStar)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (historyItem.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Star",
                                        tint = if (historyItem.isStarred) GoldStar else TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = historyItem.conceptTitle,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = historyItem.questionText,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
