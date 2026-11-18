package com.aarogya.sanjeevani.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user has asked the system to reduce motion (Settings >
 * Accessibility > Remove animations, which drives the animator duration
 * scale to 0) -- every looping/entrance/press animation this design
 * system adds must check this first and skip straight to the animation's
 * end state instead of running. There is no dedicated Compose
 * "reduced motion" API on the OS versions this app supports, so this
 * reads the same system setting Android's own animation framework
 * reads, directly.
 */
@Composable
fun isReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val animatorScale =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    return animatorScale == 0f
}
