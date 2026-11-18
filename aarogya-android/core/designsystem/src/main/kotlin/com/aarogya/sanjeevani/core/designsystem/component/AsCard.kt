package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * The base surface every list item/summary tile in either app builds
 * on, so a doctor-list row and a notification row share one elevation/
 * radius/padding convention instead of each screen inventing its own.
 * Rounded per [com.aarogya.sanjeevani.core.designsystem.theme.AarogyaShapes]'s
 * `large` shape (20dp, the approved redesign brief's card radius) with
 * a thin `outlineVariant` border -- a flat white surface with a hairline
 * edge reads calmer than a heavier shadow-only card.
 *
 * Passing [onClick] makes the whole card a single 48dp+ touch target
 * (never a smaller tap zone nested inside it), gives it click semantics
 * for TalkBack, and lifts its elevation while pressed (the brief's "tap
 * = lift" cue); omit it for a purely static card.
 */
@Composable
fun AsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AsTheme.spacing.lg),
    content: @Composable () -> Unit,
) {
    val cardModifier = modifier.sizeIn(minHeight = if (onClick != null) 48.dp else 0.dp)
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = AsTheme.elevation.level1,
                    pressedElevation = AsTheme.elevation.level2,
                ),
        ) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = AsTheme.elevation.level1),
        ) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    }
}

@Preview(name = "Card - light")
@Preview(name = "Card - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsCardPreview() {
    AarogyaTheme {
        Surface {
            AsCard(modifier = Modifier.padding(16.dp)) {
                Text("Dr. Asha Rao — General Medicine")
            }
        }
    }
}
