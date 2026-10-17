package com.learnlock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learnlock.data.preferences.AppPreferencesManager
import com.learnlock.data.preferences.ThemeMode
import com.learnlock.feature.concept.DetailRoute
import com.learnlock.feature.history.HistoryRoute
import com.learnlock.feature.home.HomeRoute
import com.learnlock.feature.learn.LearnRoute
import com.learnlock.feature.settings.SettingsRoute
import com.learnlock.ui.theme.UnlockLearnTheme
import com.learnlock.ui.tour.CoachmarkOverlay
import com.learnlock.ui.tour.CoachmarkStep
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val prefs = AppPreferencesManager(this)
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(prefs.getThemeMode()) }
            var dynamicColor by rememberSaveable { mutableStateOf(prefs.isDynamicColor()) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SideEffect {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            UnlockLearnTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
            ) {
                MainAppScreen(
                    onThemeModeChange = { themeMode = it },
                    onDynamicColorChange = { dynamicColor = it },
                )
            }
        }
        ensureServiceStartedIfEnabled()
    }

    private fun ensureServiceStartedIfEnabled() {
        val prefs = AppPreferencesManager(this)
        if (prefs.isUnlockServiceEnabled()) {
            com.learnlock.feature.unlock.UnlockOverlayService.start(this)
        }
    }

}

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Home", Icons.Default.Home, "home"),
    LEARN("Learn", Icons.Default.Quiz, "learn"),
    STATS("Stats", Icons.Default.BarChart, "history"),
    SETTINGS("Settings", Icons.Default.Key, "settings"),
}

private const val DETAIL_ROUTE = "detail/{historyId}"

private fun detailRoute(historyId: Long) = "detail/$historyId"

private fun NavController.navigateToTab(tab: NavTab) {
    navigate(tab.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainAppScreen(
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val prefsManager = remember { AppPreferencesManager(context) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTourStep by remember {
        mutableStateOf(
            if (!prefsManager.isTourCompleted()) CoachmarkStep.LEARN_TAB else null,
        )
    }

    // Coordinates of the navigation destinations for the onboarding tour.
    // A snapshot map avoids rebuilding a whole map on every placement.
    val navTabCoordinates = remember { mutableStateMapOf<NavTab, LayoutCoordinates>() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val topTabForRoute = NavTab.entries.firstOrNull { it.route == backStackEntry?.destination?.route }
    // Remember the last top-level tab so the detail destination keeps its
    // source tab highlighted (and survives process recreation).
    var lastTopTabOrdinal by rememberSaveable { mutableIntStateOf(NavTab.HOME.ordinal) }
    LaunchedEffect(topTabForRoute) {
        if (topTabForRoute != null) lastTopTabOrdinal = topTabForRoute.ordinal
    }
    val selectedTab = topTabForRoute ?: NavTab.entries[lastTopTabOrdinal]

    LaunchedEffect(activeTourStep) {
        val tab = when (activeTourStep) {
            CoachmarkStep.LEARN_TAB -> NavTab.LEARN
            CoachmarkStep.HISTORY_TAB -> NavTab.STATS
            CoachmarkStep.SETTINGS_TAB -> NavTab.SETTINGS
            null -> null
        }
        if (tab != null) navController.navigateToTab(tab)
    }

    val finishTour: () -> Unit = {
        prefsManager.setTourCompleted(completed = true)
        activeTourStep = null
        navController.navigateToTab(NavTab.LEARN)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = this.maxWidth >= 600.dp
        val colors = MaterialTheme.colorScheme

        Scaffold(
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = colors.surfaceContainer,
                    ) {
                        NavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { navController.navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,

                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onPrimaryContainer,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant,
                                ),
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    navTabCoordinates[tab] = coordinates
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            if (useRail) {
                Row(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                    NavigationRail(
                        containerColor = colors.surfaceContainer,
                        windowInsets = WindowInsets(0),
                    ) {
                        NavTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { navController.navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,

                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.onPrimaryContainer,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer,
                                    unselectedIconColor = colors.onSurfaceVariant,
                                    unselectedTextColor = colors.onSurfaceVariant,
                                ),
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    navTabCoordinates[tab] = coordinates
                                },
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        AppNavHost(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            onStartTour = { activeTourStep = CoachmarkStep.LEARN_TAB },
                            onThemeModeChange = onThemeModeChange,
                            onDynamicColorChange = onDynamicColorChange,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                AppNavHost(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    onStartTour = { activeTourStep = CoachmarkStep.LEARN_TAB },
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            }
        }

        // Onboarding overlay sits above everything, including the bottom bar,
        // so background destinations are not interactive mid-tour.
        activeTourStep?.let { step ->
            val targetKey = when (step) {
                CoachmarkStep.LEARN_TAB -> NavTab.LEARN
                CoachmarkStep.HISTORY_TAB -> NavTab.STATS
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
                        finishTour()
                    }
                },
                onSkip = finishTour,
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onStartTour: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = NavTab.HOME.route,
        modifier = modifier,
    ) {
        composable(NavTab.HOME.route) {
            HomeRoute(onOpenLearn = { navController.navigateToTab(NavTab.LEARN) }, modifier = Modifier.fillMaxSize())
        }
        composable(NavTab.LEARN.route) {
            LearnRoute(
                snackbarHostState = snackbarHostState,
                onOpenDetail = { historyId -> navController.navigate(detailRoute(historyId)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(NavTab.STATS.route) {
            HistoryRoute(
                snackbarHostState = snackbarHostState,
                onOpenDetail = { historyId -> navController.navigate(detailRoute(historyId)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(NavTab.SETTINGS.route) {
            SettingsRoute(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.fillMaxSize(),
                onStartTour = onStartTour,
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
            )
        }
        composable(
            route = DETAIL_ROUTE,
            arguments = listOf(navArgument("historyId") { type = NavType.LongType }),
        ) {
            DetailRoute(
                onBack = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}


