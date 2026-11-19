package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.Teal40
import com.aarogya.sanjeevani.core.designsystem.theme.isReducedMotionEnabled

private val CIRCLE_SIZE = 96.dp
private val CHECK_ICON_SIZE = 48.dp
private val RIPPLE_STROKE_WIDTH = 2.dp

private const val POP_IN_DURATION_MS = 450
private const val POP_IN_START_SCALE = 0.5f
private const val POP_IN_OVERSHOOT_SCALE = 1.12f
private const val POP_IN_OVERSHOOT_FRACTION_MS = POP_IN_DURATION_MS * 2 / 3
private const val POP_IN_RESTING_SCALE = 1f

private const val RIPPLE_DURATION_MS = 2200
private const val RIPPLE_MAX_SCALE = 1.5f
private const val RIPPLE_MAX_ALPHA = 1f

/**
 * The confirmation graphic for the moment a request has actually been
 * sent -- a solid [Teal40] circle (the brief's exact brand teal, not
 * the theme's `primary` token, which in dark mode is a lighter tint
 * that would wash this celebratory moment out) with a white check,
 * popping in once with a slight overshoot and then pulsing a soft
 * expanding ring on a loop for as long as this stays on screen. Both
 * animations are skipped under reduced motion -- the circle and check
 * simply render at rest with no ring, per [isReducedMotionEnabled].
 */
@Composable
fun AsSuccessCheck(modifier: Modifier = Modifier) {
    val reducedMotion = isReducedMotionEnabled()

    val popScale = remember { Animatable(if (reducedMotion) POP_IN_RESTING_SCALE else POP_IN_START_SCALE) }
    LaunchedEffect(reducedMotion) {
        if (!reducedMotion) {
            popScale.animateTo(
                targetValue = POP_IN_RESTING_SCALE,
                animationSpec =
                    keyframes {
                        durationMillis = POP_IN_DURATION_MS
                        POP_IN_START_SCALE at 0
                        POP_IN_OVERSHOOT_SCALE at POP_IN_OVERSHOOT_FRACTION_MS
                        POP_IN_RESTING_SCALE at POP_IN_DURATION_MS
                    },
            )
        }
    }

    Box(
        modifier =
            modifier
                .size(CIRCLE_SIZE)
                .semantics(mergeDescendants = true) { contentDescription = "Request sent" },
        contentAlignment = Alignment.Center,
    ) {
        if (!reducedMotion) {
            SuccessRipple()
        }
        Surface(
            modifier = Modifier.size(CIRCLE_SIZE).scale(popScale.value),
            shape = CircleShape,
            color = Teal40,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(CHECK_ICON_SIZE),
                    tint = Color.White,
                )
            }
        }
    }
}

/** The expanding, fading ring pulsing behind [AsSuccessCheck]'s circle
 *  on a loop -- split out purely to keep that composable's own body
 *  short; this is never meant to be called from anywhere else. */
@Composable
private fun SuccessRipple() {
    val transition = rememberInfiniteTransition(label = "success-ripple")
    val rippleProgress by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(RIPPLE_DURATION_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "success-ripple-progress",
        )
    val rippleScale = POP_IN_RESTING_SCALE + rippleProgress * (RIPPLE_MAX_SCALE - POP_IN_RESTING_SCALE)
    Box(
        modifier =
            Modifier
                .size(CIRCLE_SIZE)
                .scale(rippleScale)
                .alpha(RIPPLE_MAX_ALPHA - rippleProgress)
                .border(RIPPLE_STROKE_WIDTH, Teal40, CircleShape),
    )
}

@Preview(name = "Success check - light")
@Preview(name = "Success check - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsSuccessCheckPreview() {
    AarogyaTheme {
        Surface {
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                AsSuccessCheck()
            }
        }
    }
}
