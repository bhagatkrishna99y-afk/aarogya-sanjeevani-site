package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The only padding/gap values a screen in either app should reach for --
 * a 4dp rhythm, same progression as the website's Tailwind spacing scale
 * (website/docs/design-tokens.md). Reached via `AarogyaTheme.spacing`
 * (see Theme.kt); an arbitrary `.dp` literal at a call site is a sign
 * this scale is missing a step, not a reason to bypass it.
 */
data class AsSpacing(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

val AarogyaSpacing = AsSpacing()
