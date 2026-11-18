package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A container/on-container pair for one status badge -- always used
 *  together (text/icon drawn in [onContainer] on top of [container]),
 *  so the pair can never be split into a non-verified combination. */
data class AsStatusColor(
    val container: Color,
    val onContainer: Color,
)

/**
 * Semantic colors Material 3's [androidx.compose.material3.ColorScheme]
 * has no slot for: success/warning/info (error already has one), and one
 * color per request status and per doctor status. Reached via
 * `AarogyaTheme.extendedColors` (see Theme.kt) rather than
 * [MaterialTheme], exactly like `MaterialTheme.colorScheme` -- so call
 * sites never need to know whether a color came from Material's own
 * scheme or this module's extension of it.
 *
 * Deliberately **not** touched by dynamic color (see [AarogyaTheme]):
 * these are brand/status meaning, not decoration, so they stay fixed
 * regardless of which [androidx.compose.material3.ColorScheme] is
 * active.
 */
data class AsExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val requestRequested: AsStatusColor,
    val requestAccepted: AsStatusColor,
    val requestConfirmed: AsStatusColor,
    val requestCompleted: AsStatusColor,
    val requestRejected: AsStatusColor,
    val requestCancelled: AsStatusColor,
    val doctorPending: AsStatusColor,
    val doctorApproved: AsStatusColor,
    val doctorRejected: AsStatusColor,
    val doctorSuspended: AsStatusColor,
)

val LightExtendedColors =
    AsExtendedColors(
        success = Success40,
        onSuccess = Neutral99,
        successContainer = Success95,
        onSuccessContainer = Success10,
        warning = Amber40,
        onWarning = Neutral99,
        warningContainer = Amber95,
        onWarningContainer = Amber10,
        info = Blue40,
        onInfo = Neutral99,
        infoContainer = Blue95,
        onInfoContainer = Blue10,
        // Exact container/text pairs from the design brief's status table.
        requestRequested = AsStatusColor(container = Amber95, onContainer = Amber40),
        requestAccepted = AsStatusColor(container = Blue95, onContainer = Blue40),
        requestConfirmed = AsStatusColor(container = Success95, onContainer = Success20),
        requestCompleted = AsStatusColor(container = Neutral85, onContainer = Neutral40),
        requestRejected = AsStatusColor(container = Red95, onContainer = Red40),
        requestCancelled = AsStatusColor(container = Neutral90, onContainer = Neutral60),
        doctorPending = AsStatusColor(container = Amber95, onContainer = Amber40),
        doctorApproved = AsStatusColor(container = Success95, onContainer = Success20),
        doctorRejected = AsStatusColor(container = Red95, onContainer = Red40),
        doctorSuspended = AsStatusColor(container = Violet95, onContainer = Violet40),
    )

val DarkExtendedColors =
    AsExtendedColors(
        success = Success80,
        onSuccess = Success10,
        successContainer = Success30,
        onSuccessContainer = Success90,
        warning = Amber80,
        onWarning = Amber10,
        warningContainer = Amber30,
        onWarningContainer = Amber90,
        info = Blue80,
        onInfo = Blue10,
        infoContainer = Blue30,
        onInfoContainer = Blue90,
        requestRequested = AsStatusColor(container = Amber30, onContainer = Amber90),
        requestAccepted = AsStatusColor(container = Blue30, onContainer = Blue90),
        requestConfirmed = AsStatusColor(container = Success30, onContainer = Success90),
        requestCompleted = AsStatusColor(container = Neutral30, onContainer = Neutral90),
        requestRejected = AsStatusColor(container = Red30, onContainer = Red90),
        requestCancelled = AsStatusColor(container = Neutral30, onContainer = Neutral90),
        doctorPending = AsStatusColor(container = Amber30, onContainer = Amber90),
        doctorApproved = AsStatusColor(container = Success30, onContainer = Success90),
        doctorRejected = AsStatusColor(container = Red30, onContainer = Red90),
        doctorSuspended = AsStatusColor(container = Violet30, onContainer = Violet90),
    )

/** Always provided by [AarogyaTheme] -- the light default here only
 *  guards a `@Preview` or test that renders a component without going
 *  through that wrapper. */
val LocalAsExtendedColors = staticCompositionLocalOf { LightExtendedColors }
