package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val BrandDarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = DarkBackground,
    onBackground = BrandOnBackground,
    surface = DarkBackground,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceBright = BrandSurfaceBright,
    surfaceDim = BrandSurfaceDim,
    surfaceContainer = BrandSurfaceContainer,
    surfaceContainerHigh = BrandSurfaceContainerHigh,
    surfaceContainerHighest = BrandSurfaceContainerHighest,
    surfaceContainerLow = BrandSurfaceContainerLow,
    surfaceContainerLowest = BrandSurfaceContainerLowest,
    inverseSurface = BrandInverseSurface,
    inverseOnSurface = BrandInverseOnSurface,
    inversePrimary = BrandInversePrimary,
    error = ErrorRed,
    onError = BrandOnError,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * LearnLock ships a dark-only product identity. On Android 12+ the Material
 * dynamic (wallpaper) dark scheme is used so the app follows the platform color
 * language; older devices and devices without dynamic color use the brand scheme.
 */
@Composable
fun UnlockLearnTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)

        else -> BrandDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
