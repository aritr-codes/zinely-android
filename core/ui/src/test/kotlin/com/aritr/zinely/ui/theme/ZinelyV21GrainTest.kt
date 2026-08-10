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
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

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

    private val tile = File("src/main/res/drawable-nodpi/zinely_v21_grain.png")

    @Test
    fun `the tile is the exact asset its provenance claims`() {
        assertTrue("expected to run with :core:ui as the working directory — missing $tile", tile.isFile)
        val sha = MessageDigest.getInstance("SHA-256").digest(tile.readBytes())
            .joinToString("") { "%02x".format(it) }
        // tools/grain/gen_grain.py v21: fractalNoise, baseFrequency .85, numOctaves 3, unstitched,
        // seed 0, 160x160, saturate 0, linearRGB -> sRGB. Same inputs reproduce this byte for byte.
        assertEquals("f97351546930d028dbfe82501896a79b2129e5d84fbed8d03484f8bb7684e385", sha)
        assertEquals(58330L, tile.length())
        val img = ImageIO.read(tile)
        assertEquals(160, img.width)
        assertEquals(160, img.height)
    }

    @Test
    fun `the tile's own statistics are the ones the rendering model is built on`() {
        // ZinelyV21Grain documents what a rendered surface's contrast should be, and that prediction
        // takes these four numbers as inputs. A regenerated tile that changed them would leave the
        // documented model quietly wrong — the first version of that model already was, for a
        // different reason (it omitted alpha), which is why the inputs are pinned rather than retyped.
        val img = ImageIO.read(tile)
        val luma = ArrayList<Double>(img.width * img.height)
        val alpha = ArrayList<Double>(img.width * img.height)
        val modulated = ArrayList<Double>(img.width * img.height)
        for (y in 0 until img.height) {
            for (x in 0 until img.width) {
                val p = img.getRGB(x, y)
                val a = ((p ushr 24) and 0xFF) / 255.0
                val r = ((p ushr 16) and 0xFF) / 255.0
                val g = ((p ushr 8) and 0xFF) / 255.0
                val b = (p and 0xFF) / 255.0
                // Desaturated by feColorMatrix, so the three channels agree; Rec.709 regardless.
                val l = 0.2126 * r + 0.7152 * g + 0.0722 * b
                luma += l
                alpha += a
                modulated += a * (1.0 - l)
            }
        }
        assertEquals(0.7322, luma.mean(), 0.0005)
        assertEquals(0.0598, luma.sd(), 0.0005)
        assertEquals(0.5002, alpha.mean(), 0.0005)
        assertEquals(0.1185, alpha.sd(), 0.0005)
        // The quantity a `multiply` at effective alpha actually modulates. 0.04232, not luma's 0.0598.
        assertEquals(0.04232, modulated.sd(), 0.0002)
    }

    private fun List<Double>.mean() = sum() / size

    private fun List<Double>.sd(): Double {
        val m = mean()
        return kotlin.math.sqrt(sumOf { (it - m) * (it - m) } / size)
    }

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
