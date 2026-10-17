package com.learnlock.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.ColorScheme

private val ColorScheme.isDark: Boolean get() = surface.luminance() < 0.5f

// ---------------------------------------------------------------------------
// Brand tokens (Elegant Dark). Existing names are kept so current screens keep
// compiling while they are migrated to MaterialTheme.colorScheme roles.
// ---------------------------------------------------------------------------
val DarkBackground = Color(0xFF1C1B1F)
val DarkSurface = Color(0xFF2B2930)
val DarkSurfaceVariant = Color(0xFF36343B)
val DarkBorder = Color(0xFF49454F)

val ElegantPrimary = Color(0xFF75C7FF)
val ElegantOnPrimary = Color(0xFF00344E)
val ElegantPrimaryContainer = Color(0xFF004B70)
val ElegantOnPrimaryContainer = Color(0xFFC7E7FF)

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

val BrandSecondary = Color(0xFFB7C9D8)
val BrandOnSecondary = Color(0xFF22323E)
val BrandSecondaryContainer = Color(0xFF384956)
val BrandOnSecondaryContainer = Color(0xFFD3E5F4)

val BrandTertiary = Color(0xFFC9C3EA)
val BrandOnTertiary = Color(0xFF312E4B)
val BrandTertiaryContainer = Color(0xFF484563)
val BrandOnTertiaryContainer = Color(0xFFE5DFFF)

val BrandOnError = Color(0xFF601410)
val BrandErrorContainer = Color(0xFF8C1D18)
val BrandOnErrorContainer = Color(0xFFF9DEDC)

// outline carries meaningful boundaries (>= 3:1 on brand surfaces);
// outlineVariant is only for subtle, nonessential separation.
val BrandOutline = Color(0xFF938F96)
val BrandOutlineVariant = Color(0xFF49454F)

// ---------------------------------------------------------------------------
// Status colors tuned per light/dark surface so meaning survives dynamic
// color. These are fixed identity colors, not wallpaper-derived, so
// success/error/star keep recognizable contrast under any scheme.
// ---------------------------------------------------------------------------
val SuccessLight = Color(0xFF1B7A34)
val SuccessLightContainer = Color(0xFFCCE7DE)
val SuccessLightOnContainer = Color(0xFF003B1A)

val SuccessDark = Color(0xFF81C784)
val SuccessDarkContainer = Color(0xFF2A4A33)
val SuccessDarkOnContainer = Color(0xFFA7FFDB)

val WarningLight = Color(0xFF8F6600)
val WarningLightContainer = Color(0xFFFFF0C6)
val WarningLightOnContainer = Color(0xFF3D2B00)

val WarningDark = Color(0xFFFFC107)
val WarningDarkContainer = Color(0xFF3E2723)
val WarningDarkOnContainer = Color(0xFFFFF3E0)

val StarLight = Color(0xFF895000)
val StarLightContainer = Color(0xFFFFE0B2)
val StarLightOnContainer = Color(0xFF3E2723)

val StarDark = Color(0xFFFFC107)
val StarDarkContainer = Color(0xFF3E2723)
val StarDarkOnContainer = Color(0xFFFFECB3)

// ---------------------------------------------------------------------------
// Semantic roles exposed through ColorScheme so screens consume theme roles
// instead of literal brand colors. isDark distinguishes light vs dark scheme
// so status colors stay readable in both themes and under dynamic color.
// ---------------------------------------------------------------------------
val ColorScheme.success: Color
    get() = if (isDark) SuccessDark else SuccessLight

val ColorScheme.onSuccess: Color
    get() = if (isDark) Color(0xFF003910) else Color.White

val ColorScheme.successContainer: Color
    get() = if (isDark) SuccessDarkContainer else SuccessLightContainer

val ColorScheme.onSuccessContainer: Color
    get() = if (isDark) SuccessDarkOnContainer else SuccessLightOnContainer

val ColorScheme.warning: Color
    get() = if (isDark) WarningDark else WarningLight

val ColorScheme.warningContainer: Color
    get() = if (isDark) WarningDarkContainer else WarningLightContainer

val ColorScheme.onWarningContainer: Color
    get() = if (isDark) WarningDarkOnContainer else WarningLightOnContainer

val ColorScheme.star: Color
    get() = if (isDark) StarDark else StarLight

val ColorScheme.starContainer: Color
    get() = if (isDark) StarDarkContainer else StarLightContainer

val ColorScheme.onStarContainer: Color
    get() = if (isDark) StarDarkOnContainer else StarLightOnContainer

val BrandInverseSurface = Color(0xFFE6E1E5)
val BrandInverseOnSurface = Color(0xFF322F35)
val BrandInversePrimary = Color(0xFF006492)

val LightBackground = Color(0xFFF7F9FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFDDE3E9)
val LightOnSurface = Color(0xFF171C20)
val LightOnSurfaceVariant = Color(0xFF41484D)
val LightPrimary = Color(0xFF006492)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFC7E7FF)
val LightOnPrimaryContainer = Color(0xFF001E2F)
