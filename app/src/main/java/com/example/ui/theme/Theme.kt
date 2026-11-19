package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = DeepCopper,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DFD2),
    onPrimaryContainer = Color(0xFF421A08),
    secondary = Color(0xFF5D5347),
    onSecondary = Color.White,
    secondaryContainer = WarmSand,
    onSecondaryContainer = DeepInk,
    tertiary = SagePattern,
    onTertiary = Color.White,
    tertiaryContainer = SageSurface,
    onTertiaryContainer = Color(0xFF0E3824),
    background = WarmIvory,
    onBackground = DeepInk,
    surface = SoftParchment,
    onSurface = DeepInk,
    surfaceVariant = WarmSand,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD5C8B5),
    error = TerracottaAlert,
    errorContainer = TerracottaSurface,
    onErrorContainer = Color(0xFF5A1613)
)

private val DarkColorScheme = darkColorScheme(
    primary = LightCopper,
    onPrimary = Color(0xFF3E1807),
    primaryContainer = Color(0xFF5D2B14),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFCBBFA9),
    onSecondary = Color(0xFF28221A),
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkInk,
    tertiary = DarkSage,
    onTertiary = Color(0xFF0A3320),
    tertiaryContainer = Color(0xFF174430),
    onTertiaryContainer = Color(0xFFBDF2D7),
    background = DarkObsidian,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkMutedInk,
    outline = Color(0xFF3B3834),
    error = DarkAlert,
    errorContainer = Color(0xFF541D1B),
    onErrorContainer = Color(0xFFFFDAD6)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MathPadTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides isDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MathPadTypography,
            content = content
        )
    }
}
