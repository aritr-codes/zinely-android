package com.aritr.zinely.feature.editor

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * **The dim is an arithmetic claim, so it is tested as one.**
 *
 * [ADR-091 §2.1](../../../../../../../../../docs/DECISIONS.md#adr-091) accepts a `--paper` wash in place of
 * the frozen per-element opacity on exactly one ground: over paper, CSS `opacity:x` composites to
 * `x·element + (1−x)·paper`, and a `(1−x)` paper wash produces the **same pixel**. If that is true the
 * substitution is exact; if it is off by one step it is a different design wearing the spec's name.
 *
 * ADR-102 P1 moved the freeze from `.4` to **`.5`** (`v21-bench.html:153`), which makes `x` and `1−x`
 * the same number *for this one value*. The two are still different quantities and are still written
 * separately here — collapsing them would make the test agree with a wash that had stopped being the
 * complement, and it would do so silently the next time the freeze moves.
 *
 * So these tests do not assert that a scrim exists, that its alpha field matches a constant, or that a composable
 * was called — a test that rebuilds the production constant cannot fail (the corpus's rule 2). They
 * rasterise the composite and compare the **landed pixel** against the number CSS would have produced,
 * computed independently here from the frozen `.4`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchSelectionFocusTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val HOST = "focus-host"
        const val W = 200
        const val H = 200

        /** A stand-in for a drawn element: saturated, and far from both paper and any wash of it. */
        val ELEMENT = Color(0xFF1030C0)

        /** The sheet's size inside the 200dp host — smaller, so there is a desk to escape onto. */
        const val SHEET = 150

        /** The desk the sheet sits on: the V2 dark room, where the escaped wash was most visible. */
        val DESK = Color(0xFF2F2A22)

        /**
         * The frozen `.content.focusing .el:not(.selected){opacity:.5}` (`v21-bench.html:153`) — the
         * spec value, **not** the wash.
         *
         * V2.1 lifted it from `.4`, so the complement lifted with it and the two now happen to be the
         * same number. That coincidence is why this constant stays separate from `BenchFocusDimAlpha`:
         * collapsing them would be right for exactly one value of the freeze, and the whole point of
         * this test is that the wash is the *complement* of the element's opacity rather than equal
         * to it.
         */
        const val FROZEN_ELEMENT_OPACITY = 0.5f
    }

    /**
     * A synthetic page: a [ELEMENT]-filled square standing for a rendered element, over paper, with the
     * scrim composited on top exactly as [EditorPagePreview] stacks it. Deliberately **not** the real
     * editor — the arithmetic under test is the scrim's, and a real render would substitute the renderer's
     * anti-aliasing for the thing being measured.
     */
    // The scrim's inputs are hoisted into state rather than baked into the composition, because a Compose
    // rule accepts `setContent` **once** per test and every assertion here compares two scrim settings of
    // the same pixels. Measuring across two compositions would also mean measuring two rasterisations.
    private val dimAlpha = mutableStateOf(0f)
    private val holes = mutableStateOf<List<List<PtPoint>>>(emptyList())
    private val covers = mutableStateOf<List<Pair<List<PtPoint>, Float>>>(emptyList())

    private fun host() {
        composeRule.setContent {
            ZinelyTheme(darkTheme = false) {
                BenchSheetIsland(modifier = Modifier.size(W.dp, H.dp).testTag(HOST)) {
                    // The DESK first, and it is deliberately not paper. An earlier draft of this host
                    // painted paper edge-to-edge, which is the single arrangement in which a wash that
                    // escapes the sheet is invisible — and the scrim did escape it. The independent review
                    // found that defect precisely because no test could. The desk stays.
                    Box(Modifier.fillMaxSize().background(DESK))
                    Box(Modifier.align(Alignment.TopStart).size(SHEET.dp).background(ZinelyTheme.v2Colors.paper))
                    Box(Modifier.align(Alignment.TopStart).size(100.dp).background(ELEMENT))
                    BenchFocusScrim(
                        paper = ZinelyTheme.v2Colors.paper,
                        pageRect = Rect(0f, 0f, SHEET.toFloat(), SHEET.toFloat()),
                        dimAlpha = dimAlpha.value,
                        holes = holes.value,
                        covers = covers.value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    /**
     * **The regression guard for the defect the review caught.** The desk lies outside the sheet, so the
     * wash must never touch it. Before the fix this pixel bleached from `#2F2A22`-ish to a light grey on
     * every selection — in the dark theme the whole room lit up whenever the user tapped an element,
     * which is [ADR-090](../../../../../../../../../docs/DECISIONS.md#adr-090) exactly inverted: the room
     * may dim, the artifact may not.
     *
     * Mutation: pass a `pageRect` covering the whole host (the pre-fix behaviour) and this fails.
     */
    @Test
    fun `the wash never reaches the desk outside the sheet`() {
        host()
        val clean = deskPixel(scrim(dim = 0f))
        val washed = deskPixel(scrim(dim = BenchFocusDimAlpha))
        assertEquals("the dim escaped the sheet and bleached the desk", clean, washed)
    }

    /** Re-settles the scrim on the *same* composition and returns the freshly rasterised host. */
    private fun scrim(
        dim: Float,
        hole: List<List<PtPoint>> = emptyList(),
        cover: List<Pair<List<PtPoint>, Float>> = emptyList(),
    ): Bitmap {
        composeRule.runOnUiThread {
            dimAlpha.value = dim
            holes.value = hole
            covers.value = cover
        }
        return bitmap()
    }

    /** The element square, in the device px the scrim's quads are expressed in. */
    private val elementQuad = listOf(
        PtPoint(0.0, 0.0), PtPoint(100.0, 0.0), PtPoint(100.0, 100.0), PtPoint(0.0, 100.0),
    )

    /** The host, cropped out of the rasterised window — `rasterizeToBitmap` is a `View` extension. */
    private fun bitmap(): Bitmap {
        composeRule.waitForIdle()
        val full = composeRule.activity.window.decorView.rasterizeToBitmap()
        val r = composeRule.onNodeWithTag(HOST).fetchSemanticsNode().boundsInWindow
        return Bitmap.createBitmap(full, r.left.toInt(), r.top.toInt(), r.width.toInt(), r.height.toInt())
    }

    /** The pixel well inside the element square, away from every edge the rasteriser softens. */
    private fun elementPixel(b: Bitmap): Int = b.getPixel(b.width / 4, b.height / 4)

    /** The pixel well outside the element but still ON the sheet — bare paper. */
    private fun paperPixel(b: Bitmap): Int = b.getPixel(SHEET - 20, SHEET - 20)

    /** A pixel on the desk, outside the sheet entirely. */
    private fun deskPixel(b: Bitmap): Int = b.getPixel(b.width - 10, b.height - 10)

    /** What CSS `opacity:a` over [under] would land, per channel — the independent expectation. */
    private fun cssComposite(over: Int, under: Int, a: Float): Triple<Int, Int, Int> {
        fun ch(shift: Int): Int {
            val o = (over shr shift) and 0xFF
            val u = (under shr shift) and 0xFF
            return Math.round(a * o + (1 - a) * u)
        }
        return Triple(ch(16), ch(8), ch(0))
    }

    private fun rgb(p: Int) = Triple((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)

    private fun assertNear(expected: Triple<Int, Int, Int>, actual: Int, tolerance: Int, what: String) {
        val (r, g, b) = rgb(actual)
        val (er, eg, eb) = expected
        val worst = maxOf(Math.abs(r - er), Math.abs(g - eg), Math.abs(b - eb))
        assertTrue(
            "$what: expected ≈($er,$eg,$eb) but got ($r,$g,$b) — off by $worst",
            worst <= tolerance,
        )
    }

    /**
     * **Row 2.8, the decisive one.** The dimmed element's landed pixel must equal what the frozen
     * `opacity:.5` would have produced. The expectation is computed from [FROZEN_ELEMENT_OPACITY] and the
     * *undimmed* pixels measured in the same run, so it cannot drift with the production constant.
     *
     * Mutation: `BenchFocusDimAlpha` 0.5 → 0.4 or → 0.6 fails by ~25 per channel; → 0f fails by ~100.
     */
    @Test
    fun `a dimmed element lands on exactly the pixel the frozen opacity would have produced`() {
        host()
        val clean = scrim(dim = 0f)
        val element = elementPixel(clean)
        val paper = paperPixel(clean)

        val dimmed = elementPixel(scrim(dim = BenchFocusDimAlpha))

        assertNear(
            expected = cssComposite(over = element, under = paper, a = FROZEN_ELEMENT_OPACITY),
            actual = dimmed,
            tolerance = 2,
            what = "the dimmed element",
        )
    }

    /**
     * **Row 2.8's other half.** `:not(.sel-focus)` excludes the selected element, so the hole must leave it
     * *bit-identical* — not merely lighter than its neighbours.
     *
     * Mutation: remove the `holes` punch-out and the selected element dims too; assertion fails.
     */
    @Test
    fun `the punched-out selection is left completely untouched by the wash`() {
        host()
        val clean = elementPixel(scrim(dim = 0f))
        // Device px == dp here: Robolectric's default density is 1.
        val held = elementPixel(scrim(dim = BenchFocusDimAlpha, hole = listOf(elementQuad)))

        assertEquals("the selected element must not be washed at all", clean, held)
    }

    /**
     * **Row 2.8a — the recorded deviation, asserted so it stays recorded.** The wash reaches everything
     * outside the hole, including paper. This is the pixel that proves the dim is a *surface* composite
     * rather than a per-element property, which is the whole substance of §2.1's trade.
     */
    @Test
    fun `the wash reaches the bare sheet too, because it is a surface composite and not an element property`() {
        host()
        val cleanPaper = paperPixel(scrim(dim = 0f))
        val washedPaper = paperPixel(scrim(dim = BenchFocusDimAlpha))
        // Paper washed with paper is paper — the deviation is invisible where it lands, which is exactly
        // why the composite is admissible. Asserting it keeps the reasoning falsifiable rather than assumed.
        assertNear(rgb(cleanPaper), washedPaper, tolerance = 2, what = "paper washed with paper")
    }

    /**
     * **Row 2.9's fade.** At `progress = 0` the arriving element is fully covered by paper — invisible,
     * as `opacity:0` demands — and by `progress = 1` it is back to its clean pixel.
     *
     * Mutation: `coverAlphaAt` → constant `0f` and the element is visible at progress 0; the first
     * assertion fails.
     */
    @Test
    fun `the materialise fade hides the arriving element completely at its first frame`() {
        host()
        val cleanFrame = scrim(dim = 0f)
        val clean = elementPixel(cleanFrame)
        val cleanPaper = paperPixel(cleanFrame)

        val first = scrim(dim = 0f, cover = listOf(elementQuad to BenchMaterialise.coverAlphaAt(0f)))
        assertNear(rgb(cleanPaper), elementPixel(first), tolerance = 2, what = "the arriving element at progress 0")

        val last = scrim(dim = 0f, cover = listOf(elementQuad to BenchMaterialise.coverAlphaAt(1f)))
        assertEquals("fully materialised, the element is its own colour again", clean, elementPixel(last))
    }

    /** Guards the guard: without a wash the element is nowhere near the dimmed expectation. */
    @Test
    fun `the probe can tell dimmed from undimmed`() {
        host()
        val clean = scrim(dim = 0f)
        val expectedIfDimmed = cssComposite(elementPixel(clean), paperPixel(clean), FROZEN_ELEMENT_OPACITY)
        val (r, g, b) = rgb(elementPixel(clean))
        val worst = maxOf(
            Math.abs(r - expectedIfDimmed.first),
            Math.abs(g - expectedIfDimmed.second),
            Math.abs(b - expectedIfDimmed.third),
        )
        assertTrue("an undimmed element must not already look dimmed (off by $worst)", worst > 40)
    }

    // ---- The pure half: no frame clock, no rasteriser. ----

    /** Row 2.9's from-scale, read off the production function rather than restated. */
    @Test
    fun `the materialise starts at the frozen scale and ends at one`() {
        assertEquals(0.92f, BenchMaterialise.scaleAt(0f), 0.0001f)
        assertEquals(1f, BenchMaterialise.scaleAt(1f), 0.0001f)
        assertTrue("it must grow, not shrink", BenchMaterialise.scaleAt(0.5f) > BenchMaterialise.scaleAt(0f))
    }

    /**
     * `transform:scale()` scales about the **centre**. A naive width/height multiply anchors at the
     * top-left and the element crawls into place instead of settling into it — visually a different
     * animation, and one no scale-factor assertion would catch.
     *
     * Mutation: drop the `cx - w/2` re-anchoring in [BenchMaterialise.scaledAboutCentre]; the centre moves
     * and this fails while `scaleAt` stays green.
     */
    @Test
    fun `the materialise scales about the element's own centre, not its corner`() {
        val t = Transform(xPt = 40.0, yPt = 80.0, widthPt = 100.0, heightPt = 50.0, rotationDegrees = 0.0)
        val scaled = BenchMaterialise.scaledAboutCentre(t, 0.92f)

        assertEquals("centre x holds", t.xPt + t.widthPt / 2, scaled.xPt + scaled.widthPt / 2, 0.0001)
        assertEquals("centre y holds", t.yPt + t.heightPt / 2, scaled.yPt + scaled.heightPt / 2, 0.0001)
        assertEquals("width scales", 92.0, scaled.widthPt, 0.0001)
        assertNotEquals("and the corner therefore moves", t.xPt, scaled.xPt, 0.0001)
        assertEquals("rotation is untouched", t.rotationDegrees, scaled.rotationDegrees, 0.0001)
    }
}
