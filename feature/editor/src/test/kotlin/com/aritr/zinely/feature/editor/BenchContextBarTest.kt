package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The frozen `.ctx` verb bar, measured against the CSS that specifies it
 * ([ADR-092](../../../../../../../../../docs/DECISIONS.md#adr-092) rows 2.10–2.13a).
 *
 * The verb sets are asserted as **whole lists**, not as membership: `toolsFor()` is an ordered set per
 * kind, and a permutation would satisfy "each verb exists" while being the wrong bar.
 *
 * What this file deliberately does **not** claim is `Font`'s disabled state as seen by TalkBack. The
 * Compose semantics assertion below is necessary and insufficient — [ADR-058](../../../../../../../../../docs/DECISIONS.md#adr-058)'s
 * `ReframeControls.ZoomButton` passed `assertIsNotEnabled` here while telling the *platform* it was
 * enabled. Row 2.13a's real gate is `uiautomator dump` on hardware.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
class BenchContextBarTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "ctx-host"

        /** Neither `sheet` nor `chrome-line`, so the bar's own fill and border are both distinguishable. */
        val BACKDROP = Color(0xFF102030)
    }

    private var sheetArgb: Int = 0
    private var inkArgb: Int = 0
    private var consequenceArgb: Int = 0
    private var chromeLineArgb: Int = 0

    private fun host(verbs: List<BenchVerb>, visible: Boolean = true) {
        composeRule.setContent {
            ZinelyTheme {
                sheetArgb = ZinelyTheme.v2Colors.sheet.toArgb()
                inkArgb = ZinelyTheme.v2Colors.ink.toArgb()
                consequenceArgb = ZinelyTheme.v2Colors.consequence.toArgb()
                chromeLineArgb = ZinelyTheme.v2Colors.chromeLine.toArgb()
                Box(
                    Modifier.size(360.dp, 200.dp).testTag(HOST).background(BACKDROP),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BenchContextBar(
                        visible = visible,
                        verbs = verbs,
                        onVerb = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun hostBitmap(): Bitmap {
        composeRule.waitForIdle()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val r = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        return Bitmap.createBitmap(full, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    }

    // ── Row 2.13 — the frozen verb sets ─────────────────────────────────────────────────────────────

    @Test
    fun `the text verbs are the frozen five, in the frozen order`() {
        assertEquals(
            listOf(
                Copy.BenchVerbs.EDIT,
                Copy.BenchVerbs.FONT,
                Copy.BenchVerbs.SIZE,
                Copy.BenchVerbs.INK,
                Copy.BenchVerbs.DELETE,
            ),
            benchContextVerbs(BenchVerbKind.TEXT).map { it.label },
        )
    }

    @Test
    fun `the photo verbs are the frozen three, in the frozen order`() {
        assertEquals(
            listOf(Copy.BenchVerbs.REFRAME, Copy.BenchVerbs.REPLACE, Copy.BenchVerbs.DELETE),
            benchContextVerbs(BenchVerbKind.PHOTO).map { it.label },
        )
    }

    @Test
    fun `the two reachable sets are disjoint apart from Delete`() {
        val text = benchContextVerbs(BenchVerbKind.TEXT).map { it.label }.toSet()
        val photo = benchContextVerbs(BenchVerbKind.PHOTO).map { it.label }.toSet()
        // The premise of OD-11's "keep both bars": these vocabularies barely overlap, and the one verb
        // they share with the transform bar is the same one they share with each other.
        assertEquals(setOf(Copy.BenchVerbs.DELETE), text intersect photo)
    }

    @Test
    fun `the decor set fails loudly instead of defaulting to empty`() {
        // Row 2.13: `decor` is unreachable while DecorElement is re-seated (OD-2). An empty list would
        // let a future kind render a bar with no verbs and nobody would notice.
        assertThrows(IllegalStateException::class.java) { benchContextVerbs(BenchVerbKind.DECOR) }
    }

    @Test
    fun `only Font and Replace are drawn without a behaviour`() {
        val inert = (benchContextVerbs(BenchVerbKind.TEXT) + benchContextVerbs(BenchVerbKind.PHOTO))
            .filterNot { it.enabled }
            .map { it.label }
            .toSet()
        assertEquals(setOf(Copy.BenchVerbs.FONT, Copy.BenchVerbs.REPLACE), inert)
    }

    // ── Row 2.13a — drawn, and not operable ─────────────────────────────────────────────────────────

    @Test
    fun `Font is present and not enabled, while its neighbours are`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.FONT}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}").assertIsEnabled()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").assertIsEnabled()
    }

    // ── Row 2.11 — `flex:1` ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `every verb takes an equal share of the row, whatever its label measures`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        // "Delete" is twice the length of "Ink". Under `flex:none` they would measure differently; under
        // the frozen `flex:1` over a `min-width:0` basis they are equal.
        val widths = benchContextVerbs(BenchVerbKind.TEXT).map { verb ->
            composeRule.onNodeWithTag("$BenchContextBarTestTag-${verb.label}")
                .fetchSemanticsNode().boundsInWindow.width
        }
        widths.forEach { assertEquals(widths.first().toDouble(), it.toDouble(), 1.0) }
    }

    @Test
    fun `the frozen 40dp verb still offers a 48dp touch target`() {
        // The freeze draws a 40px control. Material floors every interactive component at 48dp, so the
        // DRAWN box is the frozen 40 and the TARGET is 48 - measured here at 96px on xhdpi. That gap is
        // deliberately not fought: D-009 records that no control in the frozen trilogy declares a minimum
        // target and most measure under 48dp, so the platform floor is the freeze being improved on, not
        // a parity failure. Asserted so nobody later "fixes" the bar down to a 40dp target.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val h = composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.EDIT}")
            .fetchSemanticsNode().boundsInWindow.height
        val floor = with(composeRule.density) { 48.dp.toPx() }
        val drawn = with(composeRule.density) { BenchContextBarButtonHeightDp.toPx() }
        assertEquals(floor.toDouble(), h.toDouble(), 1.0)
        assertTrue("the drawn height is the frozen 40dp, under the target floor", drawn < floor)
    }

    // ── Row 2.10 — the bar's own geometry ───────────────────────────────────────────────────────────

    @Test
    fun `the bar is inset the frozen 12dp from the left, right and bottom edges`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        // The frozen 12, transcribed HERE from `v2-bench.html:211` rather than read from the production
        // constant. Reading the constant made this test agree with any value the constant took - an
        // inset-12->0 mutation survived it, which is the whole reason the number is written out twice.
        val inset = with(composeRule.density) { 12.dp.toPx() }.toInt()

        // Scan the row through the bar's vertical middle: everything left of the inset is backdrop, and
        // the first non-backdrop pixel is the bar's own edge.
        val barBounds = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val hostBounds = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        assertEquals(inset.toDouble(), (barBounds.left - hostBounds.left).toDouble(), 1.0)
        assertEquals(inset.toDouble(), (hostBounds.right - barBounds.right).toDouble(), 1.0)
        assertEquals(inset.toDouble(), (hostBounds.bottom - barBounds.bottom).toDouble(), 1.0)

        // And the host's own corner is not the bar. It is NOT pure backdrop either — the frozen
        // `0 12px 30px -12px` shadow tints the pixels around the bar, which is what the first version of
        // this probe forgot and why it asserted equality with BACKDROP and failed.
        assertNotEquals(sheetArgb, bmp.getPixel(2, bmp.height - 3))
    }

    @Test
    fun `the bar is rounded, not square`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val bar = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val left = (bar.left - hostB.left).toInt()
        val top = (bar.top - hostB.top).toInt()
        val r = with(composeRule.density) { 16.dp.toPx() }.toInt()

        // Probe INSIDE the 1dp border but still outside a radius-16 arc: (4,4) is 39.6px from the arc's
        // centre at (32,32) on xhdpi, so a rounded bar has not painted it, while a square bar has. The
        // first version sampled (1,1) - which is the border ring at any radius, so radius-16->0 survived.
        assertNotEquals("the corner is cut by the radius", sheetArgb, bmp.getPixel(left + 4, top + 4))
        assertEquals("past the arc, the top edge is the bar's fill", sheetArgb, bmp.getPixel(left + r + 6, top + 2))
    }

    @Test
    fun `the bar is not in the tree at all when it is not showing`() {
        // Row 2.13c's floor: hidden means absent, not transparent — a bar that is merely invisible would
        // still take the taps aimed at the page beneath it.
        host(benchContextVerbs(BenchVerbKind.TEXT), visible = false)
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `the bar enters from exactly 14dp below where it settles`() {
        // Row 2.10's enter is a FIXED 14px rise, not a fraction of the bar's height — so the assertion has
        // to catch it mid-flight and compare against where it lands, which is the only way to tell 14 from
        // "some slide". The clock is driven by hand: with autoAdvance on, the animation is over before the
        // first measurement and every enter offset looks identical.
        var show by mutableStateOf(false)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            ZinelyTheme {
                Box(
                    Modifier.size(360.dp, 200.dp).testTag(HOST).background(BACKDROP),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    BenchContextBar(
                        visible = show,
                        verbs = benchContextVerbs(BenchVerbKind.TEXT),
                        onVerb = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        show = true
        // Recomposition first (the node has to exist before it can be measured), then one frame of the
        // enter. Advancing the clock without this measures a node that is not in the tree yet.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        val start = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow.top
        composeRule.mainClock.advanceTimeBy(BenchContextBarEnterMillis + 100L)
        val settled = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow.top
        val expected = with(composeRule.density) { BenchContextBarEnterOffsetDp.toPx() }
        // A couple of frames of easing have already run at `start`, so the gap is at most the full 14dp
        // and unmistakably more than zero — which is what a `14 -> 0` mutation removes entirely.
        assertTrue("entered from below: $start vs $settled", start > settled)
        assertTrue("and by no more than the frozen 14dp", start - settled <= expected + 1f)
    }

    // ── Row 2.12 — `.danger` ────────────────────────────────────────────────────────────────────────

    @Test
    fun `Delete is drawn in consequence, and its neighbours are not`() {
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        // The darkest pixel of each control is its glyph. Delete's must differ from Edit's: dropping
        // `.danger` makes them identical, which is exactly the mutation this kills.
        assertNotEquals(inkOf(bmp, Copy.BenchVerbs.EDIT), inkOf(bmp, Copy.BenchVerbs.DELETE))

        // …and differing is not enough: `.danger` names ONE colour. Swapping `consequence` for any other
        // dark tint would survive the line above, so the glyph is placed against both candidates and must
        // land nearer `consequence` than `ink` — while Edit's lands the other way round. (Nearer, not
        // equal: the sampled pixel is an antialiased 1.7px stroke, so it carries some `sheet` with it.)
        val consequence = consequenceArgb
        val ink = inkArgb
        assertTrue(
            "Delete's glyph is drawn in consequence, not in ink",
            dist(inkOf(bmp, Copy.BenchVerbs.DELETE), consequence) < dist(inkOf(bmp, Copy.BenchVerbs.DELETE), ink),
        )
        assertTrue(
            "…and Edit's is drawn in ink, not in consequence",
            dist(inkOf(bmp, Copy.BenchVerbs.EDIT), ink) < dist(inkOf(bmp, Copy.BenchVerbs.EDIT), consequence),
        )
    }

    // ── Rows 2.10 / 2.13a — the two axes `clearAndSetSemantics` silently wipes ──────────────────────

    @Test
    fun `every enabled verb publishes a click action, and the inert ones publish none`() {
        // The review finding this file exists to not repeat. `clearAndSetSemantics` wipes `OnClick` by the
        // same rule it wipes the disabled state, and the first version of this file re-published only the
        // latter — so all five verbs announced themselves as buttons exposing no way to activate them. A
        // pointer tap still worked (it never consults semantics), which is precisely why nothing caught
        // it. `uiautomator dump` on device would have read `clickable="false"` on every verb.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        for (verb in benchContextVerbs(BenchVerbKind.TEXT)) {
            val node = composeRule.onNodeWithTag("$BenchContextBarTestTag-${verb.label}")
            if (verb.enabled) node.assertHasClickAction() else node.assertHasNoClickAction()
        }
    }

    @Test
    fun `an inert verb is drawn inert, and not merely announced so`() {
        // Row 2.13a's other half. `enabled = false` on a TextButton dims via LocalContentColor, and this
        // bar overrides the content colour explicitly — so without the alpha the control says "disabled"
        // to TalkBack and "tap me" to the eye. `.icon-btn:disabled{opacity:.35}` (v2-bench.html:206) is
        // the corpus's own answer, transcribed here rather than read from production so the constant and
        // the assertion cannot agree with each other on a wrong value.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val ink = inkArgb
        val font = inkOf(bmp, Copy.BenchVerbs.FONT)
        val edit = inkOf(bmp, Copy.BenchVerbs.EDIT)
        assertNotEquals("Font is dimmed; Edit is not", edit, font)
        // .35 alpha over `sheet` lands between the two, and much nearer sheet than full ink.
        assertTrue("the dimmed glyph is lighter than the live one", luma(font) > luma(edit))
        assertTrue("…and still darker than the sheet it sits on", luma(font) < luma(sheetArgb))
        assertTrue("the dim is the frozen .35, not an arbitrary fade", dist(font, ink) > dist(font, sheetArgb))
    }

    @Test
    fun `the bar carries the frozen 1px chrome-line border`() {
        // Row 2.10 claimed a border nothing read. The straight middle of the left edge is unaffected by
        // the corner arcs, so the ring there is the border colour and nothing else.
        host(benchContextVerbs(BenchVerbKind.TEXT))
        val bmp = hostBitmap()
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val barB = composeRule.onNodeWithTag(BenchContextBarTestTag).fetchSemanticsNode().boundsInWindow
        val x = (barB.left - hostB.left).toInt() + 1
        val y = ((barB.top + barB.bottom) / 2f - hostB.top).toInt()
        val chromeLine = chromeLineArgb
        assertTrue(
            "the bar's edge is chrome-line, not its own sheet fill",
            dist(bmp.getPixel(x, y), chromeLine) < dist(bmp.getPixel(x, y), sheetArgb),
        )
    }

    // ── Pixel helpers ───────────────────────────────────────────────────────────────────────────────

    /** The darkest pixel inside a verb's box — its glyph, whatever the glyph happens to be. */
    private fun inkOf(bmp: Bitmap, label: String): Int {
        val hostB = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        val b = composeRule.onNodeWithTag("$BenchContextBarTestTag-$label").fetchSemanticsNode().boundsInWindow
        var best = 0
        var bestScore = Int.MAX_VALUE
        for (y in (b.top - hostB.top).toInt() + 2 until (b.bottom - hostB.top).toInt() - 2) {
            for (x in (b.left - hostB.left).toInt() + 2 until (b.right - hostB.left).toInt() - 2) {
                val p = bmp.getPixel(x, y)
                val score = luma(p)
                if (score < bestScore) { bestScore = score; best = p }
            }
        }
        return best
    }

    private fun luma(argb: Int): Int = (argb and 0xFF) + ((argb shr 8) and 0xFF) + ((argb shr 16) and 0xFF)

    /** Manhattan distance in RGB — enough to say "nearer this colour than that one". */
    private fun dist(a: Int, b: Int): Int =
        listOf(0, 8, 16).sumOf { s -> kotlin.math.abs(((a shr s) and 0xFF) - ((b shr s) and 0xFF)) }
}
