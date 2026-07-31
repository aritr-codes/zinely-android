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
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
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
        const val PRIVACY = "Everything you make stays on your phone — no account, nothing uploaded."
        const val SHEET_CAPTION = "ONE SHEET"
        const val BOOK_CAPTION = "A LITTLE BOOK"
        const val ARROW = "→"

        /** `.sheet-ill{width:92px;height:66px}` and `.book-ill{width:52px;height:68px}`. */
        const val SHEET_W = 92
        const val SHEET_H = 66
        const val BOOK_W = 52
        const val BOOK_H = 68

        /** `.sheet-ill .v{top:6px;bottom:6px}`, the folds at 33% · 50% · 67%, `.h{top:50%}`. */
        const val RULE_INSET = 6
        const val FIRST_FOLD = 0.33f
        const val SECOND_FOLD = 0.50f
        const val THIRD_FOLD = 0.67f

        /** `.book-ill::after{right:-3px;top:4px;bottom:4px;width:3px}`. */
        const val FORE_EDGE_W = 3
        const val FORE_EDGE_INSET = 4

        /** `.book-ill::before{left:5px}` — the crease. */
        const val BOOK_CREASE_X = 5

        /** `.v2{background:repeating-linear-gradient(… 0 3px,transparent 3px 6px)}`. */
        const val DASH_LENGTH = 3

        /** `.tf{gap:14px}`, `.tf .lbl{margin-top:9px}`, `.empty{gap:16px;padding:36px 40px 140px}`. */
        const val TRANSFORM_GAP = 14
        const val LABEL_MARGIN_TOP = 9
        const val EMPTY_GAP = 16
        const val EMPTY_PADDING_SIDE = 40

        /** Narrower than the frozen 28ch measure, so the side padding becomes the binding constraint. */
        const val NARROW_HOST = 200

        /** `#7C8A3F` — `ZinelyMakerInkId.Matcha`, a content ink and therefore theme-invariant. */
        val BOOK_FILL = Color(0xFF7C8A3F)

        /** `.book-ill::after` stripes. */
        val FORE_EDGE_LIGHT = Color(0xFFF1EBDA)
        val FORE_EDGE_DARK = Color(0xFFE3D9C2)

        /**
         * `--paper` in light, which is the stock the sheet's rules are read against, and `--matcha` in
         * dark, which is what the book would be if it were drawn from chrome.
         *
         * Both are cross-checked against the live theme by their own test rather than trusted. A
         * hard-coded copy of a token is exactly how B3's tofu control became a false fact: it was right
         * when it was written, and nothing was watching it afterwards.
         */
        val LIGHT_PAPER = Color(0xFFF7F2E7)
        val DARK_CHROME_MATCHA = Color(0xFF93A257)

        const val HALF_PIXEL = 0.5f

        val PROBE_GROUND = Color(0xFF00FF00)

        /** The width the production headline is given: the host less the frozen 40px sides. */
        const val HEADLINE_MEASURE = 400

        /** The headline at the frozen tracking and at a loose one, both at that width. */
        const val REF_TRACK_FROZEN = "ref-track-frozen"
        const val REF_TRACK_LOOSE = "ref-track-loose"

        /** Reference renderings of the headline, for the type assertion to measure against. */
        const val REF_FROZEN = "ref-frozen"
        const val REF_HEAVY = "ref-weight-600"
        const val REF_SMALL = "ref-16sp"
        const val REF_MEASURE = "ref-28ch"

        /** `.tf .arrow{margin-bottom:18px}`. */
        const val ARROW_MARGIN_BOTTOM = 18

        /** 28ch at `.pv`'s own type, which is a different number from 28ch at the paragraph's. */
        const val REF_MEASURE_PV = "ref-28ch-pv"

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
        // transformation"*. There is no carousel, no sample zine and no tour, and the three lines are the
        // whole of the copy. Asserted by text so a re-worded line is a failure rather than a silent
        // product change.
        composeRule.onNodeWithText(HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithText(BODY).assertIsDisplayed()
        composeRule.onNodeWithText(PRIVACY).assertIsDisplayed()
        composeRule.onNodeWithText(ARROW).assertIsDisplayed()
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
    fun `the headline is the voice face at the frozen size and D-005's weight`() {
        // The technique [ZineShelfTest] established for `.shelf-head h1`, applied to the second selector
        // the **D-005** ruling names: `.empty h2`'s frozen `font-weight:600` is stale, and it renders at
        // Fraunces 500. Ink coverage rather than bounds, because a centred `Text` in a fixed-width column
        // measures the same at any weight.
        emptyWithReferences()
        val raster = decorRaster()
        val threshold = (PROBE_GROUND.luminance() + capturedInk.luminance()) / 2f

        val frozen = raster.inkCoverage(bounds(REF_FROZEN, byTag = true), threshold)
        val heavier = raster.inkCoverage(bounds(REF_HEAVY, byTag = true), threshold)
        val smaller = raster.inkCoverage(bounds(REF_SMALL, byTag = true), threshold)
        // Found by its role, not its words: this host renders the same sentence three more times as
        // yardsticks, and `<h2>` is the one thing only the real headline carries.
        val headline = raster.inkCoverage(headlineBounds(), threshold)

        // Discrimination asserted before parity — the rule this programme adopted after three packages
        // shipped assertions blind to the defect class their names claimed to gate.
        assertTrue("nothing was drawn at the frozen style (coverage $frozen)", frozen > 0)
        assertTrue(
            "this host cannot tell Fraunces 500 from the file's stale 600 ($frozen vs $heavier), " +
                "so D-005 is unguarded here",
            relativeGap(frozen, heavier) > 0.03f,
        )
        assertTrue(
            "this host cannot tell the frozen 27.52sp from body 16sp ($frozen vs $smaller)",
            relativeGap(frozen, smaller) > 0.03f,
        )
        assertTrue(
            "the headline must render at Fraunces 500 / 27.52sp — coverage $headline against $frozen " +
                "frozen, $heavier at weight 600, $smaller at 16sp",
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
            "the paragraph must not exceed 28ch (${paragraph.width} against $zeros)",
            paragraph.width <= zeros + HALF_PIXEL,
        )
        // And it must actually be *bound* by that measure rather than merely narrower than it — a
        // paragraph that fits on one line would pass the assertion above with no constraint at all.
        assertTrue(
            "the paragraph must be wide enough for the measure to bind (${paragraph.width})",
            paragraph.width > zeros * 0.8f,
        )
    }

    @Test
    fun `the privacy line is measured in its own characters, not the paragraph's`() {
        emptyWithMeasure()
        val privacy = productionPrivacyBounds()
        val ownMeasure = bounds(REF_MEASURE_PV, byTag = true).width
        val paragraphMeasure = bounds(REF_MEASURE, byTag = true).width

        // `.pv` is a `<p>` inside `.empty`, so `max-width:28ch` applies to it too — and `ch` is relative to
        // the element's **own** font size, so 28 advances at 12.16sp SemiBold is far narrower than 28 at
        // 15.2sp Regular. Both halves of that were missed on the first pass, and neither was visible in a
        // bound: the line simply ran to the column's edge and looked deliberate. The raster is what showed
        // it, which is the one thing a raster is better at than a number.
        //
        // Measured against **its own** yardstick, not against the paragraph's. A first draft compared the
        // two paragraphs' rendered widths and expected the ratio of their font sizes; it failed at 0.889
        // against 0.8, for two reasons that are both real — a rendered width is where the lines happened to
        // break rather than the constraint, and SemiBold's zero is wider than Regular's, so `ch` does not
        // scale with size alone. The rendered width was never the right instrument.
        assertTrue(
            "the privacy line must not exceed its own 28ch ($privacy.width against $ownMeasure)",
            privacy.width <= ownMeasure + HALF_PIXEL,
        )
        assertTrue(
            "and the measure must actually bind it (${privacy.width} against $ownMeasure)",
            privacy.width > ownMeasure * 0.8f,
        )
        // The discrimination this rests on: the two measures are genuinely different numbers. If they were
        // not, applying the paragraph's measure to this line would pass the assertions above.
        assertTrue(
            "28ch at 12.16sp SemiBold ($ownMeasure) must differ from 28ch at 15.2sp ($paragraphMeasure), " +
                "or this test cannot see the line being measured in the wrong type",
            ownMeasure < paragraphMeasure * 0.95f,
        )
    }

    @Test
    fun `the privacy line is leaded at 1_55, which it inherits rather than declares`() {
        emptyWithMeasure()
        val privacy = productionPrivacyBounds()
        val leaded = bounds(REF_PV_LEADED, byTag = true)
        val unleaded = bounds(REF_PV_UNLEADED, byTag = true)

        // `.empty p{line-height:1.55}` reaches `.pv` too — `.pv` overrides size, colour, weight and margin
        // and nothing else. Found by mutation: dropping the leading survived the entire suite, because
        // every other assertion about this line is about its *width*. That is the "which frozen properties
        // have no test at all" question answering itself.
        //
        // The discrimination is asserted first: if this host renders the two references at the same height,
        // nothing below can see the defect.
        assertNotEquals(
            "leaded and unleaded must differ on this host, or this test guards nothing " +
                "(${leaded.height} vs ${unleaded.height})",
            leaded.height,
            unleaded.height,
        )
        assertEquals(
            "the privacy line must carry the inherited 1.55 leading",
            leaded.height,
            privacy.height,
            HALF_PIXEL,
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
        val full = SHEET_H - 2 * RULE_INSET

        // Three folds and a cut. Moving the dash to `.v1` or `.v3` draws a different set of folding
        // instructions for the same eight-page sheet, and looks entirely plausible — the illustration is
        // 92px wide and nobody counts. So the *position* is asserted, not the existence of a dash.
        assertTrue("the first fold must be a solid rule ($first of $full rows)", first >= full - 2)
        assertTrue("the third fold must be a solid rule ($third of $full rows)", third >= full - 2)
        assertTrue(
            "the middle rule must be dashed — 3px on, 3px off, so about half its run ($second of $full)",
            second in (full / 4)..(full * 3 / 4),
        )

        // A duty cycle alone does not describe a dash: `3px on / 3px off` and `8px on / 8px off` both paint
        // half the run, and the assertion above accepts either. The *period* is what the frozen repeating
        // gradient states, so it is counted rather than inferred — a 54px run at a 6px period alternates
        // nine times, and at a 16px period three or four.
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
        val y = (sheet.top.roundToInt() + RULE_INSET until sheet.bottom.roundToInt() - RULE_INSET)
            .first { raster.isPainted((sheet.left + SECOND_FOLD * SHEET_W).roundToInt(), it) }

        // `.v2{opacity:.5}` on `--ink-faint`. `isPainted` only asks "not the paper", so the alpha was
        // invisible to every assertion in this file — at 1.0 the cut line becomes the darkest mark on the
        // sheet, which inverts the drawing's own hierarchy: the cut would read as the strongest instruction
        // on a diagram about folding. Asserted against the token itself, which is what full strength *is*.
        val x = (sheet.left + SECOND_FOLD * SHEET_W).roundToInt()
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
        val x = (sheet.left + FIRST_FOLD * SHEET_W).roundToInt()

        // `top:6px;bottom:6px`. A rule run edge to edge reads as a printed line rather than a fold guide,
        // and the difference is six pixels at each end.
        assertFalse(
            "the rule must not reach the sheet's top edge",
            raster.isPainted(x, sheet.top.roundToInt() + RULE_INSET - 3),
        )
        assertTrue(
            "the rule must begin ${RULE_INSET}px down",
            raster.isPainted(x, sheet.top.roundToInt() + RULE_INSET + 1),
        )
        assertFalse(
            "and must not reach the bottom edge",
            raster.isPainted(x, sheet.bottom.roundToInt() - RULE_INSET + 3),
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
        val mid = (sheet.top + SECOND_FOLD * SHEET_H).roundToInt()

        assertTrue("the horizontal rule must cross at half height", raster.isPainted(x, mid))
        assertFalse("and nowhere else on this column", raster.isPainted(x, mid - 8))
        assertFalse("nor below it", raster.isPainted(x, mid + 8))
    }

    // ---------------------------------------------------------------------------------------------
    // The little book — `.book-ill`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the book is the frozen size, and its fore-edge hangs outside that`() {
        empty()
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()

        assertEquals("the book must be ${BOOK_W}px wide", BOOK_W.toFloat(), book.width, HALF_PIXEL)
        assertEquals("and ${BOOK_H}px tall", BOOK_H.toFloat(), book.height, HALF_PIXEL)

        // `right:-3px` puts the stacked leaves *beyond* the element, so the layout stays 52px wide and the
        // book stays optically centred under its caption. Drawn inside the bounds instead — the reading a
        // clip would force — it would shift the book and swallow three pixels of its own cover.
        val y = book.center.y.roundToInt()
        val outside = raster.colourAt(book.right.roundToInt() + 1, y)
        assertTrue(
            "the fore-edge must be painted outside the book's own bounds (found $outside)",
            outside.closeTo(FORE_EDGE_LIGHT) || outside.closeTo(FORE_EDGE_DARK),
        )
        // Beyond the sliver is **not** clean ground: the book's own `0 10px 18px -10px` shadow tints the
        // region, which is B3's lesson that a probe outside an object must assert *not the expected ink*
        // rather than *bare ground*. A first draft demanded the ground here and failed on the shadow.
        val beyond = raster.colourAt(book.right.roundToInt() + FORE_EDGE_W + 2, y)
        assertFalse(
            "the fore-edge must stop after ${FORE_EDGE_W}px (found $beyond still on a stripe)",
            beyond.closeTo(FORE_EDGE_LIGHT) || beyond.closeTo(FORE_EDGE_DARK),
        )
    }

    @Test
    fun `the book is printed in the matcha maker ink`() {
        empty(dark = false)
        val fill = bookFillPixel()
        assertTrue("the book must print in $BOOK_FILL (found $fill)", fill.closeTo(BOOK_FILL))
        // The literal is checked against the palette rather than trusted, so a change to the ink table
        // cannot leave this file quietly asserting a colour the product no longer uses.
        assertEquals("the constant above must still be the maker ink", capturedMatchaInk, BOOK_FILL)
    }

    @Test
    fun `the book keeps its ink at night`() {
        // `#7C8A3F` is `ZinelyMakerInkId.Matcha`, a **content** ink and not a chrome token — and content
        // inks do not re-tint at night, because a printed object does not. Drawn from `v2Colors.matcha`
        // instead the book would be #5E6B2F in light and #93A257 in dark, both plausible and both wrong.
        // A separate test rather than a two-theme comparison inside one: the Compose rule accepts a single
        // `setContent`, so the two readings are two tests pinned to the same constant.
        empty(dark = true)
        val fill = bookFillPixel()
        assertTrue("the book must still print in $BOOK_FILL at night (found $fill)", fill.closeTo(BOOK_FILL))
        assertNotEquals(
            "and must not be the chrome matcha, which is what would change",
            DARK_CHROME_MATCHA,
            fill,
        )
    }

    @Test
    fun `the two theme literals this file pins are still the tokens they name`() {
        // `LIGHT_PAPER` is the basis of every `isPainted` call in this file, and `DARK_CHROME_MATCHA` is
        // the whole content of the claim that the book does not follow the theme. Both were written as
        // hard-coded copies — right on the day, and unwatched after it. If a token moves, this fails here
        // rather than turning a dozen other assertions quietly inert.
        empty(dark = false)
        assertEquals("LIGHT_PAPER must still be --paper in light", capturedPaper, LIGHT_PAPER)
    }

    @Test
    fun `the dark chrome matcha this file rules out is still the token it names`() {
        empty(dark = true)
        assertEquals("DARK_CHROME_MATCHA must still be --matcha in dark", capturedChromeMatcha, DARK_CHROME_MATCHA)
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
            crease.closeTo(BOOK_FILL),
        )
        assertTrue(
            "and it is a white highlight, so it must be lighter than the fill, not darker",
            crease.luminance() > BOOK_FILL.luminance(),
        )
        // Discrimination: the fill either side must be untouched, or "lighter than the fill" would be
        // satisfied by a book drawn in the wrong colour altogether.
        assertTrue(
            "the fill must be plain two pixels further in",
            raster.colourAt(book.left.roundToInt() + BOOK_CREASE_X + 2, y).closeTo(BOOK_FILL),
        )
        assertTrue(
            "and plain two pixels outside it",
            raster.colourAt(book.left.roundToInt() + BOOK_CREASE_X - 2, y).closeTo(BOOK_FILL),
        )
    }

    @Test
    fun `the fore-edge is inset from the book's top and bottom and stacks two stripe colours`() {
        empty()
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        val x = book.right.roundToInt() + 1

        // `::after{top:4px;bottom:4px}` — the leaves stop short of the cover at both ends, which is what
        // makes them read as pages inside a binding rather than as a stripe down the whole edge.
        assertFalse(
            "the fore-edge must not reach the book's top edge",
            raster.colourAt(x, book.top.roundToInt() + 1).isForeEdge(),
        )
        assertTrue(
            "it must begin ${FORE_EDGE_INSET}px down",
            raster.colourAt(x, book.top.roundToInt() + FORE_EDGE_INSET + 1).isForeEdge(),
        )
        assertFalse(
            "and must stop ${FORE_EDGE_INSET}px above the bottom",
            raster.colourAt(x, book.bottom.roundToInt() - 1).isForeEdge(),
        )

        // `repeating-linear-gradient(90deg,#F1EBDA,#F1EBDA 1px,#E3D9C2 1px,#E3D9C2 2px)` — two colours, not
        // one. The existing size assertion reads `light OR dark`, so a fore-edge painted entirely in either
        // colour passes it; stacked leaves that do not alternate are a flat tab.
        val y = book.center.y.roundToInt()
        val stripes = (0 until FORE_EDGE_W).map { raster.colourAt(book.right.roundToInt() + it, y) }
        assertTrue(
            "the leaves must include the light stripe (found $stripes)",
            stripes.any { it.closeTo(FORE_EDGE_LIGHT) },
        )
        assertTrue(
            "and the dark one, or they are not stacked leaves at all (found $stripes)",
            stripes.any { it.closeTo(FORE_EDGE_DARK) },
        )
    }

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
        val bottom = bounds(PRIVACY)
        val contentCentre = (top.top + bottom.bottom) / 2f

        // `padding:36px 40px 140px` against `justify-content:center` centres the content in the space
        // *above* the dock. Trim the 140 and the copy sits under the button; the frozen number is five
        // times the others precisely because it is not padding, it is clearance.
        assertTrue(
            "the content must sit above the host's own centre ($contentCentre against ${host.center.y})",
            contentCentre < host.center.y,
        )
        // Specifically, by half the difference between the top and bottom paddings: (140 − 36) / 2 = 52.
        assertEquals(
            "the content must be lifted by half the padding difference",
            52f,
            host.center.y - contentCentre,
            2f,
        )
    }

    @Test
    fun `the frozen gaps hold the row, the column and the captions apart`() {
        empty()
        val sheet = bounds(ZineSheetIllustrationTestTag, byTag = true)
        val arrow = bounds(ARROW)
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
        // `.empty{padding:36px 40px 140px}` — the sides are unobservable at any width the 28ch measure fits
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
            "the host must be narrower than the 28ch measure, or the padding never binds",
            paragraph.width <= NARROW_HOST - 2 * EMPTY_PADDING_SIDE + HALF_PIXEL,
        )
    }

    @Test
    fun `the arrow rides half its own margin above the row, not all of it`() {
        empty()
        val arrow = bounds(ARROW)
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val caption = bounds(BOOK_CAPTION)

        // **The row's centre is the taller column's centre**, and that is the book's: 68 + 9 + caption
        // against the sheet's 66 + 9 + caption. `align-items:center` centres every child's *margin box* on
        // that line, so the tallest column spans the row and its own centre is the row's. Taking the
        // union of the two illustrations instead — a first draft did — measures a line neither element is
        // centred on, and reported the arrow 2.5px on the wrong side of it.
        val rowCentre = (book.top + caption.bottom) / 2f

        // `.arrow{margin-bottom:18px}` lifts the glyph by **half** the margin, because it is the box that
        // is centred and the glyph sits at its top. `Modifier.offset(y = -18.dp)` would move it the full
        // 18 and be wrong by exactly a factor of two — the sort of miss a screenshot ratifies. Compose
        // reports a padded `Text`'s semantics bounds *inside* its padding, which is why this is asserted
        // as a displacement from the row rather than as a taller box.
        assertEquals(
            "the arrow must ride ${ARROW_MARGIN_BOTTOM / 2}px above the row's centre line",
            rowCentre - ARROW_MARGIN_BOTTOM / 2f,
            arrow.center.y,
            1.5f,
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
                    Reference(REF_FROZEN, FontWeight.Medium, 27.52.sp)
                    Reference(REF_HEAVY, FontWeight.SemiBold, 27.52.sp)
                    Reference(REF_SMALL, FontWeight.Medium, 16.sp)
                }
                // The tracking pair, held to the **same width the production headline gets** — the host
                // less the frozen 40px sides. Tracking only reaches the layout through wrapping, so a
                // reference measured unconstrained would render one line at any tracking and prove nothing.
                Column(Modifier.align(Alignment.TopEnd).width(HEADLINE_MEASURE.dp)) {
                    Reference(REF_TRACK_FROZEN, FontWeight.Medium, 27.52.sp, (-0.01).em)
                    Reference(REF_TRACK_LOOSE, FontWeight.Medium, 27.52.sp, 0.05.em)
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
                    text = "0".repeat(28),
                    style = TextStyle(
                        fontFamily = ZinelyTheme.v2Typography.work,
                        fontSize = 15.2.sp,
                        lineHeight = 1.55.em,
                    ),
                    softWrap = false,
                    modifier = Modifier.testTag(REF_MEASURE).align(Alignment.TopStart),
                )
                // The same count at `.pv`'s own type — 28ch is relative to the element, not to the rule.
                val pvStyle = TextStyle(
                    fontFamily = ZinelyTheme.v2Typography.work,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.16.sp,
                    lineHeight = 1.55.em,
                )
                Text(
                    text = "0".repeat(28),
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
                    measurer.measure("0".repeat(28), pvStyle, softWrap = false).size.width.toDp()
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
        tracking: TextUnit = (-0.01).em,
    ) {
        Text(
            text = HEADLINE,
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.voice,
                fontWeight = weight,
                fontSize = size,
                letterSpacing = tracking,
                color = ZinelyTheme.v2Colors.ink,
            ),
            modifier = Modifier.testTag(tag),
        )
    }

    private var capturedInk: Color = Color.Unspecified
    private var capturedMatchaInk: Color = Color.Unspecified
    private var capturedPaper: Color = Color.Unspecified
    private var capturedChromeMatcha: Color = Color.Unspecified
    private var capturedInkFaint: Color = Color.Unspecified

    @Composable
    private fun Host(dark: Boolean = false, content: @Composable BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = dark) {
            capturedInk = ZinelyTheme.v2Colors.ink
            capturedMatchaInk = ZinelyTheme.contentInks[ZinelyMakerInkId.Matcha].value
            capturedPaper = ZinelyTheme.v2Colors.paper
            capturedChromeMatcha = ZinelyTheme.v2Colors.matcha
            capturedInkFaint = ZinelyTheme.v2Colors.inkFaint
            Box(Modifier.fillMaxSize().background(PROBE_GROUND)) { content() }
        }
    }

    /** A pixel of the book's own fill, clear of its crease at x=5 and of the fore-edge at the far side. */
    private fun bookFillPixel(): Color {
        val book = bounds(ZineBookIllustrationTestTag, byTag = true)
        val raster = decorRaster()
        return raster.colourAt(book.center.x.roundToInt(), book.center.y.roundToInt())
    }

    /** Whether a pixel is one of the fore-edge's two stripe colours. */
    private fun Color.isForeEdge(): Boolean = closeTo(FORE_EDGE_LIGHT) || closeTo(FORE_EDGE_DARK)

    /**
     * How many painted runs the rule at [fraction] breaks into — the dash's *period*, not its duty cycle.
     *
     * A 3px-on/3px-off rule over a 54px run alternates nine times; an 8px-on/8px-off rule paints the same
     * fraction of the column and alternates three. The duty-cycle assertion cannot tell them apart.
     */
    private fun Bitmap.paintedRunsAlong(sheet: Rect, fraction: Float): Int {
        val x = (sheet.left + fraction * SHEET_W).roundToInt()
        var runs = 0
        var wasPainted = false
        for (y in sheet.top.roundToInt() + RULE_INSET until sheet.bottom.roundToInt() - RULE_INSET) {
            val painted = isPainted(x, y)
            if (painted && !wasPainted) runs++
            wasPainted = painted
        }
        return runs
    }

    /** How many rows of the rule at [fraction] across the sheet are painted at all. */
    private fun Bitmap.paintedRowsAlong(sheet: Rect, fraction: Float): Int {
        val x = (sheet.left + fraction * SHEET_W).roundToInt()
        return (sheet.top.roundToInt() + RULE_INSET until sheet.bottom.roundToInt() - RULE_INSET)
            .count { isPainted(x, it) }
    }

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
