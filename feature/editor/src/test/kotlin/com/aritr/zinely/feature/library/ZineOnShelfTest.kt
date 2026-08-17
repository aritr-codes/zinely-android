package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.ui.a11y.PlatformA11yNode
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.ZinelyV2Dimens
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * `.zine` — what the two gestures do, what a press looks like, and what the platform hears.
 *
 * **Every assertion here is a behaviour or a placed bound.** The gestures are driven through the pointer
 * API rather than by calling the callbacks, the press is measured off the transformed node rather than read
 * off the source, and the roles are read out of the **platform** `AccessibilityNodeInfo` tree rather than
 * Compose's merged one — the split ADR-058 records, and the reason B2's own a11y claim needed two tests.
 *
 * `w480dp` gives the object the 216dp column width the frozen shelf produces, so the numbers here are the
 * numbers a real cell sees.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineOnShelfTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val INDEX = 3
        const val TITLE = "Sunday market"
        const val SUBTITLE = "A4 · 2 days ago"

        /**
         * `.cover:active{transform:translate(2px,2px) rotate(0deg)}` — and **the press moved objects**.
         *
         * V2 wrote `.zine:active{transform:translateY(2px) scale(.985)}`: the whole item dropped and
         * shrank. V2.1 presses the **cover** and not the item, travels on both axes, and drops the tilt
         * instead of scaling. So there is no scale factor to re-baseline — the claim it encoded is gone,
         * and a `PRESS_SCALE` kept at 1.0 would have read as a measurement.
         */
        const val PRESS_TRAVEL = 2f

        /**
         * Long enough for `transition:transform .16s` to finish.
         *
         * The press animates on **settle** (D-011), so a bound read immediately after `down()` catches the
         * cover mid-travel and measures whatever fraction the frame landed on. V2's press was read
         * straight off `waitForIdle`, which worked only because that implementation cut rather than
         * animated — the defect a review caught in the cover itself.
         */
        const val SETTLE_MILLIS = 400L

        /**
         * `.start:focus-visible{outline:2px solid var(--ink);outline-offset:5px}`.
         *
         * `.zine` declares no focus rule in V2.1, so the implementation borrows the file's own sibling
         * control rather than inventing one (D-008). V2's ring was `--matcha-text` at a 6px offset with a
         * 9px radius; all three moved, and the radius went to **0** because `.zine` has no border-radius
         * for an outline to follow — the cover inside it does.
         */
        const val RING_OFFSET = 5
        const val RING_WIDTH = 2

        /** The two columns the 2px stroke actually lands on: it centres at `offset + width/2` = 6px out. */
        const val RING_INNER = 6
        const val RING_OUTER = 7

        /**
         * The left edge is read at the cover's **centre row**, and the bottom edge at its centre column,
         * for one reason each probe shares: the tilt rotates about the cover's centre, so displacement
         * along each axis vanishes there — and the press *removes* the tilt, so anywhere else the scan
         * measures `translate(2px,2px)` **plus** however far un-tilting moved that particular edge.
         *
         * A fixed 100px down the cover used to serve. It survived only by accident of the old column
         * width: at 216dp the cover is 288 tall, the probe stood 44px above centre, and un-tilting a
         * −1.4° object walks its left edge `44 × tan 1.4° ≈ 1.07px` — so a 2px travel measured 3.0 and
         * the test failed with an expectation that was right and an instrument that was not.
         *
         * [BOTTOM_SCAN_FROM] is where the vertical scan *starts* looking, as an offset from the item's
         * top. The top border cannot be read at all: `.tape` is `top:-11px`, 56px wide and painted over
         * the cover's own edge, so at every column it covers there is no border pixel to find — two
         * earlier probe columns proved it, one by saturating at the scan's first row (a travel of
         * exactly 0.0) and one by running clean past the top border to the bottom one, at the cover's
         * full height.
         */
        const val BOTTOM_SCAN_FROM = 200

        /** One pixel, for the two anchors an antialiased edge on a tilted object can only fix to that. */
        const val ONE_PIXEL = 1f

        /** `.cover{border:1.5px solid var(--ink)}`, painted inside the box. */
        const val COVER_BORDER = 1.5f

        /** A ground no token carries, so "something was painted here" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)

        /** `.more{width:34px;height:34px;bottom:8px;right:8px}` */
        const val MORE_SIZE = 34
        const val MORE_INSET = 8

        /**
         * The column width `w480dp` produces on the frozen shelf: `(480 − 16 − 16 − 16) / 2`.
         *
         * This read **208** until a review caught it — V2's `padding:0 22px` and `gap:… 20px`, which the
         * re-freeze moved to `--gap-lg` (16) on both (`v21-library.html:149-150`, and `ZineShelfTest`
         * derives the same 216 independently). Every absolute geometry in this file was therefore
         * measured on a cell 8dp narrower than the product ever renders, under a class KDoc that called
         * it "the numbers a real cell sees".
         */
        val COLUMN = 216.dp

        /** Half a pixel, for the reason `ZineShelfTest.HALF_PIXEL` states: `assertEquals` passes at `|Δ| == delta`. */
        const val HALF_PIXEL = 0.5f

        /** The cap ADR-100 set on `.name`, which declares none in the frozen file. */
        const val NameMaxLines = 2

        /** Long enough to reach a third line in a 216dp column, which every frozen title is far from. */
        const val LONG_TITLE = "Notes from the Sunday market, volume three"

        val ITEM = ZineShelfItem(
            title = TITLE,
            recipe = ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
            subtitle = SUBTITLE,
        )
    }

    private val opened = mutableListOf<Int>()
    private val actioned = mutableListOf<Int>()

    // ---------------------------------------------------------------------------------------------
    // The two ways in, and the third — `:199`: "tap = open zine · long-press = actions · ... = actions"
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a tap opens the zine and asks for nothing else`() {
        item()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performClick()

        assertEquals("a tap must open exactly this object", listOf(INDEX), opened)
        assertEquals("and must not open the actions", emptyList<Int>(), actioned)
    }

    @Test
    fun `a long press asks for the actions, and does not also open the zine`() {
        item()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { longClick() }

        // Both halves matter. A `combinedClickable` whose `onLongClick` is null delivers a long press as a
        // *click*, so a test that only checked `actioned` would pass while the shelf opened the zine.
        assertEquals("a long press must ask for the actions", listOf(INDEX), actioned)
        assertEquals("and must not also open it", emptyList<Int>(), opened)
    }

    @Test
    fun `the ⋯ asks for the same actions without opening the zine`() {
        item()
        composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).performClick()

        assertEquals("the fallback must reach the same place as the gesture", listOf(INDEX), actioned)
        assertEquals("and must not open the zine underneath it", emptyList<Int>(), opened)
    }

    @Test
    fun `the ⋯ is drawn at rest, not revealed by hovering or by discovering the gesture`() {
        item()
        // `.more{display:flex;opacity:.5}` unconditionally — no hover gate anywhere in the frozen rule
        // (`:73-77`). It is *"the fallback; long-press is the accelerator"* (`:72`), and a fallback that
        // appears only after you have found the accelerator is not one.
        composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------------------------
    // What a press looks like — `.zine:active`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a held cover settles two pixels down and two across, and the item does not move`() {
        item()
        val restItem = coverBounds()
        val (restBottom, restLeft) = printedCoverEdges()

        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.waitForIdle()
        val (heldBottom, heldLeft) = printedCoverEdges()

        // **Two axes, and the second one is the whole difference from V2.** V2 wrote
        // `translateY(2px) scale(.985)`, so this test measured a drop and a shrink. V2.1 writes
        // `translate(2px,2px)` — toward the hard shadow, which collapses to meet it — and no scale at
        // all. Asserting the drop alone would be satisfied by V2's transform; asserting a scale would be
        // asserting a value the corpus deleted.
        assertEquals("the cover must travel ${PRESS_TRAVEL}px down", PRESS_TRAVEL, heldBottom - restBottom, HALF_PIXEL)
        // The horizontal axis carries `ONE_PIXEL` where the vertical carries half of one, and the reason
        // is the instrument rather than the claim: the bottom edge is found by scanning *rows* of a
        // border painted along a row, which quantises cleanly, while the left edge is found by scanning
        // *columns* across an antialiased near-vertical border that the un-tilt also re-samples. Two
        // independent ±0.5 roundings live in `heldLeft - restLeft`. A travel of zero still fails, which
        // is what this assertion exists to catch.
        assertEquals("and ${PRESS_TRAVEL}px across", PRESS_TRAVEL, heldLeft - restLeft, ONE_PIXEL)
        // `.zine:active .cover` — the press is on the printed object, not on the item that holds it. The
        // caption below the cover must not move with it, which is what pins the selector's scope.
        assertEquals("the item itself must not move", restItem.top, coverBounds().top, HALF_PIXEL)
        assertEquals("nor change size", restItem.width, coverBounds().width, HALF_PIXEL)
    }

    @Test
    fun `a cover at rest carries no travel at all`() {
        // The control for the test above: without it, a permanently-offset cover would satisfy every
        // relative measurement there.
        item()
        val item = coverBounds()
        assertEquals("an untouched item is the full column wide", COLUMN.value, item.width, HALF_PIXEL)
        val (bottom, left) = printedCoverEdges()
        // Both anchors, and they are also the guard on the two scans the travel test reads: an edge that
        // is not where the untouched item puts it means the scan has found the tape, the shadow or the
        // caption, and a travel measured through it would be measuring that instead.
        assertEquals("its cover starts at the item's own left edge", item.left, left, ONE_PIXEL)
        // The scan finds the **first** row of the border, and `border:1.5px` is painted inside the box, so
        // the edge it reports sits that much above the cover's own bottom. Stated as a term rather than
        // absorbed into a wider tolerance: a two-pixel tolerance here would also accept the press.
        assertEquals(
            "and ends `aspect-ratio:3/4` below the item's top, less its own border",
            item.top + item.width * 4f / 3f - COVER_BORDER,
            bottom,
            ONE_PIXEL,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // The focus ring — `.zine:focus-visible`
    // ---------------------------------------------------------------------------------------------

    // **These three tests exist because independent review proved there were none.** Offset 6dp -> 60dp and
    // radius 9dp -> 0dp both survived the entire library suite, goldens included: the ring is painted
    // *outside* the node's bounds, so no bound can see it, and no raster in this package captures a focused
    // state. It is read off the pixels or it is not read at all.
    //
    // Two things had to be measured rather than assumed, and both were wrong on the first attempt. The ring
    // is requested through `InputMode.Keyboard`, because Compose refuses focus in touch mode and
    // `requestFocus()` silently did nothing — which is also the honest transcription, since `:focus-visible`
    // *is* keyboard focus. And the ground outside a cover is **not** clean: B1's grounded shadow tints it for
    // ten pixels out, so "no ring here" is asserted as *not the ring's ink* rather than as bare ground.

    @Test
    fun `a focused cover is ringed, clear of its own edge, with a rounded corner`() {
        item()
        focusTheCover()

        val raster = decorRaster()
        val cover = coverBounds()
        val left = cover.left.roundToInt()
        val midY = cover.center.y.roundToInt()

        // A CSS outline is drawn outside the box: its inner edge sits `outline-offset` out and its 2px
        // thickness grows outward from there. At a 5px offset the stroke centres 6px out, so it lands on
        // the two columns 6 and 7 — where V2's 6px offset put it on 7 and 8.
        for (out in RING_INNER..RING_OUTER) {
            assertTrue(
                "the ring must be painted ${out}px out from the item's left edge, in ink " +
                    "(found ${raster.colourAt(left - out, midY)})",
                raster.colourAt(left - out, midY).closeTo(capturedRingInk),
            )
        }
        // The `outline-offset` gap must hold no ring, or a ring drawn flush against the edge — or a 2px
        // border eating into the layout — would satisfy the loop above.
        assertFalse(
            "the ${RING_OFFSET}px offset must be clear of the ring",
            raster.colourAt(left - 3, midY).closeTo(capturedRingInk),
        )
        assertFalse(
            "and there must be no second ring beyond it",
            raster.colourAt(left - (RING_OUTER + 4), midY).closeTo(capturedRingInk),
        )

        // **The corner, which a straight edge cannot see — and it is now square rather than round.** V2's
        // ring carried `border-radius:9px`, and this probe asserted the arc had *cut* the corner away.
        // V2.1's `.zine` has no border-radius for an outline to follow, so the ring's own corner is a
        // right angle and the pixel at the corner of its rect is painted. The probe is kept, inverted,
        // rather than deleted: it is still the only assertion here that a wrong radius could fail.
        assertTrue(
            "the ring's corner must be square — the pixel at its own corner, ${RING_INNER}px out on " +
                "both axes, must carry the stroke",
            raster.anyPixelNear(left - RING_INNER, cover.top.roundToInt() - RING_INNER, capturedRingInk),
        )
    }

    @Test
    fun `an unfocused cover has no ring at all`() {
        // The control: without it, a ring painted unconditionally would satisfy every assertion above while
        // showing on all six covers of a shelf at once.
        item()
        val raster = decorRaster()
        val cover = coverBounds()
        val midY = cover.center.y.roundToInt()
        for (out in RING_INNER..RING_OUTER) {
            assertFalse(
                "nothing may be painted ${out}px outside an unfocused cover",
                raster.colourAt(cover.left.roundToInt() - out, midY).closeTo(capturedRingInk),
            )
        }
    }

    @Test
    fun `the ring does not move when the cover is pressed`() {
        // The ring is drawn on the **item**, and the press is on the cover inside it, so the indicator
        // cannot be dragged by the transform. Asserted rather than assumed: a version that put the ring
        // on the cover composes perfectly, reads as deliberate, and slides the focus indicator two pixels
        // every time the object is touched.
        item()
        focusTheCover()
        val rest = coverBounds()
        val (_, restLeft) = printedCoverEdges()

        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.waitForIdle()

        assertTrue(
            "the cover must actually have travelled, or this test proves nothing",
            printedCoverEdges().second > restLeft,
        )
        assertTrue(
            "but the ring must still stand where it did, measured from the item's resting edge",
            decorRaster()
                .colourAt(rest.left.roundToInt() - RING_INNER, rest.center.y.roundToInt())
                .closeTo(capturedRingInk),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // D-009 — the target grows, the mark does not
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the ⋯ mark stays 34dp while what a finger hits reaches the platform floor`() {
        // **Only the first half of this name discriminates, and saying so is the point.** Independent review
        // removed `minimumInteractiveComponentSize()` from the seam and this test stayed green: Compose's
        // own pointer-input minimum already lifts `touchBoundsInRoot` to 48dp regardless. The seam's floor is
        // guarded where it lives, by `ZinelyV2ControlPlatformA11yTest`; what is guarded *here* is that
        // satisfying D-009 did not grow the printed mark, which is what D-009's ruling actually forbids.
        item()
        val node = composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX)).fetchSemanticsNode()
        val drawn = node.boundsInRoot
        val touch = node.touchBoundsInRoot
        val floor = ZinelyV2Dimens.MinTouchTarget.value

        // The D-009 ruling is two claims, and a test that checked only the floor would pass a control that
        // met it by being drawn bigger — *"do not modify the visual design solely to satisfy these
        // requirements"*. So the mark is pinned at its frozen size first.
        assertEquals("the drawn mark is ${MORE_SIZE}dp wide", MORE_SIZE.toFloat(), drawn.width, HALF_PIXEL)
        assertEquals("and ${MORE_SIZE}dp tall", MORE_SIZE.toFloat(), drawn.height, HALF_PIXEL)
        assertTrue(
            "the touch target must reach ${floor}dp (found ${touch.width} × ${touch.height})",
            touch.width >= floor - HALF_PIXEL && touch.height >= floor - HALF_PIXEL,
        )
    }

    @Test
    fun `the overflow mark is three stacked dots that carry their own contrast`() {
        // **ADR-100's ruling, and the defect it answers, both stated as pixels.**
        //
        // The mark was a `⋯` glyph at `ink.copy(alpha = .5f)`, which composites to `#978875` on the desk
        // — **2.90:1**, failing 4.5:1 as text and 3:1 as a control indicator in both themes, in the only
        // state a user sees before touching it. It is now full `ink-soft`: 5.54 light, 7.49 dark.
        //
        // Alpha is what is asserted, because alpha is what was wrong. A ratio computed from the token
        // would pass while the composable dimmed it — which is precisely how the defect survived: the
        // token was always `ink-soft`, and the `.copy(alpha)` was three lines away from it.
        item()
        val raster = decorRaster()
        val more = composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX))
            .fetchSemanticsNode().boundsInRoot

        val dots = (more.top.roundToInt() until more.bottom.roundToInt())
            .count { y ->
                (more.left.roundToInt() until more.right.roundToInt())
                    .any { x -> raster.colourAt(x, y).closeTo(capturedMoreInk) }
            }
        assertTrue(
            "the mark must be painted at full ink-soft, undimmed — found $dots rows carrying it",
            dots > 0,
        )

        // Three dots **stacked**, which is the half of the ruling that is about reading rather than
        // measuring: horizontal dots inline after a title read as a truncation ellipsis, and the parity
        // review's first-time reading of `Sunday market ⋯` was "the name is too long". A vertical mark
        // cannot be mistaken for elided text. Asserted as the shape — taller than it is wide — because
        // that is the property the misreading turns on, and it is the one a re-layout could undo.
        val columns = (more.left.roundToInt() until more.right.roundToInt())
            .count { x ->
                (more.top.roundToInt() until more.bottom.roundToInt())
                    .any { y -> raster.colourAt(x, y).closeTo(capturedMoreInk) }
            }
        assertTrue(
            "the mark must stand taller than it is wide ($dots rows against $columns columns)",
            dots > columns,
        )
    }

    @Test
    fun `a long title stops at two lines, so one zine cannot push its neighbour's cover down`() {
        // `.name` declares no `max-lines` and every frozen title is =< 14 characters, so the prototype
        // never rendered this. ADR-100 caps it at two: the grid sizes a row to its tallest cell, so a
        // three-line caption moves the *next row's covers* down by a line and a half of empty desk.
        //
        // Measured as the title's own laid-out height against a two-line ceiling, rather than by reading
        // the ellipsis: `TextOverflow.Ellipsis` is a rendering property with no semantics, so asserting
        // the string would only prove Compose truncates — not that the row held its shape.
        //
        // **The ceiling is measured, not computed from `line-height`.** `2 × 19.2 = 38.4` is the frozen
        // arithmetic and it is the wrong number here: Robolectric ignores `lineHeight` entirely (proven
        // by experiment on `ZineDockTest` — a reference at 40sp still laid out 19px), so two lines come
        // out at the font's own 40px. A ceiling written from the CSS would fail a correct
        // implementation. So a **short-titled sibling is rendered beside the long one** and its single
        // line is the unit: whatever this host's line box really is, the cap is two of them.
        composeRule.setContent {
            Host {
                Column {
                    ZineOnShelf(
                        zine = ITEM,
                        index = INDEX,
                        onOpen = {},
                        onActions = {},
                        modifier = Modifier.width(COLUMN),
                    )
                    ZineOnShelf(
                        zine = ITEM.copy(title = LONG_TITLE),
                        index = INDEX + 1,
                        onOpen = {},
                        onActions = {},
                        modifier = Modifier.width(COLUMN),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        fun heightOf(text: String) = composeRule
            .onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.height
        val oneLine = heightOf(TITLE)
        val wrapped = heightOf(LONG_TITLE)

        assertTrue(
            "a ${LONG_TITLE.length}-character title laid out ${wrapped}px against a one-line " +
                "${oneLine}px, so it reached ${wrapped / oneLine} lines — a third line pushes the next " +
                "row's covers down by that much empty desk",
            wrapped <= oneLine * NameMaxLines + HALF_PIXEL,
        )
        // The discrimination: the cap must actually be capping. A title that fitted on one line would
        // satisfy the assertion above while proving nothing about the cap.
        assertTrue(
            "the title must have wrapped at all, or the cap is untested (${wrapped}px on ${oneLine}px)",
            wrapped > oneLine * 1.5f,
        )
    }

    @Test
    fun `the ⋯ stands at the caption's trailing edge, clear of the printed cover`() {
        item()
        val item = coverBounds()
        val more = composeRule.onNodeWithTag(zineShelfMoreTestTag(INDEX))
            .fetchSemanticsNode().boundsInRoot

        // **The V2 geometry is gone, and re-baselining its two numbers would have hidden where it went.**
        // V2 wrote `.more{position:absolute;bottom:8px;right:8px}` on the cover. V2.1's cover has a
        // postmark stamp in that exact corner, overhanging it, so the `⋯` moved to the trailing end of
        // the caption row — the one place on the item it collides with nothing. The frozen file draws no
        // `⋯` at all (the departure is declared on `ZineOnShelf` and is the owner's to rule on), so what
        // is asserted here is the placement this implementation chose, stated as such.
        assertEquals(
            "the ⋯ must sit at the item's trailing edge",
            item.right,
            more.right,
            HALF_PIXEL,
        )
        assertEquals(
            "and at its bottom, level with the date rather than over the cover",
            item.bottom,
            more.bottom,
            HALF_PIXEL,
        )
        // The claim that matters is the collision, not the corner: it must be **below** the printed
        // object, where V2's sat on top of it. The cover is `aspect-ratio:3/4` on the column, so its
        // bottom is derived from the item's own width rather than from a second reading of the raster.
        val coverBottom = item.top + item.width * 4f / 3f
        assertTrue(
            "the ⋯ must stand clear of the cover (cover ends $coverBottom, ⋯ starts ${more.top})",
            more.top >= coverBottom,
        )
        // And the caption must reserve its width, or a long name runs underneath it — the reservation is
        // an `end` padding on a sibling, so nothing in the layout enforces it but this. Unmerged, because
        // the item's seam collapses its children out of the merged tree.
        val name = composeRule.onNodeWithText(TITLE, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "the name must stop before the ⋯ (name ends ${name.right}, ⋯ starts ${more.left})",
            name.right <= more.left,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // What the platform hears
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the cover is a button in the platform's own tree, named by the zine`() {
        item()
        val node = platformNode(zineShelfCoverTestTag(INDEX))
        assertEquals(
            "TalkBack must land on a button, not a bare View — the ADR-059 defect V2 exists not to inherit",
            "android.widget.Button",
            node.className,
        )
        assertEquals("and must hear the zine's own name", TITLE, node.contentDescription)
    }

    @Test
    fun `the long press has a named twin in the platform tree, not an anonymous one`() {
        item()
        val node = platformNode(zineShelfCoverTestTag(INDEX))
        // The implementation guide's rule is that *"every gesture has a named custom action twin"*. An
        // unnamed ACTION_LONG_CLICK is present but undiscoverable, so the label is the assertion.
        assertTrue("the platform node must offer a long-press action", node.isLongClickable)
        assertEquals("and must name it", "Actions", node.longClickLabel)
    }

    @Test
    fun `the ⋯ survives into the platform tree as its own named button`() {
        item()
        // This is the assertion that would have caught the shape B1 anticipated and B3 deleted: nested
        // inside the cover, the `⋯` sat under a `clearAndSetSemantics` and reached no service at all.
        val node = platformNode(zineShelfMoreTestTag(INDEX))
        assertEquals("android.widget.Button", node.className)
        assertEquals("the frozen aria-label, verbatim", "Actions for $TITLE", node.contentDescription)
        assertFalse(
            "and the fallback itself takes no long press — it *is* the non-gesture path",
            node.isLongClickable,
        )
    }

    @Test
    fun `the object discloses no metadata of its own`() {
        item()
        // `data-sub` exists on every `.zine` and the shelf shows none of it: *"Format & date are disclosed
        // there, not stamped on every card"* (`:142-144`). "There" is the sheet.
        composeRule.onNodeWithText(SUBTITLE).assertDoesNotExist()
        assertNull(
            "and it must not reach a screen reader either",
            platformNode(zineShelfCoverTestTag(INDEX)).contentDescription
                ?.takeIf { it.contains(SUBTITLE) },
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun item() {
        composeRule.setContent {
            Host {
                ZineOnShelf(
                    zine = ITEM,
                    index = INDEX,
                    onOpen = { opened += it },
                    onActions = { actioned += it },
                    modifier = Modifier.width(COLUMN),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private var capturedRingInk: Color = Color.Unspecified
    private var capturedMoreInk: Color = Color.Unspecified
    private lateinit var inputMode: InputModeManager

    /**
     * Focus the cover the way a keyboard would.
     *
     * `requestFocus()` alone does nothing here and fails silently: Compose declines focus while the input
     * mode is `Touch`, which Robolectric starts in. Requesting `Keyboard` first is also the faithful reading
     * of `:focus-visible`, which is the keyboard-focus selector rather than the focus one.
     */
    private fun focusTheCover() {
        composeRule.runOnUiThread {
            assertTrue(
                "the host must be able to enter keyboard input mode, or no focus test here means anything",
                inputMode.requestInputMode(InputMode.Keyboard),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).requestFocus()
        composeRule.waitForIdle()
    }

    @Composable
    private fun Host(content: @Composable () -> Unit) {
        ZinelyTheme(darkTheme = false) {
            capturedRingInk = ZinelyTheme.v21Colors.ink
            capturedMoreInk = ZinelyTheme.v21Colors.inkSoft
            inputMode = LocalInputModeManager.current
            // The probe ground rather than the desk: the ring is painted *outside* the cover, so the pixels
            // it lands on must be a colour no token carries, or "ring" and "room" could be the same value.
            //
            // Centred, not at the origin, and that is load-bearing for the ring tests: the frozen ring is
            // drawn *outside* the object's bounds, so a cover flush against x=0 puts its own ring off the
            // raster entirely. Centring leaves room on every side to sample.
            Box(
                Modifier.fillMaxSize().background(PROBE_GROUND),
                contentAlignment = Alignment.Center,
            ) { content() }
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

    private fun Bitmap.colourAt(x: Int, y: Int): Color = Color(getPixel(x, y))

    /** The arc crosses its diagonal at a sub-pixel point, so the probe accepts it within one pixel. */
    private fun Bitmap.anyPixelNear(x: Int, y: Int, expected: Color): Boolean =
        (-1..1).any { dy -> (-1..1).any { dx -> colourAt(x + dx, y + dy).closeTo(expected) } }

    private fun Color.closeTo(other: Color): Boolean {
        val tolerance = 1.5f / 255f
        return abs(red - other.red) <= tolerance &&
            abs(green - other.green) <= tolerance &&
            abs(blue - other.blue) <= tolerance
    }

    private fun coverBounds(): Rect =
        composeRule.onNodeWithTag(zineShelfCoverTestTag(INDEX)).fetchSemanticsNode().boundsInRoot

    /**
     * The printed cover's top and left edges, **read off the raster**.
     *
     * [zineShelfCoverTestTag] named the cover in V2, where the item *was* the cover. It names the item
     * now, and the item does not move when it is held: `.zine:active .cover`.
     *
     * ⚠️ **A placed bound cannot see this press, which is why it is measured in pixels.** The travel is
     * `Modifier.offset` *inside* the cover's chain: it re-places the content and reports the same size to
     * everything outside it, so `boundsInRoot` on any node — including the cover's own tag — reads 0px of
     * travel while the object visibly moves. Measured, not assumed: the tagged node was asserted first
     * and returned exactly `0.0`.
     *
     * The scan starts inside the `outline-offset` gap so a focus ring, when one is drawn, is behind the
     * probe rather than in front of it.
     */
    private fun printedCoverEdges(): Pair<Float, Float> {
        val item = coverBounds()
        val raster = decorRaster()
        val x = item.center.x.roundToInt()
        val y = item.center.y.roundToInt()
        val bottom = (item.top.roundToInt() + BOTTOM_SCAN_FROM until raster.height)
            .first { raster.colourAt(x, it).closeTo(capturedRingInk) }
        val left = (item.left.roundToInt() - RING_OFFSET + 2 until raster.width)
            .first { raster.colourAt(it, y).closeTo(capturedRingInk) }
        return bottom.toFloat() to left.toFloat()
    }

    /**
     * The **platform** node for a tagged composable, through the published CI-26 harness.
     *
     * Not a local walk of the view tree: `platformNode` in `:core:ui`'s test fixtures is the same reader
     * `ZinelyV2ControlPlatformA11yTest` uses, so a change in how the platform tree is read cannot leave one
     * of the two suites measuring something else. It grew `isLongClickable`/`longClickLabel` for B3.
     */
    private fun platformNode(tag: String): PlatformA11yNode =
        composeRule.onNodeWithTag(tag).platformNode(composeRule.activity)
}
