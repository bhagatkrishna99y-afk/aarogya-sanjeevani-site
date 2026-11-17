package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3's own elevation levels, named rather than left as bare
 * `.dp` literals at call sites -- a flat, low-shadow look reads calmer
 * and more clinical than heavy elevation (same call the website design
 * system made: website/docs/design-tokens.md's "Shadow" section), so
 * most surfaces in this app stay at [level0]/[level1]; [level4]/[level5]
 * are reserved for genuinely floating content (dialogs, dropdown menus).
 */
data class AsElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

val AarogyaElevation = AsElevation()
