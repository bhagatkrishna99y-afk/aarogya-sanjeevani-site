package com.aarogyasanjeevani.patient.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Brand700,
    onPrimary = Color_White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand800,
    secondary = Slate700,
    background = Slate50,
    onBackground = Slate900,
    surface = Color_White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    error = Red700,
    errorContainer = Red100,
)

private val DarkColors = darkColorScheme(
    primary = Brand400,
    onPrimary = Slate900,
    primaryContainer = Brand800,
    onPrimaryContainer = Brand100,
    secondary = Slate300,
    background = Slate900,
    onBackground = Slate100,
    surface = Color_DarkSurface,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    error = Red100,
    errorContainer = Red700,
)

@Composable
fun AarogyaSanjeevaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
