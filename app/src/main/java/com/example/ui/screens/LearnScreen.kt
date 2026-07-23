package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.entity.ConceptItem
import com.example.data.preferences.AppPreferencesManager
import com.example.service.UnlockOverlayService
import com.example.ui.theme.*

@Composable
fun LearnScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefsManager = remember { AppPreferencesManager(context) }

    var isServiceEnabled by remember { mutableStateOf(prefsManager.isUnlockServiceEnabled()) }
    val dbConcepts by db.conceptDao().getAllConcepts().collectAsState(initial = emptyList())
    var expandedConceptIds by remember { mutableStateOf(setOf<Long>()) }

    val windowStart = prefsManager.getLearningWindowStart()
    val windowEnd = prefsManager.getLearningWindowEnd()

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
                            val serviceIntent = Intent(context, UnlockOverlayService::class.java)
                            if (checked) {
                                try {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("LearnScreen", "Failed to start overlay service", e)
                                }
                            } else {
                                context.stopService(serviceIntent)
                            }
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
                        "Active: Learns a concept automatically when unlocking device."
                    else
                        "Paused: Enable to receive questions on screen unlock.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                                text = "$windowStart AM - $windowEnd PM",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Recent Concepts Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = ElegantPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Recent Concepts",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any card to expand and review detailed explanations and code snippets.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Expandable Recent Concept Cards
        if (dbConcepts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                ),
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
                        text = "No concepts learned yet",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "As you unlock your device and practice quizzes, your generated concepts will automatically appear here for review.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            dbConcepts.forEach { concept ->
                val isExpanded = expandedConceptIds.contains(concept.id)

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isExpanded) ElegantPrimary else DarkBorder
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedConceptIds = if (isExpanded) {
                                expandedConceptIds - concept.id
                            } else {
                                expandedConceptIds + concept.id
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Card Header Row
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
                                text = concept.topic,
                                color = ElegantPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = concept.conceptTitle,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!isExpanded) {
                        Text(
                            text = concept.conceptSummary,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Full Detailed Summary
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkBackground,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = ElegantPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Detailed Explanation",
                                            color = ElegantPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = concept.conceptSummary,
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp
                                    )
                                }
                            }

                            // Code / Text Example snippet if present
                            if (!concept.codeExample.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = DarkBackground,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Code,
                                                contentDescription = null,
                                                tint = CodeBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Example / Code Snippet",
                                                color = CodeBlue,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = concept.codeExample,
                                            color = CodeBlue,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }

                            // Question / Practice preview
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkBackground,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Practice Question",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = concept.questionText,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = concept.explanation,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

