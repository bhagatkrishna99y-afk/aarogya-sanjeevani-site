package com.aarogya.sanjeevani.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = Brand40,
        onPrimary = Neutral99,
        primaryContainer = Brand95,
        onPrimaryContainer = Brand10,
        inversePrimary = Brand80,
        secondary = Brand30,
        onSecondary = Neutral99,
        secondaryContainer = Brand90,
        onSecondaryContainer = Brand10,
        tertiary = Accent40,
        onTertiary = Neutral99,
        tertiaryContainer = Accent95,
        onTertiaryContainer = Accent10,
        error = Error40,
        onError = Neutral99,
        errorContainer = Error90,
        onErrorContainer = Error10,
        background = Neutral95,
        onBackground = Neutral10,
        surface = Neutral99,
        onSurface = Neutral10,
        surfaceVariant = Neutral90,
        onSurfaceVariant = Neutral30,
        surfaceTint = Brand40,
        outline = Neutral50,
        outlineVariant = Neutral80,
        inverseSurface = Neutral20,
        inverseOnSurface = Neutral95,
        surfaceDim = Neutral80,
        surfaceBright = Neutral99,
        surfaceContainerLowest = Neutral99,
        surfaceContainerLow = Neutral95,
        surfaceContainer = Neutral90,
        surfaceContainerHigh = Neutral80,
        surfaceContainerHighest = Neutral70,
        scrim = Color.Black,
    )

private val DarkColors =
    darkColorScheme(
        primary = Brand80,
        onPrimary = Brand10,
        primaryContainer = Brand30,
        onPrimaryContainer = Brand95,
        inversePrimary = Brand40,
        secondary = Brand90,
        onSecondary = Brand20,
        secondaryContainer = Brand30,
        onSecondaryContainer = Brand95,
        tertiary = Accent80,
        onTertiary = Accent10,
        tertiaryContainer = Accent30,
        onTertiaryContainer = Accent95,
        error = Error80,
        onError = Error20,
        errorContainer = Error30,
        onErrorContainer = Error90,
        background = Neutral0,
        onBackground = Neutral95,
        surface = Neutral0,
        onSurface = Neutral95,
        surfaceVariant = Neutral20,
        onSurfaceVariant = Neutral80,
        surfaceTint = Brand80,
        outline = Neutral60,
        outlineVariant = Neutral30,
        scrim = Color.Black,
        inverseSurface = Neutral90,
        inverseOnSurface = Neutral20,
        surfaceDim = Neutral0,
        surfaceBright = Neutral40,
        surfaceContainerLowest = Neutral0,
        surfaceContainerLow = Neutral10,
        surfaceContainer = Neutral20,
        surfaceContainerHigh = Neutral30,
        surfaceContainerHighest = Neutral40,
    )

/**
 * The one theme wrapper every Activity/Preview in either app uses.
 *
 * - [themeMode] resolves light/dark the same way everywhere: [ThemeMode.System]
 *   follows [isSystemInDarkTheme], [ThemeMode.Light]/[ThemeMode.Dark] force it --
 *   this is the single hook Profile's theme override needs, nothing upstream
 *   has to branch on the setting itself.
 * - [useDynamicColor] is opt-in (Android 12+), never on by default -- a
 *   healthcare app's brand teal is deliberate, not something that should
 *   silently become whatever wallpaper-derived hue the user's launcher
 *   picked. Even when it's on, [LocalAsExtendedColors] is always this
 *   module's own fixed brand colors, never derived from the dynamic
 *   palette: status meaning (a rejected request, a suspended doctor)
 *   must stay legible and consistent regardless of the active
 *   [androidx.compose.material3.ColorScheme].
 */
@Composable
fun AarogyaTheme(
    themeMode: ThemeMode = ThemeMode.System,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    val context = LocalContext.current
    val colorScheme =
        when {
            useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            darkTheme -> DarkColors
            else -> LightColors
        }
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalAsExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AarogyaTypography,
            shapes = AarogyaShapes,
            content = content,
        )
    }
}

/** Token access alongside [MaterialTheme] -- `AsTheme.extendedColors`,
 *  `AsTheme.spacing`, `AsTheme.elevation` read exactly like
 *  `MaterialTheme.colorScheme` does, so a component never needs to know
 *  which of the two theme objects a given token lives on by anything
 *  other than its name. */
object AsTheme {
    val extendedColors: AsExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAsExtendedColors.current

    val spacing: AsSpacing
        get() = AarogyaSpacing

    val elevation: AsElevation
        get() = AarogyaElevation
}
