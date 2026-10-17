# Material 3 implementation and verification

## UI contract

- Keep the existing Home, Learn, Stats, and Settings destinations. The existing 600dp navigation-rail breakpoint remains; compact windows use bottom navigation.
- Use theme color, type, shape, and status roles throughout. Brand light/dark schemes cover the fixed and inverse roles as well as surface roles. Success, warning, and star roles retain identity across wallpaper schemes.
- Appearance and daily goal use scrollable modal sheets with radio-group semantics. Appearance updates immediately and stays open for changing wallpaper colors; Done dismisses it. Goal selection persists and dismisses its sheet.
- Dynamic color is persisted, available on API 31+, and shared by the main, quiz, and Tutor Controls activities. Older Android uses the brand palette and shows the switch disabled.
- Concentrate expressive emphasis on daily progress, generation, and quiz results. Keep utility controls conventional.
- Preserve loading placeholders in Learn, add a first-use Home message, and keep generation errors recoverable with Retry.
- Consume scaffold insets before child screens; Tutor Controls uses safe drawing insets and scrolling. Quiz content accounts for IME insets, and result dialogs scroll at large font sizes.

## Dependency and API decision

Compose BOM `2025.10.00` resolves `androidx.compose.material3:material3-android:1.4.0`. No alpha dependency was added.

The handoff's API assumption was incorrect: `MaterialExpressiveTheme` and `MotionScheme` exist in the binary but are **internal** to Kotlin consumers in 1.4.0. `MaterialShapes` and the expressive loading component are unavailable on this stable line. The implementation therefore uses stable Compose spring animations centralized in `ui/theme/Motion.kt`, Material component motion, theme shapes, and contrasting typography. It does not call internal APIs or claim use of the alpha expressive components.

Source: https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01

## Reproduce

Use a JDK compatible with the project's Gradle installation, then run:

```text
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
./gradlew :app:recordRoborazziDebug
./gradlew :app:lintDebug
```

`MaterialUiTest` exercises appearance selection, immediate dynamic-color changes, goal selection/dismissal, and disabled dynamic color on API 30. It includes screenshot captures for brand light/dark, dynamic light/dark, Home empty state, 411×891dp phone and 840×1000dp tablet, generation progress/error, Learn loading, picker sheets, and quiz results with 1.5× text. Run the record task to actually emit images; ordinary test execution validates interactions without writing the screenshots.

Screenshots are written to `app/build/outputs/material/`. They are generated verification artifacts, not approved regression baselines.

## Limits

Robolectric dynamic schemes are simulated platform schemes, not a physical device's chosen wallpaper. Device-level TalkBack, real keyboard/IME behavior, display cutouts, foldables, rotation, and wallpaper changes need device verification. UI tests do not establish those observations.

## Observed verification (2026-10-17)

- Kotlin compilation passed with stable Material 3 1.4.0.
- 38 tests passed, including 13 Material UI tests; API 30 fallback behavior was exercised.
- Roborazzi recorded 12 screen images. Inspected phone brand/dynamic light and dark, tablet Home, empty Home, Learn loading/generation/error, both selection sheets, and the large-text quiz result.
- Visual inspection prompted full expansion of picker sheets, a 720dp Home content width limit, and matching on-primary-container foregrounds for the dynamic-color progress hero.
- Lint initially crashed in ExperimentalDetector with `Unexpected owner function: null`. Retrying with `--no-configuration-cache` succeeded: 0 errors, 77 warnings. No lint checks were disabled. Warnings remain in the project (including dependency, deprecated API, and existing resource recommendations).
- No emulator or physical-device interaction was performed. The navigation rail and IME/system inset changes were reviewed in code, not observed on a live device.
