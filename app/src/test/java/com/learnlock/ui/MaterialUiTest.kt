package com.learnlock.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.learnlock.data.preferences.ThemeMode
import com.learnlock.data.remote.ai.ConceptGenerationState
import com.learnlock.feature.home.HomeScreen
import com.learnlock.feature.home.HomeUiState
import com.learnlock.feature.learn.LearnScreen
import com.learnlock.feature.learn.LearnUiState
import com.learnlock.feature.settings.AppearanceBottomSheet
import com.learnlock.ui.theme.UnlockLearnTheme
import com.learnlock.feature.unlock.QuizResult
import com.learnlock.feature.unlock.QuizResultSheet
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MaterialUiTest {
    @get:Rule val compose = createComposeRule()

    private fun home(mode: ThemeMode, dynamic: Boolean = false, empty: Boolean = false, suffix: String = "phone") {
        compose.setContent {
            UnlockLearnTheme(mode, dynamic) {
                Surface(Modifier.fillMaxSize()) {
                    HomeScreen(if (empty) HomeUiState() else HomeUiState(totalXp = 30, todayCompletedQuizzes = 3))
                }
            }
        }
        compose.onNodeWithText("Generate Quiz").assertHasClickAction()
        compose.onRoot().captureRoboImage("build/outputs/material/home-${mode.name}-$dynamic-$empty-$suffix.png")
    }
    @Test fun homeLight() = home(ThemeMode.LIGHT)
    @Test fun homeDark() = home(ThemeMode.DARK)
    @Test fun homeDynamicLight() = home(ThemeMode.LIGHT, dynamic = true)
    @Test fun homeDynamicDark() = home(ThemeMode.DARK, dynamic = true)
    @Test fun homeEmpty() = home(ThemeMode.LIGHT, empty = true)
    @Test @Config(qualifiers = "w840dp-h1000dp-xhdpi")
    fun homeExpanded() = home(ThemeMode.LIGHT, suffix = "tablet")

    @Test fun appearanceSelectionAndDynamicColor() {
        compose.setContent {
            var mode by remember { mutableStateOf(ThemeMode.LIGHT) }
            var dynamic by remember { mutableStateOf(false) }
            UnlockLearnTheme(mode, dynamic) {
                AppearanceBottomSheet(true, {}, mode, dynamic, { mode = it }, { dynamic = it })
            }
        }
        compose.onNodeWithText("Dark Theme").performClick().assertIsSelected()
        compose.onNodeWithText("Dynamic colors").performClick().assertIsOn()
        compose.onRoot().captureRoboImage("build/outputs/material/appearance-dark-dynamic.png")
    }

    @Test fun dailyGoalSelection() {
        var selected = 5
        var dismissed = false
        compose.setContent {
            UnlockLearnTheme(ThemeMode.LIGHT, false) {
                com.learnlock.feature.settings.DailyQuizGoalBottomSheet(true, { dismissed = true }, selected, { selected = it })
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/material/daily-goal.png")
        compose.onNodeWithText("8 Quizzes").performClick()
        org.junit.Assert.assertEquals(8, selected)
        org.junit.Assert.assertTrue(dismissed)
    }

    @Test @Config(sdk = [30])
    fun dynamicColorUnavailableOnOlderAndroid() {
        compose.setContent {
            UnlockLearnTheme(ThemeMode.LIGHT, true) {
                AppearanceBottomSheet(true, {}, ThemeMode.LIGHT, true, {}, {})
            }
        }
        compose.onNodeWithText("Dynamic colors").assertIsNotEnabled()
    }

    @Test fun quizResultLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                UnlockLearnTheme(ThemeMode.DARK, false) {
                    QuizResultSheet(QuizResult(true, 3, 3, 0.3, 0.6, 2, "Learning"), {}, {}, {})
                }
            }
        }
        compose.onNodeWithText("Unlock device").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage("build/outputs/material/quiz-result-dark-large-text.png")
    }
    private fun learn(generation: ConceptGenerationState, name: String, loading: Boolean = false) {
        compose.setContent {
            UnlockLearnTheme(ThemeMode.LIGHT, false) {
                Surface(Modifier.fillMaxSize()) {
                    LearnScreen(
                        state = LearnUiState(generation = generation, recentHistory = if (loading) null else emptyList()),
                        snackbarHostState = remember { SnackbarHostState() },
                        onOpenDetail = {}, onToggleService = {}, onGenerate = {}, onToggleStar = {}, onGrantPermission = {},
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/material/learn-$name.png")
    }
    @Test fun generationProgress() = learn(ConceptGenerationState.Running("Creating programming concepts…"), "generating")
    @Test fun generationError() = learn(ConceptGenerationState.Error("Unable to connect. Check your connection and retry."), "error")
    @Test fun learnLoading() = learn(ConceptGenerationState.Idle, "loading", loading = true)
}

