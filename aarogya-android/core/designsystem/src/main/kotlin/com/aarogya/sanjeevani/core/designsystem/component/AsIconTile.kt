// AsIconTileColor lives here, next to the AsIconTile this file is really about.
@file:Suppress("MatchingDeclarationName")

package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.Amber50
import com.aarogya.sanjeevani.core.designsystem.theme.Amber95
import com.aarogya.sanjeevani.core.designsystem.theme.Coral40
import com.aarogya.sanjeevani.core.designsystem.theme.Coral95
import com.aarogya.sanjeevani.core.designsystem.theme.Success40
import com.aarogya.sanjeevani.core.designsystem.theme.Success95
import com.aarogya.sanjeevani.core.designsystem.theme.Violet40
import com.aarogya.sanjeevani.core.designsystem.theme.Violet95
import com.aarogya.sanjeevani.core.designsystem.theme.isReducedMotionEnabled

/** One colored container/foreground pair for [AsIconTile] -- the
 *  approved redesign brief assigns one fixed color per *kind* of care
 *  (coral/violet/amber/teal/green), not per screen, so services and
 *  categories read consistently wherever a patient sees them again. */
enum class AsIconTileColor {
    Coral,
    Violet,
    Amber,
    Teal,
    Green,
}

private data class TileColorPair(val container: Color, val content: Color)

@Composable
private fun AsIconTileColor.toPair(): TileColorPair =
    when (this) {
        AsIconTileColor.Coral -> TileColorPair(Coral95, Coral40)
        AsIconTileColor.Violet -> TileColorPair(Violet95, Violet40)
        AsIconTileColor.Amber -> TileColorPair(Amber95, Amber50)
        AsIconTileColor.Teal ->
            TileColorPair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        AsIconTileColor.Green -> TileColorPair(Success95, Success40)
    }

private const val PRESS_SCALE = 1.08f
private const val PRESS_ROTATE_DEGREES = -6f

/**
 * A rounded-square tile with a colored container and matching icon --
 * every service category, notification kind, and "browse" entry point
 * uses one of these so a patient learns to recognize a kind of care by
 * its color/shape, not just its label. Rotates and grows slightly on
 * press (the brief's own motion cue for these specifically); when
 * [onClick] is null it's purely decorative and skips the press motion
 * and interaction semantics entirely.
 */
@Composable
fun AsIconTile(
    icon: ImageVector,
    color: AsIconTileColor,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    iconSize: Dp = 24.dp,
    onClick: (() -> Unit)? = null,
) {
    val pair = color.toPair()
    val shape = MaterialTheme.shapes.medium
    if (onClick == null) {
        Surface(
            modifier = modifier.size(size),
            shape = shape,
            color = pair.container,
            contentColor = pair.content,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
            }
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val reducedMotion = isReducedMotionEnabled()
        val scale by
            animateFloatAsState(
                targetValue = if (pressed && !reducedMotion) PRESS_SCALE else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "AsIconTilePressScale",
            )
        val rotation by
            animateFloatAsState(
                targetValue = if (pressed && !reducedMotion) PRESS_ROTATE_DEGREES else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "AsIconTilePressRotation",
            )
        Surface(
            onClick = onClick,
            modifier =
                modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    },
            shape = shape,
            color = pair.container,
            contentColor = pair.content,
            interactionSource = interactionSource,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Preview(name = "Icon tiles - light")
@Preview(name = "Icon tiles - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsIconTilePreview() {
    AarogyaTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                AsIconTile(icon = Icons.Outlined.Favorite, color = AsIconTileColor.Coral)
                AsIconTile(
                    icon = Icons.Outlined.MonitorHeart,
                    color = AsIconTileColor.Violet,
                    modifier = Modifier.padding(start = 10.dp),
                )
                AsIconTile(
                    icon = Icons.Outlined.Science,
                    color = AsIconTileColor.Amber,
                    modifier = Modifier.padding(start = 10.dp),
                )
                AsIconTile(
                    icon = Icons.Outlined.Home,
                    color = AsIconTileColor.Teal,
                    modifier = Modifier.padding(start = 10.dp),
                )
                AsIconTile(
                    icon = Icons.Outlined.Add,
                    color = AsIconTileColor.Green,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}
