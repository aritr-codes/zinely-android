package com.aritr.zinely.ui.catalog

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The **regression** half of A9's two-proof shape. [ZinelyV2CatalogParityTest] is the other half, and the
 * order between them is the whole point.
 *
 * A golden compares today's render to a PNG recorded from a previous render, so on its own it can only
 * certify *self-consistency*: record a golden of a catalog with the wrong cream in it and the gate stays
 * green for the life of the project, faithfully protecting the defect. Parity against the frozen corpus is
 * what establishes the values are right; these goldens exist to keep them right, and to catch the whole
 * class of drift a pixel probe cannot see — a shifted baseline, a lost hairline, a changed glyph, a swatch
 * that started rounding its corners.
 *
 * So a golden here is never evidence that the design was reproduced. It is evidence that nothing moved
 * since the run in which that was proven separately.
 *
 * ## Recording
 *
 * `./gradlew :core:ui:recordRoborazziDebug` writes into `src/test/roborazzi/`;
 * `:core:ui:verifyRoborazziDebug` is the gate. Under a plain `testDebugUnitTest` `captureRoboImage` is a
 * no-op, which is why every capture below is preceded by an assertion that runs in *both* modes — a golden
 * suite whose only failure mode is "the PNG differs" is green on a build where nothing was captured at all.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h1600dp")
class ZinelyV2CatalogGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun options() = RoborazziOptions(
        // Matches the V1 component goldens' tolerance, so a V2 golden is not held to a different standard
        // than the ones already in the tree.
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
    )

    /** Crop the current raster to the node tagged [tag]. */
    private fun section(tag: String): Bitmap {
        composeRule.waitForIdle()
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        return cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), bounds)
    }

    /**
     * Capture [tag] in both themes.
     *
     * The non-vacuity assertion is not decoration. A section that failed to lay out crops to a 1×1 clamp
     * and a section that never drew is one flat colour; either records a perfectly stable golden that
     * asserts nothing forever after. Both are checked before the PNG is written, so the failure surfaces
     * in the recording run rather than being baked into the baseline.
     */
    private fun captureBothThemes(name: String, tag: String, content: @Composable () -> Unit) {
        // One composition, theme toggled through state: `setContent` may be called only once per rule, so
        // the two themes cannot be two separate compositions inside one test.
        val dark = mutableStateOf(false)
        composeRule.setContent { ZinelyTheme(darkTheme = dark.value) { content() } }
        val captured = mutableMapOf<String, List<Int>>()
        listOf(false to "light", true to "dark").forEach { (isDark, suffix) ->
            composeRule.runOnUiThread { dark.value = isDark }
            val bmp = section(tag)
            assertTrue(
                "$name/$suffix laid out to ${bmp.width}x${bmp.height} — the section did not render",
                bmp.width > 32 && bmp.height > 32,
            )
            assertTrue(
                "$name/$suffix is a single flat colour, so the golden would assert nothing",
                bmp.pixelVariety() > 1,
            )
            captured[suffix] = bmp.pixels()
            bmp.captureRoboImage("$GOLDEN_DIR/v2_catalog_${name}_$suffix.png", options())
        }
        // A theme toggle that silently failed to recompose would record two identical PNGs, and both
        // would verify green forever — half the golden set asserting nothing about dark theme at all.
        assertTrue(
            "$name recorded pixel-identical light and dark goldens — the theme did not take effect",
            captured["light"] != captured["dark"],
        )
    }

    private fun Bitmap.pixels(): List<Int> =
        IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }.toList()

    private fun Bitmap.pixelVariety(): Int {
        val first = getPixel(0, 0)
        for (y in 0 until height) for (x in 0 until width) if (getPixel(x, y) != first) return 2
        return 1
    }

    @Test
    fun `palette golden`() {
        captureBothThemes("palette", ZinelyV2CatalogTags.Palette) { ZinelyV2CatalogPalette() }
    }

    @Test
    fun `type golden`() {
        captureBothThemes("type", ZinelyV2CatalogTags.Type) { ZinelyV2CatalogType() }
    }

    @Test
    fun `icons golden`() {
        captureBothThemes("icons", ZinelyV2CatalogTags.Icons) { ZinelyV2CatalogIcons() }
    }

    @Test
    fun `material golden`() {
        captureBothThemes("material", ZinelyV2CatalogTags.Material) { ZinelyV2CatalogMaterial() }
    }

    @Test
    fun `whole catalog golden`() {
        // The one image a human can read the foundation off in a single glance. The per-section goldens
        // are what localise a diff; this one is what makes a diff noticeable at review time.
        captureBothThemes("all", ZinelyV2CatalogTags.Root) { ZinelyV2Catalog() }
    }
}

private const val GOLDEN_DIR = "src/test/roborazzi"
