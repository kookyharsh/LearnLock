package com.learnlock.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Stable Compose equivalents for the three expressive moments.
 * Material 3 1.4 keeps MotionScheme internal; do not depend on internal APIs.
 * Compose's animation clock respects the platform animator duration scale.
 */
object AppMotion {
    val heroSpatial = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    val progress = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val fade = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
