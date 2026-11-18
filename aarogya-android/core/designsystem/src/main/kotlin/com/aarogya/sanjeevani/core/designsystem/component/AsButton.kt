// AsButtonVariant lives here, next to the AsButton this file is really about.
@file:Suppress("MatchingDeclarationName")

package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.isReducedMotionEnabled

/** [AsButton]'s visual treatment. [Danger] is the red-outline style for
 *  a destructive-but-not-yet-committed action (Cancel request); it is
 *  visually an outlined button, never a filled one, so it never reads
 *  as more "done" than a Primary action before the user confirms. */
enum class AsButtonVariant {
    Primary,
    Secondary,
    Danger,
    Text,
}

private const val BUTTON_HEIGHT_DP = 54
private const val PRESS_SCALE = 0.97f

/**
 * The one button every primary/secondary/danger/text action in either
 * app should use. Loading state keeps the label on screen and adds a
 * spinner alongside it -- never replaces it, so a screen reader doesn't
 * lose the action's name mid-load (same convention the original
 * scaffold's `AarogyaButton` already established).
 *
 * Always 54dp tall regardless of [variant] or content, per the approved
 * redesign brief (well above this design system's 48dp touch-target
 * floor). Presses scale down slightly with a spring, per the same
 * brief's motion spec.
 */
@Composable
fun AsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AsButtonVariant = AsButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val isEnabled = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = isReducedMotionEnabled()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed && !reducedMotion) PRESS_SCALE else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "AsButtonPressScale",
        )
    val buttonModifier =
        modifier
            .defaultMinSize(minHeight = BUTTON_HEIGHT_DP.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                if (loading) stateDescription = "Loading"
                if (!enabled) disabled()
            }
    val shape = MaterialTheme.shapes.medium

    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }

    AsButtonForVariant(
        variant = variant,
        onClick = onClick,
        modifier = buttonModifier,
        enabled = isEnabled,
        shape = shape,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
private fun AsButtonForVariant(
    variant: AsButtonVariant,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit,
) {
    when (variant) {
        AsButtonVariant.Primary ->
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = { content() },
            )
        AsButtonVariant.Secondary ->
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                content = { content() },
            )
        AsButtonVariant.Danger ->
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                interactionSource = interactionSource,
                colors = dangerButtonColors(),
                border = dangerButtonBorder(enabled),
                content = { content() },
            )
        AsButtonVariant.Text ->
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                interactionSource = interactionSource,
                content = { content() },
            )
    }
}

private const val DANGER_BORDER_WIDTH_DP = 1.5f

@Composable
private fun dangerButtonColors() = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)

@Composable
private fun dangerButtonBorder(enabled: Boolean) =
    ButtonDefaults.outlinedButtonBorder(enabled = enabled)
        .copy(width = DANGER_BORDER_WIDTH_DP.dp, brush = SolidColor(MaterialTheme.colorScheme.error))

@Preview(name = "Buttons - light")
@Preview(name = "Buttons - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsButtonPreview() {
    AarogyaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsButton(text = "Book appointment", onClick = {})
                AsButton(text = "View details", onClick = {}, variant = AsButtonVariant.Secondary)
                AsButton(text = "Cancel request", onClick = {}, variant = AsButtonVariant.Danger)
                AsButton(text = "Cancel", onClick = {}, variant = AsButtonVariant.Text)
                AsButton(text = "Booking...", onClick = {}, loading = true)
                AsButton(text = "Unavailable", onClick = {}, enabled = false)
            }
        }
    }
}
