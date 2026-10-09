package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.DocumentVoice
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.TextStyle
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
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
 * FR-3 Type-bar **goldens**, light + dark (ADR-055 §5 pixel-parity gate; ADR-028 §7.5 golden discipline).
 *
 * The two-proof shape [ZComponentGoldenTest] and [SelectionChromeGoldenTest] ship:
 *
 *  1. **Behavioural pixel assertions** — deterministic and mode-independent, so they run green under a
 *     plain `testDebugUnitTest` before any PNG exists. They prove the card actually painted its frozen
 *     tokens, so a golden can never be recorded off a blank capture.
 *  2. A committed **Roborazzi golden** — [captureRoboImage] is a no-op under a plain unit run;
 *     `:feature:editor:recordRoborazziDebug` on the pinned CI image (`record-goldens.yml`) produces the
 *     PNG and `verifyRoborazziDebug` then gates drift. **The PNGs are not in this change**: there is no
 *     local Android build in this project, and a golden is only valid against the image that verifies it
 *     — the same convention ADR-028 records for `ZComponentGoldenTest`.
 *
 * [TypeBar] is composed directly rather than driven through [EditorScreen]: it owns no styling state
 * (every control reads `element.style`), so a `TextElement` fixture IS the card's state, and the golden
 * pins the surface rather than the host's disclosure flag.
 *
 * Capture path: `captureToImage()` does not work headless under Robolectric NATIVE (see
 * [ComposeCanvasProbeTest]) — draw the laid-out decor view ([rasterizeToBitmap]) and crop to the card's
 * tagged bounds, exactly as [ZComponentGoldenTest] does.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// The prototype's own viewport (bench `--w:430px`), the qualifier [TypeBarTest] pins for the same reason:
// the four-row card measures off Robolectric's default (far shorter than any real phone) device.
@Config(qualifiers = "w430dp-h932dp-xhdpi")
class TypeBarGoldenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val GOLDEN_DIR = "src/test/roborazzi"
        const val HOST_TAG = "typeBarGoldenHost"

        // The committed AA tolerance ([SelectionChromeGoldenTest].aa()): the card's hairline border and
        // 16dp corners are AA edges that jitter a fraction of a pixel run-to-run.
        fun aa() = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f),
        )

        /**
         * The card at a mid-ramp, non-default style — Teal ink, bold, centered, 24pt — so the golden pins
         * the *selected* state of every row (a swatch ring, two lit toggles, a lit align segment) rather
         * than the all-default state, where selection chrome would be invisible.
         */
        val StyledText = TextElement(
            id = "t1",
            transform = Transform(40.0, 40.0, 120.0, 40.0),
            text = "Zine",
            style = TextStyle(
                sizePt = 24.0,
                color = ColorRgba(0x2A, 0x9D, 0x8F),
                align = TextAlign.CENTER,
                bold = true,
                italic = false,
            ),
        )

        /** The same card parked at the ramp floor (10pt), where "Smaller" disables and "Larger" does not. */
        val MinSizeText = StyledText.copy(style = StyledText.style.copy(sizePt = TypeSizesPt.first()))
    }

    /**
     * Compose the card on the desk and let it settle.
     *
     * [room] is the space the host gives the card, for the A29 states that need one: `null` leaves the
     * card unbounded (every pre-A29 golden), a size is the room it must stay inside and scroll within.
     */
    private fun showCard(darkTheme: Boolean, element: TextElement, fontScale: Float = 1f, room: DpSize? = null) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                ZinelyTheme(darkTheme = darkTheme) {
                    Box(
                        modifier = Modifier
                            .testTag(HOST_TAG)
                            .background(ZinelyTheme.v21Colors.desk)
                            .then(if (room != null) Modifier.sizeIn(maxWidth = room.width, maxHeight = room.height) else Modifier)
                            .padding(12.dp),
                    ) {
                        TypeBar(element = element, dispatch = {}, onAnnounce = {}, onPreview = {})
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Compose the card on the desk, draw the decor view, crop to the card's ACTUAL placed bounds. */
    private fun cardBitmap(
        darkTheme: Boolean,
        element: TextElement = StyledText,
        fontScale: Float = 1f,
        room: DpSize? = null,
    ): Bitmap {
        showCard(darkTheme, element, fontScale, room)
        val bounds = composeRule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = bounds.left.roundToInt().coerceAtLeast(0)
        val y = bounds.top.roundToInt().coerceAtLeast(0)
        val w = bounds.width.roundToInt().coerceAtMost(full.width - x)
        val h = bounds.height.roundToInt().coerceAtMost(full.height - y)
        return Bitmap.createBitmap(full, x, y, w, h)
    }

    private fun Bitmap.countColour(argb: Int): Int {
        var n = 0
        for (yy in 0 until height) for (xx in 0 until width) if (getPixel(xx, yy) == argb) n++
        return n
    }

    /**
     * Crop to a step button's **layout** bounds — the frozen 40dp chip, which is what paints. (The 48dp
     * touch target is an input-layer expansion and is not what `boundsInRoot` reports; ADR-055 §8.)
     */
    private fun stepChipBitmap(description: String): Bitmap {
        val b = composeRule.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val x = b.left.roundToInt().coerceAtLeast(0)
        val y = b.top.roundToInt().coerceAtLeast(0)
        return Bitmap.createBitmap(
            full, x, y,
            b.width.roundToInt().coerceAtMost(full.width - x),
            b.height.roundToInt().coerceAtMost(full.height - y),
        )
    }

    /**
     * The card's `width:max-content` width, **measured rather than derived** — 288dp under this
     * fixture's density and font scale.
     *
     * The August 29, 2026 Samsung refinement stacks the Colour caption above one five-tile line. The
     * physical pots still paint 30dp, every tile remains 48dp, the selected cue is explicit, and the
     * card is still inside the frozen `max-width:calc(100% - 24px)` on the narrowest phone
     * ([TypeBarTest.the_card_honours_the_frozen_max_width_on_the_smallest_supported_phone]).
     *
     * V1's version of this KDoc computed the number from the rules (`28 + 60 + 192 = 280`) and named
     * Colour as the widest row. The V2.1 re-skin moves both terms in that sum and in opposite directions:
     * the pots go 32dp → 30dp, taking 10dp off the Colour cluster, while the row label goes from 11sp
     * sentence case to `.6rem` uppercase on `.13em` tracking, which widens the label column. The net is
     * 1.5dp, which is exactly the kind of near-agreement that makes a recomputed figure look verified when
     * it is not. **Which row is widest at a given font scale is a measurement** — it is open question 2 in
     * `v21-typebar.html`, now answered — so this asserts the fixture's measured width and claims
     * nothing about which row produced it.
     *
     * Asserted in the golden tier too, and not only in [TypeBarTest], because this is the number a golden
     * silently bakes in: record a PNG off a wrong card and the wrong card *becomes* the reference.
     *
     * What it guards is `Swatch`/`StyleToggle` re-inflating (the ADR-055 §8 defect, which put Colour at
     * 272dp and the card edge-to-edge at `w360dp`). What it does **not** guard is a stepper-cluster
     * regression, which stays under the widest row and is absorbed by `SpaceBetween`; that one is pinned
     * in paint terms by
     * [TypeBarTest.the_size_stepper_paints_the_frozen_40dp_chips_at_the_frozen_8dp_pitch].
     */
    private fun assertFrozenCardWidth() {
        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            assertEquals(
                "the Type bar is not the measured 288dp wide; got ${card.width.toDp()}",
                288f, card.width.toDp().value, 0.5f,
            )
        }
    }

    @Test
    fun type_bar_light() {
        val bmp = cardBitmap(darkTheme = false)
        assertFrozenCardWidth()
        // Behavioural: the selected Teal swatch must actually paint its frozen ink token (bench `.tyinks`
        // #2A9D8F). A 32dp swatch leaves far more than 200 flat-colour pixels; a blank capture leaves none.
        assertTrue(
            "the teal ink swatch did not paint in the light Type bar",
            bmp.countColour(Color(0xFF2A9D8F).toArgb()) > 200,
        )
        // The light 37596 room (#E9E29B) must be the ground the card floats on.
        // dark case asserts.
        assertTrue(
            "the light desk did not paint behind the Type bar",
            bmp.countColour(Color(0xFFE9E29B).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/type_bar_light.png", aa())
    }

    @Test
    fun type_bar_dark() {
        val bmp = cardBitmap(darkTheme = true)
        assertFrozenCardWidth()
        // The ink swatches are paper-space colours: Teal is the same token in dark (the sheet does not
        // invert), which is exactly the invariant worth pinning here.
        assertTrue(
            "the teal ink swatch did not paint in the dark Type bar",
            bmp.countColour(Color(0xFF2A9D8F).toArgb()) > 200,
        )
        // Dark room (#242312) must be the ground the card floats on.
        assertTrue(
            "the dark desk did not paint behind the Type bar",
            bmp.countColour(Color(0xFF242312).toArgb()) > 200,
        )
        bmp.captureRoboImage("$GOLDEN_DIR/type_bar_dark.png", aa())
    }

    // ── A29: the Font row's states (ADR-126) ──────────────────────────────────────────────────────
    //
    // `type_bar_light` / `type_bar_dark` above are the Plain-chosen state. These are the others the
    // frozen page draws: Book chosen, Book unavailable with its reason, Smaller at Book's floor, and a
    // font this build does not know. Then the card at font scale 2.0 in a 360dp-wide room it cannot
    // fit, where it must scroll inside itself (A29 rule 11).

    private val bookText = StyledText.copy(style = StyledText.style.copy(fontFamily = "Fraunces"))
    private val greekText = StyledText.copy(text = "Zine. \u039A\u03B1\u03BB\u03B7\u03BC\u03AD\u03C1\u03B1")
    private val floorText = bookText.copy(style = bookText.style.copy(sizePt = DocumentVoice.BOOK_MIN_SIZE_PT))
    private val unknownText = StyledText.copy(style = StyledText.style.copy(fontFamily = "Averia Sans Libre"))

    private fun fontState(name: String, dark: Boolean, element: TextElement, chosen: String?, reason: Boolean) {
        val bmp = cardBitmap(darkTheme = dark, element = element)
        assertFrozenCardWidth()
        // Non-vacuity: the state the name claims is the state on screen.
        listOf("Book", "Plain").forEach { word ->
            val cue = composeRule.onNodeWithTag(selectionCueTag("type-bar-font-$word"), useUnmergedTree = true)
            if (word == chosen) cue.assertExists() else cue.assertDoesNotExist()
        }
        composeRule.onNodeWithTag(TypeBarFontReasonTestTag).let { if (reason) it.assertExists() else it.assertDoesNotExist() }
        bmp.captureRoboImage("$GOLDEN_DIR/type_bar_font_${name}_${if (dark) "dark" else "light"}.png", aa())
    }

    @Test fun type_bar_font_book_light() = fontState("book", false, bookText, chosen = "Book", reason = false)

    @Test fun type_bar_font_book_dark() = fontState("book", true, bookText, chosen = "Book", reason = false)

    @Test fun type_bar_font_book_unavailable_light() = fontState("book_unavailable", false, greekText, chosen = "Plain", reason = true)

    @Test fun type_bar_font_book_unavailable_dark() = fontState("book_unavailable", true, greekText, chosen = "Plain", reason = true)

    @Test fun type_bar_font_book_floor_light() = fontState("book_floor", false, floorText, chosen = "Book", reason = true)

    @Test fun type_bar_font_book_floor_dark() = fontState("book_floor", true, floorText, chosen = "Book", reason = true)

    @Test fun type_bar_font_unknown_light() = fontState("unknown", false, unknownText, chosen = null, reason = true)

    @Test fun type_bar_font_unknown_dark() = fontState("unknown", true, unknownText, chosen = null, reason = true)

    private fun largestText(name: String, element: TextElement, reason: Boolean) {
        // 360 x 640 dp: the smaller of the two rooms R24 names. The host's 12dp inset is the editor's.
        val bmp = cardBitmap(darkTheme = false, element = element, fontScale = 2f, room = DpSize(360.dp, 640.dp))
        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            assertTrue("the card is ${card.width.toDp()} wide in a 336dp room", card.width.toDp() <= 336.5.dp)
            assertTrue("the card is ${card.height.toDp()} tall in a 616dp room", card.height.toDp() <= 616.5.dp)
        }
        composeRule.onNodeWithTag(TypeBarFontReasonTestTag).let { if (reason) it.assertExists() else it.assertDoesNotExist() }
        bmp.captureRoboImage("$GOLDEN_DIR/type_bar_font_scale2_$name.png", aa())
    }

    @Test
    fun type_bar_font_scale2_plain() = largestText("plain", StyledText, reason = false)

    @Test
    fun type_bar_font_scale2_book_unavailable() =
        largestText("book_unavailable", greekText, reason = true)

    @Test
    fun type_bar_font_scale2_unknown() =
        largestText("unknown", unknownText, reason = true)

    /**
     * A29 rule 11 as the maker sees it: the three goldens above are given the whole 640dp screen, where the
     * card fits. Inside the editor on that screen it is given 378dp (measured by `TypeBarFontRowTest`), so
     * it scrolls. This is that room: a row cut by the card's edge, and the shade.
     */
    @Test
    fun type_bar_font_scale2_unknown_scrolling() {
        val bmp = cardBitmap(darkTheme = false, element = unknownText, fontScale = 2f, room = DpSize(360.dp, 402.dp))
        val card = composeRule.onNodeWithTag(TypeBarTestTag).fetchSemanticsNode().boundsInRoot
        with(composeRule.density) {
            assertTrue("the card is ${card.height.toDp()} tall in a 378dp room", card.height.toDp() <= 378.5.dp)
        }
        bmp.captureRoboImage("$GOLDEN_DIR/type_bar_font_scale2_unknown_scrolling.png", aa())
    }

    /**
     * Bench freezes `.tysize button:disabled{ opacity:.4 }` — the fade covers the **whole chip**, edge
     * included. The port faded the glyph alone and left the 1dp edge at full strength (ADR-055 §8, closed
     * 2026-07-17).
     *
     * Pinned here rather than left to the PNG because the golden fixture sits mid-ramp at 24pt, where
     * *neither* button is disabled — the recorded image structurally cannot see this state, which is how
     * the defect shipped. The assertion is the **absence of the un-faded edge** rather than the presence of
     * the faded one: `#DED4C2` at `.4` over `#FBF8F1` lands on ~`#EFEADE`, and pinning that exact composite
     * would pin a rounding mode, not the parity fact.
     */
    @Test
    fun the_disabled_step_chip_fades_whole_not_only_its_glyph() {
        showCard(darkTheme = false, element = MinSizeText)
        // V2.1 `--ink`, light (`v21-typebar.html` `.tysize button{border:1.5px solid var(--ink)}`). Was
        // V1's `--field-edge` #DED4C2, a token V2.1 does not define.
        val unfadedEdge = Color(0xFF27270F).toArgb()

        // Non-vacuity: at the ramp floor "Larger" is still enabled, so the edge IS on screen — proving the
        // crop and the colour are right before the disabled case asserts an absence.
        assertTrue(
            "the enabled Larger chip did not paint the ink edge",
            stepChipBitmap("Larger").countColour(unfadedEdge) > 50,
        )
        assertEquals(
            "the disabled Smaller chip still paints a full-strength edge; opacity:.35 fades the whole chip",
            0,
            stepChipBitmap("Smaller").countColour(unfadedEdge),
        )
    }
}
