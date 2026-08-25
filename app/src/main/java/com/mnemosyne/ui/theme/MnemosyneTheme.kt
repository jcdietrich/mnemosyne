package com.mnemosyne.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern OLED / Deep Dark Palette
private val PrimaryAccent = Color(0xFF64B5F6)       // Vibrant memory blue
private val SecondaryAccent = Color(0xFF81C784)     // Soft emerald for search/secondary
private val BackgroundDark = Color(0xFF0F111A)      // Deep obsidian
private val SurfaceDark = Color(0xFF181B26)         // Elevated surface
private val SurfaceVariantDark = Color(0xFF222636)  // Card background
private val OutlineDark = Color(0xFF33384D)

private val PrimaryContainerDark = Color(0xFF1E2D4A)
private val SecondaryContainerDark = Color(0xFF1A332B)
private val OnPrimaryDark = Color(0xFF0A192F)
private val OnSecondaryDark = Color(0xFF0D281E)

private val MnemosyneDarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = PrimaryAccent,

    secondary = SecondaryAccent,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = SecondaryAccent,

    tertiary = Color(0xFFFFB74D), // Amber for voice search recording
    onTertiary = Color(0xFF3E2723),

    background = BackgroundDark,
    onBackground = Color(0xFFECEFF1),

    surface = SurfaceDark,
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB0BEC5),

    outline = OutlineDark,
    outlineVariant = Color(0xFF2A2E40),

    error = Color(0xFFFF5252),
    onError = Color.White
)

@Composable
fun MnemosyneTheme(
    darkTheme: Boolean = true, // Default to deep dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MnemosyneDarkColorScheme,
        content = content
    )
}
