package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppDatabase
import com.example.data.preferences.AppPreferencesManager
import com.example.service.UnlockQuizActivity
import com.example.service.ConceptGenerator
import java.util.Calendar

@Composable
fun HomeScreen(onOpenLearn: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prefs = remember { AppPreferencesManager(context) }
    val history by db.historyDao().getAllHistory().collectAsState(initial = emptyList())
    LaunchedEffect(history) {
        history.filter { it.status == "GRADING_PENDING" && it.questionType == "SHORT_ANSWER" }.forEach { attempt ->
            val grade = ConceptGenerator(prefs).evaluateShortAnswer(attempt.questionText, attempt.correctAnswer, attempt.userAnswer)
            if (grade != null) db.historyDao().finishShortAnswerGrading(attempt.id, grade)
        }
    }
    val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val todayStart = calendar.timeInMillis
    val gradedHistory = history.filter { it.questionType != "SESSION_SUMMARY" }
    val todayCorrect = gradedHistory.filter { it.answeredAt >= todayStart && it.isCorrect }.sumOf { 10 }
    val goal = prefs.getDailyXpGoal()
    val xp = todayCorrect
    val ordered = gradedHistory.sortedBy { it.answeredAt }
    var combo = 0
    var lifetimeXp = 0
    var computedTodayXp = 0
    ordered.forEach { attempt ->
        if (attempt.isCorrect) combo++ else combo = 0
        if (attempt.isCorrect) {
            val multiplier = when {
                combo >= 6 -> 1.5
                combo == 5 -> 1.4
                combo == 4 -> 1.3
                combo == 3 -> 1.2
                combo == 2 -> 1.1
                else -> 1.0
            }
            val points = (10 * multiplier).toInt()
            lifetimeXp += points
            if (attempt.answeredAt >= todayStart) computedTodayXp += points
        }
    }
    val displayCombo = ordered.takeLastWhile { it.isCorrect }.size
    val level = when {
        lifetimeXp >= 10000 -> "Master"
        lifetimeXp >= 3000 -> "Advanced"
        lifetimeXp >= 1500 -> "Skilled"
        lifetimeXp >= 750 -> "Practitioner"
        lifetimeXp >= 300 -> "Explorer"
        lifetimeXp >= 100 -> "Learner"
        else -> "Starter"
    }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Today's progress", style = MaterialTheme.typography.titleLarge)
                    Text("$computedTodayXp / $goal XP", color = colors.primary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { (computedTodayXp.toFloat() / goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("$level · $lifetimeXp lifetime XP · ${displayCombo} answer combo", color = colors.onSurfaceVariant)
                Button(onClick = { context.startActivity(Intent(context, UnlockQuizActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.Icon(Icons.Default.LockOpen, null)
                    Spacer(Modifier.height(1.dp))
                    Text("Start a quiz")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), onClick = onOpenLearn) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Continue learning", style = MaterialTheme.typography.titleMedium)
                Text("Browse concepts and prepare questions for your next unlock.", color = colors.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Icon(Icons.Default.AutoAwesome, null, tint = colors.primary)
                    Text("Your learning", style = MaterialTheme.typography.titleMedium)
                }
                Text("${gradedHistory.size} question attempts saved on this device", color = colors.onSurfaceVariant)
                Text("Daily goal: $goal XP · change it in Settings", color = colors.onSurfaceVariant)
            }
        }
    }
}
