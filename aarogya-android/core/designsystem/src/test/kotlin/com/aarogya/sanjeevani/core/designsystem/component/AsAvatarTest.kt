package com.aarogya.sanjeevani.core.designsystem.component

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsAvatarTest {
    @Test
    fun `initials render at the same pixel size regardless of the system font scale`() {
        // The raw Sp *value* is expected to differ between these two --
        // that's exactly how dividing by fontScale cancels it back out
        // once rendered. What must stay constant is the actual pixel
        // size, which is what a clipping/overlap bug is really about.
        val normalDensity = Density(density = 2f, fontScale = 1f)
        val doubleScaleDensity = Density(density = 2f, fontScale = 2f)

        val pxAtNormalScale = with(normalDensity) { initialsFontSize(48.dp, normalDensity).toPx() }
        val pxAtDoubleScale = with(doubleScaleDensity) { initialsFontSize(48.dp, doubleScaleDensity).toPx() }

        assertEquals(pxAtNormalScale, pxAtDoubleScale, 0.01f)
    }

    @Test
    fun `initials font size still scales with the avatar's own dp size`() {
        val small = initialsFontSize(24.dp, Density(density = 2f, fontScale = 1f))
        val large = initialsFontSize(72.dp, Density(density = 2f, fontScale = 1f))

        assertTrue(large.value > small.value)
    }
}
