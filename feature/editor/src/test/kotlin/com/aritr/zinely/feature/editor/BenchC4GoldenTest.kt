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
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
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
 * The golden net for C4's painted surfaces — the bar, the status strip and the snack, light + dark, in the
 * [BenchStyleRowGoldenTest] two-proof shape ([ADR-094](../../../../../../../docs/DECISIONS.md#adr-094)
 * rows 4.1, 4.2, 4.10, 4.11, 4.12).
 *
 * **This is where C4's paint is asserted.** [BenchC4Test] reads order, enabled-ness, timing and geometry
 * off the semantics tree, and none of those can see a `--chrome` ground, a 1px `--chrome-line` hairline,
 * `--matcha` under `--on-matcha` on `Add`, `--matcha-text` on the saved chip, or the snack's inverted
 * `--ink` ground with `--accent-on-ink` on its one button.
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
    private var matchaArgb = 0
    private var chromeArgb = 0
    private var inkArgb = 0
    private var accentOnInkArgb = 0
    private var matchaTextArgb = 0
    private var inkSoftArgb = 0

    private fun host(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            ZinelyTheme(darkTheme = darkTheme) {
                deskArgb = ZinelyTheme.colors.desk.toArgb()
                matchaArgb = ZinelyTheme.v2Colors.matcha.toArgb()
                chromeArgb = ZinelyTheme.v2Colors.chrome.toArgb()
                inkArgb = ZinelyTheme.v2Colors.ink.toArgb()
                accentOnInkArgb = ZinelyTheme.v2Colors.accentOnInk.toArgb()
                matchaTextArgb = ZinelyTheme.v2Colors.matchaText.toArgb()
                inkSoftArgb = ZinelyTheme.v2Colors.inkSoft.toArgb()
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

        // Row 4.1 — the `--chrome` ground. A 66dp bar at xhdpi is tens of thousands of pixels; 2000 is far
        // above any incidental match and far below the real fill.
        assertTrue("the bar did not paint its --chrome ground ($name)", barCrop.pixelCountOf(chromeArgb) > 2000)

        // Row 4.1 — `border-top:1px solid var(--chrome-line)`. Compared against the ground **as actually
        // painted** and read from the full raster in root coordinates, for the two reasons C3 recorded: the
        // rule composites over `--chrome` so it equals neither token, and a re-crop can round a pixel onto
        // the desk and "differ" whether or not a hairline exists.
        val left = bar.left.toInt() + 2
        val right = bar.right.toInt() - 2
        val ground = full.getPixel(left, (bar.top + bar.height / 2f).toInt())
        var hairline = 0
        for (x in left until right) if (full.getPixel(x, bar.top.toInt() + 1) != ground) hairline++
        assertTrue(
            "the bar's top hairline is missing ($name): only $hairline/${right - left} px on its first " +
                "scanline differ from its own ground",
            hairline > (right - left) / 2,
        )

        // Row 4.4 — `Add` is the bar's one filled control, `--matcha`. A 44dp-tall pill across most of the
        // bar's width is many thousands of pixels at xhdpi.
        assertTrue("Add did not paint its --matcha fill ($name)", barCrop.pixelCountOf(matchaArgb) > 2000)

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
     * Row 4.2's `:active{transform:scale(.94)}`, measured rather than declared: the mutation `.94`→`.8`
     * survived every test in C4 because nothing looked at a pressed control. `graphicsLayer` scaling does
     * not move the semantics box, so the drawn box is found in the raster — the outline is the only
     * `--chrome-line` in the button's slot, so its extent **is** the drawn width.
     */
    @Test
    fun a_pressed_icon_button_shrinks_to_the_frozen_ninety_four_percent() {
        host(darkTheme = false) {
            BenchBottomBar(
                canUndo = true, canRedo = true, doneEnabled = true,
                onUndo = {}, onRedo = {}, onAdd = {}, onDone = {},
            )
        }
        val slot = boundsOf(BenchBarUndoTag)
        val restingWidth = drawnWidth(hostBitmap(), slot)

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(BenchBarUndoTag).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(BenchBarPressMillis + 100L)
        composeRule.waitForIdle()
        val pressedWidth = drawnWidth(hostBitmap(), slot)
        composeRule.onNodeWithTag(BenchBarUndoTag).performTouchInput { up() }

        // The literal `.94`, **not** `BenchIconBtnPressedScale`: computing the expectation from the
        // constant under test makes the assertion agree with any value the constant takes, which is how
        // the `.94` → `.8` mutation survived this test's first cut. Same defect as the two timing tests,
        // found the same way.
        val expected = restingWidth * 0.94f
        assertTrue(
            "a pressed control must draw at the frozen .94 (resting ${restingWidth}px, pressed " +
                "${pressedWidth}px, expected ~${expected}px)",
            kotlin.math.abs(pressedWidth - expected) <= 2f,
        )
        assertTrue("the frozen pressed scale is .94", BenchIconBtnPressedScale == 0.94f)
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
        val inset = 6
        val ground = full.getPixel(slot.left.toInt() + 2, slot.top.toInt() + 2)
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
        // Row 4.10 — the chip is `--matcha-text`. 11sp of text is a few hundred pixels of glyph; 40 clears
        // antialiasing while staying far under the real count, and a chip drawn in `--ink` fails it.
        assertTrue("the saved chip is not painted in --matcha-text ($name)", chip.pixelCountOf(matchaTextArgb) > 40)

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
        // Row 4.11 — the ground **inverts**: `--ink` under `--paper` text. This is the property that makes
        // the surface read as a system message rather than as more chrome, and it is the one a re-skin of
        // the wrong snackbar (there are three in-tree) would silently lose.
        assertTrue("the snack did not paint its inverted --ink ground ($name)", snack.pixelCountOf(inkArgb) > 2000)

        // Row 4.12 — `--accent-on-ink` on the one button, which exists in the palette **only** for this
        // pairing. Scoped to the action's own rect so the message's `--paper` cannot stand in for it.
        val action = cropToBounds(full, boundsOf(BenchSnackActionTestTag))
        assertTrue(
            "the snack's Undo is not painted in --accent-on-ink ($name)",
            action.pixelCountOf(accentOnInkArgb) > 20,
        )

        cropToBounds(full, boundsOf(HOST_TAG)).captureRoboImage("$GOLDEN_DIR/$name.png", aa())
    }

    @Test
    fun bench_snack_light() = captureSnack("bench_snack_light", darkTheme = false)

    @Test
    fun bench_snack_dark() = captureSnack("bench_snack_dark", darkTheme = true)
}
