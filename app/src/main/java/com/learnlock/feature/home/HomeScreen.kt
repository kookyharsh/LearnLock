package com.learnlock.feature.home

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import com.learnlock.ui.theme.AppMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learnlock.feature.unlock.UnlockQuizActivity

@Composable
fun HomeRoute(
    onOpenLearn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPreferences()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    HomeScreen(
        state = state,
        onOpenLearn = onOpenLearn,
        onStartQuiz = { context.startActivity(Intent(context, UnlockQuizActivity::class.java)) },
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenLearn: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = AppMotion.progress,
        label = "Daily goal progress",
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Today's progress", style = MaterialTheme.typography.titleLarge)
                    Text("${state.todayXp} / ${state.goal} XP", color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(156.dp),
                        strokeWidth = 9.dp,
                        color = colors.onPrimaryContainer,
                        trackColor = colors.onPrimaryContainer.copy(alpha = 0.2f),
                    )
                    Text(
                        "${state.progressPercent}%",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    "${state.level} · ${state.lifetimeXp} lifetime XP · ${state.combo} answer combo",
                    color = colors.onPrimaryContainer,
                )
                Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.LockOpen, null)
                    Spacer(Modifier.size(width = 8.dp, height = 1.dp))
                    Text("Start a quiz")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh), onClick = onOpenLearn) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.gradedCount == 0) "Start your learning journey" else "Continue learning", style = MaterialTheme.typography.titleMedium)
                Text(if (state.gradedCount == 0) "Generate your first concepts, then take a quiz to earn XP." else "Browse concepts and prepare questions for your next unlock.", color = colors.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = colors.primary)
                Text("Your learning", style = MaterialTheme.typography.titleMedium)
            }
            Text("${state.gradedCount} question attempts saved on this device", color = colors.onSurfaceVariant)
            Text("${state.retryCount} questions to retry · ${state.gradingCount} awaiting review", color = colors.onSurfaceVariant)
            Text(if (state.tutorEnabled) "Unlock tutor is on" else "Unlock tutor is off", color = colors.onSurfaceVariant)
            Text("Daily goal: ${state.goal} XP · change it in Settings", color = colors.onSurfaceVariant)
        }
    }
}
}

