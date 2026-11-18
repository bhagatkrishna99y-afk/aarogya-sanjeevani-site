package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** How much smaller the initials are than the avatar's own diameter --
 *  tuned so two initials sit comfortably inside the circle at every
 *  [AsAvatar] call site in this app (40dp through 72dp). */
private const val INITIALS_SIZE_DIVISOR = 2.2f

private const val RING_WHITE_WIDTH_DP = 3f
private const val RING_TINT_WIDTH_DP = 2f

/** A person's circular avatar: [imageUrl] when there is one and it loads,
 *  initials from [name] otherwise -- the fallback is never a generic
 *  person glyph, since initials still tell a patient/doctor list rows
 *  apart from each other at a glance. Violet fill per the approved
 *  redesign brief, with the brief's white-ring-then-tint-ring halo
 *  (drawn just outside the circle, so it never eats into [size]). */
@Composable
fun AsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 40.dp,
) {
    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    val showImage = imageUrl != null && !loadFailed
    val tintRingColor = MaterialTheme.colorScheme.primaryContainer
    val whiteRingColor = Color.White
    // The ring halo is drawn inside the component's own `size` footprint
    // (the fill circle shrinks to make room) rather than overflowing it,
    // so a call site reserving exactly `size` of layout space never gets
    // clipped or overlapped by neighbouring content.
    val fillSize = size - (RING_WHITE_WIDTH_DP + RING_TINT_WIDTH_DP).dp * 2

    Box(
        modifier =
            modifier
                .size(size)
                .drawBehind {
                    val radius = this.size.minDimension / 2f
                    drawCircle(color = tintRingColor, radius = radius)
                    drawCircle(color = whiteRingColor, radius = radius - RING_TINT_WIDTH_DP.dp.toPx())
                }
                .clearAndSetSemantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(fillSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            if (showImage) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(fillSize).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { loadFailed = true },
                )
            } else {
                Text(
                    text = initialsOf(name),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = initialsFontSize(fillSize, LocalDensity.current),
                )
            }
        }
    }
}

/** A plain `.sp` size here would scale with the user's system font-size
 *  setting while [AsAvatar]'s own `size` (a [Dp]) never does -- at a
 *  large enough setting the initials outgrow the fixed circle and
 *  clip/overlap whatever sits next to it (seen at 200% font scale).
 *  Deriving the [TextUnit] from the avatar's own [Dp] size via
 *  [Density.toSp] instead keeps the initials' rendered size tied to the
 *  avatar, immune to `fontScale`, the same way the circle itself
 *  already is -- pulled out of the composable so this is testable
 *  without a live Composition. */
internal fun initialsFontSize(
    size: Dp,
    density: Density,
): TextUnit = with(density) { (size / INITIALS_SIZE_DIVISOR).toSp() }

private fun initialsOf(name: String): String =
    name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

@Preview(name = "Avatar - light")
@Preview(name = "Avatar - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsAvatarPreview() {
    AarogyaTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                AsAvatar(name = "Asha Rao")
                AsAvatar(name = "Jane Doe", size = 56.dp, modifier = Modifier.padding(start = 12.dp))
                AsAvatar(name = "X", modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}
