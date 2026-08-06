package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.runtime.mutableStateOf
import kotlin.math.PI
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The golden net for [BenchInkPopover] — the frozen `.inkpop` / `.band` / `.sw2` / `.preset` / `.inkuse`
 * ([ADR-096](../../../../../../../docs/DECISIONS.md#adr-096) rows 6.1b, 6.1c, 6.3c, 6.5b, 6.6, 6.10d,
 * 6.12c), light + dark.
 *
 * **This is where C6's painted properties are asserted, and the recorded raster alone does not assert
 * them.** Roborazzi compares at `changeThreshold = 0.02f`, and every property in this file is smaller
 * than that: a 1px `--desk-edge` halo around a 26dp circle, a 1.5px `--matcha` ring, a 1.7-unit stroke on
 * a 13dp glyph. Each would survive its own deletion in a threshold comparison — the lesson C3, C4 and C5
 * each had to learn — so they are counted in pixels here, and the image keeps its real job: catching what
 * nobody thought to assert.
 *
 * Two of the counts are the ones the ruling turns on. The `.sel` ring must appear **only** on the swatch
 * the element's own ink matches, and the fenced `Paper tints` must paint **nothing** for a text target —
 * a fence that is real in the band list and invisible in the raster would be no fence at all.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchC6GoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchInkPopoverGoldenHost"

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private val inks = zinelyContentInks()

    private var sheetArgb = 0
    private var chromeLineArgb = 0
    private var deskEdgeArgb = 0
    private var matchaArgb = 0
    private var inkFaintArgb = 0

    private fun host(darkTheme: Boolean, selected: Color?, inkCount: Int = 2) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                sheetArgb = ZinelyTheme.v2Colors.sheet.toArgb()
                chromeLineArgb = ZinelyTheme.v2Colors.chromeLine.toArgb()
                deskEdgeArgb = ZinelyTheme.v2Colors.deskEdge.toArgb()
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                inkFaintArgb = ZinelyTheme.v2Colors.inkFaint.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v2Colors.desk),
                ) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.TEXT),
                        presets = benchInkPresets(inks),
                        selected = selected,
                        inkCount = inkCount,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun crop(tag: String, full: Bitmap): Bitmap = cropToBounds(
        full,
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot,
    )

    private fun full() = composeRule.activity.window.decorView.rasterizeToBitmap()

    private fun swatchRect(index: Int) =
        composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()[index].boundsInRoot

    /** The drawn thickness, in px, of a border of [argb] on this bitmap's left edge at mid-height. */
    private fun Bitmap.leftBorderThickness(argb: Int): Int {
        val y = height / 2
        var x = 0
        while (x < width && getPixel(x, y) != argb) x++
        var n = 0
        while (x < width && getPixel(x, y) == argb) { x++; n++ }
        return n
    }

    private fun px(dp: Float) = with(composeRule.density) { dp.dp.toPx() }

    // =================================================================================================

    /**
     * Rows 6.1b/6.1c — the card's ground and its hairline, plus the recorded light frame.
     *
     * The hairline is counted as a **run length** rather than as a pixel total: a count cannot tell 1px
     * from 2px without knowing the density, and 1px is what the freeze declares.
     */
    @Test
    fun the_popover_stands_on_the_frozen_sheet_behind_a_one_pixel_hairline() {
        host(darkTheme = false, selected = null)
        composeRule.onNodeWithTag(BenchInkPopoverTestTag, useUnmergedTree = true).assertExists()
        val card = crop(BenchInkPopoverTestTag, full())
        assertTrue(
            "the popover did not paint its --sheet ground",
            card.pixelCountOf(sheetArgb) > 5_000,
        )
        assertEquals(
            // In PIXELS, so the expectation is the frozen 1dp scaled by this raster's density — at xhdpi
            // a correct hairline is 2px wide and a literal `1` would fail a correct implementation.
            "`.inkpop{border:1px solid var(--chrome-line)}` — measured as a run inward from the left edge",
            px(1f).roundToInt(),
            card.leftBorderThickness(chromeLineArgb),
        )
        // Row 6.1d — `border-radius:16px`. A corner is a handful of pixels on a 336dp card, far under
        // the 2 % threshold, so it is read directly: 3px in from the top-left corner is INSIDE a square
        // card and OUTSIDE a 16dp-rounded one.
        assertTrue(
            "the top-left corner is filled, so the card is drawing no 16dp radius",
            card.getPixel(3, 3) != sheetArgb,
        )
        assertEquals(
            "…and the same card IS sheet well inside the radius",
            sheetArgb,
            card.getPixel(px(20f).toInt(), px(20f).toInt()),
        )

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_light.png", roborazziOptions = aa())
    }

    /**
     * The dark frame, and with it row 6.18: the popover is **chrome over the artifact**, so it takes the
     * *room's* `--sheet` and not the sheet island's. Drawn under the island it would carry the light
     * `ink` onto a dark fill — the 1.05:1 defect C2b measured on a device and the reason every overlay
     * since is composed inside the room's provider.
     */
    @Test
    fun the_popover_dims_with_the_room_because_it_is_chrome() {
        host(darkTheme = true, selected = null)
        val card = crop(BenchInkPopoverTestTag, full())
        assertTrue(
            "the dark popover did not paint the ROOM's --sheet",
            card.pixelCountOf(sheetArgb) > 5_000,
        )
        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_dark.png", roborazziOptions = aa())
    }

    /**
     * Row 6.5b — `.sw2{box-shadow:0 0 0 1px var(--desk-edge)}`, a ring drawn **outside** the 26dp box and
     * therefore invisible to every bounds-based assertion in [BenchC6Test].
     *
     * Sampled just outside the swatch's own left edge at mid-height, where nothing else paints.
     */
    @Test
    fun every_swatch_carries_the_frozen_hairline_halo_outside_its_paint() {
        host(darkTheme = false, selected = null)
        val bmp = full()
        val r = swatchRect(0)
        val y = (r.top + r.height / 2f).toInt()
        val outside = bmp.getPixel((r.left - 1f).toInt(), y)
        assertEquals(
            "the 1px --desk-edge ring is missing from the swatch's outside edge",
            deskEdgeArgb,
            outside,
        )
    }

    /**
     * Row 6.6 — the `.sel` ring, in pixels, and **only** on the matching swatch.
     *
     * One composition, two rasters — Robolectric permits `setContent` once per rule, and this test's
     * first cut called `host` twice and died on that. The element's ink is hoisted into state instead,
     * which is also closer to what the screen does: the ring is a function of the document, so moving
     * the document is the honest way to move the ring.
     */
    @Test
    fun the_selection_ring_is_drawn_in_matcha_on_exactly_one_swatch() {
        val selected = mutableStateOf<Color?>(null)
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v2Colors.desk),
                ) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.TEXT),
                        presets = benchInkPresets(inks),
                        selected = selected.value,
                        inkCount = 2,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val unringed = full().pixelCountOf(matchaArgb)

        selected.value = Color(0xFF3E5E3A)
        composeRule.waitForIdle()
        val ringed = full().pixelCountOf(matchaArgb)
        assertTrue(
            "no --matcha appeared when the element's ink matched a swatch ($unringed -> $ringed)",
            ringed > unringed,
        )

        // The ring is 1.5dp on a circle of radius 13 + 5 + .75dp — about `2*PI*18.75*1.5` px² of ink.
        // The band is generous because antialiasing on a curve is not exactly countable; what it refuses
        // is a ring drawn on all fourteen swatches, which would be an order of magnitude more.
        val one = 2 * PI * px(18.75f) * px(1.5f)
        assertTrue(
            "the --matcha ink measures ${ringed - unringed}px, which is not one ring of about " +
                "${one.toInt()}px — fourteen would be ${(one * 14).toInt()}",
            (ringed - unringed).toDouble() < one * 3,
        )

        // The colour that appears in TWO bands. `Ink #2A251E` is both a riso ink and a neutral
        // (`ZinelyContentInks.kt:219`, `:231`), and both bands are drawn for a text target — so this is
        // the one value that can ring twice. `< one * 3` above would not have noticed two rings; this
        // measures the same ink again with the same instrument and a bound that only one ring fits.
        selected.value = Color(0xFF2A251E)
        composeRule.waitForIdle()
        val duplicated = full().pixelCountOf(matchaArgb) - unringed
        assertTrue(
            "the duplicated ink drew ${duplicated}px of --matcha; one ring is about ${one.toInt()}px " +
                "and two would be ${(one * 2).toInt()}",
            duplicated.toDouble() < one * 1.6,
        )
    }

    /**
     * Row 6.9d — **the fence, in pixels.** A text element's popover must contain no paper tint anywhere.
     *
     * This is the assertion that would catch a fence that is honest in the band list and undone in the
     * composable — the shape of failure C5 shipped twice, where the table said one thing and the raster
     * said another.
     */
    @Test
    fun no_paper_tint_is_painted_anywhere_in_a_text_elements_popover() {
        host(darkTheme = false, selected = null)
        val card = crop(BenchInkPopoverTestTag, full())
        inks.paperTints.forEach { tint ->
            // `Cream` and `Sky` still appear as PRESET DOTS, which is correct and frozen — a recipe shows
            // its paper. So the count is bounded by one dot's area rather than required to be zero, and
            // a 26dp swatch would blow straight through it.
            val dot = PI * px(6f) * px(6f)
            val n = card.pixelCountOf(tint.value.toArgb())
            assertTrue(
                "${benchInkName(tint.id)} paints ${n}px, more than the ${dot.toInt()}px a preset dot can " +
                    "account for — the fenced band is being drawn to a text element",
                n < dot * 2,
            )
        }
    }

    /**
     * Row 6.12c — `.inkuse svg{stroke-width:1.7}` on a 13px viewBox. A weight, not a size: [BenchC6Test]
     * can measure the 13dp box and could not tell a hairline shield from a fat one.
     */
    @Test
    fun the_ink_note_shield_is_drawn_at_the_frozen_stroke_weight() {
        host(darkTheme = false, selected = null)
        val note = crop(BenchInkUseNoteTestTag, full())
        // The GLYPH's own box, not the note's. Measured across the whole note this assertion was
        // vacuous — the sentence beside the shield is drawn in the same `--ink-faint`, and its pixels
        // swamped the outline's, so a stroke at a quarter of the frozen weight passed (battery M26,
        // GREEN). Cropping to the leading 13dp leaves only the shield in frame.
        val glyphBox = Bitmap.createBitmap(note, 0, 0, px(13f).roundToInt(), note.height)
        val ink = glyphBox.pixelCountOf(inkFaintArgb)
        // The shield's outline is about 46 units of path on a 24-unit box scaled to 13dp, stroked at
        // 1.7dp. Computed from the frozen numbers rather than from the production constants, so a change
        // to either is visible here. Only fully-opaque core pixels match exactly, so the floor sits well
        // under the ideal area; what it refuses is a stroke at a fraction of the frozen weight.
        val glyph = 46f * (px(13f) / 24f) * px(1.7f)
        assertTrue(
            "the shield paints ${ink}px of --ink-faint inside its own 13dp box; at the frozen 1.7dp " +
                "stroke its outline covers about ${glyph.toInt()}px",
            ink > glyph * 0.25f,
        )
    }

    /**
     * Row 6.10d — `.preset .dots span{margin-right:-4px}`. The overlap is what makes three circles read
     * as one recipe, and it is measured in the raster rather than computed: three 12dp dots at a −4dp
     * margin span **28dp**, and three at no margin would span 36.
     *
     * The span is read from `Warm zine`'s own colours — leftmost `Brick` to rightmost `Cream` — so it
     * cannot accidentally measure the pill, its hairline, or its label.
     */
    @Test
    fun a_recipes_dots_overlap_by_the_frozen_four_dp() {
        host(darkTheme = false, selected = null)
        val pill = cropToBounds(
            full(),
            composeRule.onAllNodesWithTag(BenchInkPresetTestTag, useUnmergedTree = true)
                .fetchSemanticsNodes()[1].boundsInRoot,
        )
        val brick = inks[com.aritr.zinely.ui.theme.ZinelyMakerInkId.Brick].value.toArgb()
        val cream = inks[com.aritr.zinely.ui.theme.ZinelyPaperTintId.Cream].value.toArgb()
        var first = pill.width
        var last = -1
        for (y in 0 until pill.height) for (x in 0 until pill.width) {
            val p = pill.getPixel(x, y)
            if (p == brick && x < first) first = x
            if (p == cream && x > last) last = x
        }
        assertTrue("neither end of the recipe was painted (first=$first last=$last)", last > first)
        // 28dp end to end, less the 1.5dp `--sheet` border each dot carries on its outer edge.
        assertEquals(
            "three 12dp dots at a -4dp margin span 28dp; at no margin they would span 36",
            px(28f) - px(3f),
            (last - first).toFloat(),
            px(2.5f),
        )
    }
}
