package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Grain
import com.aritr.zinely.ui.theme.zinelyContentInks
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the cover actually rasterises — read back out of the bitmap, not asserted against the source.
 *
 * **Why this reads pixels.** Phase A's record names the failure this file exists to avoid: *"three
 * packages in a row shipped an assertion blind to the defect class it claimed to gate"*. A semantics
 * test cannot see a band drawn with the wrong blend mode, a fore-edge on the wrong side, a fold spine
 * that vanished, or a grain that stopped being drawn. Every one of those composes cleanly.
 *
 * **The two API levels are the method, not duplication.** Below API 29 the paper grain draws nothing at
 * all (**D-014**; [ZinelyV2Grain.IsSupported] is the predicate), and that is the only way to get *exact*
 * frozen colours out of a rasterised cover — at API 29+ soft-light noise perturbs every pixel by design.
 * So colour transcription is asserted at **sdk 28**, where each pixel has one unambiguous right answer,
 * and the grain's own presence is asserted at **sdk 29**, where its absence would be the defect.
 *
 * **Rasterisation goes through [rasterizeToBitmap]**, not `captureToImage()`: under Robolectric
 * `NATIVE`, compose-ui-test's capture never gets its window-redraw handshake and times out, and
 * Roborazzi's capture is a no-op without `-Proborazzi.test.record`. That lesson is recorded in
 * `ComposeCanvasProbeTest` and the same harness backs `PagePreviewParityTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp")
class ZineCoverRenderTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val TITLE = "Sunday market"
        const val HOST = "cover-host"
        const val REST = "cover-rest"
        const val PRESSED = "cover-pressed"

        /** 140dp of cover — at density 1 that is 140px, so every frozen inset is several pixels wide. */
        val CELL = 140.dp

        /** Room for the cast shadow to land on the desk rather than off the raster. */
        val GUTTER = 24.dp

        fun tagOf(surface: ZineCoverSurface) = "cover-${surface.name}"
    }

    private val inks = zinelyContentInks()

    // ---------------------------------------------------------------------------------------------
    // Geometry
    // ---------------------------------------------------------------------------------------------

    @Test
    @Config(sdk = [28])
    fun `the cover is a 3 to 4 sheet`() {
        single(ZineCoverSurface.MatchaInk)
        val bounds = composeRule.onNodeWithTag(tagOf(ZineCoverSurface.MatchaInk)).fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "expected 3:4, measured ${bounds.width}×${bounds.height}",
            abs(bounds.height * 3f / 4f - bounds.width) <= 1f,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Colour transcription, grain-free (sdk 28)
    // ---------------------------------------------------------------------------------------------

    @Test
    @Config(sdk = [28])
    fun `every surface prints its frozen stock`() {
        sheetOfAllSix()
        val decor = decorRaster()
        for (surface in ZineCoverSurface.entries) {
            val cover = crop(decor, tagOf(surface))
            // 20% down the middle: below the stamp's row, above the band's 33% edge, clear of both
            // vertical edges — the one region of a cover that is nothing but stock.
            assertColour("$surface stock", surface.palette(inks).fill, cover.colourAt(0.5f, 0.20f))
        }
    }

    @Test
    @Config(sdk = [29])
    fun `the band is ink multiplied into the paper, not ink laid on top of it`() {
        assertTrue("this test needs a platform that blends multiply", BlendMode.Multiply.isSupported())
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val palette = ZineCoverSurface.MatchaInk.palette(inks)

        // The grain is live at this API level, so the comparison is on the region's mean rather than a
        // single pixel — and the two candidate renderings are 0.14 apart in red, an order of magnitude
        // above the noise, so the mean still tells them apart decisively.
        val measured = meanRed(cover, from = 0.35f, to = 0.45f)
        val multiplied = 0.9f * (palette.fill.red * palette.band.red) + 0.1f * palette.fill.red
        val laidOnTop = 0.9f * palette.band.red + 0.1f * palette.fill.red
        assertTrue(
            "the band must read as multiplied ink (measured $measured, multiply $multiplied, " +
                "src-over $laidOnTop)",
            abs(measured - multiplied) < abs(measured - laidOnTop),
        )
    }

    @Test
    @Config(sdk = [28])
    fun `below API 29 the band is omitted rather than approximated`() {
        // D-018, ruled 2026-07-30 on D-014's precedent: `BlendMode.Multiply` has no PorterDuff
        // equivalent, so Compose composites it as SRC_OVER below Q — a sticker where the design asks for
        // ink. The ruling is omit, do not emulate and do not substitute another blend mode. So the whole
        // band region must be *exactly* the stock: not a lighter band, not a tint, nothing.
        //
        // An earlier build drew the band anyway and asserted the src-over value here. That assertion
        // passing is what the ruling reversed, so this test is the same pixels read for the opposite
        // answer — which is also why it cannot pass vacuously: a drawn band of any kind fails it.
        assertFalse("sdk 28 must not claim multiply support", BlendMode.Multiply.isSupported())
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val stock = ZineCoverSurface.MatchaInk.palette(inks).fill
        // Every row the band would have covered (33%→47%), clear of the crease and the fore-edge.
        for (fy in listOf(0.34f, 0.38f, 0.42f, 0.46f)) {
            val y = (cover.height * fy).roundToInt()
            val unprinted = (dpToPx(13f) until cover.width - dpToPx(6f))
                .all { cover.colourAt(it, y) == stock }
            assertTrue("no band may print at $fy on a platform that cannot multiply", unprinted)
        }
    }

    @Test
    @Config(sdk = [29])
    fun `the band spans exactly the frozen thirty-three to forty-seven percent`() {
        // Read at sdk 29, because that is now the only API level where the band is drawn at all (D-018).
        // The grain is live here, so the edges are found from row means rather than single pixels: the
        // band darkens a row by ~0.14 in red where the grain moves it by well under 0.01.
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val stockRow = rowMeanRed(cover, 0.20f)
        // Halfway between "stock" and "fully banded" — any row past the boundary clears it decisively.
        val threshold = (stockRow + rowMeanRed(cover, 0.40f)) / 2f
        val banded = (0 until cover.height).filter { rowMeanRed(cover, it) < threshold }

        assertTrue("the band must print at all", banded.isNotEmpty())
        // A band that started at 32% or ran to 50% would still look like a band; these two edges are
        // what make the frozen fractions assertable rather than merely present. One pixel of tolerance
        // for the boundary row's own partial coverage.
        assertEquals("the band's top edge is 33% of the sheet", 0.33f * cover.height, banded.first().toFloat(), 1.5f)
        assertEquals("the band's bottom edge is 47%", 0.47f * cover.height, banded.last().toFloat(), 1.5f)
    }

    @Test
    @Config(sdk = [28])
    fun `the fore-edge darkens the open side and only the open side`() {
        single(ZineCoverSurface.PaperMatchaBand)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.PaperMatchaBand))
        val stock = ZineCoverSurface.PaperMatchaBand.palette(inks).fill
        val y = (cover.height * 0.20f).roundToInt()

        val foreEdge = cover.colourAt(cover.width - 2, y)
        assertTrue(
            "the fore-edge must darken the open side (found $foreEdge against stock $stock)",
            foreEdge.luminance() < stock.luminance() - 0.01f,
        )
        // The bound edge carries a highlight, not a shadow. Sampled at x=4: clear of the 1px inner
        // hairline at x=0..1 and of the fold spine at x=9.
        assertColour("the bound edge is not a fore-edge", stock, cover.colourAt(4, y))
    }

    @Test
    @Config(sdk = [28])
    fun `the fold spine is a hairline highlight nine pixels in from the bound edge`() {
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val stock = ZineCoverSurface.MatchaInk.palette(inks).fill
        val y = cover.height / 2 // mid-sheet: inside the spine's 6px top and bottom insets
        val spineX = dpToPx(9f)

        val spine = cover.colourAt(spineX, y)
        assertTrue(
            "the crease must read as a highlight, not a line (found $spine against stock $stock)",
            spine.luminance() > stock.luminance(),
        )
        assertColour("four pixels right of the crease is plain stock", stock, cover.colourAt(spineX + 4, y))
    }

    @Test
    @Config(sdk = [28])
    fun `a mirrored layout does not mirror the printed object`() {
        // The frozen file states every mark of this object **physically**: `border-radius:6px 9px 9px 6px`,
        // the crease at `left:9px`, the fore-edge at `right:0`. Compose's default `RoundedCornerShape` is
        // *logical* and mirrors under RTL, while a `Brush` drawn at an absolute offset does not — so the
        // first implementation would have put the tight bound-edge radius on the right while the crease
        // stayed on the left, giving a cover creased down its own cut edge. Nothing about that composes
        // wrongly and no LTR screenshot shows it.
        //
        // The trilogy contains no RTL reading, so the fix is literal transcription (all physical, nothing
        // mirrors) and the localisation question — should an RTL shelf show right-bound books? — is raised
        // for the owner instead of being answered by an implementation detail.
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Host {
                    ZineCover(
                        title = TITLE,
                        recipe = ZineCoverRecipe(ZineCoverSurface.PaperMatchaBand, ZineCoverStamp.Sun),
                        modifier = Modifier
                            .padding(GUTTER)
                            .width(CELL)
                            .testTag(tagOf(ZineCoverSurface.PaperMatchaBand)),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.PaperMatchaBand))
        val stock = ZineCoverSurface.PaperMatchaBand.palette(inks).fill
        val y = cover.height / 2

        assertTrue(
            "the crease must stay 9px from the physical left even in RTL",
            cover.colourAt(dpToPx(9f), y).luminance() > stock.luminance(),
        )
        assertTrue(
            "the fore-edge must stay on the physical right even in RTL",
            cover.colourAt(cover.width - 2, y).luminance() < stock.luminance() - 0.01f,
        )
        // The corner radii are the half that used to mirror: sample the pixel just inside each top
        // corner. At (2px, 3px) in from the corner, the two radii disagree (6px fills it a full pixel
        // clear of its own arc; 9px still cuts it) — a 7px inset does not, since both radii already
        // close their arc by then and the sample reads the surrounding hairline instead of the corner
        // it names. (3px, 1px) sits almost exactly on the 6px arc itself and anti-aliases, which is the
        // failure this replaces.
        val insideBoundCorner = cover.colourAt(dpToPx(2f), dpToPx(3f))
        val insideCutCorner = cover.colourAt(cover.width - 1 - dpToPx(2f), dpToPx(3f))
        assertColour("the 6px bound corner is filled a pixel down", stock, insideBoundCorner)
        assertTrue(
            "the 9px cut corner must still be cut away at the same offset (found $insideCutCorner)",
            insideCutCorner != stock,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The grain, where the platform can blend it (sdk 29)
    // ---------------------------------------------------------------------------------------------

    @Test
    @Config(sdk = [29])
    fun `the grain is in the paper where the platform can blend soft-light`() {
        assertTrue("this test needs a platform that blends soft-light", ZinelyV2Grain.IsSupported)
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val fill = ZineCoverSurface.MatchaInk.palette(inks).fill
        // 25% down: below the stamp's own row (which ends 34px from the top) and above the band.
        val y = (cover.height * 0.25f).roundToInt()
        val varies = (dpToPx(13f) until cover.width - dpToPx(6f))
            .any { abs(cover.colourAt(it, y).luminance() - fill.luminance()) > 0.004f }
        assertTrue("a grained cover must not be a flat fill", varies)
    }

    @Test
    @Config(sdk = [28])
    fun `a cover that cannot blend soft-light is flat stock, not an approximation`() {
        // D-014, ruled: where the platform cannot express the material, implementation omits and
        // discloses. This asserts the omission is total — no tint, no substitute blend, nothing.
        assertFalse("sdk 28 must not claim soft-light support", ZinelyV2Grain.IsSupported)
        single(ZineCoverSurface.MatchaInk)
        val cover = crop(decorRaster(), tagOf(ZineCoverSurface.MatchaInk))
        val fill = ZineCoverSurface.MatchaInk.palette(inks).fill
        // 25% down: below the stamp's own row (which ends 34px from the top) and above the band.
        val y = (cover.height * 0.25f).roundToInt()
        // Between the fold spine and the fore-edge: the span that is stock and nothing else.
        val flat = (dpToPx(13f) until cover.width - dpToPx(6f)).all { cover.colourAt(it, y) == fill }
        assertTrue("an ungrained cover must be exactly its fill", flat)
    }

    // ---------------------------------------------------------------------------------------------
    // The press, and the title
    // ---------------------------------------------------------------------------------------------

    @Test
    @Config(sdk = [28])
    fun `pressing settles the cover toward the desk instead of lifting it`() {
        // Both states in one composition, side by side at the same y — the only way to compare two
        // shadow sets in a raster without re-entering setContent.
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().testTag(HOST)) {
                    Row(Modifier.padding(GUTTER), horizontalArrangement = Arrangement.spacedBy(GUTTER)) {
                        ZineCover(
                            title = TITLE,
                            recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
                            pressed = false,
                            modifier = Modifier.width(CELL).testTag(REST),
                        )
                        ZineCover(
                            title = TITLE,
                            recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
                            pressed = true,
                            modifier = Modifier.width(CELL).testTag(PRESSED),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        val decor = decorRaster()
        val rest = castShadowWeight(decor, REST)
        val pressed = castShadowWeight(decor, PRESSED)
        assertTrue(
            "a pressed cover must cast less shadow than a resting one (rest $rest, pressed $pressed)",
            pressed < rest,
        )
    }

    @Test
    @Config(sdk = [28])
    fun `the title is real text a screen reader can read`() {
        single(ZineCoverSurface.MatchaInk)
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [28])
    fun `a long title clamps to two lines instead of climbing over the band`() {
        val long = "A title far too long to sit on any little cover at all"
        single(ZineCoverSurface.MatchaInk, title = long)
        val height = composeRule.onNodeWithText(long).fetchSemanticsNode().size.height
        // Two lines of 18.56sp at a 1.2 line-height ≈ 45px at density 1, plus `.ct`'s 1px bottom pad.
        assertTrue("a clamped title must not grow past two lines (measured $height)", height <= 52)
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun single(surface: ZineCoverSurface, title: String = TITLE) {
        composeRule.setContent {
            Host {
                ZineCover(
                    title = title,
                    recipe = ZineCoverRecipe(surface, ZineCoverStamp.Sun),
                    modifier = Modifier.padding(GUTTER).width(CELL).testTag(tagOf(surface)),
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** All six frozen surfaces in one composition, so one `setContent` can answer for all of them. */
    private fun sheetOfAllSix() {
        composeRule.setContent {
            Host {
                Column(
                    Modifier.padding(GUTTER),
                    verticalArrangement = Arrangement.spacedBy(GUTTER),
                ) {
                    ZineCoverSurface.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(GUTTER)) {
                            row.forEach { surface ->
                                ZineCover(
                                    title = TITLE,
                                    recipe = ZineCoverRecipe(surface, ZineCoverStamp.Sun),
                                    modifier = Modifier.width(CELL).testTag(tagOf(surface)),
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Host(content: @Composable () -> Unit) {
        ZinelyTheme(darkTheme = false) {
            Box(Modifier.fillMaxSize().testTag(HOST)) { content() }
        }
    }

    private fun decorRaster(): Bitmap {
        assertEquals(
            "these pixel offsets assume dp == px; density was ${composeRule.density.density}",
            1.0f,
            composeRule.density.density,
            0.0001f,
        )
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    /** The tagged node's own pixels, cut out of the decor raster at its actual placed bounds. */
    private fun crop(decor: Bitmap, tag: String): Bitmap {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val x = bounds.left.roundToInt()
        val y = bounds.top.roundToInt()
        val w = bounds.width.roundToInt()
        val h = bounds.height.roundToInt()
        require(x >= 0 && y >= 0 && x + w <= decor.width && y + h <= decor.height) {
            "$tag at ($x,$y,${x + w},${y + h}) falls outside the raster ${decor.width}×${decor.height}"
        }
        return Bitmap.createBitmap(decor, x, y, w, h)
    }

    /**
     * How much dark the cast shadow lays on the desk just under a cover — the one observable that
     * separates the rest and pressed shadow sets in a raster.
     */
    private fun castShadowWeight(decor: Bitmap, tag: String): Float {
        val bounds = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val top = bounds.bottom.roundToInt() + 1
        val bottom = minOf(top + dpToPx(12f), decor.height - 1)
        val left = bounds.left.roundToInt() + dpToPx(20f)
        val right = minOf(bounds.right.roundToInt() - dpToPx(20f), decor.width - 1)
        var weight = 0f
        var counted = 0
        for (y in top..bottom) {
            for (x in left..right) {
                weight += 1f - Color(decor.getPixel(x, y)).luminance()
                counted++
            }
        }
        require(counted > 0) { "no desk pixels sampled below $tag" }
        return weight / counted
    }

    private fun dpToPx(dp: Float): Int = (dp * composeRule.density.density).roundToInt()

    /** Mean red of one row, clear of both vertical edges. */
    private fun rowMeanRed(cover: Bitmap, y: Int): Float {
        val xs = dpToPx(13f) until cover.width - dpToPx(6f)
        return xs.sumOf { cover.colourAt(it, y).red.toDouble() }.toFloat() / xs.count()
    }

    private fun rowMeanRed(cover: Bitmap, fy: Float): Float =
        rowMeanRed(cover, (cover.height * fy).roundToInt())

    /** Mean red across the rows between two height fractions, clear of both vertical edges. */
    private fun meanRed(cover: Bitmap, from: Float, to: Float): Float {
        var sum = 0f
        var counted = 0
        for (y in (cover.height * from).roundToInt()..(cover.height * to).roundToInt()) {
            for (x in dpToPx(13f) until cover.width - dpToPx(6f)) {
                sum += cover.colourAt(x, y).red
                counted++
            }
        }
        require(counted > 0) { "no band pixels sampled" }
        return sum / counted
    }

    private fun Bitmap.colourAt(fx: Float, fy: Float): Color =
        colourAt((width * fx).roundToInt(), (height * fy).roundToInt())

    private fun Bitmap.colourAt(x: Int, y: Int): Color = Color(getPixel(x, y))

    private fun assertColour(what: String, expected: Color, actual: Color) {
        // One 8-bit step of tolerance: an exact value still rounds through the bitmap's own channels.
        val tolerance = 1.5f / 255f
        assertEquals("$what red ($actual against $expected)", expected.red, actual.red, tolerance)
        assertEquals("$what green ($actual against $expected)", expected.green, actual.green, tolerance)
        assertEquals("$what blue ($actual against $expected)", expected.blue, actual.blue, tolerance)
    }

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
}
