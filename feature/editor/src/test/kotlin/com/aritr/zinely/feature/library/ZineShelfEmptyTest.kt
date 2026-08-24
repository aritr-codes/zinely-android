package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The empty state — the transformation it draws, the copy it sets, and the room it leaves for the dock.
 *
 * **The illustrations are the risky part, and they are risky in a way screenshots ratify.** A sheet with
 * its dashed rule on the wrong fold draws a *different set of folding instructions* while looking entirely
 * correct; a book drawn in a chrome token instead of a content ink looks right in light and quietly
 * re-tints at night; a fore-edge drawn inside the bounds instead of outside shifts the book 1.5px off its
 * caption and reads as a rounding. So every claim about them is a **rasterised pixel** at a named offset.
 *
 * `sdk = [28]` puts the host below the API-29 grain floor (**D-014**), which is deliberate: the fills are
 * then flat and exactly assertable, and the grain path is what the goldens exist for. That split is itself
 * a claim — `the illustrations are flat stock below API 29` asserts the omission rather than assuming it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineShelfEmptyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HEADLINE = "Make your first little zine."
        const val BODY = "One sheet of paper becomes a little eight-page book you print and " +
            "fold yourself — we’ll show you each step."
        const val PRIVACY = "Everything stays on your phone — no account, nothing uploaded."
        const val SHEET_CAPTION = "ONE SHEET"
        const val BOOK_CAPTION = "A LITTLE BOOK"
        const val ARROW = "→"

        /** `.sheet-ill{width:94px;height:68px}` and `.book-ill{width:54px;height:70px}` — each 2px up
         * on V2, which is small enough to be invisible and large enough to be a wrong transcription. */
        const val SHEET_W = 94
        const val SHEET_H = 68
        const val BOOK_W = 54
        const val BOOK_H = 70

        /** `.sheet-ill .v{top:7px;bottom:7px}`, the folds at 33% · 50% · 67%, `.h{top:50%}`. */
        const val RULE_INSET = 7
        const val FIRST_FOLD = 0.33f
        const val SECOND_FOLD = 0.50f
        const val THIRD_FOLD = 0.67f

        /**
          * `.book-ill::before{left:6px;background:rgba(255,255,255,.3)}` — the crease, which resolves
          * against the **padding box**, so it stands `6 + 1.5` from the book's own left edge. V2 wrote 5
          * and a dark hairline; V2.1 writes 6 and a white one, so both the column and the *sign* of the
          * comparison had to be re-derived rather than nudged.
          */
        const val BOOK_CREASE_X = 8

        /** `.v2{background:repeating-linear-gradient(… 0 3px,transparent 3px 6px)}`. */
        const val DASH_LENGTH = 3

        /** How far `rotate(-2deg)` plus antialiasing walk a 1px rule off its nominal column. */
        const val RULE_DRIFT = 2

        /**
         * `.sheet-ill{border:1.5px}` / `.book-ill{border:1.5px}`, rounded up to the pixel a probe can
         * name. Every `left`/`top`/`%` inside either illustration resolves against the padding box, so
         * this term appears in each of them.
         */
        const val ILL_BORDER = 2

        /**
          * `.tf{gap:var(--gap-lg)}`, `.tf .col{gap:var(--gap-sm)}`,
          * `.empty{gap:var(--gap-md);padding:var(--gap-2xl) var(--gap-2xl) 206px}` — the frozen
          * 150dp dock clearance plus the quiet backup companion.
          * where V2 wrote 14 / 9 / 16 / 40. The caption's own margin is now the **column's** gap rather
          * than a margin on the label, which is why that name changed with its value.
          */
        const val TRANSFORM_GAP = 16
        const val LABEL_MARGIN_TOP = 8
        const val EMPTY_GAP = 12
        const val EMPTY_PADDING_SIDE = 36

        /** Narrower than the frozen 29ch measure, so the side padding becomes the binding constraint. */
        const val NARROW_HOST = 200

        /**
         * `--leaf` — `#4E7A3C` light, `#8FAE6B` dark.
         *
         * V2's book was a **content** ink (`ZinelyMakerInkId.Matcha`, `#7C8A3F`) and did not flip; V2.1's
         * is a chrome token and does. Both values are pinned so the change is visible in the diff and so
         * a token that moves fails here rather than turning the fill assertions quietly inert.
         */
        val LIGHT_LEAF = Color(0xFF4E7A3C)
        val DARK_LEAF = Color(0xFF8FAE6B)

        /**
         * `--paper` in light, which is the stock the sheet's rules are read against.
         *
         * Both are cross-checked against the live theme by their own test rather than trusted. A
         * hard-coded copy of a token is exactly how B3's tofu control became a false fact: it was right
         * when it was written, and nothing was watching it afterwards.
         */
        val LIGHT_PAPER = Color(0xFFFFF6E8)

        const val HALF_PIXEL = 0.5f

        val PROBE_GROUND = Color(0xFF00FF00)

        /** `.empty p{font-size:.94rem;max-width:29ch}` and `.pv{font-size:.75rem}` — V2's were .95rem,
         * 28ch and .76rem, so the yardsticks moved with the type they measure. */
        val BODY_SIZE = 15.04.sp
        val PRIVACY_SIZE = 12.sp
        const val MEASURE_CHARACTERS = 29

        /** The width the production headline is given: the host less the frozen 36px sides. */
        const val HEADLINE_MEASURE = 408

        /** The headline at the frozen tracking and at a loose one, both at that width. */
        const val REF_TRACK_FROZEN = "ref-track-frozen"
        const val REF_TRACK_LOOSE = "ref-track-loose"

        /** Reference renderings of the headline, for the type assertion to measure against. */
        const val REF_FROZEN = "ref-frozen"
        const val REF_LIGHTER = "ref-weight-500"
        const val REF_SMALL = "ref-16sp"
        const val REF_MEASURE = "ref-29ch"

        /**
         * `.empty h2{font-family:var(--voice);font-size:1.75rem;font-weight:700;line-height:1.12}` — 28px
         * in Averia 700, where V2 wrote Fraunces 500 at 27.52px, and **no `letter-spacing` at all**: V2's
         * `-.01em` is not in the V2.1 rule, so the frozen tracking is now `0`.
         */
        val HEADLINE_SIZE = 28.sp
        val HEADLINE_LINE_HEIGHT = 31.36.sp
        val FROZEN_TRACKING = 0.em

        /** `.tf .arrow{margin-bottom:var(--gap-lg)}` — 16, where V2 wrote 18. */
        const val ARROW_MARGIN_BOTTOM = 16

        /** The square the arrow is drawn into, since Pass 1 made it a `Canvas` rather than a glyph. */
        const val ARROW_BOX = 24

        /** Dock clearance including the quiet backup companion, not content padding. */
        const val EMPTY_PADDING_BOTTOM = 206

        /** 29ch at `.pv`'s own type, which is a different number from 29ch at the paragraph's. */
        const val REF_MEASURE_PV = "ref-29ch-pv"

        /** The privacy line with and without the `line-height:1.55` it inherits from `.empty p`. */
        const val REF_PV_LEADED = "ref-pv-leaded"
        const val REF_PV_UNLEADED = "ref-pv-unleaded"
    }

    // ---------------------------------------------------------------------------------------------
    // What the screen says
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the empty state teaches the transformation rather than the app`() {
        empty()
        // The frozen file's own comment: *"empty state — teaches the concept by showing the
        // transformation"*. There is no carousel, no sample zine and no tour, and the two lines are the
        // whole of the copy. Asserted by text so a re-worded line is a failure rather than a silent
        // product change.
        composeRule.onNodeWithText(HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithText(BODY).assertIsDisplayed()
        composeRule.onNodeWithText(PRIVACY).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineArrowTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ZineSheetIllustrationTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ZineBookIllustrationTestTag).assertIsDisplayed()
    }

    @Test
    fun `the captions are the frozen words, uppercased by the style rather than by the copy`() {
        empty()
        // `text-transform:uppercase` is a *rendering* instruction; the DOM text stays lowercase. Compose
        // has no equivalent, so the case change happens in the composable and the frozen lowercase source
        // stays in the constant. Both halves are asserted: the drawn form, and that the lowercase form is
        // not what reaches the tree — otherwise "uppercase" could be satisfied by a constant nobody
        // transformed.
        composeRule.onNodeWithText(SHEET_CAPTION).assertIsDisplayed()
        composeRule.onNodeWithText(BOOK_CAPTION).assertIsDisplayed()
        composeRule.onNodeWithText("one sheet").assertDoesNotExist()
        assertNotEquals("the two forms must differ, or this test discriminates nothing", "one sheet", SHEET_CAPTION)
    }

    @Test
    fun `the headline is the voice face at the frozen size and weight`() {
        // The technique [ZineShelfTest] established for `.shelf-head h1`, applied to `.empty h2`:
        // `font-family:var(--voice);font-size:1.75rem;font-weight:700` — **Averia 700 at 28px**, where V2
        // wrote Fraunces 500 at 27.52px and D-005 had to rule on the file's stale 600. Ink coverage rather
        // than bounds, because a centred `Text` in a fixed-width column measures the same at any weight.
        emptyWithReferences()
        val raster = decorRaster()
        val threshold = (PROBE_GROUND.luminance() + capturedInk.luminance()) / 2f

        val frozen = raster.inkCoverage(bounds(REF_FROZEN, byTag = true), threshold)
        val lighter = raster.inkCoverage(bounds(REF_LIGHTER, byTag = true), threshold)
        val smaller = raster.inkCoverage(bounds(REF_SMALL, byTag = true), threshold)
        // Found by its role, not its words: this host renders the same sentence three more times as
        // yardsticks, and `<h2>` is the one thing only the real headline carries.
        val headline = raster.inkCoverage(headlineBounds(), threshold)

        // Discrimination asserted before parity — the rule this programme adopted after three packages
        // shipped assertions blind to the defect class their names claimed to gate.
        assertTrue("nothing was drawn at the frozen style (coverage $frozen)", frozen > 0)
        assertTrue(
            "this host cannot tell the frozen 700 from V2's 500 ($frozen vs $lighter), " +
                "so the weight is unguarded here",
            relativeGap(frozen, lighter) > 0.03f,
        )
        assertTrue(
            "this host cannot tell the frozen ${HEADLINE_SIZE} from body 16sp ($frozen vs $smaller)",
            relativeGap(frozen, smaller) > 0.03f,
        )
        assertTrue(
            "the headline must render at Averia 700 / ${HEADLINE_SIZE} — coverage $headline against " +
                "$frozen frozen, $lighter at weight 500, $smaller at 16sp",
            relativeGap(headline, frozen) <= 0.03f,
        )
    }

    @Test
    fun `the headline is tracked in, which is what lets it hold one line`() {
        // `letter-spacing:-.01em` had no assertion: ink coverage over a node's own bounds is essentially
        // tracking-invariant, so the three references this file already renders could not see it. Tracking
        // shows up in *width*, and at this size the effect is structural rather than cosmetic — the frozen
        // headline is close enough to the column's width that loosening the tracking wraps it.
        emptyWithReferences()
        val tight = bounds(REF_TRACK_FROZEN, byTag = true)
        val loose = bounds(REF_TRACK_LOOSE, byTag = true)
        val headline = headlineBounds()

        assertTrue(
            "the two trackings must render differently, or this test discriminates nothing " +
                "(${tight.width}x${tight.height} against ${loose.width}x${loose.height})",
            abs(tight.width - loose.width) > 1f || abs(tight.height - loose.height) > 1f,
        )
        assertEquals(
            "the headline must render at the frozen tracking",
            tight.height,
            headline.height,
            HALF_PIXEL,
        )
        assertEquals("and at its width", tight.width, headline.width, HALF_PIXEL)
    }

    @Test
    fun `the headline is a heading to a screen reader`() {
        empty()
        // The frozen markup is an `<h2>`. Same claim B2 makes for `.shelf-head h1` and the same loss if it
        // is dropped: a landmark becomes a stray phrase that looks identical.
        composeRule.onNodeWithText(HEADLINE)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `the paragraph is measured in characters, not in a fraction of the screen`() {
        emptyWithMeasure()
        val paragraph = bounds(BODY)
        val zeros = bounds(REF_MEASURE, byTag = true).width
        // `max-width:28ch` — twenty-eight advances of "0" in Inter, which is what the CSS unit *is*. A
        // fraction of the screen would look identical at this width and be wrong at every other, and
        // wrong again under font scaling.
        assertTrue(
            "the paragraph must not exceed ${MEASURE_CHARACTERS}ch (${paragraph.width} against $zeros)",
            paragraph.width <= zeros + HALF_PIXEL,
        )
        // And it must actually be *bound* by that measure rather than merely narrower than it — a
        // paragraph that fits on one line would pass the assertion above with no constraint at all.
        assertTrue(
            "the paragraph must be wide enough for the measure to bind (${paragraph.width})",
            paragraph.width > zeros * 0.8f,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The loose sheet — `.sheet-ill`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the sheet is the frozen size`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        assertEquals("the sheet must be ${SHEET_W}px wide", SHEET_W.toFloat(), sheet.width, HALF_PIXEL)
        assertEquals("and ${SHEET_H}px tall", SHEET_H.toFloat(), sheet.height, HALF_PIXEL)
    }

    @Test
    fun `the dashed rule is the middle one`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val raster = decorRaster()

        val first = raster.paintedRowsAlong(sheet, FIRST_FOLD)
        val second = raster.paintedRowsAlong(sheet, SECOND_FOLD)
        val third = raster.paintedRowsAlong(sheet, THIRD_FOLD)
        val full = SHEET_H - 2 * ILL_BORDER - 2 * RULE_INSET

        // Three folds and a cut. Moving the dash to `.v1` or `.v3` draws a different set of folding
        // instructions for the same eight-page sheet, and looks entirely plausible — the illustration is
        // 94px wide and nobody counts. So the *position* is asserted, not the existence of a dash.
        assertTrue("the first fold must be a solid rule ($first of $full rows)", first >= full - 2)
        assertTrue("the third fold must be a solid rule ($third of $full rows)", third >= full - 2)
        assertTrue(
            "the middle rule must be dashed — 3px on, 3px off, so about half its run ($second of $full)",
            second in (full / 4)..(full * 3 / 4),
        )

        // A duty cycle alone does not describe a dash: `3px on / 3px off` and `8px on / 8px off` both paint
        // half the run, and the assertion above accepts either. The *period* is what the frozen repeating
        // gradient states, so it is counted rather than inferred — a 50px run at a 6px period alternates
        // eight times, and at a 16px period three or four.
        val runs = raster.paintedRunsAlong(sheet, SECOND_FOLD)
        val expected = full / (2 * DASH_LENGTH)
        assertEquals(
            "the middle rule must alternate every ${DASH_LENGTH}px, so about $expected times ($runs)",
            expected.toFloat(),
            runs.toFloat(),
            1f,
        )
        assertTrue("a solid rule must not alternate at all", raster.paintedRunsAlong(sheet, FIRST_FOLD) == 1)
    }

    @Test
    fun `the cut line is drawn at half strength, softer than the folds it sits between`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        // A dashed rule is unpainted for half its run, so the row is found rather than assumed — a fixed
        // offset lands in a gap one time in two, which is how the first version of this failed.
        //
        // Routed through the same three helpers as its siblings. It used to carry its own border-box
        // column (`left + .50 × 94`) and its own row range, which happen to agree with the padding-box
        // arithmetic **at 50% only** — at any other fold the two differ and `.first {}` would throw
        // rather than fail. A review caught it standing on the uncorrected geometry while green.
        val nominal = foldColumn(sheet, SECOND_FOLD)
        val y = ruleRun(sheet).first { raster.isPaintedNear(nominal, it) }
        // The rotation walks the rule off `nominal`, so the *darkest* column in the window is the one
        // carrying the mark; sampling `nominal` itself would read an antialiased neighbour and call a
        // full-strength rule half-strength.
        val x = (nominal - RULE_DRIFT..nominal + RULE_DRIFT).minBy { raster.colourAt(it, y).luminance() }

        // `.v2{opacity:.55}` on `--ink-faint`. `isPainted` only asks "not the paper", so the alpha was
        // invisible to every assertion in this file — at 1.0 the cut line becomes the darkest mark on the
        // sheet, which inverts the drawing's own hierarchy: the cut would read as the strongest instruction
        // on a diagram about folding. Asserted against the token itself, which is what full strength *is*.
        val dash = raster.colourAt(x, y)

        assertTrue("the cut line must be painted at all (found $dash on paper $LIGHT_PAPER)", raster.isPainted(x, y))
        assertFalse(
            "the cut line must be half-strength, not full --ink-faint ($capturedInkFaint)",
            dash.closeTo(capturedInkFaint),
        )
        assertTrue(
            "and half strength means between the paper and the ink, not beyond either " +
                "($dash against paper $LIGHT_PAPER and ink $capturedInkFaint)",
            dash.luminance() in capturedInkFaint.luminance()..LIGHT_PAPER.luminance(),
        )
    }

    @Test
    fun `the sheet's rules stop six pixels short of its edges`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        val x = foldColumn(sheet, FIRST_FOLD)

        // `top:7px;bottom:7px`, against the **padding box** — so the rule starts `7 + 1.5` from the
        // element's own top edge, and the probes carry that term rather than absorbing it into slack.
        assertFalse(
            "the rule must not reach the sheet's top edge",
            raster.isPaintedNear(x, sheet.top.roundToInt() + RULE_INSET - 3),
        )
        assertTrue(
            "the rule must begin ${RULE_INSET}px down from its padding box",
            raster.isPaintedNear(x, sheet.top.roundToInt() + RULE_INSET + ILL_BORDER + 1),
        )
        assertFalse(
            "and must not reach the bottom edge",
            raster.isPaintedNear(x, sheet.bottom.roundToInt() - RULE_INSET + 3),
        )
    }

    @Test
    fun `the horizontal rule crosses at half the sheet's height`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        // Sampled between the folds so only the horizontal rule can be responsible for the hit — at 20%
        // across, which is clear of all three verticals.
        val x = (sheet.left + 0.20f * SHEET_W).roundToInt()
        val mid = (sheet.top + ILL_BORDER + SECOND_FOLD * (SHEET_H - 2 * ILL_BORDER)).roundToInt()

        assertTrue("the horizontal rule must cross at half height", raster.isPainted(x, mid))
        assertFalse("and nowhere else on this column", raster.isPainted(x, mid - 8))
        assertFalse("nor below it", raster.isPainted(x, mid + 8))
    }

    // ---------------------------------------------------------------------------------------------
    // The little book — `.book-ill`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the book is the frozen size, and nothing hangs outside it`() {
        empty()
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()

        assertEquals("the book must be ${BOOK_W}px wide", BOOK_W.toFloat(), book.width, HALF_PIXEL)
        assertEquals("and ${BOOK_H}px tall", BOOK_H.toFloat(), book.height, HALF_PIXEL)

        // **V2's fore-edge is gone, and the assertions it carried are deleted rather than re-baselined.**
        // V2 drew stacked leaves at `::after{right:-3px}`, outside the element, and two tests measured
        // their inset, their stripes and where they stopped. The V2.1 `.book-ill` has one pseudo-element
        // — the crease — and no leaves at all, so those numbers have nothing left to describe. What the
        // deleted test was really guarding is kept: the book's own painted extent is its bounds.
        val y = book.center.y.roundToInt()
        assertTrue(
            "the book's fill must reach its own right edge",
            raster.colourAt(book.right.roundToInt() - 3, y).closeTo(capturedLeaf),
        )
        assertFalse(
            "and nothing of the book may be painted beyond it",
            raster.colourAt(book.right.roundToInt() + 3, y).closeTo(capturedLeaf),
        )
    }

    @Test
    fun `the book is printed in leaf, the theme's own green`() {
        empty(dark = false)
        val fill = bookFillPixel()
        assertTrue("the book must print in --leaf ($capturedLeaf, found $fill)", fill.closeTo(capturedLeaf))
        assertEquals("and --leaf in light is the frozen #4E7A3C", LIGHT_LEAF, capturedLeaf)
    }

    @Test
    fun `the book's green follows the theme, which is the reading that changed`() {
        // **The claim here is the inverse of V2's, and it is not a re-baseline.** V2 drew this book in
        // `ZinelyMakerInkId.Matcha` — a **content** ink, theme-invariant on the argument that a printed
        // object does not re-tint at night — and this test existed to hold that. V2.1 writes
        // `.book-ill{background:var(--leaf)}`, a chrome token with a dark value, so the illustration
        // *does* flip: #4E7A3C by day, #8FAE6B by night.
        //
        // That is a real change of reading and worth saying rather than swapping a constant over: the
        // book in the empty state is not a zine the user made, it is a diagram of what one becomes, so
        // the corpus treats it as chrome. The covers on the shelf are the printed objects, and they still
        // do not flip.
        empty(dark = true)
        val fill = bookFillPixel()
        assertTrue("the book must print in dark --leaf ($capturedLeaf, found $fill)", fill.closeTo(capturedLeaf))
        assertEquals("and --leaf in dark is the frozen #8FAE6B", DARK_LEAF, capturedLeaf)
        assertNotEquals("which is not the light value, or nothing here flipped", LIGHT_LEAF, capturedLeaf)
    }

    @Test
    fun `the two theme literals this file pins are still the tokens they name`() {
        // `LIGHT_PAPER` is the basis of every `isPainted` call in this file, and `DARK_CHROME_MATCHA` is
        // the whole content of the claim that the book does not follow the theme. Both were written as
        // hard-coded copies — right on the day, and unwatched after it. If a token moves, this fails here
        // rather than turning a dozen other assertions quietly inert.
        empty(dark = false)
        assertEquals("LIGHT_PAPER must still be --paper in light", capturedPaper, LIGHT_PAPER)
        assertEquals("LIGHT_LEAF must still be --leaf in light", capturedLeaf, LIGHT_LEAF)
    }

    @Test
    fun `the dark leaf this file pins is still the token it names`() {
        empty(dark = true)
        assertEquals("DARK_LEAF must still be --leaf in dark", capturedLeaf, DARK_LEAF)
    }

    @Test
    fun `the book is creased down its binding edge`() {
        empty()
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        val y = book.center.y.roundToInt()

        // `::before{left:5px;top:5px;bottom:5px;width:1px;background:rgba(255,255,255,.25)}` — the fold,
        // and the single mark that makes this rectangle read as a *folded* object rather than a card.
        // It had no assertion of any kind: deleting the whole `drawRect` left all 32 tests green, and the
        // one helper that mentions it only says which pixels to avoid so it does *not* get measured.
        val crease = raster.colourAt(book.left.roundToInt() + BOOK_CREASE_X, y)
        assertFalse(
            "the crease must lighten the ink at ${BOOK_CREASE_X}px in (found $crease, the plain fill)",
            crease.closeTo(capturedLeaf),
        )
        assertTrue(
            "and it is a white highlight, so it must be lighter than the fill, not darker",
            crease.luminance() > capturedLeaf.luminance(),
        )
        // Discrimination: the fill either side must be untouched, or "lighter than the fill" would be
        // satisfied by a book drawn in the wrong colour altogether.
        assertTrue(
            "the fill must be plain two pixels further in",
            raster.colourAt(book.left.roundToInt() + BOOK_CREASE_X + 3, y).closeTo(capturedLeaf),
        )
        assertTrue(
            "and plain two pixels outside it",
            raster.colourAt(book.left.roundToInt() + BOOK_CREASE_X - 3, y).closeTo(capturedLeaf),
        )
    }

    // The fore-edge's own test stood here. `.book-ill` has no `::after` in V2.1 — the stacked leaves are
    // not in the frozen file — so the test is deleted rather than re-baselined onto pixels that no longer
    // exist. What it shared with the size test (nothing of the book is painted outside its bounds) moved
    // there.

    @Test
    fun `the illustrations are flat stock below API 29`() {
        // **D-014**, ruled: where the platform cannot blend soft-light the implementation omits rather
        // than approximates, because an approximation of a material is a second material. This host is
        // API 28, so a uniform patch of the book must be *exactly* uniform — grain would break it, and a
        // silent fallback to `src-over` noise is the defect the ruling exists to prevent.
        empty()
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        val patch = (book.center.y.roundToInt() - 4..book.center.y.roundToInt() + 4).flatMap { y ->
            (book.center.x.roundToInt() - 4..book.center.x.roundToInt() + 4).map { x -> raster.colourAt(x, y) }
        }
        assertEquals("the book's fill must be one colour, not a grained field", 1, patch.toSet().size)

        // The sheet too — the test's name says *illustrations*, and the guard it rests on is shared, so a
        // book-only probe leaves half the claim unmade. Sampled in the panel between the first fold and the
        // dashed one, clear of every rule.
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val sx = (sheet.left + 0.41f * SHEET_W).roundToInt()
        val sy = sheet.center.y.roundToInt() + 10
        val stock = (sy - 3..sy + 3).flatMap { y -> (sx - 3..sx + 3).map { x -> raster.colourAt(x, y) } }
        assertEquals("the sheet's stock must be one colour too", 1, stock.toSet().size)
    }

    @Test
    fun `the sheet is paper, and paper inverts with the theme`() {
        // `.sheet-ill{background:var(--paper)}` — a chrome token, unlike the book's content ink beside it,
        // and the pair is the point: the same illustration row holds one thing that must follow the theme
        // and one that must not. Nothing rendered the sheet in dark at all, so a hard-coded `#F7F2E7` in
        // `SheetIllustration` passed the whole suite while the book's theme-invariance was pinned twice.
        empty(dark = true)
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        val stock = raster.colourAt(
            (sheet.left + 0.41f * SHEET_W).roundToInt(),
            sheet.center.y.roundToInt() + 10,
        )

        assertTrue(
            "the sheet must be --paper in dark ($capturedPaper), not the light stock (found $stock)",
            stock.closeTo(capturedPaper),
        )
        assertFalse(
            "and the two must differ, or this asserts nothing",
            capturedPaper.closeTo(LIGHT_PAPER),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Layout — the room the dock needs
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the content is centred above the dock's room, not in the screen`() {
        empty()
        val host = bounds(ZineShelfEmptyTestTag, byTag = true)
        val top = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val bottom = bounds(BODY)
        val contentCentre = (top.top + bottom.bottom) / 2f

        // The asymmetric bottom clearance against `justify-content:center` centres the content in the
        // space *above* the dock. Trim it and the copy sits under the buttons; the
        // frozen number is four times the others precisely because it is not padding, it is clearance.
        assertTrue(
            "the content must sit above the host's own centre ($contentCentre against ${host.center.y})",
            contentCentre < host.center.y,
        )
        // Specifically, by half the difference between the top and bottom clearances.
        assertEquals(
            "the content must be lifted by half the padding difference",
            (EMPTY_PADDING_BOTTOM - EMPTY_PADDING_SIDE) / 2f,
            host.center.y - contentCentre,
            2f,
        )
    }

    @Test
    fun `the frozen gaps hold the row, the column and the captions apart`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val arrow = bounds(ZineArrowTestTag, byTag = true)
        val caption = bounds(SHEET_CAPTION)
        val headline = headlineBounds()
        val paragraph = bounds(BODY)

        // `.tf{gap:14px}`. Measured on the sheet's side, where the illustration is wider than its caption,
        // so the column's edge is the illustration's edge and the gap is readable without inferring the
        // column box.
        assertTrue(
            "the sheet's caption must be narrower than the sheet, or the column edge is not the sheet's",
            caption.width < sheet.width,
        )
        assertEquals(
            "the row must hold ${TRANSFORM_GAP}px between the sheet and the arrow",
            TRANSFORM_GAP.toFloat(),
            arrow.left - sheet.right,
            HALF_PIXEL,
        )

        // `.tf .lbl{margin-top:9px}`. Structurally invisible to the arrow test, which derives the row's
        // centre from `caption.bottom` — so both sides of that equality move together and the margin
        // cancels itself out.
        assertEquals(
            "the caption must sit ${LABEL_MARGIN_TOP}px under its illustration",
            LABEL_MARGIN_TOP.toFloat(),
            caption.top - sheet.bottom,
            HALF_PIXEL,
        )

        // `.empty{gap:16px}`. Symmetric about the content's centre, so the centring test is blind to it:
        // collapse the gap and every element moves toward a centre that does not itself move. The
        // paragraph carries `margin:0`, so the space between it and the headline is the gap alone.
        assertEquals(
            "the column must hold ${EMPTY_GAP}px between the headline and the paragraph",
            EMPTY_GAP.toFloat(),
            paragraph.top - headline.bottom,
            HALF_PIXEL,
        )
    }

    @Test
    fun `the side padding binds the paragraph when the screen is narrower than its measure`() {
        // The side padding is unobservable at any width the
        // 29ch measure fits
        // in, and the vertical pair is unobservable *at all* under `justify-content:center`, which centres
        // in the box the two of them leave: only their difference reaches the layout, and the existing
        // centring test pins exactly that. Narrowing the host until the padding binds is what makes the 40
        // a measurement rather than a number in a file.
        composeRule.setContent {
            Host { ZineShelfEmpty(Modifier.width(NARROW_HOST.dp)) }
        }
        composeRule.waitForIdle()

        val host = bounds(ZineShelfEmptyTestTag, byTag = true)
        val paragraph = bounds(BODY)
        assertEquals(
            "the paragraph must stop ${EMPTY_PADDING_SIDE}px short of the left edge",
            EMPTY_PADDING_SIDE.toFloat(),
            paragraph.left - host.left,
            HALF_PIXEL,
        )
        assertEquals(
            "and ${EMPTY_PADDING_SIDE}px short of the right",
            EMPTY_PADDING_SIDE.toFloat(),
            host.right - paragraph.right,
            HALF_PIXEL,
        )
        // The discrimination: the padding only binds because the frozen measure is wider than what is left.
        assertTrue(
            "the host must be narrower than the ${MEASURE_CHARACTERS}ch measure, or the padding never binds",
            paragraph.width <= NARROW_HOST - 2 * EMPTY_PADDING_SIDE + HALF_PIXEL,
        )
    }

    @Test
    fun `the arrow rides half its own margin above the row, not all of it`() {
        empty()
        val arrow = bounds(ZineArrowTestTag, byTag = true)
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val caption = bounds(BOOK_CAPTION)

        // **The row's centre is the taller column's centre**, and that is the book's: 68 + 9 + caption
        // against the sheet's 66 + 9 + caption. `align-items:center` centres every child's *margin box* on
        // that line, so the tallest column spans the row and its own centre is the row's. Taking the
        // union of the two illustrations instead — a first draft did — measures a line neither element is
        // centred on, and reported the arrow 2.5px on the wrong side of it.
        val rowCentre = (book.top + caption.bottom) / 2f

        // `.arrow{margin-bottom:var(--gap-lg)}` lifts the mark by **half** the margin, because it is the
        // margin box that is centred and the mark sits at its top. `Modifier.offset(y = -16.dp)` would
        // move it the full 16 and be wrong by exactly a factor of two — the sort of miss a screenshot
        // ratifies.
        //
        // **Measured through the margin box, because the arrow is a `Canvas` now.** Device Pass 1 turned
        // the typed `→` into a drawn path, and the tag sits outside the padding, so `arrow.center.y` is
        // the *margin box's* centre — which is the row's centre, exactly as `align-items:center` says. The
        // lift is then the box's own asymmetry, so it is asserted as such: the drawn square is at the top
        // of a box that is `ArrowBox + margin` tall. A test that kept reading `arrow.center.y` as the
        // mark's centre would have measured the centring and called it the lift.
        assertEquals(
            "the arrow's margin box must be centred on the row",
            rowCentre,
            arrow.center.y,
            1.5f,
        )
        assertEquals(
            "and it must carry the ${ARROW_MARGIN_BOTTOM}px margin below the mark, which is what " +
                "lifts the mark ${ARROW_MARGIN_BOTTOM / 2}px above that line",
            (ARROW_BOX + ARROW_MARGIN_BOTTOM).toFloat(),
            arrow.height,
            HALF_PIXEL,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun empty(dark: Boolean = false) {
        composeRule.setContent { Host(dark) { ZineShelfEmpty() } }
        composeRule.waitForIdle()
    }

    /** The empty state, plus the headline rendered three more times at known styles on the same ground. */
    private fun emptyWithReferences() {
        composeRule.setContent {
            Host {
                ZineShelfEmpty()
                Column(Modifier.align(Alignment.BottomStart)) {
                    Reference(REF_FROZEN, FontWeight.Bold, HEADLINE_SIZE)
                    Reference(REF_LIGHTER, FontWeight.Medium, HEADLINE_SIZE)
                    Reference(REF_SMALL, FontWeight.Bold, 16.sp)
                }
                // The tracking pair, held to the **same width the production headline gets** — the host
                // less the frozen ${EMPTY_PADDING_SIDE}px sides. Tracking only reaches the layout through wrapping, so a
                // reference measured unconstrained would render one line at any tracking and prove nothing.
                Column(Modifier.align(Alignment.TopEnd).width(HEADLINE_MEASURE.dp)) {
                    Reference(REF_TRACK_FROZEN, FontWeight.Bold, HEADLINE_SIZE, FROZEN_TRACKING)
                    Reference(REF_TRACK_LOOSE, FontWeight.Bold, HEADLINE_SIZE, 0.05.em)
                }
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * The empty state, plus twenty-eight zeros at the paragraph's own style — the `ch` yardstick.
     *
     * Composed in the same host rather than measured in a second `setContent`, which the Compose rule does
     * not allow: one composition per test, so every yardstick a test needs must stand beside the subject.
     */
    private fun emptyWithMeasure() {
        composeRule.setContent {
            Host {
                ZineShelfEmpty()
                Text(
                    text = "0".repeat(MEASURE_CHARACTERS),
                    style = TextStyle(
                        fontFamily = ZinelyV21Fonts.Work,
                        fontSize = BODY_SIZE,
                        lineHeight = 1.55.em,
                    ),
                    softWrap = false,
                    modifier = Modifier.testTag(REF_MEASURE).align(Alignment.TopStart),
                )
                // The same count at `.pv`'s own type — the measure is relative to the element, not to the rule.
                val pvStyle = TextStyle(
                    fontFamily = ZinelyV21Fonts.Work,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = PRIVACY_SIZE,
                    lineHeight = 1.55.em,
                )
                Text(
                    text = "0".repeat(MEASURE_CHARACTERS),
                    style = pvStyle,
                    softWrap = false,
                    modifier = Modifier.testTag(REF_MEASURE_PV).align(Alignment.TopEnd),
                )

                // The privacy line's own text at its own measure, with the inherited leading and without
                // it. Wrapping identically is the whole point, so the measure is rebuilt here the way
                // production builds it — that is a *yardstick*, not a second reading of the claim: what is
                // pinned below is the **leading**, and the measure itself is pinned independently by
                // `the privacy line is measured in its own characters`.
                val measurer = rememberTextMeasurer()
                val pvMeasure = with(LocalDensity.current) {
                    measurer.measure("0".repeat(MEASURE_CHARACTERS), pvStyle, softWrap = false).size.width.toDp()
                }
                Text(
                    text = PRIVACY,
                    style = pvStyle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag(REF_PV_LEADED)
                        .align(Alignment.BottomStart)
                        .widthIn(max = pvMeasure),
                )
                Text(
                    text = PRIVACY,
                    style = pvStyle.copy(lineHeight = TextUnit.Unspecified),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag(REF_PV_UNLEADED)
                        .align(Alignment.BottomEnd)
                        .widthIn(max = pvMeasure),
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Reference(
        tag: String,
        weight: FontWeight,
        size: TextUnit,
        tracking: TextUnit = FROZEN_TRACKING,
    ) {
        Text(
            text = HEADLINE,
            style = TextStyle(
                fontFamily = ZinelyV21Fonts.Voice,
                fontWeight = weight,
                fontSize = size,
                letterSpacing = tracking,
                lineHeight = HEADLINE_LINE_HEIGHT,
                color = ZinelyTheme.v21Colors.ink,
            ),
            modifier = Modifier.testTag(tag),
        )
    }

    private var capturedInk: Color = Color.Unspecified
    private var capturedLeaf: Color = Color.Unspecified
    private var capturedPaper: Color = Color.Unspecified
    private var capturedInkSoft: Color = Color.Unspecified
    private var capturedInkFaint: Color = Color.Unspecified

    @Composable
    private fun Host(dark: Boolean = false, content: @Composable BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = dark) {
            capturedInk = ZinelyTheme.v21Colors.ink
            capturedLeaf = ZinelyTheme.v21Colors.leaf
            capturedPaper = ZinelyTheme.v21Colors.paper
            capturedInkSoft = ZinelyTheme.v21Colors.inkSoft
            capturedInkFaint = ZinelyTheme.v21Colors.inkFaint
            Box(Modifier.fillMaxSize().background(PROBE_GROUND)) { content() }
        }
    }

    /** A pixel of the book's own fill, clear of its crease at x=5 and of the fore-edge at the far side. */
    private fun bookFillPixel(): Color {
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        return raster.colourAt(book.center.x.roundToInt(), book.center.y.roundToInt())
    }

    /**
     * How many painted runs the rule at [fraction] breaks into — the dash's *period*, not its duty cycle.
     *
     * A 3px-on/3px-off rule over a 54px run alternates nine times; an 8px-on/8px-off rule paints the same
     * fraction of the column and alternates three. The duty-cycle assertion cannot tell them apart.
     */
    private fun Bitmap.paintedRunsAlong(sheet: Rect, fraction: Float): Int {
        val x = foldColumn(sheet, fraction)
        var runs = 0
        var wasPainted = false
        for (y in ruleRun(sheet)) {
            val painted = isPaintedNear(x, y)
            if (painted && !wasPainted) runs++
            wasPainted = painted
        }
        return runs
    }

    /** How many rows of the rule at [fraction] across the sheet are painted at all. */
    private fun Bitmap.paintedRowsAlong(sheet: Rect, fraction: Float): Int =
        ruleRun(sheet).count { isPaintedNear(foldColumn(sheet, fraction), it) }

    /**
     * The rows a `.v` rule actually occupies: `top:7px;bottom:7px` **against the padding box**, so the run
     * is inset by the 1.5px border at both ends as well. 68 − 2×2 − 2×7 = 50 rows, not the 54 an
     * arithmetic on the border box gives.
     */
    private fun ruleRun(sheet: Rect): IntRange =
        (sheet.top.roundToInt() + ILL_BORDER + RULE_INSET) until (sheet.bottom.roundToInt() - ILL_BORDER - RULE_INSET)

    /**
     * The pixel column a fold rule falls on — resolved against the **padding box**.
     *
     * `.sheet-ill i{position:absolute}` percentages resolve against the padding box, which
     * `*{box-sizing:border-box}` insets by the 1.5px border, so `left:33%` is `1.5 + .33 × (94 − 3)` =
     * 31.5px from the element's own left.
     */
    private fun foldColumn(sheet: Rect, fraction: Float): Int =
        (sheet.left + ILL_BORDER + fraction * (SHEET_W - 2 * ILL_BORDER)).roundToInt()

    /**
     * Whether the rule is painted on row [y] — searched across [RULE_DRIFT] columns either side of [x]
     * rather than on [x] alone.
     *
     * **The sheet is rotated, so a "vertical" rule is not in one column.** `.sheet-ill{transform:
     * rotate(-2deg)}` walks a 50px rule about `50 × tan 2° ≈ 1.8px` across, and a 1px line on a
     * fractional column is antialiased over two more. A single-column probe therefore reads a *solid*
     * rule as 39 painted rows of 50 and calls the drawing broken — which is exactly what it did before
     * the rotation was accounted for. The window follows the mark; it does not loosen the claim, because
     * the dash's gaps run the full width of the rule and remain gaps at every column in the window.
     */
    private fun Bitmap.isPaintedNear(x: Int, y: Int): Boolean =
        (x - RULE_DRIFT..x + RULE_DRIFT).any { isPainted(it, y) }

    /**
     * Whether a pixel carries a rule rather than the sheet's own stock.
     *
     * Read as *"not the paper"* rather than *"is the hairline"*: `--hair` is ink at 12% over paper and the
     * dashed rule is ink-faint at 50%, so the two rules are different colours and only their presence is
     * the shared claim. The paper underneath is flat at this SDK (D-014), so the comparison is exact.
     */
    private fun Bitmap.isPainted(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        return !colourAt(x, y).closeTo(LIGHT_PAPER)
    }

    /**
     * The **production** privacy line, in a host where two yardsticks render the same sentence.
     *
     * Excluded by tag rather than picked by index: composition order is a fact about the harness, not about
     * the claim, and an index would keep passing while silently measuring a reference.
     */
    private fun productionPrivacyBounds(): Rect = composeRule
        .onAllNodesWithText(PRIVACY)
        .filterToOne(hasTestTag(REF_PV_LEADED).not() and hasTestTag(REF_PV_UNLEADED).not())
        .fetchSemanticsNode()
        .boundsInRoot

    /** The empty state's own `<h2>`, found by the role the yardsticks do not carry. */
    private fun headlineBounds(): Rect = composeRule
        .onNode(hasText(HEADLINE) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        .fetchSemanticsNode()
        .boundsInRoot

    private fun bounds(finder: String, byTag: Boolean = false): Rect {
        val node = if (byTag) composeRule.onNodeWithTag(finder) else composeRule.onNodeWithText(finder)
        return node.fetchSemanticsNode().boundsInRoot
    }

    private fun Bitmap.inkCoverage(region: Rect, threshold: Float): Int {
        var covered = 0
        for (y in region.top.roundToInt() until region.bottom.roundToInt().coerceAtMost(height)) {
            for (x in region.left.roundToInt() until region.right.roundToInt().coerceAtMost(width)) {
                if (x >= 0 && y >= 0 && colourAt(x, y).luminance() < threshold) covered++
            }
        }
        return covered
    }

    private fun relativeGap(a: Int, b: Int): Float {
        val larger = maxOf(a, b)
        return if (larger == 0) 0f else abs(a - b).toFloat() / larger
    }

    private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

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

    private fun Color.closeTo(expected: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - expected.red) <= tolerance &&
            abs(green - expected.green) <= tolerance &&
            abs(blue - expected.blue) <= tolerance
    }
}
