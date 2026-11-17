package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

private const val SHIMMER_ANIMATION_DURATION_MS = 800

/** Apply to the `Column`/`Box` wrapping a group of [SkeletonShimmer] bars
 *  so TalkBack announces "Loading" once for the whole group -- otherwise
 *  it's silent, since every bar clears its own semantics (see
 *  [SkeletonShimmer]'s own doc comment). */
fun Modifier.loadingGroupSemantics(label: String = "Loading"): Modifier = semantics { contentDescription = label }

/**
 * A loading placeholder bar -- a pulsing `surfaceVariant` block shaped
 * like the content it stands in for (a list row, a line of text).
 * Carries no semantics of its own ([clearAndSetSemantics] with nothing
 * set): a screen with several of these in a row should announce
 * "Loading" once for the whole list/section, not once per shimmer bar.
 */
@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val alpha by
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec =
                infiniteRepeatable(animation = tween(SHIMMER_ANIMATION_DURATION_MS), repeatMode = RepeatMode.Reverse),
            label = "skeleton-shimmer-alpha",
        )

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
                .clearAndSetSemantics {},
    )
}

@Preview(name = "Skeleton - light")
@Preview(name = "Skeleton - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SkeletonShimmerPreview() {
    AarogyaTheme {
        Surface {
            Column(modifier = Modifier.padding(AsTheme.spacing.lg)) {
                SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(20.dp))
                Box(modifier = Modifier.height(AsTheme.spacing.sm))
                SkeletonShimmer(modifier = Modifier.width(160.dp).height(16.dp))
            }
        }
    }
}
