package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.ResizeHandle
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.golden.cropToBounds
import com.aritr.zinely.ui.golden.pixelCountOf
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * C3 — the frozen in-place editing surface, the rigid page pan and the editing-state style row
 * ([ADR-093](../../../../../../../docs/DECISIONS.md#adr-093) rows 3.1–3.13).
 *
 * The rows this suite cannot close are named where they are skipped rather than quietly omitted: **3.2**
 * (the pixel-identical return) and the clearance half of **3.1a** are hardware gates, and the line-breaking
 * residue of **3.11** is a device question — all three are device-verification items by the ADR's own text.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchC3Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)

    /**
     * A text box the frozen pan leaves **fully on screen**, so a test about something else is not
     * confounded by [D-043](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-043).
     *
     * The arithmetic is worth writing down, because getting it wrong is how an assertion turns into
     * decoration. On [Hosts.SNUG] the canvas measures ~132dp for a 100pt page, i.e. **1.32 dp/pt**.
     *
     * It was chosen when the pan was an unconditional 96dp, which on that host removed everything above
     * page-space y ≈ 73pt — hence 76pt. **Since [OD-16] that host pans by 0dp and any box would do**, and
     * the value is kept rather than re-derived precisely because it is now over-safe: it is still fully
     * visible on all three hosts, so the tests that use it keep meaning what they meant on the day they
     * were written. A box placed higher used to pass `assertIsDisplayed` on a sliver, which is the trap the
     * first cut of this suite fell into and the reason this comment exists at all.
     */
    private val midPageBox = Transform(20.0, 76.0, 60.0, 18.0)

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    /**
     * The three hosts this suite needs, and why there are three rather than one.
     *
     * Since [OD-16](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-043-ruling) the pan is
     * `min(96dp, slack + clearance)`, so **the host's geometry is now an input to the property under test**
     * and a single host can only ever exercise one branch of it. Each of these picks a branch deliberately:
     *
     * | host | canvas | page | slack above the page | the pan it produces |
     * |---|---|---|---|---|
     * | [SNUG] `300×400` | 132dp tall | fills it | **0dp** | **0dp** — the shipped device's shape |
     * | [ROOMY] `360×720` | 452dp tall | 360dp | **46dp** | **46dp** — the clamp below the ceiling |
     * | [CAVERNOUS] `360×1200` | 623dp tall | 360dp | **132dp** | **96dp** — the ceiling itself |
     *
     * `SNUG` is the honest model of a real phone (a portrait page is height-bound, so it fills the canvas
     * and there is nothing above it), which is exactly why D-043 bit there and why the frozen `−96` had
     * nothing to spend. `CAVERNOUS` is the only host on which the frozen literal is still *reachable*, so
     * it is the one that keeps row 3.1's `−96 → −48` mutation alive.
     */
    private object Hosts {
        val SNUG = 300.dp to 400.dp
        val ROOMY = 360.dp to 720.dp
        val CAVERNOUS = 360.dp to 1200.dp
    }

    private fun setScreen(
        store: EditorStore,
        host: Pair<Dp, Dp> = Hosts.SNUG,
        withTopBar: Boolean = false,
    ) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    // The `Preview` row is composed only when the host supplies a destination, and without
                    // it the canvas begins at the window's own top edge — which makes "paints above the
                    // canvas" mean "paints off the window", i.e. unobservable. Only the clip test needs it.
                    onPreview = if (withTopBar) ({}) else null,
                    modifier = Modifier.size(host.first, host.second),
                )
            }
        }
    }

    /** Places one text box and returns its id, with the screen mounted and idle. */
    private fun placedText(
        store: EditorStore,
        box: Transform = midPageBox,
        host: Pair<Dp, Dp> = Hosts.SNUG,
        withTopBar: Boolean = false,
    ): String {
        store.dispatch(Intent.PlaceText(box, "hi"))
        val id = store.uiState.value.selection.single()
        setScreen(store, host, withTopBar)
        composeRule.waitForIdle()
        return id
    }

    private fun paperBounds() =
        composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot

    private fun canvasBounds() =
        composeRule.onNodeWithTag(EditorCanvasTestTag).fetchSemanticsNode().boundsInRoot

    // --- Row 3.1 / 3.1a: the amended pan RULE, in isolation ---------------------------------------
    //
    // OD-16 turned the frozen literal into `min(96, slack + clearance)`, and a rule is testable in a way a
    // rendered displacement is not: these run on literal numbers with no host, no density and no layout, so
    // they say what the arithmetic IS rather than what one canvas happened to produce. They also carry the
    // half of the rule Robolectric structurally cannot reach: the clearance term needs an occluder INSIDE
    // the canvas, which on a device is the IME, and Robolectric has no IME. There is therefore no
    // screen-level test of that term anywhere — it is carried here and by
    // [device checklist](../../../../../../../docs/DECISIONS.md#adr-093-device-checklist) item 11.

    @Test
    fun the_pan_never_exceeds_the_frozen_maximum() {
        // The freeze's number survives as a ceiling — `v2-bench.html` `edit()` as amended, 2026-08-03.
        // Literal 96, not `BenchEditPanDp`: an assertion written against the constant under test cannot
        // fail when that constant changes.
        assertEquals(96f, benchEditPanMagnitudeDp(96f, 400f, 900f, 100f), 0.001f)
        assertEquals(96f, benchEditPanMagnitudeDp(96f, 96.5f, 0f, 0f), 0.001f)
    }

    @Test
    fun the_pan_spends_the_slack_above_the_page() {
        // First term: the empty canvas band. Free to spend — the page cannot leave a canvas it has not
        // reached the top of — and it is the ONLY term on a screen where nothing occludes the element.
        assertEquals(46f, benchEditPanMagnitudeDp(96f, 46f, 0f, 500f), 0.001f)
    }

    @Test
    fun the_pan_adds_only_what_the_element_needs_to_clear_the_row() {
        // Second term: how far the edited box's bottom sits BELOW the docked `.kbstack`. Zero when it is
        // already clear — which is the whole of D-043's remedy, because that is the top-of-page case.
        assertEquals(0f, benchEditPanMagnitudeDp(96f, 0f, 100f, 400f), 0.001f)
        assertEquals(0f, benchEditPanMagnitudeDp(96f, 0f, 400f, 400f), 0.001f)
        assertEquals(30f, benchEditPanMagnitudeDp(96f, 0f, 430f, 400f), 0.001f)
        assertEquals(34f, benchEditPanMagnitudeDp(96f, 4f, 430f, 400f), 0.001f)
    }

    @Test
    fun the_pan_is_never_negative() {
        // A lift is a lift. Negative slack (a page larger than its canvas) and an already-clear element
        // must not add up to the page being pushed DOWN into the row it is trying to escape.
        assertEquals(0f, benchEditPanMagnitudeDp(96f, -50f, 0f, 400f), 0.001f)
        assertEquals(0f, benchEditPanMagnitudeDp(0f, 400f, 900f, 0f), 0.001f)
    }

    // --- Row 3.1 / 3.3: the rigid page pan ------------------------------------------------------

    @Test
    // The default Robolectric window is smaller than [Hosts.CAVERNOUS], and a `Modifier.size` larger
    // than its window is silently constrained back to it — which quietly turns this host into SNUG and
    // the assertion below into "the pan is 0". The qualifier is the host.
    @Config(qualifiers = "w420dp-h1300dp")
    fun entering_edit_pans_the_whole_page_by_the_frozen_ninety_six_dp() {
        // Row 3.1: frozen `edit()` sets `pageWrap.transform = translateY(-96px)` (`v2-bench.html:551`).
        // Measured on the paper surface, which IS the canonical page geometry (D-033).
        //
        // On CAVERNOUS, and that is the point rather than a convenience: since OD-16 the literal is a
        // ceiling, so it is only *reachable* where `slack + clearance` exceeds it. This host has 132dp of
        // slack against a 96dp ceiling, so the amended rule must still produce exactly the frozen number —
        // which is what keeps row 3.1's `−96 → −48` mutation alive after the amendment.
        val store = store()
        val id = placedText(store, host = Hosts.CAVERNOUS)
        val rest = paperBounds()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        val panned = paperBounds()

        // The literal frozen number, NOT `BenchEditPanDp` — an assertion written against the constant it
        // is testing cannot fail when that constant changes, which is the whole point of row 3.1's
        // `−96 → −48` mutation. The first cut of this test did exactly that and the mutation survived it.
        val expected = with(composeRule.density) { (-96).dp.toPx() }
        // Sub-pixel, and that tolerance is load-bearing in a way the number alone does not show. The `else
        // 0.dp → else (-1).dp` mutation is an EQUIVALENT MUTANT for `ending_the_session_returns_the_page_to_rest`
        // — that test compares rest-before with rest-after, and the mutation shifts both by the same 1dp, so
        // no tolerance can catch it there. It is only visible HERE, as a displacement of 95dp instead of 96.
        // At a 1f tolerance that is exactly swallowed; at 0.25f it is not.
        assertEquals(expected, panned.top - rest.top, 0.25f)
    }

    @Test
    // Same trap the CAVERNOUS tests carry: the default Robolectric window is 320x470dp, and a
    // `Modifier.size` larger than its window is silently constrained back to it. Left unqualified,
    // ROOMY collapses to a height-bound page with ZERO slack — and this test then asserts
    // `assertEquals(-0f, 0f)`, which is exactly the kind of green nothing this suite exists to catch.
    @Config(qualifiers = "w420dp-h800dp")
    fun the_pan_spends_only_the_slack_when_the_ceiling_is_out_of_reach() {
        // Row 3.1 as amended by OD-16, at screen level and on the branch a real phone actually takes. On
        // ROOMY the band above the sheet is 46dp and nothing occludes the element, so the whole rule
        // evaluates to 46 — NOT the frozen 96, and that difference is the entire remedy for D-043.
        //
        // The expected value is READ FROM THE LAYOUT (the sheet's own rest inset inside the canvas), not
        // written down as 46: a literal here would assert this host's arithmetic rather than the rule, and
        // would have to be re-derived every time the sheet or the tray changes height. What is asserted is
        // the *relation* — the page rises by exactly the empty band above it, and stops.
        val store = store()
        val id = placedText(store, host = Hosts.ROOMY)
        val rest = paperBounds()
        val slack = rest.top - canvasBounds().top

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        val panned = paperBounds()

        val ceiling = with(composeRule.density) { 96.dp.toPx() }
        // Both guards BEFORE the assertion, and both are load-bearing. `slack > 0` is the one that was
        // missing: without it a host silently shrunk to its window has zero slack, and `assertEquals(-0f,
        // 0f)` passes while proving nothing at all. `slack < ceiling` keeps this test distinct from the
        // ceiling test above.
        assertTrue("this host must HAVE slack or the assertion is 0 == 0 (slack $slack)", slack > 1f)
        assertTrue("this host must sit BELOW the ceiling or it tests nothing new (slack $slack)",
            slack < ceiling)
        assertEquals(-slack, panned.top - rest.top, 0.25f)
    }

    @Test
    // Same trap the CAVERNOUS tests carry: the default Robolectric window is 320x470dp, and a
    // `Modifier.size` larger than its window is silently constrained back to it. Left unqualified,
    // ROOMY collapses to a height-bound page with ZERO slack — and this test then asserts
    // `assertEquals(-0f, 0f)`, which is exactly the kind of green nothing this suite exists to catch.
    @Config(qualifiers = "w420dp-h800dp")
    fun a_top_of_page_element_is_not_lifted_out_of_the_canvas_while_it_is_edited() {
        // D-043 inverted, and the reason this package was reopened. The old unconditional −96 took a box
        // near the page top clean off the canvas — `EditorScreenTest`'s companion test used to assert
        // exactly that failure. Under OD-16 the lift can never exceed the slack unless clearance demands
        // it, and clearance is zero for a box this high, so the page cannot leave its own canvas.
        val store = store()
        val id = placedText(store, box = Transform(20.0, 4.0, 60.0, 18.0), host = Hosts.ROOMY)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        val canvas = canvasBounds()
        val field = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("the edited box (top ${field.top}) must stay inside the canvas (top ${canvas.top})",
            field.top >= canvas.top)
        assertTrue("and the page's own top must not leave it either", paperBounds().top >= canvas.top)
        composeRule.onNodeWithTag(EditTextSessionTestTag).assertIsDisplayed()
    }

    @Test
    fun the_canvas_clips_whatever_leaves_it() {
        // D-045: the frozen `.canvasArea{overflow:hidden}` (`v2-bench.html:171`), which this host never
        // implemented. Asserted on the raster, because a clip is a painting property and the semantics
        // tree reports unclipped bounds either way — a `boundsInRoot` assertion here would pass with the
        // modifier deleted, which is the definition of decoration.
        //
        // The overflow used is real product content rather than a contrived one: on SNUG the sheet FILLS
        // the canvas (`centreOffset.y` is `coerceAtLeast(0.0)`, so page-top == canvas-top), and a selected
        // element sitting at the page's TOP edge puts its **selection chrome** above the canvas — the ring
        // at `SelectionOutlineInsetDp` 5.2dp, and half of each 12dp handle halo — over the `Preview` bar.
        // That is the same defect the panned page produced on device (paper on the chrome), in the one
        // direction Robolectric can be made to show.
        //
        // ⚠ P1 shrank the halo envelope 16dp → 12dp, so the headroom this test works in fell from ~8dp to
        // ~6dp. It is still several pixel rows, but a package that shrinks the chrome again should re-read
        // this test rather than assume it still has something to see.
        //
        // The direction is not a detail, and the first cut got it wrong. Aimed DOWNWARD the test was pure
        // decoration and the `clipToBounds()` mutation survived it: the supply sheet is a later sibling in
        // the Column with an opaque ground, so it paints over anything the canvas spills downward whether
        // the canvas clips or not. Upward there is nothing to cover the spill — which is exactly why the
        // device saw paper on the top bar and not under the tray.
        //
        // `withTopBar` is likewise load-bearing: without a `Preview` destination the canvas starts at the
        // window's top edge, so "above the canvas" is off-window and unobservable either way.
        //
        // ⚠ What this does NOT cover, and is not claimed to: the hardware case is the panned PAGE
        // overflowing, which needs the pan to exceed the slack, which needs the clearance term, which needs
        // an IME. Robolectric has none. That is
        // [device checklist](../../../../../../../docs/DECISIONS.md#adr-093-device-checklist) item 10.
        //
        // **The probe is a DIFFERENCE, not a colour — and it is the third probe this test has had.** Two
        // palette probes were falsified in a row, and the pattern is the finding:
        //
        //  - `--matcha` +/-24, justified as *"above the canvas the only things drawn are the `Preview`
        //    bar's dark ground and its coral label."* P1 recoloured that label to `leafText` `#3E6330`
        //    for an unrelated contrast fix and its antialiased edges walked through the window: 32 false
        //    hits on a canvas that clips correctly.
        //  - `--paper` +/-24, chosen as *"the brightest thing the canvas holds against the darkest thing
        //    above it."* V2.1 writes `background:var(--paper)` on the chrome above the canvas too, so the
        //    probe found 1460 pixels of top bar and called them spill.
        //
        // Any probe whose discriminator is *"nothing else on screen is this colour"* is one re-skin away
        // from lying, in either direction, because chrome and artifact share one palette by design. So the
        // screen is rasterised twice — once holding the overflowing element, once after deleting it — and
        // the claim becomes what it always meant: **the element's presence must change nothing above the
        // canvas.** That is true in every palette, and it needs no theory about what else is on screen.
        val store = store()
        val id = placedText(store, box = Transform(20.0, 0.0, 60.0, 12.0), withTopBar = true)
        val canvas = canvasBounds()
        assertTrue("the canvas must start below the window top or this test is vacuous (${canvas.top})",
            canvas.top > 1f)

        val held = composeRule.activity.window.decorView.rasterizeToBitmap()
        store.dispatch(Intent.Delete(setOf(id)))
        composeRule.waitForIdle()
        val gone = composeRule.activity.window.decorView.rasterizeToBitmap()

        // Up to the row BEFORE the boundary row: `canvas.top` is fractional, so the pixel row it lands in
        // is legitimately part-canvas and antialiases against what is under it.
        val lastCleanRow = (canvas.top.toInt() - 1).coerceAtLeast(0)
        fun differing(fromY: Int, toY: Int): Int {
            var n = 0
            for (y in fromY until toY) for (x in 0 until held.width) {
                if (held.getPixel(x, y) != gone.getPixel(x, y)) n++
            }
            return n
        }
        // Vacuity guard, and the one the colour probes could not carry: deleting the element must visibly
        // change **the canvas**. A placement that missed the page, a Delete that no-opped, or a clip that
        // erased the element outright all leave the rows above the canvas identical for the wrong reason.
        //
        // Bounded to the canvas on purpose. A first cut swept every row below the boundary, which includes
        // the style row, the context bar and the tray — all of which repaint on the selection change Delete
        // causes whether or not the element ever rendered on the page. That guard would have passed on an
        // empty canvas, i.e. it guarded nothing it named.
        assertTrue(
            "deleting the element changed nothing INSIDE the canvas — the overflow this test needs never " +
                "rendered, so the assertion below would pass on an empty page",
            differing(canvas.top.toInt() + 1, canvas.bottom.toInt().coerceAtMost(held.height)) > 0,
        )
        assertEquals("nothing the canvas holds may paint above the canvas", 0, differing(0, lastCleanRow))
    }

    @Test
    // The default Robolectric window is smaller than [Hosts.CAVERNOUS], and a `Modifier.size` larger
    // than its window is silently constrained back to it — which quietly turns this host into SNUG and
    // the assertion below into "the pan is 0". The qualifier is the host.
    @Config(qualifiers = "w420dp-h1300dp")
    fun the_pan_is_rigid_the_page_does_not_resize_or_reflow() {
        // The other half of row 3.1, and the whole of row 3.11: it is a TRANSLATION. If the page changed
        // size the render, the chrome and the gesture surface would have re-laid-out at different times
        // and drifted apart — the desync the row exists to forbid. Same width and height, moved only in y.
        // CAVERNOUS, so there IS a displacement to be rigid about: on the shipped device's own shape the
        // amended pan is 0dp and "it moved without reflowing" degenerates into "it did not move".
        val store = store()
        val id = placedText(store, host = Hosts.CAVERNOUS)
        val rest = paperBounds()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        val panned = paperBounds()

        assertTrue("this test is vacuous without a pan", rest.top - panned.top > 1f)
        assertEquals(rest.width, panned.width, 0.5f)
        assertEquals(rest.height, panned.height, 0.5f)
        assertEquals(rest.left, panned.left, 0.5f)
    }

    @Test
    // The default Robolectric window is smaller than [Hosts.CAVERNOUS], and a `Modifier.size` larger
    // than its window is silently constrained back to it — which quietly turns this host into SNUG and
    // the assertion below into "the pan is 0". The qualifier is the host.
    @Config(qualifiers = "w420dp-h1300dp")
    fun ending_the_session_returns_the_page_to_rest() {
        // Row 3.2's testable half. The *pixel-identical* claim is a device gate (a raster byte-compare);
        // what a unit test can hold is that the geometry returns exactly, with no residue.
        // CAVERNOUS for the same reason as the rigidity test: a return-to-rest assertion on a host whose
        // pan is 0dp passes whatever `endEdit` does.
        val store = store()
        val id = placedText(store, host = Hosts.CAVERNOUS)
        val rest = paperBounds()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        assertTrue("this test is vacuous without a pan", rest.top - paperBounds().top > 1f)
        composeRule.onNodeWithTag("$BenchStyleRowTestTag-done").performClick()
        composeRule.waitForIdle()

        val returned = paperBounds()
        // Sub-pixel, deliberately. "Returns to rest" is an EXACT property — the frozen word is
        // *pixel-identical* — so any tolerance wide enough to swallow a whole dp is wide enough to swallow
        // a real residue. At this host's density 1dp is 1px, and a 1f tolerance let the `else (-1).dp`
        // mutation survive the whole suite. The animation settles on the target value, so 0.25f is not
        // tight-fitting; it is simply not loose.
        assertEquals(rest.top, returned.top, 0.25f)
        assertEquals(rest.left, returned.left, 0.25f)
    }

    @Test
    // The default Robolectric window is smaller than [Hosts.CAVERNOUS], and a `Modifier.size` larger
    // than its window is silently constrained back to it — which quietly turns this host into SNUG and
    // the assertion below into "the pan is 0". The qualifier is the host.
    @Config(qualifiers = "w420dp-h1300dp")
    fun the_pan_rides_the_frozen_settle_curve_not_a_linear_one() {
        // Row 3.3: `.pageWrap{transition:transform .34s var(--settle)}` (`:172`), `--settle` =
        // `cubic-bezier(.05,.7,.1,1)` (`:111`). Read MID-flight, because an end-state assertion cannot tell
        // a settle from a linear ramp from an instant jump — all three arrive at the same place.
        //
        // The curve is strongly front-loaded: at 50 % of the duration it has covered far more than 50 % of
        // the distance. That is the property, and it is what a `--standard` swap would break.
        // CAVERNOUS: the sampling below is a FRACTION of the total distance, so the total has to be a
        // known, non-zero one. Here the amended rule still yields the frozen 96dp, which is why the
        // denominator on the next page can stay the literal.
        val store = store()
        val id = placedText(store, host = Hosts.CAVERNOUS)
        // The clock is frozen only AFTER the host has settled. Stopped from the start, the viewport push
        // (`LaunchedEffect` → `SetViewport`) has not landed a scale by the time `rest` is read, so "rest"
        // is a pre-layout position and the fraction below is measured against that jump rather than
        // against the pan. On the old SNUG host the two happened to coincide at zero and it did not show.
        composeRule.mainClock.autoAdvance = false
        val rest = paperBounds().top

        store.dispatch(Intent.BeginEditText(id))
        composeRule.mainClock.advanceTimeByFrame() // let the animation start
        // Sampled at a QUARTER, not a half. Both curves are front-loaded, and by 50 % they have converged:
        // settle is at 0.950 and `--standard` at 0.878, so a `> 0.8` assertion there passes for both and
        // the swap mutation survived it. At 25 % they are 0.832 vs 0.607 — far enough apart that frame
        // quantisation (±1 frame ≈ ±5 % of a 340ms tween) cannot close the gap.
        // The LITERAL frozen 340ms, quartered — not `BenchEditPanMillis / 4`, which samples the animation at
        // a quarter of whatever duration it happens to have and therefore cannot see the duration change at
        // all. The re-review's `340 → 680` mutation survived the constant-relative form.
        composeRule.mainClock.advanceTimeBy(340L / 4L)
        val quarter = paperBounds().top

        val total = with(composeRule.density) { (-96).dp.toPx() }
        val covered = (quarter - rest) / total
        assertTrue("at a quarter of the duration the pan has covered $covered, expected > 0.75 " +
            "(--settle reaches 0.832 here; --standard only 0.607)", covered > 0.75f)
        assertTrue("it has not already finished — an instant jump is not a settle", covered < 1f)

        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun the_edited_element_clears_the_style_row_after_the_pan() {
        // Row 3.1a's **functional** half, which the first cut of C3 left unbuilt: the pan exists to make
        // room, so the edited element's box must end up above the `.kbstack`, not merely displaced by a
        // literal 96dp. (The IME's own height is a device question; what a unit test can hold is that the
        // element clears the row that docks on top of it.)
        //
        // ⚠ **This test does NOT assert the clamp, and must not be cited as though it did.** The row is a
        // screen-level overlay at the bottom of the host while the sheet occupies only the canvas above it,
        // so on every host in this suite the two are separated by the whole supply tray and the comparison
        // is true whatever the pan does. It is a **layering** assertion — the editing row and the edited
        // box are never stacked on top of each other — and that is all it is worth.
        //
        // The clearance term that would make this bite needs an occluder INSIDE the canvas, i.e. the IME.
        // Robolectric has none. An earlier draft of ADR-093 row 3.1a listed this test as "row 3.1a's own
        // clearance assertion"; that claim is withdrawn, and checklist item 11 carries the property.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        val field = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag(BenchStyleRowTestTag).fetchSemanticsNode().boundsInRoot
        assertTrue("the edited box (bottom ${field.bottom}) must clear the style row (top ${row.top})",
            field.bottom <= row.top)
        assertTrue("and it must still be on screen", field.top >= 0f)
    }

    @Test
    fun the_field_is_placed_on_the_elements_own_box() {
        // Row 3.11: editing happens IN PLACE. The field's rect is the element's rect mapped through the
        // shared `(pagePt + pageOffset) × scale` seam, then carried by the pan — so this is both the
        // placement assertion and the proof that the field rides the pan with the page rather than
        // floating over it.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        val view = store.uiState.value.view
        val paper = paperBounds()
        val field = composeRule.onNodeWithTag(EditTextSessionTestTag).fetchSemanticsNode().boundsInRoot

        // Measured from the PANNED PAPER's own top rather than from the canvas origin plus a pan literal.
        // Since OD-16 the pan is a function of the host's geometry, so a hard-coded −96 here would be
        // asserting the old unconditional rule under the name of a placement test — and would pass or fail
        // for reasons that have nothing to do with whether the field lands on its element. Anchoring to the
        // paper is also the stronger claim: the field rides the pan WITH the page, whatever the pan is.
        val expectedTop = paper.top + (midPageBox.yPt * view.screenPxPerPt).toFloat()
        assertEquals(expectedTop, field.top, 1.5f)
        assertEquals((midPageBox.widthPt * view.screenPxPerPt).toFloat(), field.width, 1.5f)
        assertTrue("the field sits on the paper, not beside it", field.left >= paper.left - 1f)
    }

    /**
     * Counts pixels that are recognisably the probe ink, rather than exactly equal to it.
     *
     * Exact equality is the right test on a flat fill and the wrong one on **glyphs**: text is antialiased
     * against the paper, and the editor's focus wash sits over it, so a magenta `M` at 14pt contributes a
     * spread of near-magentas and very few pure ones. A predicate wide enough to see the glyph and narrow
     * enough that no paper, ink, chrome or matcha tone can satisfy it is the honest reading — nothing else
     * on this canvas is simultaneously red-high, blue-high and green-low.
     */
    private fun android.graphics.Bitmap.probeInkPixels(): Int {
        var n = 0
        for (y in 0 until height) for (x in 0 until width) {
            val p = getPixel(x, y)
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (r > 150 && b > 150 && g < 110) n++
        }
        return n
    }

    @Test
    fun the_screen_hands_the_edited_elements_id_to_the_tape() {
        // Row 3.11's **wiring**, a separate property from the suppression itself and asserted by nothing
        // until now: `EditorPagePreviewTest` proves the tape honours `hiddenElementId`, and this proves the
        // screen actually passes one. Replacing the call site with `hiddenElementId = null` survived the
        // entire suite AND the golden gate.
        //
        // Why it survived, and why this test is shaped the way it is: the field and the tape draw the same
        // words at the same rect, so with suppression off the two draws land on top of each other and no
        // raster can tell them apart. So make them DIFFER — open the session, then empty the draft. The
        // document still holds the original text (the draft commits on Done), so an unsuppressed tape keeps
        // painting it while the field paints nothing.
        val store = store()
        store.dispatch(Intent.PlaceText(midPageBox, "MMMM"))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.StyleText(id = id, sizePt = 14.0, color = ColorRgba(255, 0, 255)))
        // Deselected for the baseline: a live selection draws the focus wash over the page, which mutes the
        // probe toward the paper and would make the "before" reading a fraction of what the tape drew.
        store.dispatch(Intent.ClearSelection)
        setScreen(store)
        composeRule.waitForIdle()

        val canvas = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        val before = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), canvas)
            .probeInkPixels()
        assertTrue("the probe ink never painted at rest — the test cannot detect anything ($before px)",
            before > 100)

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("")
        composeRule.waitForIdle()

        val panned = composeRule.onNodeWithTag(EditorPaperSurfaceTestTag).fetchSemanticsNode().boundsInRoot
        val during = cropToBounds(composeRule.activity.window.decorView.rasterizeToBitmap(), panned)
            .probeInkPixels()
        assertEquals(
            "the tape is still drawing the edited element — the screen did not pass its id",
            0, during,
        )
    }

    // --- Rows 3.4–3.7, 3.9: the frozen style row ------------------------------------------------

    @Test
    fun the_style_row_is_absent_at_rest_and_docked_while_editing() {
        // Row 3.4: `.kbstack` is `translateY(102%)` — offscreen — except under `.editing`. Absent rather
        // than merely translated: a `graphicsLayer` moves pixels, not nodes, and four controls left in the
        // accessibility tree at rest is a defect (`SurfaceTraversalOrderTest` reads that tree directly).
        val store = store()
        val id = placedText(store)
        composeRule.onNodeWithTag(BenchStyleRowTestTag).assertDoesNotExist()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchStyleRowTestTag).assertIsDisplayed()
    }

    @Test
    fun three_of_the_four_style_controls_are_inert_and_say_so() {
        // Row 3.6. `Fraunces`, `A 23` and `Ink` are wired to NOTHING in the freeze — `#editColour` included,
        // because `openInk` is bound only in `buildCtx` to the `.ctx` bar's Ink verb. They ship drawn and
        // disabled under OD-9, and the announcement must agree with the paint (ADR-092 row 2.13c-i).
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("$BenchStyleRowTestTag-${Copy.BenchVerbs.FONT}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchStyleRowTestTag-${Copy.BenchVerbs.SIZE}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchStyleRowTestTag-${Copy.BenchVerbs.INK}").assertIsNotEnabled()
        composeRule.onNodeWithTag("$BenchStyleRowTestTag-done").assertIsEnabled()
    }

    @Test
    fun done_is_right_anchored_past_the_grow_spacer() {
        // Row 3.6's `.grow{flex:1}`: the chips pack left, Done anchors right. Asserted as a gap rather than
        // a coordinate, so it survives a font-scale change that moves every chip.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        val ink = composeRule.onNodeWithTag("$BenchStyleRowTestTag-${Copy.BenchVerbs.INK}")
            .fetchSemanticsNode().boundsInRoot
        val done = composeRule.onNodeWithTag("$BenchStyleRowTestTag-done").fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithTag(BenchStyleRowTestTag).fetchSemanticsNode().boundsInRoot

        assertTrue("Done sits after the Ink chip", done.left > ink.right)
        // The spacer is real: the gap before Done is wider than the row's own 8dp inter-chip gap.
        val gap = done.left - ink.right
        assertTrue("the .grow spacer separates them", gap > with(composeRule.density) { BenchStyleRowGap.toPx() })
        assertTrue("Done reaches the row's trailing edge", row.right - done.right < with(composeRule.density) {
            (BenchStyleRowPaddingH + 1.dp).toPx()
        })
    }

    @Test
    fun the_chips_are_spaced_by_the_frozen_eight_px_gap() {
        // Frozen `.styletb{gap:var(--gap-sm)}` (`v21-bench.html:267`) — asserted on the chips' ANNOUNCED bounds, which
        // makes it two assertions in one. The gap is a frozen property in its own right; and because a
        // chip's node covers its padding only when `testTag` sits above `.padding`, a chip that reports its
        // inner content box instead shows this gap as 6 + 12 + 12 = 30dp. That defect shipped once — the
        // chips announced 28dp wide where they are drawn 52dp, so TalkBack's focus rectangle was 24dp
        // narrower than the control it outlined — and nothing here could see it until this test existed.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        fun chip(label: String) = composeRule
            .onNodeWithTag("$BenchStyleRowTestTag-$label", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

        val expected = with(composeRule.density) { 8.dp.toPx() }
        val font = chip(Copy.BenchVerbs.FONT)
        val size = chip(Copy.BenchVerbs.SIZE)
        val ink = chip(Copy.BenchVerbs.INK)
        assertEquals("Font→Size gap", expected, size.left - font.right, 1f)
        assertEquals("Size→Ink gap", expected, ink.left - size.right, 1f)
        // ⚠ V2.1's chips declare NO height (`v21-bench.html:269-272`), where V2 pinned `height:34px`.
        assertEquals("the chips share one content-derived height", font.height, ink.height, 0.5f)
        // ⚠ **The line above is a sibling comparison, and on its own it proves nothing about the property
        // it is named for.** Re-pinning `height:34.dp` makes both chips 34dp and it still passes; so do the
        // padding readings below, because a 34dp chip around a 15dp swatch leaves 9.5dp top and bottom,
        // which clears the 8dp floor and is perfectly centred. A review pointed out that **no assertion in
        // this file would have failed if the frozen "no declared height" regressed to a pinned one** — the
        // comment here even conceded the gap and then pointed at the checks that do not close it.
        //
        // The chip is its content plus 8dp above and below; against a 15dp swatch and a 12.48sp line that
        // lands near 31–32dp and cannot reach 34 without the type growing. So the pinned value is excluded
        // by name. Written against the LITERAL 34dp rather than a constant, for the reason the swatch test
        // below gives: an expectation phrased in terms of the code under test walks through mutations.
        assertNotEquals(
            "the chip height is derived, not pinned — 34dp is V2's `height:34px` returning",
            with(composeRule.density) { 34.dp.toPx() },
            ink.height,
            1f,
        )

        // Frozen `.styletb .chip{padding:0 12px}` (`:262`), measured from the chip's announced left edge
        // to the swatch — the only landmark inside a chip that keeps its own node, since
        // `clearAndSetSemantics` erases the label's. One assertion, two defects: it reads 0 if the
        // padding is dropped, and 0 again if `testTag` slips back below `.padding` so the node reports
        // the inner content box. Both were live until this line existed.
        val swatch = composeRule.onNodeWithTag("$BenchStyleRowTestTag-swatch", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals("the Ink chip's leading padding", with(composeRule.density) { 12.dp.toPx() },
            swatch.left - ink.left, 1f)

        // Frozen `.chip{padding:var(--gap-sm) var(--gap-md)}` — the VERTICAL half, which V2 never had to
        // assert because a pinned `height:34px` made it decorative. It is load-bearing now: with the height
        // gone, deleting the vertical padding is what makes the chip collapse onto its own text. Asserted as
        // a floor plus a centring, because whether the swatch or the label is the taller child is a font
        // question, and the padding is the same either way.
        val padV = with(composeRule.density) { 8.dp.toPx() }
        assertTrue("the chip pads its content vertically (${swatch.top - ink.top}px)",
            swatch.top - ink.top >= padV - 1f)
        assertEquals("and the content sits centred in it",
            swatch.top - ink.top, ink.bottom - swatch.bottom, 1.5f)
    }

    @Test
    fun the_ink_swatch_is_the_frozen_fifteen_px_dot() {
        // Row 3.9: `editSw.style.background = getComputedStyle(t).color`. The chip is
        // inert but the dot is not decorative — it is the one thing about it that tells the truth.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.StyleText(id = id, color = com.aritr.zinely.core.model.ColorRgba(0xA6, 0x3C, 0x22)))
        composeRule.waitForIdle()
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        // The frozen `.chip .sw{width:15px;height:15px}` (`v21-bench.html:279`) — V2 drew 14 — asserted
        // against the LITERAL 15dp. Written against `BenchStyleSwatchSize` it was circular: the expectation
        // was the constant under test, so the re-review's `14.dp → 22.dp` mutation walked straight through
        // it. This test is named for what it actually holds, too: the dot's *colour* (row 3.9's real
        // subject) is a painted property and is asserted on the raster in `BenchStyleRowGoldenTest`, which
        // is the only honest place for it.
        val sw = composeRule.onNodeWithTag("$BenchStyleRowTestTag-swatch", useUnmergedTree = true)
            .fetchSemanticsNode().size
        assertEquals(with(composeRule.density) { 15.dp.roundToPx() }, sw.width)
        assertEquals(with(composeRule.density) { 15.dp.roundToPx() }, sw.height)
    }

    // --- Row 3.8: the caret -----------------------------------------------------------------------

    @Test
    fun the_caret_blinks_as_a_square_wave_and_holds_still_under_reduced_motion() {
        // Row 3.8: `blink 1.05s steps(1)` — full on for half the period, full off for the other half, no
        // interpolation. Under reduced motion it does not run at all (D-012 is open and C9's; this is the
        // one reading all three frozen files agree on).
        assertEquals(1f, benchCaretAlphaAt(0, reduceMotion = false), 0f)
        assertEquals(1f, benchCaretAlphaAt(524, reduceMotion = false), 0f)
        assertEquals(0f, benchCaretAlphaAt(525, reduceMotion = false), 0f)
        assertEquals(0f, benchCaretAlphaAt(1049, reduceMotion = false), 0f)
        assertEquals(1f, benchCaretAlphaAt(1050, reduceMotion = false), 0f)

        for (t in longArrayOf(0, 300, 525, 900, 1050, 9999)) {
            assertEquals("still at ${t}ms", 1f, benchCaretAlphaAt(t, reduceMotion = true), 0f)
        }
    }

    // --- Row 3.10: Done returns to Selected -------------------------------------------------------

    @Test
    fun done_ends_the_session_and_returns_the_element_to_selected() {
        // Row 3.10: `endEdit()` brings the `.ctx` bar back (`:561`) — i.e. the post-edit state is Selected,
        // not Rest. The selection surviving the commit is what makes that true.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("$BenchStyleRowTestTag-done").performClick()
        composeRule.waitForIdle()

        assertTrue(store.uiState.value.interaction !is Interaction.EditingText)
        assertEquals(setOf(id), store.uiState.value.selection)
    }

    @Test
    fun done_commits_the_draft_rather_than_discarding_it() {
        // The reason Done clears focus instead of dispatching an end: the draft is feature-ephemeral and
        // lives inside the session composable, so an end dispatched from outside would close the session
        // first and the field's dispose-commit would be rejected by its own token guard — silently losing
        // whatever was typed. This test is the guard on that reasoning.
        val store = store()
        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("edited")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("$BenchStyleRowTestTag-done").performClick()
        composeRule.waitForIdle()

        val text = (store.uiState.value.document.pages[0].elements
            .first { it.id == id } as TextElement).text
        assertEquals("edited", text)
    }

    // --- Row 3.12: tap an already-selected text element enters edit -------------------------------

    @Test
    fun tapping_an_already_selected_text_box_enters_edit() {
        // Row 3.12, frozen `wire()`: `if(kindOf(n)==='text'&&selNode===n){edit();return;}` (`:618`).
        // Asserted on the pure decision (see [benchTapIntent]) rather than through an injected touch:
        // Robolectric touches do not reach `detectTapGestures` through this screen's layer stack, which is
        // why the shipped editor has no tap-to-select unit test either. Delivery is a Pass 1 item.
        val store = store()
        val id = placedText(store)
        val centre = PtPoint(midPageBox.xPt + midPageBox.widthPt / 2.0, midPageBox.yPt + midPageBox.heightPt / 2.0)

        val intent = benchTapIntent(store.uiState.value, centre)
        assertEquals(Intent.BeginEditText(id), intent)
    }

    @Test
    fun a_tap_that_is_not_a_reselected_text_box_still_selects_or_clears() {
        // The row 3.12 branch is additive: D-037's dismissal is untouched, and so is first selection. Three
        // cases, because each is a separate clause of that ruling.
        val store = store()
        val id = placedText(store)
        val centre = PtPoint(midPageBox.xPt + midPageBox.widthPt / 2.0, midPageBox.yPt + midPageBox.heightPt / 2.0)
        val empty = PtPoint(95.0, 95.0)

        // · blank paper, with the box selected → SelectAt, whose miss branch clears
        assertEquals(Intent.SelectAt(empty), benchTapIntent(store.uiState.value, empty))

        // · the same box while NOT selected → ordinary selection, not edit
        store.dispatch(Intent.ClearSelection)
        composeRule.waitForIdle()
        assertEquals(Intent.SelectAt(centre), benchTapIntent(store.uiState.value, centre))

        // · selected again → and only now does it edit
        store.dispatch(Intent.Select(id))
        composeRule.waitForIdle()
        assertEquals(Intent.BeginEditText(id), benchTapIntent(store.uiState.value, centre))
    }

    // --- Row 3.13, and ADR-093 §4: nothing is removed ---------------------------------------------

    @Test
    fun the_canvas_gesture_surface_is_inert_while_editing() {
        // Row 3.13: a canvas tap during a session does not deselect. Asserted **structurally** — the whole
        // page gesture surface and its resize handles are not composed while a session is open, so there
        // is nothing for a stray tap to reach. That is a stronger claim than "a tap did nothing", and it
        // is the claim the code actually makes (`if (!editing && reframing == null)`).
        val store = store()
        val id = placedText(store)
        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}${ResizeHandle.BOTTOM_RIGHT.name}").assertExists()

        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("${ResizeHandleTagPrefix}${ResizeHandle.BOTTOM_RIGHT.name}")
            .assertDoesNotExist()
        assertTrue(store.uiState.value.interaction is Interaction.EditingText)
        assertEquals(setOf(id), store.uiState.value.selection)
    }

    @Test
    fun the_type_bar_keeps_every_style_capability_it_had() {
        // ADR-093 §4's first bullet, asserted rather than intended: C3 adds an editing-state row and
        // removes nothing from the selected state. OD-11 / OD-14 / D-042.
        val store = store()
        val id = placedText(store)
        // Reach the Type bar the way the shipped product does — the context bar's Size verb (OD-9).
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.SIZE}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TypeBarTestTag).assertIsDisplayed()
        // The five inks, both size steps, three alignments, bold and italic — all still there.
        assertEquals(5, TextInk.entries.size)
        TextInk.entries.forEach {
            composeRule.onNodeWithTag("$TypeBarTestTag-ink-${it.label}").assertExists()
        }
        assertTrue(store.uiState.value.selection.contains(id))
    }
}
