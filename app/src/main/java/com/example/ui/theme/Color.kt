package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand tokens (Elegant Dark). Existing names are kept so current screens keep
// compiling while they are migrated to MaterialTheme.colorScheme roles.
// ---------------------------------------------------------------------------
val DarkBackground = Color(0xFF1C1B1F)
val DarkSurface = Color(0xFF2B2930)
val DarkSurfaceVariant = Color(0xFF36343B)
val DarkBorder = Color(0xFF49454F)

val ElegantPrimary = Color(0xFFD0BCFF)
val ElegantOnPrimary = Color(0xFF381E72)
val ElegantPrimaryContainer = Color(0xFF381E72)
val ElegantOnPrimaryContainer = Color(0xFFEADDFF)

val CodeBlue = Color(0xFF7DB3FF)
val SuccessGreen = Color(0xFF81C784)
val ErrorRed = Color(0xFFF2B8B5)

val TextPrimary = Color(0xFFE6E1E5)
val TextSecondary = Color(0xFFCAC4D0)
val TextMuted = Color(0xFF938F96)
val GoldStar = Color(0xFFFFC107)

// ---------------------------------------------------------------------------
// Full-role additions so every Material role has a value in both brand and
// dynamic schemes. Values follow the Material 3 tonal palette the brand was
// derived from, tuned so the existing surfaces stay unchanged.
// ---------------------------------------------------------------------------
val BrandSurfaceDim = Color(0xFF141218)
val BrandSurfaceBright = Color(0xFF3B383E)

val BrandSurfaceContainerLowest = Color(0xFF0F0D13)
val BrandSurfaceContainerLow = Color(0xFF1D1B20)
val BrandSurfaceContainer = Color(0xFF211F26)
val BrandSurfaceContainerHigh = Color(0xFF2B2930)
val BrandSurfaceContainerHighest = Color(0xFF36343B)

val BrandOnBackground = Color(0xFFE6E1E5)

val BrandSecondary = Color(0xFFCCC2DC)
val BrandOnSecondary = Color(0xFF332D41)
val BrandSecondaryContainer = Color(0xFF4A4458)
val BrandOnSecondaryContainer = Color(0xFFE8DEF8)

val BrandTertiary = Color(0xFFEFB8C8)
val BrandOnTertiary = Color(0xFF492532)
val BrandTertiaryContainer = Color(0xFF633B48)
val BrandOnTertiaryContainer = Color(0xFFFFD8E4)

val BrandOnError = Color(0xFF601410)
val BrandErrorContainer = Color(0xFF8C1D18)
val BrandOnErrorContainer = Color(0xFFF9DEDC)

// outline carries meaningful boundaries (>= 3:1 on brand surfaces);
// outlineVariant is only for subtle, nonessential separation.
val BrandOutline = Color(0xFF938F96)
val BrandOutlineVariant = Color(0xFF49454F)

val BrandInverseSurface = Color(0xFFE6E1E5)
val BrandInverseOnSurface = Color(0xFF322F35)
val BrandInversePrimary = Color(0xFF6750A4)
