package com.aarogya.sanjeevani.patient.auth.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.isReducedMotionEnabled

/**
 * This app's brand mark -- a rounded badge with a pulse/cross motif,
 * drawn directly with [Canvas] primitives rather than an imported
 * asset, so it's original artwork and scales/recolors (light/dark,
 * [tint]) for free. [clearAndSetSemantics] with no description: it's
 * decorative next to the "Aarogya Sanjeevani" wordmark [WelcomeScreen]
 * already renders as real text, so TalkBack would otherwise announce
 * the same name twice. Freehand decorative geometry below -- each
 * ratio is a one-off proportion of this specific drawing, not a
 * reusable quantity, so naming each as its own constant would add
 * noise rather than clarity.
 */
@Suppress("MagicNumber")
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(
        modifier =
            modifier
                .size(64.dp)
                .clearAndSetSemantics {},
    ) {
        val badgeRadius = size.minDimension / 2f
        drawCircle(color = tint, radius = badgeRadius)

        val crossColor = Color.White
        val armLength = size.minDimension * 0.42f
        val armThickness = size.minDimension * 0.14f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawRoundRect(
            color = crossColor,
            topLeft = Offset(center.x - armThickness / 2f, center.y - armLength / 2f),
            size = Size(armThickness, armLength),
            cornerRadius = CornerRadius(armThickness / 2f),
        )
        drawRoundRect(
            color = crossColor,
            topLeft = Offset(center.x - armLength / 2f, center.y - armThickness / 2f),
            size = Size(armLength, armThickness),
            cornerRadius = CornerRadius(armThickness / 2f),
        )
    }
}

/**
 * The Welcome screen's illustration -- an abstract "remote consultation"
 * scene (a device showing a pulse line, a floating status dot) built
 * entirely from [Canvas] primitives, original artwork rather than an
 * imported asset. Purely decorative: [clearAndSetSemantics] keeps
 * TalkBack from describing shapes that carry no information on their
 * own -- the value proposition next to it is the real content.
 */
private const val FLOAT_AMPLITUDE_DP = 8f
private const val FLOAT_DURATION_MS = 4000

/** The brief's "gentle float" for the Welcome screen's logo/illustration
 *  -- a slow +/-8dp vertical drift, skipped entirely under reduced
 *  motion rather than snapped to a fixed offset, since a non-looping
 *  static illustration needs no "settled" position of its own. */
@Composable
private fun rememberFloatOffset(): Float {
    if (isReducedMotionEnabled()) return 0f
    val transition = rememberInfiniteTransition(label = "welcome-float")
    val offset by
        transition.animateFloat(
            initialValue = -FLOAT_AMPLITUDE_DP,
            targetValue = FLOAT_AMPLITUDE_DP,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(FLOAT_DURATION_MS / 2, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "welcome-float-offset",
        )
    return offset
}

@Suppress("MagicNumber")
@Composable
fun WelcomeIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val floatOffset = rememberFloatOffset()

    Canvas(
        modifier =
            modifier
                .size(240.dp)
                .aspectRatio(1f)
                .offset(y = floatOffset.dp)
                .clearAndSetSemantics {},
    ) {
        val w = size.width
        val h = size.height

        // Soft backdrop blob.
        drawCircle(color = primaryContainer, radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.48f))

        // The "device": a rounded card showing a pulse line.
        val deviceSize = Size(w * 0.58f, h * 0.72f)
        val deviceTopLeft = Offset((w - deviceSize.width) / 2f, (h - deviceSize.height) / 2f)
        drawRoundRect(
            color = surface,
            topLeft = deviceTopLeft,
            size = deviceSize,
            cornerRadius = CornerRadius(w * 0.06f),
        )
        drawRoundRect(
            color = primary,
            topLeft = deviceTopLeft,
            size = deviceSize,
            cornerRadius = CornerRadius(w * 0.06f),
            style = Stroke(width = w * 0.012f),
        )

        val pulseY = deviceTopLeft.y + deviceSize.height * 0.52f
        val pulseLeft = deviceTopLeft.x + deviceSize.width * 0.12f
        val pulseRight = deviceTopLeft.x + deviceSize.width * 0.88f
        val pulseHeight = deviceSize.height * 0.16f
        val pulsePath =
            Path().apply {
                moveTo(pulseLeft, pulseY)
                lineTo(pulseLeft + deviceSize.width * 0.18f, pulseY)
                lineTo(pulseLeft + deviceSize.width * 0.26f, pulseY - pulseHeight)
                lineTo(pulseLeft + deviceSize.width * 0.34f, pulseY + pulseHeight)
                lineTo(pulseLeft + deviceSize.width * 0.42f, pulseY)
                lineTo(pulseRight, pulseY)
            }
        drawPath(path = pulsePath, color = primary, style = Stroke(width = w * 0.018f))

        // A small floating "online" status dot, top-right of the device.
        drawCircle(
            color = tertiary,
            radius = w * 0.07f,
            center = Offset(deviceTopLeft.x + deviceSize.width + w * 0.02f, deviceTopLeft.y + h * 0.03f),
        )
    }
}
