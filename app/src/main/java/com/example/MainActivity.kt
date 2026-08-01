package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.data.preferences.AppPreferencesManager
import com.example.ui.tour.CoachmarkOverlay
import com.example.ui.tour.CoachmarkStep
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElegantOnPrimary
import com.example.ui.theme.ElegantPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.UnlockLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnlockLearnTheme {
                MainAppScreen()
            }
        }
    }
}

enum class NavTab(val title: String, val icon: ImageVector) {
    LEARN("Learn", Icons.Default.Quiz),
    HISTORY("History", Icons.Default.History),
    SETTINGS("API & Setup", Icons.Default.Key)
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val prefsManager = remember { AppPreferencesManager(context) }

    var selectedTab by remember { mutableStateOf(NavTab.LEARN) }
    var activeTourStep by remember {
        mutableStateOf(
            if (!prefsManager.isTourCompleted()) CoachmarkStep.LEARN_TAB else null,
        )
    }

    var navTabCoordinates by remember { mutableStateOf(mapOf<NavTab, LayoutCoordinates>()) }

    LaunchedEffect(activeTourStep) {
        when (activeTourStep) {
            CoachmarkStep.LEARN_TAB -> selectedTab = NavTab.LEARN
            CoachmarkStep.HISTORY_TAB -> selectedTab = NavTab.HISTORY
            CoachmarkStep.SETTINGS_TAB -> selectedTab = NavTab.SETTINGS
            null -> {}
        }
    }

    LaunchedEffect(Unit) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElegantOnPrimary,
                            selectedTextColor = ElegantPrimary,
                            indicatorColor = ElegantPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            navTabCoordinates += (tab to coordinates)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = DarkBackground
        ) {
            when (selectedTab) {
                NavTab.LEARN -> LearnScreen()
                NavTab.HISTORY -> HistoryScreen()
                NavTab.SETTINGS -> SettingsScreen { activeTourStep = CoachmarkStep.LEARN_TAB }
            }
        }

        activeTourStep?.let { step ->
            val targetKey = when (step) {
                CoachmarkStep.LEARN_TAB -> NavTab.LEARN
                CoachmarkStep.HISTORY_TAB -> NavTab.HISTORY
                CoachmarkStep.SETTINGS_TAB -> NavTab.SETTINGS
            }
            
            CoachmarkOverlay(
                activeStep = step,
                targetCoordinates = navTabCoordinates[targetKey],
                onNext = {
                    val nextStepIndex = step.ordinal + 1
                    if (nextStepIndex < CoachmarkStep.entries.size) {
                        activeTourStep = CoachmarkStep.entries[nextStepIndex]
                    } else {
                        prefsManager.setTourCompleted(completed = true)
                        activeTourStep = null
                        selectedTab = NavTab.LEARN
                    }
                },
                onSkip = {
                    prefsManager.setTourCompleted(completed = true)
                    activeTourStep = null
                    selectedTab = NavTab.LEARN
                }
            )
        }
    }
}
