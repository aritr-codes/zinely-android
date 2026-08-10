package com.aritr.zinely.ui.theme

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the V2.1 grain against its frozen source, and pins the one thing about it that differs from V2
 * in a way a device can feel: **paper grain has no API floor and chrome grain does.**
 *
 * Robolectric, unlike [ZinelyV2GrainTest] which is plain JVM: `BlendMode.isSupported()` reads
 * `Build.VERSION.SDK_INT`, so the API-floor assertions are only meaningful under a runner that can set
 * it. Asserting them against an unmocked `SDK_INT` of 0 would pass for the wrong reason.
 */
@RunWith(RobolectricTestRunner::class)
class ZinelyV21GrainTest {

    @Test
    fun `the tile matches the frozen background-size`() {
        assertEquals(160.dp, ZinelyV21Grain.SourceTileSize)
    }

    @Test
    fun `the baked alpha is the corpus figure and is the same in all three prototypes`() {
        // v21-{library,proof,bench}.html all write opacity='.42' on the SVG rect. V2's two definitions
        // disagreed (.5 in two files, none in the third), which is why ZinelyV2Grain has to publish an
        // effective column to compare surfaces at all. V2.1 does not have that problem.
        assertEquals(0.42f, ZinelyV21Grain.BakedAlpha, 0.0001f)
    }

    @Test
    fun `V2_1 is a different material from V2, not a restyle of it`() {
        // Every parameter moved: .85 vs .9, 3 octaves vs 2, 160 vs 140, unstitched vs stitched. The
        // tile sizes are the part reachable from Kotlin, and they must not converge — a future tidy
        // that pointed both at one asset would silently repaint every V2 paper surface.
        assertNotEquals(ZinelyV2Grain.SourceTileSize, ZinelyV21Grain.SourceTileSize)
        assertNotEquals(ZinelyV2Grain.BakedAlpha, ZinelyV21Grain.BakedAlpha)
    }

    @Test
    fun `paper and chrome blend differently, and that is the whole point`() {
        assertEquals(BlendMode.Multiply, ZinelyV21Grain.PaperBlend)
        assertEquals(BlendMode.Softlight, ZinelyV21Grain.ChromeBlend)
        // V2 blends all ten of its grain rules soft-light. If these two ever agree, V2.1 has lost the
        // distinction between the paper and the room it lies on.
        assertNotEquals(ZinelyV21Grain.PaperBlend, ZinelyV21Grain.ChromeBlend)
    }

    @Test
    @Config(sdk = [29])
    fun `both materials draw on API 29`() {
        assertTrue(ZinelyV21Grain.IsSupported)
        assertTrue(BlendMode.Multiply.isSupported())
        assertTrue(BlendMode.Softlight.isSupported())
    }

    @Test
    @Config(sdk = [24])
    fun `neither material draws on minSdk, multiply included`() {
        // This test is why the class exists, and it failed on its first run against the opposite
        // assertion. The reasoning that produced that assertion was: `multiply` has a PorterDuff
        // equivalent and predates android.graphics.BlendMode (API 29), so V2.1's paper grain ought to
        // survive where V2's all-soft-light grain could not. It does not — Compose reports
        // isSupported() false below 29 for multiply too, and the docs were written from what the
        // platform ought to do rather than from what it does.
        //
        // Pinned in that direction deliberately. If a future Compose makes multiply supported here,
        // this test fails, and that failure is the signal to REVISIT the D-014 carry-over — not to
        // delete the assertion.
        assertFalse("soft-light cannot exist below API 29", BlendMode.Softlight.isSupported())
        assertFalse("nor, measured, can multiply", BlendMode.Multiply.isSupported())
        assertFalse(ZinelyV21Grain.IsSupported)
    }
}
