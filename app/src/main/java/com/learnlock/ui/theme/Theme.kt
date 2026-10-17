package com.learnlock.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.learnlock.data.preferences.ThemeMode

private val BrandDarkColorScheme = darkColorScheme(
    primaryFixed = LightPrimaryContainer,
    primaryFixedDim = ElegantPrimary,
    onPrimaryFixed = LightOnPrimaryContainer,
    onPrimaryFixedVariant = ElegantPrimaryContainer,
    secondaryFixed = BrandOnSecondaryContainer,
    secondaryFixedDim = BrandSecondary,
    onSecondaryFixed = Color(0xFF0C1D28),
    onSecondaryFixedVariant = BrandSecondaryContainer,
    tertiaryFixed = BrandOnTertiaryContainer,
    tertiaryFixedDim = BrandTertiary,
    onTertiaryFixed = Color(0xFF1D1936),
    onTertiaryFixedVariant = BrandTertiaryContainer,
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

private val BrandLightColorScheme = lightColorScheme(
    primaryFixed = LightPrimaryContainer,
    primaryFixedDim = ElegantPrimary,
    onPrimaryFixed = LightOnPrimaryContainer,
    onPrimaryFixedVariant = ElegantPrimaryContainer,
    secondaryFixed = BrandOnSecondaryContainer,
    secondaryFixedDim = BrandSecondary,
    onSecondaryFixed = Color(0xFF0C1D28),
    onSecondaryFixedVariant = BrandSecondaryContainer,
    tertiaryFixed = BrandOnTertiaryContainer,
    tertiaryFixedDim = BrandTertiary,
    onTertiaryFixed = Color(0xFF1D1936),
    onTertiaryFixedVariant = BrandTertiaryContainer,
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF50606D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E5F4),
    onSecondaryContainer = Color(0xFF0C1D28),
    tertiary = Color(0xFF605D7D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5DFFF),
    onTertiaryContainer = Color(0xFF1D1936),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F4F7),
    surfaceContainer = Color(0xFFEBEFF3),
    surfaceContainerHigh = Color(0xFFE5E9ED),
    surfaceContainerHighest = Color(0xFFDDE3E9),
    surfaceBright = LightBackground,
    surfaceDim = Color(0xFFD7DBDF),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEEF1F5),
    inversePrimary = ElegantPrimary,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFC1C7CD),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun UnlockLearnTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> BrandDarkColorScheme
        else -> BrandLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

