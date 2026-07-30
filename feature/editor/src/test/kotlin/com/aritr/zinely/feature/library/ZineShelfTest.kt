package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
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
 * The shelf's geometry, read back off the placed layout rather than asserted against the source.
 *
 * **Why measured and not inspected.** Phase A's record names the failure this file exists to avoid —
 * *"three packages in a row shipped an assertion blind to the defect class it claimed to gate"* — and a
 * grid has an unusually rich supply of defects that compose perfectly and screenshot plausibly: a
 * transposed `gap`, a heading that pins instead of scrolling, a padding on the wrong axis, a column count
 * that happens to look fine at one width. Every assertion here therefore reads a **placed bound** or a
 * **rasterised pixel**.
 *
 * `w480dp` is the measuring stick: it makes each column `(480 − 22 − 22 − 20) / 2 = 208dp` wide, so every
 * frozen inset is tens of pixels and no assertion rests on a rounding.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineShelfTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val SHELF = "shelf"
        const val HEADING = "Your shelf"

        /** `.shelf{padding:30px 22px 152px}` and `gap:28px 20px`, as the numbers under test. */
        const val PAD_TOP = 30
        const val PAD_SIDE = 22
        const val PAD_BOTTOM = 152
        const val ROW_GAP = 28
        const val COLUMN_GAP = 20
        const val COLUMNS = 2

        /** `.shelf-head{padding:2px 2px 0}`. */
        const val HEAD_PAD = 2

        /**
         * Every geometry assertion's tolerance — **half a pixel, not one**.
         *
         * `assertEquals(expected, actual, delta)` passes at `|Δ| == delta`, so a 1f tolerance accepts a
         * value one pixel off the frozen one — exactly the *"a number merely close to the frozen one is
         * the defect"* case this file exists to catch. Independent review proved it by setting the column
         * gap to **21dp** and watching the whole suite stay green. Density is pinned to `1.0` and every
         * frozen value divides exactly at `w480dp` (columns land on whole pixels: `(480−44−20)/2 = 208`),
         * so there is no rounding for a tolerance to absorb and half a pixel costs nothing.
         */
        const val HALF_PIXEL = 0.5f

        /** A ground no token in the palette carries, so "the shelf painted over it" is unambiguous. */
        val PROBE_GROUND = Color(0xFF00FF00)

        /** Reference renderings of the heading, for the type assertions to measure against. */
        const val REF_FROZEN = "ref-frozen"
        const val REF_HEAVY = "ref-weight-600"
        const val REF_SMALL = "ref-16sp"

        /** The frozen shelf's own six, plus enough more to overflow a 960dp viewport. */
        fun zines(count: Int): List<ZineShelfItem> = List(count) { i ->
            ZineShelfItem(
                title = "Zine ${i + 1}",
                recipe = ZineCoverRecipe(
                    surface = ZineCoverSurface.entries[i % ZineCoverSurface.entries.size],
                    stamp = ZineCoverStamp.entries[i % ZineCoverStamp.entries.size],
                ),
                subtitle = subtitleOf(i),
            )
        }

        fun titleOf(index: Int) = "Zine ${index + 1}"

        /** The line the sheet discloses and the shelf must not — `data-sub`, B3's field. */
        fun subtitleOf(index: Int) = "A4 · $index days ago"
    }

    // ---------------------------------------------------------------------------------------------
    // The grid — `grid-template-columns:1fr 1fr`, `gap:28px 20px`, `align-content:start`
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the shelf stands its covers two to a row`() {
        shelf(zines(4))
        // Read the rows off the placed tops rather than trusting the column count: a one-column shelf and
        // a three-column shelf both compose, and both would pass a test that only counted covers.
        val rows = (0 until 4).map { cover(it) }.groupBy { it.top.roundToInt() }
        assertEquals("four covers must stand in two rows of two", 2, rows.size)
        for ((top, row) in rows) {
            assertEquals("the row at y=$top must hold $COLUMNS covers", COLUMNS, row.size)
        }
    }

    @Test
    fun `the row gap and the column gap are not interchangeable`() {
        shelf(zines(4))
        val topLeft = cover(0)
        val topRight = cover(1)
        val below = cover(2)

        // `gap:28px 20px` is row-gap FIRST, then column-gap. Transposing them is this package's most
        // likely defect and its least visible: 28 between the columns and 20 between the rows looks
        // deliberate on any screenshot. So both numbers are named.
        //
        // The last assertion compares two constants of this file and therefore cannot fail from anything
        // the shelf does — it is not a second reading of production. It guards a *future edit of this
        // test*: if someone ever "simplifies" the two frozen numbers into one shared constant, the two
        // assertions above silently stop discriminating a transposition and this one says so.
        assertEquals(
            "the column gap must be ${COLUMN_GAP}px",
            COLUMN_GAP.toFloat(),
            topRight.left - topLeft.right,
            HALF_PIXEL,
        )
        assertEquals(
            "the row gap must be ${ROW_GAP}px",
            ROW_GAP.toFloat(),
            below.top - topLeft.bottom,
            HALF_PIXEL,
        )
        assertNotEquals(
            "the two gaps must differ, or neither assertion above can catch a transposition",
            ROW_GAP,
            COLUMN_GAP,
        )
    }

    @Test
    fun `the covers sit inside the frozen side padding`() {
        shelf(zines(2))
        val shelf = bounds(SHELF, byTag = true)
        val left = cover(0)
        val right = cover(1)

        assertEquals(
            "the first column must start ${PAD_SIDE}px in",
            PAD_SIDE.toFloat(),
            left.left - shelf.left,
            HALF_PIXEL,
        )
        assertEquals(
            "the last column must end ${PAD_SIDE}px in",
            PAD_SIDE.toFloat(),
            shelf.right - right.right,
            HALF_PIXEL,
        )
        // `1fr 1fr` — equal columns, which is a different claim from "inside the padding" and fails
        // separately if a fixed cell width is ever introduced.
        assertEquals("the two columns must be equal", left.width, right.width, HALF_PIXEL)
    }

    @Test
    fun `the heading opens the shelf at its frozen top padding`() {
        shelf(zines(2))
        val shelf = bounds(SHELF, byTag = true)
        val head = bounds(HEADING)
        // 30px of shelf padding, then the heading cell's own 2px. Both are asserted through one edge
        // because that is the one edge the design states; splitting them would assert an internal.
        assertEquals(
            "the heading must open ${PAD_TOP + HEAD_PAD}px down",
            (PAD_TOP + HEAD_PAD).toFloat(),
            head.top - shelf.top,
            HALF_PIXEL,
        )
    }

    @Test
    fun `the heading spans the shelf, so no cover stands beside it`() {
        shelf(zines(4))
        val shelf = bounds(SHELF, byTag = true)
        val head = bounds(HEADING)

        // `grid-column:1 / -1` — two independent consequences, because either can fail alone. First: the
        // cell really is the full line. A grid cell constrains its content to a fixed width, so the
        // heading's own `Text` fills it, and a one-column heading would measure one column wide.
        assertEquals(
            "the heading cell must span both columns",
            (shelf.width - 2 * PAD_SIDE - 2 * HEAD_PAD),
            head.width,
            HALF_PIXEL,
        )
        // Second: the line was consumed, so nothing shares it. Were the heading a one-column item, the
        // first cover would stand in column 1 on the heading's own row.
        for (i in 0 until 4) {
            val placed = cover(i)
            assertTrue(
                "${titleOf(i)} must stand below the heading, not beside it " +
                    "(cover top ${placed.top}, heading bottom ${head.bottom})",
                placed.top >= head.bottom,
            )
        }
        assertEquals(
            "the first row must follow the heading by the row gap",
            ROW_GAP.toFloat(),
            cover(0).top - head.bottom,
            HALF_PIXEL,
        )
    }

    @Test
    fun `the shelf keeps the dock's room clear below the last cover`() {
        // `padding-bottom:152px` clears the `.dock` that B4 will place there. Asserted at the end of a
        // shelf long enough to overflow: an eighth cover puts the content ~1430px into a 960px viewport,
        // so scrolling to the last item clamps at the true end of the scroll.
        val many = zines(8)
        shelf(many)
        composeRule.onNodeWithTag(SHELF).performScrollToIndex(many.lastIndex)
        composeRule.waitForIdle()

        val shelf = bounds(SHELF, byTag = true)
        val last = cover(many.lastIndex)
        assertEquals(
            "the last cover must end ${PAD_BOTTOM}px above the shelf's own bottom",
            PAD_BOTTOM.toFloat(),
            shelf.bottom - last.bottom,
            HALF_PIXEL,
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Scrolling — `overflow-y:auto`, and a heading that is a cell rather than a bar
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the heading scrolls away with the covers`() {
        val many = zines(8)
        shelf(many)
        composeRule.onNodeWithText(HEADING).assertIsDisplayed()

        composeRule.onNodeWithTag(SHELF).performScrollToIndex(many.lastIndex)
        composeRule.waitForIdle()

        // `.shelf-head` is a cell *inside* the scrolling `.shelf`, not a bar above it. A pinned heading
        // composes cleanly, reads as intentional, and is a different screen — so it is asserted.
        composeRule.onNodeWithText(HEADING).assertDoesNotExist()
    }

    @Test
    fun `a shelf longer than the viewport reaches its last cover`() {
        val many = zines(8)
        shelf(many)
        // `overflow-y:auto`. Without it the eighth cover is composed and unreachable, which no static
        // screenshot of the first screenful would show.
        composeRule.onNodeWithTag(SHELF).performScrollToIndex(many.lastIndex)
        composeRule.waitForIdle()
        // Found by tag: B3 collapsed the cover's semantics into one control node, so the title is that
        // node's own name rather than a `Text` beneath it.
        composeRule.onNodeWithTag(zineShelfCoverTestTag(many.lastIndex)).assertIsDisplayed()
    }

    @Test
    fun `the shelf places every zine it is given, in the order it is given`() {
        shelf(zines(6))
        // The frozen file states no sort — V1's sort control was dropped by ruling — so the order is the
        // caller's and the shelf must not impose one of its own.
        //
        // **The title is bound to its position, because "every title exists" cannot see a permutation.**
        // The first version of this test asserted two things that were each individually true of any
        // ordering: that the placement bounds ascend in reading order — which they do by construction,
        // since `cover(i)` finds the cover *placed* at index i whatever order the source list was
        // consumed in — and that each title existed *somewhere* on the shelf. Independent review broke it
        // by feeding the grid `zines.reversed()` and watching this test, the one whose name is the claim,
        // stay green. Only the coupled match below sees it.
        //
        // **B3 changed how the coupling is read, not what it claims.** Until B3 the cover's title was a
        // `Text` node inside it and this matched `hasAnyDescendant(hasText(…))`. The cover is now a control
        // whose semantics collapse to one node — the only shape that reaches TalkBack as a real button —
        // so the title arrives as that node's own `contentDescription` instead. Same claim, same
        // position-to-identity binding, and the same mutation (`zines.reversed()`) still turns it red.
        val placed = (0 until 6).map { cover(it) }
        for (i in 0 until 6) {
            composeRule.onNode(
                hasTestTag(zineShelfCoverTestTag(i)) and hasContentDescription(titleOf(i)),
            ).assertExists()

            if (i == 0) continue
            val previous = placed[i - 1]
            val current = placed[i]
            assertTrue(
                "${titleOf(i)} must follow ${titleOf(i - 1)} in reading order",
                current.top > previous.top || (current.top == previous.top && current.left > previous.left),
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The heading's type and ink — rendered, not declared
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the heading is set in the voice face at the frozen size and weight`() {
        // **Why ink coverage and not the node's width.** A grid cell constrains its content to a fixed
        // width, so the heading's `Text` fills its span whatever type is inside it — the placed bounds
        // are 432px at 26sp Medium, at 16sp, and at any weight. Height alone would catch a wrong size and
        // never a wrong weight, and weight is precisely what **D-005** ruled on. What does change is how
        // much ink the glyphs lay down, so that is what is counted: the shelf's own heading against three
        // reference renderings composed beside it on the same ground.
        shelfWithReferences(zines(2))
        val decor = decorRaster()
        val ink = capturedInk
        val threshold = (PROBE_GROUND.luminance() + ink.luminance()) / 2f

        val frozen = decor.inkCoverage(bounds(REF_FROZEN, byTag = true), threshold)
        val heavier = decor.inkCoverage(bounds(REF_HEAVY, byTag = true), threshold)
        val smaller = decor.inkCoverage(bounds(REF_SMALL, byTag = true), threshold)
        val heading = decor.inkCoverage(headingBounds(), threshold)

        // The discriminating power is asserted BEFORE the parity claim. If this host cannot tell 25.92sp
        // from 16sp, or Fraunces 500 from the file's stale 600, then the comparison below proves nothing
        // and would pass over the very defect it exists to catch — the failure mode this file is written
        // against. `> 3%` is the tolerance the last assertion allows, so anything inside it is not a
        // difference this test could act on.
        assertTrue("nothing was drawn at the frozen style (coverage $frozen)", frozen > 0)
        assertTrue(
            "this host cannot distinguish Fraunces 500 from the file's stale 600 " +
                "($frozen vs $heavier), so D-005 is unguarded here",
            relativeGap(frozen, heavier) > 0.03f,
        )
        assertTrue(
            "this host cannot distinguish the frozen 25.92sp from body 16sp ($frozen vs $smaller)",
            relativeGap(frozen, smaller) > 0.03f,
        )

        assertTrue(
            "the heading must render at Fraunces 500 / 25.92sp — coverage $heading against " +
                "$frozen frozen, $heavier at weight 600, $smaller at 16sp",
            relativeGap(heading, frozen) <= 0.03f,
        )
    }

    @Test
    fun `the heading is printed in ink, not in a softer grey`() {
        shelf(zines(2))
        val ink = capturedInk
        val head = bounds(HEADING)
        val decor = decorRaster()

        // A wrong ink token — `inkSoft`, `inkFaint`, a Material default — composes identically and reads
        // as a design choice. The glyph cores of 26px type at weight 500 land on the exact value, so an
        // exact hit is available and its absence is the defect.
        val hit = (head.top.roundToInt()..head.bottom.roundToInt()).any { y ->
            (head.left.roundToInt()..head.right.roundToInt()).any { x ->
                decor.matches(x, y, ink)
            }
        }
        assertTrue("no pixel of the heading is $ink", hit)
    }

    // ---------------------------------------------------------------------------------------------
    // A11y and ground
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the heading is a heading to a screen reader`() {
        shelf(zines(2))
        // The frozen markup is an `<h1>`. Dropping the role leaves a stray phrase that looks identical
        // and costs TalkBack its landmark, which is the kind of loss only this assertion notices.
        composeRule.onNodeWithText(HEADING)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `the heading is a heading in the platform's own tree, not only in Compose's`() {
        shelf(zines(2))
        // **The assertion above is not sufficient, and this project has the scar to prove it.** A Compose
        // semantics test reads the *merged* semantics tree; TalkBack reads the platform
        // `AccessibilityNodeInfo` tree, and the two are not the same thing. ADR-058 records a control that
        // passed `assertIsNotEnabled` in Robolectric while telling the platform it was enabled — green
        // suite, shipped defect. `heading()` is exactly the kind of property that could be dropped in
        // translation, so it is read back off the real provider.
        val node = requireNotNull(headingNodeInfo()) { "no platform node carries the text '$HEADING'" }
        assertTrue("the platform tree must report the heading as a heading", node.isHeading)
        assertEquals("and it must still be the heading's own text", HEADING, node.text?.toString())
    }

    @Test
    fun `the grid mirrors under RTL while each printed cover does not`() {
        // The same two surfaces `ZineCoverRenderTest` probes, so the crease reading below is the reading
        // that file already proved discriminates — a plain stock and a banded stock, both creased.
        val two = listOf(
            ZineShelfItem(
                titleOf(0),
                ZineCoverRecipe(ZineCoverSurface.MatchaInk, ZineCoverStamp.Sun),
                subtitleOf(0),
            ),
            ZineShelfItem(
                titleOf(1),
                ZineCoverRecipe(ZineCoverSurface.PaperMatchaBand, ZineCoverStamp.Sprig),
                subtitleOf(1),
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Host(PROBE_GROUND) { ZineShelf(two, onOpen = {}, onActions = {}, modifier = shelfModifier()) }
            }
        }
        composeRule.waitForIdle()

        // **D-019's chrome-versus-artifact split, asserted rather than reasoned.** ADR-082 claimed this
        // held without testing it, which independent review flagged: the shelf is *chrome*, so its columns
        // may mirror, while each cover is a *printed artifact* and must not. Both halves are checked here
        // because either alone would be consistent with the wrong implementation of the other.
        val shelf = bounds(SHELF, byTag = true)
        val first = cover(0)
        val second = cover(1)

        // Chrome mirrors: item 0 takes the right-hand column under RTL.
        assertTrue(
            "the first item must take the right-hand column under RTL (first at ${first.left}, " +
                "second at ${second.left})",
            first.left > second.left,
        )
        // The frozen side padding is symmetric, so it cannot discriminate direction — but it must still
        // hold, and a one-sided padding bug would show here.
        assertEquals(
            "the outermost column must still end ${PAD_SIDE}px in",
            PAD_SIDE.toFloat(),
            shelf.right - first.right,
            HALF_PIXEL,
        )
        // The artifact does not mirror: the fold spine stays at the cover's physical left, which is what
        // `ZineCoverRenderTest.a mirrored layout does not mirror the printed object` pins in detail. Here
        // it is re-checked *inside the grid*, because a mirroring container could have flipped the cover
        // even though the cover's own shape is absolute.
        val decor = decorRaster()
        for (placed in listOf(first, second)) {
            val spineX = placed.left.roundToInt() + 9
            val y = placed.center.y.roundToInt()
            val spine = decor.colourAt(spineX, y)
            // Four pixels right of the crease is plain stock — the same pair of samples
            // `ZineCoverRenderTest.the fold spine is a hairline highlight…` reads, so a cover flipped by
            // its container puts stock at `spineX` and the highlight beyond the far edge.
            val stock = decor.colourAt(spineX + 4, y)
            assertTrue(
                "the crease must stay 9px from each cover's physical left inside a mirrored grid " +
                    "(found $spine against stock $stock)",
                spine.luminance() > stock.luminance(),
            )
        }
    }

    @Test
    fun `the shelf paints no ground of its own`() {
        shelf(zines(2), ground = PROBE_GROUND)
        val shelf = bounds(SHELF, byTag = true)
        val decor = decorRaster()
        // `.shelf` declares no `background` — the desk belongs to `.phone`, which is B5's screen. A
        // `background(desk)` added here would be a second place the room's colour is decided, and it
        // would look right in every raster until B5 disagreed with it. Sampled in the top padding, above
        // the heading and clear of any cover's cast shadow.
        val x = shelf.center.x.roundToInt()
        val y = shelf.top.roundToInt() + 5
        assertTrue(
            "the shelf covered its host's ground at ($x,$y): found ${decor.colourAt(x, y)}",
            decor.matches(x, y, PROBE_GROUND),
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private fun shelf(zines: List<ZineShelfItem>, ground: Color = PROBE_GROUND) {
        composeRule.setContent { Host(ground) { ZineShelf(zines, onOpen = {}, onActions = {}, modifier = shelfModifier()) } }
        composeRule.waitForIdle()
    }

    /**
     * The shelf, plus the heading rendered three more times at known styles on the same ground.
     *
     * Composed bottom-aligned, inside the shelf's own 152px of dock room, so the references sit on
     * untouched ground clear of every cover and its cast shadow.
     */
    private fun shelfWithReferences(zines: List<ZineShelfItem>) {
        composeRule.setContent {
            Host(PROBE_GROUND) {
                ZineShelf(zines, onOpen = {}, onActions = {}, modifier = shelfModifier())
                Column(Modifier.align(Alignment.BottomStart)) {
                    Reference(REF_FROZEN, FontWeight.Medium, 25.92.sp)
                    Reference(REF_HEAVY, FontWeight.SemiBold, 25.92.sp)
                    Reference(REF_SMALL, FontWeight.Medium, 16.sp)
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The heading's text at a named style — the yardstick the shelf's own heading is measured against. */
    @Composable
    private fun Reference(tag: String, weight: FontWeight, size: TextUnit) {
        Text(
            text = HEADING,
            style = TextStyle(
                fontFamily = ZinelyTheme.v2Typography.voice,
                fontWeight = weight,
                fontSize = size,
                letterSpacing = (-0.01).em,
                color = ZinelyTheme.v2Colors.ink,
            ),
            modifier = Modifier.testTag(tag),
        )
    }

    /** `.shelf{flex:1 1 auto}` — the shelf takes the space its host has left. */
    private fun shelfModifier() = Modifier.fillMaxSize().testTag(SHELF)

    /** `--ink`, read out of the same composition the shelf drew in rather than re-derived here. */
    private var capturedInk: Color = Color.Unspecified

    @Composable
    private fun Host(ground: Color, content: @Composable BoxScope.() -> Unit) {
        ZinelyTheme(darkTheme = false) {
            capturedInk = ZinelyTheme.v2Colors.ink
            Box(Modifier.fillMaxSize().background(ground)) { content() }
        }
    }

    private fun bounds(finder: String, byTag: Boolean = false): Rect {
        val node = if (byTag) composeRule.onNodeWithTag(finder) else composeRule.onNodeWithText(finder)
        return node.fetchSemanticsNode().boundsInRoot
    }

    /**
     * The shelf's **own** heading, found by its role rather than its text.
     *
     * The type test composes the same words three more times as yardsticks, so text alone is ambiguous
     * there — and the `<h1>` role is the one thing only the real heading carries, which makes this finder
     * a second reading of the a11y claim rather than a workaround.
     */
    private fun headingBounds(): Rect = composeRule
        .onNode(hasText(HEADING) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        .fetchSemanticsNode()
        .boundsInRoot

    /**
     * One placed **cover's** bounds — not its title's.
     *
     * The distinction is the whole reason [zineShelfCoverTestTag] exists: searching for a zine's title
     * finds the `Text` inside the cover, inset by `padding:15px 15px 18px`, and every geometry assertion
     * in this file measured that instead on its first run — reporting the 20px column gap as 177px.
     */
    private fun cover(index: Int): Rect = bounds(zineShelfCoverTestTag(index), byTag = true)

    /**
     * The **platform** `AccessibilityNodeInfo` for the heading, walked off the real provider.
     *
     * The recipe is [docs/DEVICE-VERIFICATION.md](docs/DEVICE-VERIFICATION.md)'s, run in-process: ask the
     * `AndroidComposeView` for its `AccessibilityNodeProvider` and walk it, rather than asking Compose's
     * own test tree — which is the whole point, since the two trees can disagree.
     */
    private fun headingNodeInfo(): AccessibilityNodeInfo? {
        // Compose exposes each semantics node to the platform as a virtual view whose id *is* the
        // semantics id, so the heading's own node can be asked for directly rather than by walking.
        val semanticsId = composeRule
            .onNode(hasText(HEADING) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .fetchSemanticsNode()
            .id
        val provider = composeRule.activity.window.decorView.composeNodeProvider()
        return provider?.createAccessibilityNodeInfo(semanticsId)
    }

    /** The `AndroidComposeView`'s own provider — the object TalkBack actually talks to. */
    private fun View.composeNodeProvider(): AccessibilityNodeProvider? {
        if (javaClass.simpleName == "AndroidComposeView") return accessibilityNodeProvider
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                getChildAt(i).composeNodeProvider()?.let { return it }
            }
        }
        return null
    }

    /** The share of pixels in [region] the glyphs actually darkened. */
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

    /** One 8-bit step of tolerance: an exact value still rounds through the bitmap's own channels. */
    private fun Bitmap.matches(x: Int, y: Int, expected: Color): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val actual = colourAt(x, y)
        val tolerance = 1.5f / 255f
        return abs(actual.red - expected.red) <= tolerance &&
            abs(actual.green - expected.green) <= tolerance &&
            abs(actual.blue - expected.blue) <= tolerance
    }
}
