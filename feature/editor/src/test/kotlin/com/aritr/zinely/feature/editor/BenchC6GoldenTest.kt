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
 * The golden net for [BenchInkPopover] — the frozen `.inkpop` / `.inklbl` / `.pot` / `.preset`
 * ([ADR-096](../../../../../../../docs/DECISIONS.md#adr-096) rows 6.1b, 6.1c, 6.3c, 6.5b, 6.6, 6.10d,
 * 6.12c), light + dark, re-skinned to V2.1 by
 * [ADR-102](../../../../../../../docs/DECISIONS.md#adr-102) package P4.
 *
 * **This is where C6's painted properties are asserted, and the recorded raster alone does not assert
 * them.** Roborazzi compares at `changeThreshold = 0.02f`, and every property in this file is smaller
 * than that: a 1.6px dashed ink ring around a 30dp circle, a 1.5px ink border, a 1.7-unit stroke on a 13dp
 * glyph. Each would survive its own deletion in a threshold comparison — the lesson C3, C4 and C5 each had
 * to learn — so they are counted in pixels here, and the image keeps its real job: catching what nobody
 * thought to assert.
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

    private var surfaceArgb = 0
    private var inkArgb = 0

    private fun host(darkTheme: Boolean, selected: Color?) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                surfaceArgb = ZinelyTheme.v21Colors.surface.toArgb()
                inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v21Colors.desk),
                ) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.TEXT),
                        presets = benchInkPresets(inks),
                        selected = selected,
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
     * Rows 6.1b/6.1c — the card's ground and its border, plus the recorded light frame.
     *
     * The 37596 amendment moves the chrome ground to `--surface`; the outline remains a
     * 1.5dp real `--ink` (`v21-bench.html:237-238`). The border is counted as a **run length** rather than
     * as a pixel total: a count cannot tell 1.5px from 3px without knowing the density.
     */
    @Test
    fun the_popover_stands_on_the_frozen_surface_behind_an_ink_border() {
        host(darkTheme = false, selected = null)
        composeRule.onNodeWithTag(BenchInkPopoverTestTag, useUnmergedTree = true).assertExists()
        val card = crop(BenchInkPopoverTestTag, full())
        assertTrue(
            "the popover did not paint its --surface ground",
            card.pixelCountOf(surfaceArgb) > 5_000,
        )
        assertEquals(
            // In PIXELS, so the expectation is the frozen 1.5dp scaled by this raster's density — at xhdpi
            // a correct border is 3px wide and a literal `1` would fail a correct implementation.
            "`.inkpop{border:1.5px solid var(--ink)}` — measured as a run inward from the left edge",
            px(1.5f).roundToInt(),
            card.leftBorderThickness(inkArgb),
        )
        // Row 6.1d — `border-radius:var(--br-lg)`, 22dp. A corner is a handful of pixels on a 336dp card,
        // far under the 2 % threshold, so it is read directly: 3px in from the top-left corner is INSIDE a
        // square card and OUTSIDE a 22dp-rounded one.
        assertTrue(
            "the top-left corner is filled, so the card is drawing no 22dp radius",
            card.getPixel(3, 3) != surfaceArgb,
        )
        assertEquals(
            "…and the same card IS paper well inside the radius",
            surfaceArgb,
            card.getPixel(px(20f).toInt(), px(20f).toInt()),
        )

        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_light.png", roborazziOptions = aa())
    }

    /**
     * The dark frame, and with it row 6.18: the popover is **chrome over the artifact**, so it takes the
     * *room's* `--paper` and not the sheet island's. Drawn under the island it would carry the light
     * `ink` onto a dark fill — the 1.05:1 defect C2b measured on a device and the reason every overlay
     * since is composed inside the room's provider.
     */
    @Test
    fun the_popover_dims_with_the_room_because_it_is_chrome() {
        host(darkTheme = true, selected = null)
        val card = crop(BenchInkPopoverTestTag, full())
        val cardBounds = composeRule.onNodeWithTag(BenchInkPopoverTestTag).fetchSemanticsNode().boundsInRoot
        val currentBounds = composeRule.onNodeWithTag(BenchInkCurrentTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the current-ink paper must span the tray in dark theme (${currentBounds.width}/${cardBounds.width})",
            currentBounds.width > cardBounds.width * 0.8f,
        )
        assertTrue(
            "the dark popover did not paint the ROOM's --surface",
            card.pixelCountOf(surfaceArgb) > 5_000,
        )
        composeRule.onNodeWithTag(HOST_TAG)
            .captureRoboImage("$GOLDEN_DIR/bench_ink_popover_dark.png", roborazziOptions = aa())
    }

    /**
     * **Inverted by ADR-102 P4 — the pot no longer wears a halo, and must not grow one back.**
     *
     * V2's `.sw2{box-shadow:0 0 0 1px var(--desk-edge)}` drew a hairline ring outside the swatch's own
     * box. `.pot` (`v21-bench.html:250-251`) declares no `box-shadow` at all: the 1.5dp ink border inside
     * the box is the whole of its edge now. The test is kept, pointing the other way, because a ring drawn
     * outside the bounds is invisible to every bounds-based assertion in [BenchC6Test] — so its return
     * would be caught nowhere else.
     *
     * Sampled just outside the pot's own left edge at mid-height, where only the card's ground should be.
     */
    @Test
    fun no_swatch_carries_a_halo_outside_its_paint() {
        host(darkTheme = false, selected = null)
        val bmp = full()
        val r = swatchRect(0)
        val y = (r.top + r.height / 2f).toInt()
        val outside = bmp.getPixel((r.left - 2f).toInt(), y)
        assertEquals(
            "a ring is being drawn outside the pot; V2.1 gives it a border and nothing else",
            surfaceArgb,
            outside,
        )
    }

    /**
     * Row 6.6 — the `.pot.on` ring, in pixels, and **only** on the matching pot.
     *
     * V2.1 draws it as `1.6px dashed var(--ink)` at `inset:-5px` (`v21-bench.html:252-253`), where V2 drew
     * a 1.5px **solid** `--matcha`. Counted in `ink`, against a baseline taken with nothing chosen — the
     * card is full of ink already (its border, its title, every pot's own ring), so the *delta* is the
     * only readable quantity.
     *
     * One composition, two rasters — Robolectric permits `setContent` once per rule, and this test's
     * first cut called `host` twice and died on that. The element's ink is hoisted into state instead,
     * which is also closer to what the screen does: the ring is a function of the document, so moving
     * the document is the honest way to move the ring.
     */
    @Test
    fun the_selection_ring_is_drawn_dashed_in_ink_on_exactly_one_swatch() {
        val selected = mutableStateOf<Color?>(null)
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                inkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.v21Colors.desk),
                ) {
                    BenchInkPopover(
                        visible = true,
                        bands = benchInkBands(inks, BenchVerbKind.TEXT),
                        presets = benchInkPresets(inks),
                        selected = selected.value,
                        onPick = {},
                        onPreset = {},
                        onDone = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val baseline = full()
        val unringedForest = cropToBounds(baseline, swatchRect(1)).pixelCountOf(inkArgb)
        val unringedInk = cropToBounds(baseline, swatchRect(9)).pixelCountOf(inkArgb)

        selected.value = Color(0xFF3E5E3A)
        composeRule.waitForIdle()
        val ringed = cropToBounds(full(), swatchRect(1)).pixelCountOf(inkArgb)
        assertTrue(
            "no ink ring appeared when the element's ink matched a pot ($unringedForest -> $ringed)",
            ringed > unringedForest,
        )

        // The ring is 1.6dp on a circle of radius 15 + 5 + .8dp — at most `2*PI*20.8*1.6` px² of ink, and
        // rather less because it is DASHED. The band is generous because antialiasing on a curve is not
        // exactly countable and the dash rhythm is an approximation; what it refuses is a ring drawn on
        // all fourteen pots, which would be an order of magnitude more.
        val one = 2 * PI * px(20.8f) * px(1.6f)
        assertTrue(
            "the ring measures ${ringed - unringedForest}px, which is not one ring of at most " +
                "${one.toInt()}px — fourteen would be ${(one * 14).toInt()}",
            (ringed - unringedForest).toDouble() < one * 3,
        )

        // The colour that appears in TWO bands. `Ink #2A251E` is both a riso ink and a neutral
        // (`ZinelyContentInks.kt:219`, `:231`), and both bands are drawn for a text target — so this is
        // the one value that can ring twice. `< one * 3` above would not have noticed two rings; this
        // measures the same ink again with the same instrument and a bound that only one ring fits.
        selected.value = Color(0xFF2A251E)
        composeRule.waitForIdle()
        val duplicated = cropToBounds(full(), swatchRect(9)).pixelCountOf(inkArgb) - unringedInk
        assertTrue(
            "the duplicated ink drew ${duplicated}px of ring; one is at most ${one.toInt()}px " +
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
            // a 30dp pot would blow straight through it.
            val dot = PI * px(6.5f) * px(6.5f)
            val n = card.pixelCountOf(tint.value.toArgb())
            assertTrue(
                "${benchInkName(tint.id)} paints ${n}px, more than the ${dot.toInt()}px a preset dot can " +
                    "account for — the fenced band is being drawn to a text element",
                n < dot * 2,
            )
        }
    }

    /**
     * Row 6.10d — `.preset i b{margin-left:-5px}` (`v21-bench.html:260`). The overlap is what makes three
     * circles read as one recipe, and it is measured in the raster rather than computed: three 13dp dots
     * at a −5dp margin span **29dp**, and three at no margin would span 39. V2 drew 12dp dots at −4.
     *
     * The span is read from `Warm zine`'s own colours — leftmost `Brick` to rightmost `Cream` — so it
     * cannot accidentally measure the pill, its border, or its label.
     */
    @Test
    fun a_recipes_dots_overlap_by_the_frozen_five_dp() {
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
        // 38dp end to end, less the 1.5dp ink border each dot carries on its outer edge.
        assertEquals(
            "three 16dp dots at a -5dp margin span 38dp; at no margin they would span 48",
            px(38f) - px(3f),
            (last - first).toFloat(),
            px(2.5f),
        )
    }
}
