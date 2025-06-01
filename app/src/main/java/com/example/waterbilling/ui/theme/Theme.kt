package com.example.waterbilling.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2196F3),        // Blue
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),      // Teal
    tertiary = Color(0xFF03A9F4),       // Light Blue
    surface = Color(0xFF121212),        // Dark surface
    onSurface = Color.White,
    background = Color(0xFF000000),     // Dark background
    onBackground = Color.White,
    error = Color(0xFFCF6679),          // Error color
    surfaceVariant = Color(0xFF1F1F1F)  // Slightly lighter surface
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2196F3),        // Blue
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC5),      // Teal
    tertiary = Color(0xFF03A9F4),       // Light Blue
    surface = Color.White,
    onSurface = Color(0xFF121212),
    background = Color(0xFFF5F5F5),     // Light gray background
    onBackground = Color(0xFF121212),
    error = Color(0xFFB00020),          // Error color
    surfaceVariant = Color(0xFFE1E1E1)  // Light gray surface
)

@Composable
fun DaloyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}