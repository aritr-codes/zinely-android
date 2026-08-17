package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV21Fonts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The frozen action sheet — its geometry, its type, its inks, its five rows, and its dismissal paths.
 *
 * **Two hosts, deliberately.** The dismissal and modality claims need the real [ZineActionSheet], window and
 * all; the geometry and pixel claims need a surface the decor-view raster can see, which a `Dialog`'s own
 * window is not — so those use [ZineActionSheetSurface] directly, exactly as V1's golden tests use
 * `ZSheetSurface`. Each test says which host it is on and why.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineActionSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val TITLE = "Sunday market"
        const val SUBTITLE = "A4 · 2 days ago"
        val TARGET = ZineActionTarget(TITLE, SUBTITLE)

        /**
         * `.sheet{left:0;right:0;bottom:0}` — **the card stopped floating**.
         *
         * V2 inset it 10px on three sides. V2.1 runs it to the window's edges and rounds only the two top
         * corners, which is why the radius below more than doubled in the same re-freeze: a full-bleed
         * sheet with a small radius reads as a panel rather than as a card being drawn up.
         */
        const val INSET = 0

        /**
         * `.sh-head{padding:var(--gap-xs) var(--gap-xl) var(--gap-md)}` · `.act{padding:var(--gap-lg)
         * var(--gap-xl)}` — 4 / 24 / 12 and 16 / 24, where V2 wrote 17 / 20 / 13 and 15 / 20. All of them
         * are steps on the published scale (§3.3) now rather than hand-set numbers.
         */
        const val HEAD_TOP = 4
        const val HEAD_SIDE = 24
        const val ROW_SIDE = 24

        /**
         * `.grab{height:5px;margin:var(--gap-md) auto var(--gap-xs)}` — 12 + 5 + 4, the room the handle
         * takes above the head. The head's own padding is measured on top of it.
         */
        const val GRAB_BLOCK = 21

        /** `.act .ic{width:30px;height:30px}` and `.act{gap:var(--gap-lg)}` — V2's were 20 and 14. */
        const val ICON_CHIP = 30
        const val ROW_GAP = 16

        /** `border-top:1px solid var(--hair)` · `.danger{border-top:8px solid var(--desk)}` */
        /** `.sh-head{border-bottom:1.5px dashed var(--hair)}` — the sheet's one divider. */
        const val HEAD_DIVIDER = 1.5f

        /** `.sh-ttl{font-family:var(--voice);font-size:1.22rem;font-weight:700}` — 19.52px in Averia. */
        val TITLE_SIZE = 19.52.sp

        /** `.sheet{border-radius:var(--br-xl) var(--br-xl) 0 0}` — 36px, on the two top corners only. */
        const val CARD_RADIUS = 36

        const val HALF_PIXEL = 0.5f

        /** A ground no token carries, so "the sheet painted here" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)

        /** Reference renderings for the type test. */
        const val REF_FROZEN = "ref-frozen"
        const val REF_LIGHTER = "ref-500"
        const val REF_SMALL = "ref-12sp"

        /**
         * **Two** tofu controls, and the pair is the point.
         *
         * The first version of this used `U+E000`, on the reasoning that a Private Use Area codepoint is one
         * no font carries. That reasoning was wrong, and independent review proved it by rendering a
         * guaranteed-tofu glyph through the passing test: **the bundled Inter maps `U+E000` to a real glyph**
         * (id 1863, in all four weights). The control was a normal character, so `assertNotEquals` was
         * satisfied by any two glyphs that merely differ from each other.
         *
         * Both codepoints below were checked against every `cmap` subtable of all four bundled Inter weights
         * and are absent from every one, so each falls through to the platform's notdef. **Two are used
         * rather than one because the identity is the evidence:** distinct codepoints that render
         * *identically* can only be doing so because neither has a glyph, so that shared raster **is** the
         * tofu box — established by measurement rather than by assumption. One control can only be assumed.
         */
        const val TOFU_A = "\uE001"
        const val TOFU_B = "\uE05F"
    }

    private val chosen = mutableListOf<ZineAction>()
    private var dismissals = 0

    // ---------------------------------------------------------------------------------------------
    // Presence and dismissal — on the real sheet, window and all
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `there is no sheet until a zine is chosen`() {
        sheet(target = null)
        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineActionScrimTestTag).assertDoesNotExist()
    }

    @Test
    fun `tapping the scrim dismisses the sheet`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(ZineActionScrimTestTag).performClick()
        // `document.getElementById('scrim').addEventListener('click',close)` — `:197`.
        assertEquals("the scrim is a dismissal path", 1, dismissals)
        assertEquals("and dismissing chooses nothing", emptyList<ZineAction>(), chosen)
    }

    @Test
    fun `the system back dismisses the sheet`() {
        sheet(TARGET)
        // The frozen file's `aria-modal="true"` (`:171`) is a behaviour, and Escape is its browser half.
        // Android's is system back, which the implementation guide authorises as platform truth the HTML
        // cannot express. A `Dialog` provides it — this asserts that it actually reaches `onDismiss`.
        composeRule.runOnUiThread { ShadowDialog.getLatestDialog()?.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals("back must dismiss", 1, dismissals)
    }

    @Test
    fun `choosing an action reports it once and leaves the sheet standing`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Rename)).performClick()

        assertEquals("the choice is reported", listOf(ZineAction.Rename), chosen)
        // **Deliberate, and the frozen file's own doing.** The `.act` buttons carry no handler at all
        // (`:195-209` wires the scrim and the `⋯`, nothing else), so what follows Rename is undesigned
        // here and every one of the five leads somewhere B5 owns. The sheet reports and holds still; a
        // self-dismissing sheet would be inventing the flow.
        assertEquals("and the sheet does not dismiss itself", 0, dismissals)
        composeRule.onNodeWithTag(ZineActionSheetTestTag).assertExists()
    }

    @Test
    fun `the sheet names itself to a screen reader`() {
        sheet(TARGET)
        composeRule.onNodeWithTag(ZineActionSheetTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, ZineActionSheetPaneTitle))
    }

    @Test
    fun `the sheet floats ten pixels clear of the two sides and the bottom`() {
        sheet(TARGET)
        val window = composeRule.onNodeWithTag(ZineActionScrimTestTag).fetchSemanticsNode().boundsInRoot
        val card = composeRule.onNodeWithTag(ZineActionSheetTestTag).fetchSemanticsNode().boundsInRoot

        // Measured against the scrim, which is `inset:0` — the frozen sheet's own coordinate space.
        assertEquals("${INSET}px in from the left", INSET.toFloat(), card.left - window.left, HALF_PIXEL)
        assertEquals("${INSET}px in from the right", INSET.toFloat(), window.right - card.right, HALF_PIXEL)
        assertEquals("${INSET}px up from the bottom", INSET.toFloat(), window.bottom - card.bottom, HALF_PIXEL)
        assertTrue(
            "and it is a card, not a full-height panel (measured ${card.height} of ${window.height})",
            card.height < window.height / 2f,
        )
    }

    @Test
    fun `the card's top corners are rounded away, not square`() {
        // `.sheet{border-radius:20px}`. No bound can see this — the node's rectangle is identical either
        // way — so it is read off the pixels: inside the arc there is no paper, and a few rows further down
        // the very same column is paper. The second half is the discriminator: without it, a sheet drawn
        // somewhere else entirely would pass the first.
        surface()
        val raster = decorRaster()
        val card = tagBounds(ZineActionSheetTestTag)
        val left = card.left.roundToInt()
        val top = card.top.roundToInt()

        // (3,3) from the corner is outside a 20px arc by six pixels, and outside the 1px hairline ring too.
        assertTrue(
            "the top-left corner must be cut away by the ${CARD_RADIUS}px radius",
            !raster.colourAt(left + 3, top + 3).closeTo(capturedPaper),
        )
        assertTrue(
            "and so must the top-right",
            !raster.colourAt(card.right.roundToInt() - 4, top + 3).closeTo(capturedPaper),
        )
        // The same column, below the arc — paper. This is what fails if the sheet simply is not there.
        assertTrue(
            "three pixels in, well below the corner, must be the sheet's own paper",
            raster.colourAt(left + 3, top + CARD_RADIUS * 2).closeTo(capturedPaper),
        )
        // The arc's own centre is paper — read on the **right**, because the 36px radius now reaches down
        // past the head's top padding and the title's glyphs stand at the same point on the left. That is
        // a consequence of re-deriving the radius rather than of the sheet being wrong, and moving the
        // probe is the honest fix: a tolerance wide enough to accept ink here would accept anything.
        assertTrue(
            "and the corner's own diagonal must be paper once past the arc",
            raster.colourAt(card.right.roundToInt() - CARD_RADIUS, top + CARD_RADIUS).closeTo(capturedPaper),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The five rows — on the surface, where they can be measured and sampled
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the five actions stand in the frozen order`() {
        surface()
        // `:173-177`. The order is the design's argument about consequence — Open first, Delete last and
        // apart — so it is asserted by placement rather than by the enum's own declaration order, which
        // would be the test asserting itself.
        val tops = ZineAction.entries.map { action ->
            action to composeRule.onNodeWithTag(zineActionTestTag(action))
                .fetchSemanticsNode().boundsInRoot.top
        }
        val expected = listOf(
            ZineAction.Open,
            ZineAction.ShareExport,
            ZineAction.Rename,
            ZineAction.Duplicate,
            ZineAction.Delete,
        )
        assertEquals(
            "the rows must be placed in the frozen order",
            expected,
            tops.sortedBy { it.second }.map { it.first },
        )
        // And each row *says* its frozen label — asserted against the file's own strings rather than
        // against `ZineAction.label`, which would be the enum agreeing with itself. Read as the control's
        // spoken name, because the row collapses to one node: the same string a screen reader hears is the
        // string that is printed, which is exactly the guarantee wanted here.
        listOf(
            "Open on the bench",
            "Share & export",
            "Rename",
            "Duplicate",
            "Delete",
        ).forEach { label ->
            // Twice over, because the two are different claims and either can fail alone: the row *says*
            // the label to a screen reader, and the row *prints* it.
            composeRule.onNode(hasContentDescription(label)).assertExists()
            composeRule.onNodeWithText(label, useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `the head is the sheet's only divider, and Delete is set apart by colour alone`() {
        surface()
        // `.sh-head{border-bottom:1.5px dashed var(--hair)}` — one divider, under the header.
        val divider = composeRule.onNodeWithTag(ZineActionHeadDividerTestTag)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(
            "the head's dashed rule is ${HEAD_DIVIDER}px",
            HEAD_DIVIDER,
            divider.height,
            HALF_PIXEL,
        )
        val title = composeRule.onNodeWithTag(ZineActionTitleTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("and it sits below the header, not above it", divider.top > title.bottom)

        // **And nothing separates the rows.** V2's `.act` carried `border-top:1px solid var(--hair)`
        // and Delete carried `border-top:8px solid var(--desk)` — a band of the desk showing through
        // the sheet, so the destructive row sat visibly apart. V2.1 writes `.act{border:none}`.
        //
        // Asserted as an *absence*, and asserted by geometry rather than by the missing tag: the five
        // rows stack with no gap at all, so any separator would show up as a gap between them. A test
        // that only checked the tag was gone would pass against a separator drawn some other way.
        val rows = ZineAction.entries.map {
            composeRule.onNodeWithTag(zineActionTestTag(it)).fetchSemanticsNode().boundsInRoot
        }
        rows.zipWithNext().forEach { (above, below) ->
            assertEquals(
                "the rows touch — no border, no band, no gap",
                above.bottom,
                below.top,
                HALF_PIXEL,
            )
        }
    }

    @Test
    fun `every row is inset twenty pixels, header included`() {
        surface()
        val card = composeRule.onNodeWithTag(ZineActionSheetTestTag).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithTag(ZineActionTitleTestTag).fetchSemanticsNode().boundsInRoot

        assertEquals(
            "`.sh-head` is inset ${HEAD_SIDE}px",
            HEAD_SIDE.toFloat(),
            title.left - card.left,
            HALF_PIXEL,
        )
        // The head's own padding stands **below the grab handle**, which is `margin:var(--gap-md) auto
        // var(--gap-xs)` around a 5px bar — 21px of it before `.sh-head` starts. V2's expectation was the
        // head padding alone; keeping that shape and re-baselining 17 to 25 would have folded the handle
        // into a number nobody could read back to the frozen file.
        assertEquals(
            "and opens ${GRAB_BLOCK} + ${HEAD_TOP}px down — the handle, then the head's own padding",
            (GRAB_BLOCK + HEAD_TOP).toFloat(),
            title.top - card.top,
            HALF_PIXEL,
        )

        // **The row's own parts are found in the unmerged tree, and that is not a workaround.** A row
        // collapses to one node in the merged tree — the shape that reaches TalkBack as a `Button` named by
        // its action, without announcing the decorative glyph beside it. The parts still exist underneath,
        // which is where a *geometry* question belongs anyway: a11y asks what a service hears, layout asks
        // where the ink went, and they are different trees on purpose.
        val row = composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Open))
            .fetchSemanticsNode().boundsInRoot
        val glyph = composeRule
            .onNodeWithText(ZineAction.Open.glyph, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val label = composeRule
            .onNodeWithText(ZineAction.Open.label, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        // **Measured through the glyph's centre, not its left edge.** `.ic` is a 30px chip with the
        // character centred in it (`display:grid;place-items:center`), so the glyph's own left edge is
        // the padding plus half the difference between the chip and the character — a number that moves
        // with the *font*. Its centre does not: it is the padding plus half the chip, which pins both
        // frozen values at once and nothing else.
        assertEquals(
            "`.act{padding:var(--gap-lg) var(--gap-xl)}` and `.ic{width:${ICON_CHIP}px}` — the chip's " +
                "centre stands ${ROW_SIDE} + ${ICON_CHIP / 2}px in",
            (ROW_SIDE + ICON_CHIP / 2f),
            glyph.center.x - row.left,
            HALF_PIXEL,
        )
        assertEquals(
            "`gap:var(--gap-lg)` between the chip and the label",
            (ROW_SIDE + ICON_CHIP + ROW_GAP).toFloat(),
            label.left - row.left,
            HALF_PIXEL,
        )

        // **And the chip's own width, read off the raster.** The two assertions above both measure from
        // `row.left`, so they are two equations in three unknowns: padding 19 / chip 40 / gap 11 satisfies
        // both, and `.ic{width:30px}` would be asserted by nothing at all. The predecessor pinned it by
        // measuring the glyph's box directly; that box is the *character's*, which moves with the font,
        // which is why it was dropped. The chip is not a semantics node either — but it is `--butter-tint`
        // on `--paper`, so it can simply be counted.
        val raster = decorRaster()
        // Measured as the **span** of chip-coloured columns on the chip's centre line, not as a count of
        // them. Two readings had to be discarded first: a count on the centre line reads 25, because the
        // glyph is centred in the chip and its own ink interrupts the run; a count three rows below the
        // top edge reads 27, because `--br-sm` is an 8px radius and the corners are still cutting in.
        // The span is blind to both — the glyph is interior and the widest row is the centre one.
        val chipRow = glyph.center.y.roundToInt()
        val columns = (row.left.roundToInt() until row.right.roundToInt())
            .filter { raster.colourAt(it, chipRow).closeTo(capturedChip) }
        assertEquals(
            "`.act .ic{width:${ICON_CHIP}px;background:var(--butter-tint)}` — the chip must be that wide",
            ICON_CHIP.toFloat(),
            (columns.last() - columns.first() + 1).toFloat(),
            // One pixel for the antialiased column at either end of the span.
            2f,
        )

        // `padding:var(--gap-lg) var(--gap-xl)` is symmetric top and bottom, so the label sits centred. An
        // asymmetric transcription — the header's own 4/12, say — passes every horizontal check above.
        assertEquals(
            "the label must sit centred between equal ${ROW_GAP}px paddings",
            row.center.y,
            label.center.y,
            1.5f,
        )
    }

    @Test
    fun `the header discloses exactly what the shelf withheld`() {
        surface()
        composeRule.onNodeWithText(TITLE).assertExists()
        composeRule.onNodeWithText(SUBTITLE).assertExists()
    }

    // ---------------------------------------------------------------------------------------------
    // Type and ink
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the sheet title is set in the voice face at the frozen size and weight`() {
        // Ink coverage, for the reason `ZineShelfTest` had to adopt it: two weights of one face at one
        // size differ by about a pixel of advance, so nothing about the node's width or height can tell
        // them apart.
        //
        // `.sh-ttl{font-family:var(--voice);font-size:1.22rem;font-weight:700}` — **Averia 700 at
        // 19.52px**, where V2 wrote Fraunces 500 at 17.92px. Face, size and weight all moved together, so
        // the wrong-weight foil is now V2's own 500 rather than the V2 file's stale 600.
        surfaceWithReferences()
        val raster = decorRaster()
        val threshold = inkThreshold(capturedPaper)

        val frozen = raster.inkCoverage(tagBounds(REF_FROZEN), threshold)
        val lighter = raster.inkCoverage(tagBounds(REF_LIGHTER), threshold)
        val smaller = raster.inkCoverage(tagBounds(REF_SMALL), threshold)
        val actual = raster.inkCoverage(tagBounds(ZineActionTitleTestTag), threshold)

        assertTrue("nothing was drawn at the frozen style", frozen > 0)
        assertTrue(
            "this host cannot tell the frozen 700 from V2's 500 ($frozen vs $lighter), " +
                "so the weight would be unguarded here",
            relativeGap(frozen, lighter) > 0.03f,
        )
        assertTrue(
            "this host cannot tell ${TITLE_SIZE} from the subtitle's 12.48sp ($frozen vs $smaller)",
            relativeGap(frozen, smaller) > 0.03f,
        )
        assertTrue(
            "the title must render at Averia 700 / $TITLE_SIZE — coverage $actual against $frozen " +
                "frozen, $lighter at 500, $smaller at 12.48sp",
            relativeGap(actual, frozen) <= 0.03f,
        )
    }

    @Test
    fun `the subtitle is printed in the faintest ink and the title in the full one`() {
        surface()
        val raster = decorRaster()
        // Three tokens are plausible here and two of them are wrong: `.sh-sub{color:var(--ink-faint)}`
        // while the title takes plain `--ink`. Swapping either composes perfectly and reads as a choice.
        assertTrue(
            "some pixel of the subtitle must be exactly inkFaint",
            raster.anyPixel(tagBounds(ZineActionSubtitleTestTag), capturedInkSoft),
        )
        assertTrue(
            "and some pixel of the title exactly ink",
            raster.anyPixel(tagBounds(ZineActionTitleTestTag), capturedInk),
        )
        assertNotEquals("the two inks must differ, or neither assertion discriminates", capturedInk, capturedInkSoft)
    }

    @Test
    fun `Delete is printed in the consequence ink, and the other four are not`() {
        surface()
        val raster = decorRaster()
        assertTrue(
            "`.act.danger{color:var(--consequence)}`",
            raster.anyPixel(tagBounds(zineActionTestTag(ZineAction.Delete)), capturedConsequence),
        )
        ZineAction.entries.filterNot { it.danger }.forEach { action ->
            assertTrue(
                "${action.label} must not be printed in the consequence ink",
                !raster.anyPixel(tagBounds(zineActionTestTag(action)), capturedConsequence),
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D-021 — the icons are characters, and half of them are not in the app's own font
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `every frozen glyph draws a real mark, not a tofu box`() {
        surfaceWithReferences()
        val raster = decorRaster()

        // **This is the register entry, measured.** `.ic` holds a literal character, and of the six the
        // design uses — the five action icons plus the shelf's `\u22EF` — only `\u2197`, `\u21EA` and `\u232B` are in
        // the bundled Inter. `\u270E`, `\u29C9` and `\u22EF` come from whatever the platform falls back to, and a
        // platform with no glyph at all draws the tofu box. Ink alone cannot tell the two apart: the tofu
        // box is a rectangle and paints *more* ink than a thin ellipsis does. So each glyph is compared
        // against the tofu box pixel for pixel.
        val threshold = inkThreshold(capturedPaper)
        val tofuA = raster.signature(tagBounds("glyph-$TOFU_A"), threshold)
        val tofuB = raster.signature(tagBounds("glyph-$TOFU_B"), threshold)

        // **The positive control, and the assertion this test previously lacked.** Two codepoints with no
        // glyph in any bundled weight must render *the same thing*, because that thing is the notdef. If
        // they differ, at least one resolved to a real glyph and the comparison below would be meaningless
        // — which is exactly the state review found this test shipped in.
        assertTrue("the tofu control must draw something, or it discriminates nothing", tofuA.any { it })
        assertEquals(
            "two codepoints absent from every bundled weight must render identically — if they do not, " +
                "one of them has a glyph and is not a tofu control at all",
            tofuA,
            tofuB,
        )
        assertTrue(
            "and the tofu box must not be a solid block — a threshold that called the whole cell ink " +
                "would make every glyph equal to it (which is what the first run of this test did)",
            tofuA.count { it } < tofuA.size / 2,
        )

        val glyphs = ZineAction.entries.map { it.glyph } + listOf("\u22EF")
        glyphs.forEach { glyph ->
            val drawn = raster.signature(tagBounds("glyph-$glyph"), threshold)
            assertTrue("'$glyph' drew nothing at all", drawn.any { it })
            assertNotEquals(
                "'$glyph' rendered as the tofu box — the platform has no glyph for it (D-021)",
                tofuA,
                drawn,
            )
        }
    }

    @Test
    fun `the scrim is one wash in both themes, which is what the re-freeze wrote`() {
        // **This assertion is the inverse of the one it replaces, for the second time, and that is the
        // finding rather than an embarrassment.** B3 first pinned the two themes as *equal*; the D-022
        // ruling made the corpus's theme-aware `--scrim` authoritative and the test was rewritten to
        // assert they *differ*. V2.1 writes `.scrim{background:rgba(38,26,16,.42)}` in the shared block
        // with no dark override anywhere in the file, so the re-freeze answers the same question a third
        // time — back to one wash, at a new value.
        //
        // Re-baselining the two expected colours would have kept the `assertNotEquals` and gone on
        // requiring a difference the frozen file does not state. See [FROZEN_SCRIM] for why this is
        // recorded against D-022 rather than treated as closing it.
        val (light, dark) = scrimPixels()
        val expected = FROZEN_SCRIM.compositeOverWhite()

        assertTrue(
            "light must be the frozen rgba(38,26,16,.42) over white — expected $expected, found $light",
            light.closeTo(expected),
        )
        assertTrue(
            "and dark must be the same wash — expected $expected, found $dark",
            dark.closeTo(expected),
        )
    }

    @Test
    fun `the stale Library literal is not what the sheet paints`() {
        // The control. Without it, *any* single colour painted in both themes would satisfy the test
        // above — which, read carelessly, is a description of V2's defect as well as of V2.1's design.
        // Naming the rejected value is what keeps the two apart.
        val (light, _) = scrimPixels()
        assertFalse(
            "V2's rgba(30,25,18,.36) is not the V2.1 wash and must not be painted",
            light.closeTo(STALE_LIBRARY_SCRIM.compositeOverWhite()),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    /** The real component, `Dialog` and all — for behaviour, dismissal and modality. */
    private fun sheet(target: ZineActionTarget?) {
        composeRule.setContent {
            Host(dark = false) {
                ZineActionSheet(
                    target = target,
                    onAction = { chosen += it },
                    onDismiss = { dismissals++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** The sheet's body in a plain host — for geometry and pixels the decor raster must be able to see. */
    private fun surface() {
        composeRule.setContent {
            Host(dark = false) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    ZineActionSheetSurface(target = TARGET, onAction = { chosen += it })
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The surface plus the yardsticks the type and glyph tests measure against. */
    private fun surfaceWithReferences() {
        composeRule.setContent {
            Host(dark = false) {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    ZineActionSheetSurface(target = TARGET, onAction = { chosen += it })
                }
                // **On the sheet's own paper, not on the probe ground.** Ink coverage counts pixels darker
                // than a threshold, and how many a glyph's antialiased edge contributes depends on what is
                // behind it — references measured over a mid-luminance green and a title measured over
                // near-white paper differ by a fifth for that reason alone, which is what the first run of
                // this test reported.
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .background(ZinelyTheme.v2Colors.paper),
                ) {
                    Reference(REF_FROZEN, FontWeight.Bold, TITLE_SIZE, TITLE)
                    Reference(REF_LIGHTER, FontWeight.Medium, TITLE_SIZE, TITLE)
                    Reference(REF_SMALL, FontWeight.Bold, 12.48.sp, TITLE)
                }
                Column(
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(ZinelyTheme.v2Colors.paper),
                ) {
                    (ZineAction.entries.map { it.glyph } + listOf("⋯", TOFU_A, TOFU_B)).forEach { glyph ->
                        GlyphCell(glyph)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Reference(tag: String, weight: FontWeight, size: TextUnit, text: String) {
        Text(
            text = text,
            modifier = Modifier.testTag(tag),
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = weight,
                fontSize = size,
                color = ZinelyTheme.v2Colors.ink,
            ),
        )
    }

    /** One glyph in a fixed cell, so every signature is compared over the same box. */
    @Composable
    private fun GlyphCell(glyph: String) {
        Box(Modifier.size(GLYPH_CELL).testTag("glyph-$glyph")) {
            Text(
                text = glyph,
                style = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontSize = 16.sp,
                    color = ZinelyTheme.v2Colors.ink,
                ),
            )
        }
    }

    private var capturedInk: Color = Color.Unspecified
    private var capturedInkSoft: Color = Color.Unspecified
    private var capturedConsequence: Color = Color.Unspecified
    private var capturedDesk: Color = Color.Unspecified
    private var capturedPaper: Color = Color.Unspecified
    private var capturedChip: Color = Color.Unspecified

    @Composable
    private fun Host(dark: Boolean, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = dark) {
            capturedInk = ZinelyTheme.v21Colors.ink
            capturedInkSoft = ZinelyTheme.v21Colors.inkSoft
            capturedConsequence = ZinelyTheme.v21Colors.jamText
            capturedDesk = ZinelyTheme.v21Colors.desk
            capturedPaper = ZinelyTheme.v21Colors.paper
            capturedChip = ZinelyTheme.v21Colors.butterTint
            Box(Modifier.fillMaxSize().background(PROBE_GROUND)) { content() }
        }
    }

    /**
     * Both themes' scrims in **one** composition, over the same white ground.
     *
     * `setContent` may be called once per test, and the claim needs the two side by side anyway: the
     * question is whether they differ, and a single composition removes any doubt that they were rendered
     * under identical conditions.
     */
    private fun scrimPixels(): Pair<Color, Color> {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                ZinelyTheme(darkTheme = false) {
                    ScrimProbe("scrim-light", Alignment.TopStart)
                }
                ZinelyTheme(darkTheme = true) {
                    ScrimProbe("scrim-dark", Alignment.BottomEnd)
                }
            }
        }
        composeRule.waitForIdle()
        val raster = decorRaster()
        fun probe(tag: String): Color {
            val at = tagBounds(tag)
            return raster.colourAt(at.center.x.roundToInt(), at.center.y.roundToInt())
        }
        return probe("scrim-light") to probe("scrim-dark")
    }

    /**
     * A patch of the **production** scrim.
     *
     * It composes [ZineActionScrim] rather than a `Box` filled from a local copy of the literal, and that is
     * the whole point of the shape: the first version of this test painted its own patch, so swapping the
     * sheet's paint for a theme-aware token left it green — a test agreeing with a constant while production
     * did something else. The clipping `Box` is only there to give the fullscreen scrim a samplable patch.
     */
    @Composable
    private fun ScrimProbe(tag: String, corner: Alignment) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(corner).size(PROBE_PATCH).testTag(tag)) {
                ZineActionScrim(onDismiss = {})
            }
        }
    }

    private fun tagBounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun decorRaster(): Bitmap {
        assertEquals(
            "these pixel offsets assume dp == px; density was ${composeRule.density.density}",
            1.0f,
            composeRule.density.density,
            0.0001f,
        )
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    private fun Bitmap.colourAt(x: Int, y: Int): Color = Color(getPixel(x, y))

    private fun Bitmap.signature(region: Rect, threshold: Float): List<Boolean> {
        val out = mutableListOf<Boolean>()
        for (dy in 0 until GLYPH_CELL_PX) {
            for (dx in 0 until GLYPH_CELL_PX) {
                val x = region.left.roundToInt() + dx
                val y = region.top.roundToInt() + dy
                out += if (x in 0 until width && y in 0 until height) {
                    colourAt(x, y).luminance() < threshold
                } else {
                    false
                }
            }
        }
        return out
    }

    /** Halfway between the ground and the ink — the only threshold that means the same thing on any ground. */
    private fun inkThreshold(ground: Color): Float = (ground.luminance() + capturedInk.luminance()) / 2f

    private fun Bitmap.inkCoverage(region: Rect, threshold: Float): Int {
        var covered = 0
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0 && colourAt(x, y).luminance() < threshold) covered++
            }
        }
        return covered
    }

    private fun Bitmap.anyPixel(region: Rect, expected: Color): Boolean {
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0 && colourAt(x, y).closeTo(expected)) return true
            }
        }
        return false
    }

    private fun Color.closeTo(other: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - other.red) <= tolerance &&
            abs(green - other.green) <= tolerance &&
            abs(blue - other.blue) <= tolerance
    }

    /** The scrim's literal has alpha; the raster does not. Composite it the way the raster did. */
    private fun Color.compositeOverWhite(): Color = Color(
        red = red * alpha + (1f - alpha),
        green = green * alpha + (1f - alpha),
        blue = blue * alpha + (1f - alpha),
    )

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

    private fun relativeGap(a: Int, b: Int): Float {
        val larger = maxOf(a, b)
        return if (larger == 0) 0f else abs(a - b).toFloat() / larger
    }
}

/**
 * The V2.1 scrim — `.scrim{background:rgba(38,26,16,.42)}`, **one value for both themes**.
 *
 * **This is where D-022 landed, and it did not land on the ruling.** That defect was raised because V2's
 * Library wrote its scrim as a hard literal outside `:root`, so the frozen dark block could never reach it
 * and both themes dimmed identically — while the corpus's own `--scrim` *was* theme-aware, and the owner
 * ruled the corpus authoritative. The re-freeze writes a single literal again, deliberately: `.scrim` sits
 * in the shared block and no dark override in the file touches it. So V2.1's answer to *"should the scrim
 * invert?"* is **no**, at a new value, and the assertion the ruling produced is what that answer flips.
 * Recorded rather than closed — D-022 was raised against V2's file, and closing it is the owner's.
 *
 * Written out here rather than read from `ZinelyV21Colors` on purpose: a test that took the token would
 * agree with whatever the token said, including a wrong value. These are the bytes the frozen file names.
 */
private val FROZEN_SCRIM = Color(0xFF261A10).copy(alpha = 0.42f)

/** What the ruling rejected — pinned so a revert to it is a failure rather than a silence. */
private val STALE_LIBRARY_SCRIM = Color(0xFF1E1912).copy(alpha = 0.36f)

/** A patch big enough to sample away from any edge. */
private val PROBE_PATCH = 40.dp

/** The glyph probe's cell, in dp and in px — density is pinned to 1, and asserted to be. */
private val GLYPH_CELL = 28.dp
private const val GLYPH_CELL_PX = 28
