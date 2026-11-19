package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// Editorial MathPad Brand & Surface Tokens
val WarmIvory = Color(0xFFFAF6EE)
val SoftParchment = Color(0xFFF3ECE0)
val WarmSand = Color(0xFFE7DDD0)
val DeepInk = Color(0xFF1E1C19)
val MutedInk = Color(0xFF635C52)
val CopperAccent = Color(0xFFC86D44)
val DeepCopper = Color(0xFF9E4F2C)
val SagePattern = Color(0xFF2E7D5B)
val SageSurface = Color(0xFFDDF2E7)
val AmberDrift = Color(0xFFB5691E)
val AmberSurface = Color(0xFFFAEBDC)
val TerracottaAlert = Color(0xFFB83B35)
val TerracottaSurface = Color(0xFFFCE4E2)

val DarkObsidian = Color(0xFF111114)
val DarkSurface = Color(0xFF1A1A1E)
val DarkSurfaceElevated = Color(0xFF25252B)
val DarkInk = Color(0xFFF3EFE8)
val DarkMutedInk = Color(0xFFB0A99E)
val LightCopper = Color(0xFFE89B75)
val DarkSage = Color(0xFF6CD4A4)
val DarkAmber = Color(0xFFF0AD65)
val DarkAlert = Color(0xFFF28B82)

object ComplexityPalette {
    // 5-stop Light Mode spectrum: Light Beige -> Deep Charcoal
    private val lightStops = listOf(
        0.00f to Color(0xFFF6F0E6), // Light Beige
        0.25f to Color(0xFFE9DFCD), // Warm Sand Beige
        0.50f to Color(0xFFC8B8A0), // Muted Taupe
        0.75f to Color(0xFF524E48), // Warm Charcoal Slate
        1.00f to Color(0xFF1E1E22)  // Deep Charcoal
    )

    // 5-stop Dark Mode spectrum: Warm Dark Taupe -> Obsidian Charcoal
    private val darkStops = listOf(
        0.00f to Color(0xFF474036), // Warm Muted Beige-Taupe
        0.25f to Color(0xFF3A352E), // Dark Sand
        0.50f to Color(0xFF2D2A26), // Charcoal Taupe
        0.75f to Color(0xFF1F1F23), // Slate Charcoal
        1.00f to Color(0xFF131317)  // Obsidian Charcoal
    )

    fun colorForComplexity(score: Float, isDarkMode: Boolean): Color {
        val clamped = score.coerceIn(0f, 1f)
        val stops = if (isDarkMode) darkStops else lightStops
        for (i in 0 until stops.lastIndex) {
            val (t0, c0) = stops[i]
            val (t1, c1) = stops[i + 1]
            if (clamped <= t1) {
                val fraction = ((clamped - t0) / (t1 - t0)).coerceIn(0f, 1f)
                return lerp(c0, c1, fraction)
            }
        }
        return stops.last().second
    }

    fun isLightSurface(color: Color): Boolean = color.luminance() > 0.38f

    fun primaryInkOn(background: Color): Color =
        if (isLightSurface(background)) Color(0xFF1C1A17) else Color(0xFFF5F1EA)

    fun secondaryInkOn(background: Color): Color =
        if (isLightSurface(background)) Color(0xFF5A5349) else Color(0xFFC5BEB2)

    fun borderOn(background: Color): Color =
        if (isLightSurface(background)) Color(0xFF1C1A17).copy(alpha = 0.12f)
        else Color(0xFFF5F1EA).copy(alpha = 0.16f)

    fun chipSurfaceOn(background: Color): Color =
        if (isLightSurface(background)) Color(0xFF1C1A17).copy(alpha = 0.08f)
        else Color(0xFFF5F1EA).copy(alpha = 0.12f)
}
