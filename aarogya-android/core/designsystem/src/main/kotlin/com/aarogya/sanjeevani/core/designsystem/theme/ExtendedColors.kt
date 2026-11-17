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
        warning = Warning40,
        onWarning = Neutral99,
        warningContainer = Warning95,
        onWarningContainer = Warning10,
        info = Info40,
        onInfo = Neutral99,
        infoContainer = Info95,
        onInfoContainer = Info10,
        requestRequested = AsStatusColor(container = Info95, onContainer = Info10),
        requestAccepted = AsStatusColor(container = Warning95, onContainer = Warning10),
        requestConfirmed = AsStatusColor(container = Brand95, onContainer = Brand10),
        requestCompleted = AsStatusColor(container = Success95, onContainer = Success10),
        requestRejected = AsStatusColor(container = Error95, onContainer = Error10),
        requestCancelled = AsStatusColor(container = Neutral90, onContainer = Neutral10),
        doctorPending = AsStatusColor(container = Warning95, onContainer = Warning10),
        doctorApproved = AsStatusColor(container = Success95, onContainer = Success10),
        doctorRejected = AsStatusColor(container = Error95, onContainer = Error10),
        doctorSuspended = AsStatusColor(container = Violet95, onContainer = Violet10),
    )

val DarkExtendedColors =
    AsExtendedColors(
        success = Success80,
        onSuccess = Success10,
        successContainer = Success30,
        onSuccessContainer = Success90,
        warning = Warning80,
        onWarning = Warning10,
        warningContainer = Warning30,
        onWarningContainer = Warning90,
        info = Info80,
        onInfo = Info10,
        infoContainer = Info30,
        onInfoContainer = Info90,
        requestRequested = AsStatusColor(container = Info30, onContainer = Info90),
        requestAccepted = AsStatusColor(container = Warning30, onContainer = Warning90),
        requestConfirmed = AsStatusColor(container = Brand30, onContainer = Brand90),
        requestCompleted = AsStatusColor(container = Success30, onContainer = Success90),
        requestRejected = AsStatusColor(container = Error30, onContainer = Error90),
        requestCancelled = AsStatusColor(container = Neutral30, onContainer = Neutral90),
        doctorPending = AsStatusColor(container = Warning30, onContainer = Warning90),
        doctorApproved = AsStatusColor(container = Success30, onContainer = Success90),
        doctorRejected = AsStatusColor(container = Error30, onContainer = Error90),
        doctorSuspended = AsStatusColor(container = Violet30, onContainer = Violet90),
    )

/** Always provided by [AarogyaTheme] -- the light default here only
 *  guards a `@Preview` or test that renders a component without going
 *  through that wrapper. */
val LocalAsExtendedColors = staticCompositionLocalOf { LightExtendedColors }
