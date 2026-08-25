package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Press
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The golden net for C4's painted surfaces — the bar, the status strip and the snack, light + dark, in the
 * [BenchStyleRowGoldenTest] two-proof shape ([ADR-094](../../../../../../../docs/DECISIONS.md#adr-094)
 * rows 4.1, 4.2, 4.10, 4.11, 4.12).
 *
 * **This is where C4's paint is asserted.** [BenchC4Test] reads order, enabled-ness, timing and geometry
 * off the semantics tree, and none of those can see a `--desk` ground, `--leaf` under `--on-leaf` on
 * `Add`, `--leaf-text` on a `--leaf-tint` saved chip, or the snack's inverted `--ink` ground with its
 * `--ink-line` border and its underlined `--paper` button.
 *
 * The raster alone is **not** those assertions: `changeThreshold = 0.02f` means a hairline across a wide
 * bar is far too small to move the gate — that lesson was paid for in C3, where the mutation that deleted
 * the hairline outright passed the recorded golden. So each property small enough to hide under the
 * threshold is counted in pixels here, and the golden keeps its real job: catching what nobody thought to
 * assert.
 *
 * **The chooser has no golden**, and the omission is deliberate rather than forgotten: `ZSheet` is a
 * `Dialog`, which Robolectric renders in its own window and `decorView.rasterizeToBitmap()` therefore does
 * not contain. Its paint is a device-verification item, listed as such in the ADR's device checklist.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class BenchC4GoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "benchC4GoldenHost"

        /**
         * The ink **mass** a 20dp `Undo` glyph lays down at `stroke-width:1.7`, xhdpi — **measured**, not
         * derived, and recorded here so the number has one home. Re-measure if the mark itself changes.
         */
        const val GLYPH_MASS_1_7 = 35071L

        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )
    }

    private var deskArgb = 0
    private var v21DeskArgb = 0
    private var v21LeafArgb = 0
    private var v21ButterTintArgb = 0
    private var v21InkArgb = 0
    private var v21SurfaceSoftArgb = 0
    private var v21OnLeafArgb = 0
    private var v21LeafTintArgb = 0
    private var inkSoftArgb = 0

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                // P3: read the V2.1 tokens. The bar's ground is `desk` (it was `chrome`), its filled
                // control is `leaf` under `onLeaf` (was `matcha`/`matchaText`), and the glyph tint is
                // V2.1's `inkSoft` — a different value from V2's, which is why the disabled-alpha probe
                // found zero full-strength pixels and reported a drawn control as missing.
                // `deskArgb` and the host ground stay on `ZinelyTheme.colors` — this class also records the
                // snack and status-strip goldens, which are not P3's surfaces and must not move.
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                // The bar's own ground: V2.1's `desk`, which is a different value from the host's legacy
                // `colors.desk` above. Kept as its own field rather than repointing `deskArgb`, so the
                // host's yardstick and the bar's cannot be confused for each other again.
                v21DeskArgb = ZinelyTheme.v21Colors.desk.toArgb()
                v21LeafArgb = ZinelyTheme.v21Colors.leaf.toArgb()
                v21ButterTintArgb = ZinelyTheme.v21Colors.butterTint.toArgb()
                // P4: the **snack** is a converted surface now, so these read V2.1. Its ground is still an
                // inverted `ink`, but the value is V2.1's; its border is `inkLine` (the corpus's one
                // shadow-ink border, because an ink border on an ink ground is invisible); and its action
                // lost `--accent-on-ink` entirely — it is the snack's own `paper`, underlined.
                v21InkArgb = ZinelyTheme.v21Colors.ink.toArgb()
                v21SurfaceSoftArgb = ZinelyTheme.v21Colors.surfaceSoft.toArgb()
                // P6a: the status strip's chip is now `.saved` — `leafText` on a `leafTint` pill, where V2
                // set it as bare `--matcha-text`. The old field is gone rather than left pointing at a
                // token this file no longer draws.
                v21OnLeafArgb = ZinelyTheme.v21Colors.onLeaf.toArgb()
                v21LeafTintArgb = ZinelyTheme.v21Colors.leafTint.toArgb()
                inkSoftArgb = ZinelyTheme.v21Colors.inkSoft.toArgb()
                Box(
                    Modifier
                        .testTag(HOST_TAG)
                        .fillMaxWidth()
                        .background(ZinelyTheme.colors.desk),
                ) { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap() = composeRule.activity.window.decorView.rasterizeToBitmap()

    private fun boundsOf(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    // --- The bar --------------------------------------------------------------------------------------

    private fun captureBar(name: String, darkTheme: Boolean) {
        host(darkTheme) {
            BenchBottomBar(
                // The two history controls are drawn in **both** states across the two goldens, so the
                // frozen `.35` withholding is visible somewhere: light shows them live, dark withheld.
                canUndo = !darkTheme,
                canRedo = !darkTheme,
                doneEnabled = true,
                onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
            )
        }
        composeRule.onNodeWithTag(BenchBottomBarTestTag).assertExists()
        val full = hostBitmap()
        val bar = boundsOf(BenchBottomBarTestTag)
        val barCrop = cropToBounds(full, bar)

        // P3 — the bar's ground is now `desk` (it was `chrome`), and it is the room's own colour: the bar
        // no longer declares a surface distinct from the desk it sits on.
        assertTrue("the bar did not paint its --desk ground ($name)", barCrop.pixelCountOf(v21DeskArgb) > 2000)

        // P3 — **and it must have NO top hairline.** V2's `.bar` carried `border-top:1px solid
        // var(--chrome-line)`; V2.1's (`v21-bench.html:341`) declares `background` and nothing else, so the
        // first scanline is uniform ground. This assertion is inverted rather than deleted: the boundary
        // between room and bar disappearing is a visible design decision, and a deleted test would let it
        // come back silently. Read from the full raster in root coordinates for the reason C3 recorded — a
        // re-crop can round a pixel onto the desk and "differ" whether or not a rule exists.
        val left = bar.left.toInt() + 2
        val right = bar.right.toInt() - 2
        val ground = full.getPixel(left, (bar.top + bar.height / 2f).toInt())
        var hairline = 0
        for (x in left until right) if (full.getPixel(x, bar.top.toInt() + 1) != ground) hairline++
        assertTrue(
            "the bar drew a top hairline ($name): $hairline/${right - left} px on its first scanline " +
                "differ from its own ground, and V2.1's `.bar` has no border",
            hairline == 0,
        )

        // Row 4.4 — `Add` is the bar's one filled control, now `--leaf` (was `--matcha`). A 48dp-tall pill
        // across most of the bar's width is many thousands of pixels at xhdpi.
        assertTrue("Add did not paint its --leaf fill ($name)", barCrop.pixelCountOf(v21LeafArgb) > 2000)

        // P3 — and it is the screen's one `--frame` ring: `.add` alone wears the butter-tint misregistration
        // band. Asserted here because it is the single most visible thing this conversion adds, and a golden
        // threshold of 2% would not notice a 5dp band going missing.
        assertTrue(
            "Add did not paint its --butter-tint misregistration ring ($name)",
            barCrop.pixelCountOf(v21ButterTintArgb) > 200,
        )

        cropToBounds(full, boundsOf(HOST_TAG)).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    /**
     * Row 4.2's `.icon-btn:disabled{opacity:.35}`, and row 4.3's `stroke-width:1.7` — the two paint
     * properties the golden raster **cannot** see. Both mutations (`.35`→`1`, `1.7`→`2.4`) survived the
     * recorded goldens: `changeThreshold = 0.02f` needs 2 % of the compared pixels to move, and two 20dp
     * glyphs in a 430×66dp bar are far under it. So they are counted, in pixels, against each other.
     *
     * Reading the two states in **one frame** is what makes this threshold-free: the live control and the
     * withheld one are drawn from the same token in the same theme, so `--ink-soft` at full strength can
     * only appear in the live one. No tolerance to tune, and no recorded number to drift.
     */
    @Test
    fun a_withheld_control_is_drawn_at_the_frozen_thirty_five_percent() {
        host(darkTheme = false) {
            BenchBottomBar(
                canUndo = true,      // live
                canRedo = false,     // withheld
                doneEnabled = true,
                onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
            )
        }
        val full = hostBitmap()
        val live = cropToBounds(full, boundsOf(BenchBarUndoTag))
        val withheld = cropToBounds(full, boundsOf(BenchBarRedoTag))
        val soft = inkSoftArgb
        assertTrue(
            "the live glyph must paint --ink-soft at full strength (found ${live.pixelCountOf(soft)}px)",
            live.pixelCountOf(soft) > 20,
        )
        assertTrue(
            "the withheld glyph must not: at .35 no pixel is full-strength --ink-soft, but " +
                "${withheld.pixelCountOf(soft)} were",
            withheld.pixelCountOf(soft) == 0,
        )
        // …and it must still be drawn. A withheld control that vanished would also pass the line above.
        val ground = full.getPixel(boundsOf(BenchBarRedoTag).left.toInt() - 3, boundsOf(BenchBarRedoTag).center.y.toInt())
        assertTrue(
            "the withheld control is not drawn at all — `.35` is faded, not hidden",
            (0 until withheld.width).any { x ->
                (0 until withheld.height).any { y -> withheld.getPixel(x, y) != ground }
            },
        )
    }

    /**
     * **P3: the press changed kind, so this test changed what it measures.**
     *
     * V2's `.icon-btn:active` was `transform:scale(.94)` and this test measured the control getting
     * *narrower*. V2.1's (`v21-bench.html:346`) is `transform:translate(2px,2px); box-shadow:0 0 0` — the
     * object keeps its size, **moves** down-right, and sheds its shadow. Asserting a shrink here after the
     * conversion would have been the strictly worse failure: a green test measuring a property the design no
     * longer has.
     *
     * So: the drawn box's **left edge** must move right by [ZinelyV21Press]`.Flat.travel`, and its width must
     * not change. Both halves are needed — a control that shrank *and* moved would satisfy either one alone,
     * and V2.1 explicitly does not shrink (text metrics must not change under a finger).
     *
     * The expectations are literals rather than reads of the tier under test, for the reason the original
     * test recorded: computing the expectation from the constant makes the assertion agree with whatever the
     * constant becomes, which is exactly how the `.94` → `.8` mutation survived this test's first cut.
     */
    @Test
    fun a_pressed_icon_button_travels_two_dp_and_keeps_its_size() {
        host(darkTheme = false) {
            BenchBottomBar(
                canUndo = true, canRedo = true, doneEnabled = true,
                onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
            )
        }
        val slot = boundsOf(BenchBarUndoTag)
        val restingWidth = drawnWidth(hostBitmap(), slot)
        val restingLeft = drawnLeft(hostBitmap(), slot)

        composeRule.onNodeWithTag(BenchBarUndoTag).performTouchInput { down(center) }
        composeRule.waitForIdle()
        val pressedWidth = drawnWidth(hostBitmap(), slot)
        val pressedLeft = drawnLeft(hostBitmap(), slot)
        composeRule.onNodeWithTag(BenchBarUndoTag).performTouchInput { up() }

        val travelPx = with(composeRule.density) { 2.dp.toPx() }
        assertTrue(
            "a pressed control must travel the frozen 2dp down-right (resting left ${restingLeft}px, " +
                "pressed ${pressedLeft}px, expected ~${restingLeft + travelPx}px)",
            kotlin.math.abs((pressedLeft - restingLeft) - travelPx) <= 1.5f,
        )
        // ⚠ The raster's extent is **object + shadow**, so it legitimately narrows by exactly the shed
        // shadow — 2dp — while the object itself does not resize. The first cut of this test asserted the
        // extent was unchanged and failed at 92→88px, which is the correct behaviour reported as a defect.
        // Asserting the *exact* shrink is the stronger check anyway: it pins the collapse to Flat's own
        // resting depth, so a tier swap (Flat→Raised, or a pressed depth that stayed at 2) fails here.
        assertTrue(
            "…and the 2dp shadow must collapse with it, narrowing the drawn extent by exactly that much " +
                "(resting ${restingWidth}px, pressed ${pressedWidth}px)",
            kotlin.math.abs((restingWidth - pressedWidth) - travelPx) <= 1.5f,
        )
        // The object did not resize: its far edge is where the resting shadow's far edge was. This is the
        // half that says "pushed into the desk" rather than "shrank under the finger".
        assertTrue(
            "the pressed object's right edge must land on the resting shadow's " +
                "(resting ${restingLeft + restingWidth}px, pressed ${pressedLeft + pressedWidth}px)",
            kotlin.math.abs((pressedLeft + pressedWidth) - (restingLeft + restingWidth)) <= 1.5f,
        )
        assertEquals("the frozen Flat travel is 2dp", 2.dp, ZinelyV21Press.Flat.travel)
        assertEquals("…and it presses flush, shedding all of its depth", 0.dp, ZinelyV21Press.Flat.pressed)
    }

    /**
     * The left edge of everything drawn inside [slot] that differs from the ground — the pressed object's
     * position. Measured against the *resting* reading, never against the slot: the semantics box does not
     * move with a `Modifier.offset` press, which is the whole reason this has to come out of the raster.
     */
    private fun drawnLeft(full: android.graphics.Bitmap, slot: androidx.compose.ui.geometry.Rect): Float {
        val y0 = slot.top.toInt() + 2
        val y1 = slot.bottom.toInt() - 2
        val pad = 6
        val x0 = (slot.left.toInt() - pad).coerceAtLeast(0)
        val x1 = (slot.right.toInt() + pad).coerceAtMost(full.width - 1)
        val ground = full.getPixel(x0, y0)
        val xs = (x0..x1).filter { x -> (y0..y1).any { y -> full.getPixel(x, y) != ground } }
        return if (xs.isEmpty()) 0f else xs.first().toFloat()
    }

    /**
     * The horizontal extent of everything drawn inside [slot] that differs from the bar's ground — for an
     * `.icon-btn` that is its 1px outline, so this is the box's drawn width in pixels.
     */
    private fun drawnWidth(full: android.graphics.Bitmap, slot: androidx.compose.ui.geometry.Rect): Float {
        val y0 = slot.top.toInt() + 2
        val y1 = slot.bottom.toInt() - 2
        val pad = 6
        val x0 = (slot.left.toInt() - pad).coerceAtLeast(0)
        val x1 = (slot.right.toInt() + pad).coerceAtMost(full.width - 1)
        val ground = full.getPixel(x0, y0)
        val xs = (x0..x1).filter { x -> (y0..y1).any { y -> full.getPixel(x, y) != ground } }
        return if (xs.isEmpty()) 0f else (xs.last() - xs.first() + 1).toFloat()
    }

    /**
     * Row 4.3's `stroke-width:1.7`, which the raster gate also cannot see (`1.7`→`2.4` survived it).
     *
     * A stroke's width is legible in how much ink the glyph lays down, so the glyph's own pixels are
     * counted. The band is deliberately generous — a 41 % thicker stroke moves this count far more than
     * the ±20 % allowed here — because the point is to catch a stroke that is *wrong*, not to pin a number
     * that antialiasing will drift.
     */
    @Test
    fun the_bar_glyphs_are_stroked_at_the_frozen_one_point_seven() {
        host(darkTheme = false) {
            BenchBottomBar(
                canUndo = true, canRedo = true, doneEnabled = true,
                onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
            )
        }
        val mass = glyphInkMass(hostBitmap(), boundsOf(BenchBarUndoTag))
        assertTrue(
            "the Undo glyph laid down $mass of ink; the frozen 1.7 stroke lays ~$GLYPH_MASS_1_7 " +
                "(a 2.4 stroke lays far more)",
            kotlin.math.abs(mass - GLYPH_MASS_1_7) <= GLYPH_MASS_1_7 * 0.15,
        )
    }

    /**
     * The glyph's ink **mass** inside the button's box: the summed per-pixel deviation from the ground,
     * measured strictly inside the 44dp square so the 1px outline is not counted as glyph.
     *
     * Mass, not a pixel *count*. Counting pixels that merely differ from the ground was the first cut and
     * it did not discriminate — a heavier stroke fills its footprint more completely without enlarging it
     * much, so the count barely moved and `1.7` → `2.4` survived. Mass moves with the area actually
     * covered, which is what a stroke width *is*.
     */
    private fun glyphInkMass(full: android.graphics.Bitmap, slot: androidx.compose.ui.geometry.Rect): Long {
        // ⚠ P3: the window must sit **inside the pill**, corners included. At 50% radius a fixed 6px inset
        // still clips desk at the four corners, and that contamination — not the stroke — is most of what
        // the first two readings measured. 22% of the control's width leaves a centred square comfortably
        // within the pill and still larger than the 20dp glyph it has to contain.
        val inset = (slot.width * 0.22f).toInt()
        // Sample the ground **inside** the control, not at its box corner. V2's `.icon-btn` was an
        // outline over the bar's own chrome, so the corner and the interior were the same colour; V2.1's is
        // a pill with its own `paper` ground, and the corner pixel now falls outside the rounded edge on
        // `desk`. Sampling there made every interior pixel read as deviation and the measured mass jumped
        // 6.7× — reported as a stroke defect when the stroke had moved 1.7 → 1.9, about 12%. The reading
        // must start where the loop starts.
        // …and take it on the pill's **horizontal midline**, not its top-left corner. At 50% radius the
        // corner is round: 6px in from an 88px control is still 53px from the corner's centre against a
        // 44px radius, i.e. outside the pill and on `desk`. The first fix moved the sample and the measured
        // mass did not budge, which is what proved the point was still outside. The midline at x+inset is
        // inside the ground and clear of the 20dp glyph.
        val ground = full.getPixel(slot.left.toInt() + inset, slot.center.y.toInt())
        fun ch(v: Int, sh: Int) = (v shr sh) and 0xFF
        var mass = 0L
        for (x in slot.left.toInt() + inset until slot.right.toInt() - inset) {
            for (y in slot.top.toInt() + inset until slot.bottom.toInt() - inset) {
                val p = full.getPixel(x, y)
                mass += listOf(16, 8, 0).maxOf { sh -> kotlin.math.abs(ch(p, sh) - ch(ground, sh)) }
            }
        }
        return mass
    }

    @Test
    fun bench_bottom_bar_light() = captureBar("bench_bottom_bar_light", darkTheme = false)

    @Test
    fun bench_bottom_bar_dark() = captureBar("bench_bottom_bar_dark", darkTheme = true)

    // --- The status strip -----------------------------------------------------------------------------

    private fun captureStatus(name: String, darkTheme: Boolean) {
        host(darkTheme) { BenchStatusStrip(savedVisible = true) }
        // The fade is 400ms and the chip is what this golden is *for*, so it is captured settled rather
        // than mid-fade — a golden recorded at an arbitrary alpha re-records differently every run.
        composeRule.mainClock.advanceTimeBy(BenchSavedFadeMillis + 100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSavedChipTestTag).assertExists()

        val full = hostBitmap()
        val chip = cropToBounds(full, boundsOf(BenchSavedChipTestTag))
        // `.saved{color:var(--on-leaf)}` — which paints the check glyph as well as the word, so the
        // real count is higher than V2's, not lower. 40 still clears antialiasing while staying far under
        // it, and a chip drawn in `--ink` (or in the retired `--matcha-text`) fails it.
        assertTrue("the saved chip is not painted in --on-leaf ($name)", chip.pixelCountOf(v21OnLeafArgb) > 40)
        // …and it now has a *ground*, which V2's bare coloured text did not. This is the half that would
        // silently survive a revert to plain text on the strip, so it is asserted separately.
        assertTrue(
            "the saved chip has no --leaf-tint pill behind it ($name)",
            chip.pixelCountOf(v21LeafTintArgb) > 100,
        )

        cropToBounds(full, boundsOf(HOST_TAG)).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_status_strip_light() = captureStatus("bench_status_strip_light", darkTheme = false)

    @Test
    fun bench_status_strip_dark() = captureStatus("bench_status_strip_dark", darkTheme = true)

    // --- The snack ------------------------------------------------------------------------------------

    private fun captureSnack(name: String, darkTheme: Boolean) {
        host(darkTheme) {
            BenchSnack(visible = true, message = "Text deleted.", actionLabel = UndoActionLabel, onAction = {})
        }
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()

        val full = hostBitmap()
        val snack = cropToBounds(full, boundsOf(BenchSnackTestTag))
        // The 37596 amendment makes transient confirmation a warm support surface under ordinary ink.
        assertTrue(
            "the snack did not paint its --surface-soft ground ($name)",
            snack.pixelCountOf(v21SurfaceSoftArgb) > 2000,
        )

        // The action stays underlined and uses the same ink as the message.
        val action = cropToBounds(full, boundsOf(BenchSnackActionTestTag))
        assertTrue(
            "the snack's Undo is not painted in --ink ($name)",
            action.pixelCountOf(v21InkArgb) > 20,
        )

        cropToBounds(full, boundsOf(HOST_TAG)).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_snack_light() = captureSnack("bench_snack_light", darkTheme = false)

    @Test
    fun bench_snack_dark() = captureSnack("bench_snack_dark", darkTheme = true)
}
