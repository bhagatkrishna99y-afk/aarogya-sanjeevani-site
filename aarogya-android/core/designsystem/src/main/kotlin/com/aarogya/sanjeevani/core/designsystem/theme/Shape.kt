package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded surfaces throughout, 16-24dp -- a "premium but never
 * complicated" feel reads calmer with soft, generous corners than
 * Material's sharper defaults. [extraLarge] (24dp) is this scale's
 * ceiling, used for full-screen sheets/dialogs; nothing in this design
 * system goes rounder than that except genuinely circular shapes
 * (avatars, FABs), which bypass this scale entirely.
 */
val AarogyaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

private const val PILL_SHAPE_PERCENT = 50

/** The fully-rounded "pill" shape every badge/chip-like component
 *  (AsBadge, AsSearchField, StatusChip) uses -- defined once so none of
 *  them repeats the same `RoundedCornerShape(50)` literal. */
val PillShape = RoundedCornerShape(percent = PILL_SHAPE_PERCENT)

/** 14dp -- the approved redesign brief's own corner radius for text
 *  inputs, between [AarogyaShapes]'s `small` (12dp) and `medium` (16dp)
 *  steps rather than reusing either. */
val InputShape = RoundedCornerShape(14.dp)
