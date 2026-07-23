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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppPreferencesManager
import com.example.ui.tour.InteractiveTourDialog
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
    var showTourDialog by remember { mutableStateOf(!prefsManager.isTourCompleted()) }

    LaunchedEffect(Unit) {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
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
                        )
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
                NavTab.SETTINGS -> SettingsScreen(onStartTour = { showTourDialog = true })
            }
        }

        if (showTourDialog) {
            InteractiveTourDialog(
                onDismiss = { showTourDialog = false },
                onNavigateTab = { tab ->
                    selectedTab = tab
                }
            )
        }
    }
}
