package com.college.quizapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    onPrimary = TextPrimary,
    primaryContainer = Purple20,
    onPrimaryContainer = Purple80,
    secondary = Teal60,
    onSecondary = DarkBackground,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal80,
    tertiary = Purple60,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = TextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = LightBackground,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple20,
    secondary = Teal60,
    onSecondary = LightBackground,
    secondaryContainer = Teal80,
    onSecondaryContainer = Teal40,
    background = LightBackground,
    onBackground = DarkBackground,
    surface = LightSurface,
    onSurface = DarkBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
    onError = LightBackground,
    outline = TextMuted
)

@Composable
fun CollegeQuizAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
