package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
 * `medium` shape (16dp) -- Material3's own `Card` default is sharper
 * than this design system's "premium, rounded surfaces" brief calls for.
 *
 * Passing [onClick] makes the whole card a single 48dp+ touch target
 * (never a smaller tap zone nested inside it) and gives it click
 * semantics for TalkBack; omit it for a purely static card.
 */
@Composable
fun AsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AsTheme.spacing.lg),
    content: @Composable () -> Unit,
) {
    val cardModifier = modifier.sizeIn(minHeight = if (onClick != null) 48.dp else 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            elevation = CardDefaults.cardElevation(defaultElevation = AsTheme.elevation.level1),
        ) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    } else {
        Card(
            modifier = cardModifier,
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
