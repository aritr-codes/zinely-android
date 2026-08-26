package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.DocumentDefaults
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyV21DarkColors
import com.aritr.zinely.ui.theme.zinelyV21LightColors
import com.aritr.zinely.ui.theme.zinelyV2DarkColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * C5 — the filmstrip of little sheets and the summoned page grid
 * ([ADR-095](../../../../../../../docs/DECISIONS.md#adr-095) rows 5.1–5.16).
 *
 * The rows this suite deliberately does **not** close are named rather than quietly skipped:
 *
 * | row | where it is closed instead | why not here |
 * |---|---|---|
 * | 5.1–5.7 the strip's paint (ground, hairline, radius, spine, shadow, lift) | [BenchC5GoldenTest] | they are paint, and a raster assertion is the honest instrument for paint |
 * | 5.8 the live miniature ([OD-22](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-053-ruling)) | [BenchC5GoldenTest] | "the page is drawn inside the sheet" is a claim about pixels |
 * | 5.9's **platform** clause | the mandatory device passes | `Role.Tab` rides `roleDescription`, which the CI-26 harness does not snapshot — see [com.aritr.zinely.feature.editor.a11y.BenchPageNavA11yTest] |
 *
 * Row 5.13a's `:active` scale was on that list, with the excuse *"a 100ms press transform has no assertable
 * resting state"*. Independent review refused it — the suite already drives an animation with the clock
 * held — and it is asserted below, under a held pointer.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchC5Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 130.0)
    private val host: Pair<Dp, Dp> = 360.dp to 720.dp

    /** Eight sheets — the real `SINGLE_SHEET_8` count, so `N` is never confusable with the loop bound. */
    private fun store(pageCount: Int = 8): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        return EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = (0 until pageCount).map {
                        Page(
                            index = it,
                            // Every page is INTERIOR, exactly as the product builds them (`EditorBootstrap.kt:26`,
                            // `RoomProjectRepository.kt:475`). These fixtures used to fabricate cover roles, which is
                            // why the suite proved three frozen rows that never fired on a real document until Device
                            // Pass 1 found them dead. Covers are a matter of POSITION now, per the freeze.
                            role = PageRole.INTERIOR,
                        )
                    },
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
    }

    private fun setScreen(store: EditorStore) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = pageSizePt,
                    modifier = Modifier.size(host.first, host.second),
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun px(dp: Dp) = with(composeRule.density) { dp.toPx() }

    // ---------------------------------------------------------------------------------------------
    // The filmstrip (rows 5.1-5.10)
    // ---------------------------------------------------------------------------------------------

    /** How many nodes carry exactly this tag — 0 is the "not composed at all" answer, not "hidden". */
    private fun count(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    /**
     * How many nodes draw exactly this string. The unmerged tree, deliberately: a merged parent reports
     * its children's text concatenated, so a merged search for a deleted word can be satisfied — or
     * defeated — by a neighbour it was never about.
     */
    private fun textCount(text: String) =
        composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    /**
     * Row 5.16: one thumb per page of the *document*, not per constant.
     *
     * Counted by asking for each numbered tag in turn rather than by prefix: a prefix count is the kind
     * of assertion that looks strict and quietly counts the wrong thing.
     */
    @Test
    fun the_strip_holds_one_sheet_for_every_page_the_document_has() {
        setScreen(store(pageCount = 6))
        (1..6).forEach { assertEquals("thumb $it", 1, count(benchThumbTag(it))) }
        assertEquals("there is no seventh sheet", 0, count(benchThumbTag(7)))
    }

    /**
     * Row 5.2: the frozen `.navrow{height:56px}`.
     *
     * Written against the literal 56, **not** against [BenchNavRowHeight]. Reading the production constant
     * on both sides is a tautology that moves with any edit to it — the mutation battery caught exactly
     * that shape here and on the sheet below, and the frozen file is the authority a test should quote.
     */
    @Test
    fun the_navigation_row_stands_at_the_frozen_height() {
        setScreen(store())
        assertEquals(px(56.dp), bounds(BenchNavRowTestTag).height, 1f)
    }

    /** D-009: the frozen `.fpage{29×38}` sheet, kept in terms by the ruling that changed its interior. */
    @Test
    fun a_sheet_keeps_the_frozen_twenty_nine_by_thirty_eight() {
        setScreen(store())
        val thumb = bounds(benchThumbTag(2))
        assertEquals(px(29.dp), thumb.width, 1f)
        assertEquals(px(38.dp), thumb.height, 1f)
    }

    /**
     * **P3: the current sheet no longer moves, so this asserts that it doesn't.**
     *
     * V2 said *which page you are on* with `transform:scale(1.16) translateY(-2px)` and this test measured
     * both magnitudes. V2.1 (`v21-bench.html:338-340`) says it with a flat `box-shadow:0 0 0 3px var(--berry)`
     * ring and nothing else — no scale, no lift, no raised elevation, no dot.
     *
     * Inverted rather than deleted, and geometrically rather than by colour: **every sheet in the strip is
     * the same size and sits on the same line**, current or not. A conversion that left the old transform in
     * would pass a colour probe for the ring and still be wrong, and this is the assertion that catches it.
     * The ring itself is a paint property, so it is asserted in `BenchC5GoldenTest` where the raster is.
     */
    @Test
    fun the_current_sheet_stands_exactly_level_with_its_neighbours() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(2))
        composeRule.waitForIdle()
        val current = bounds(benchThumbTag(3))
        val neighbour = bounds(benchThumbTag(4))
        assertEquals("the current sheet is not enlarged", neighbour.height, current.height, 1f)
        assertEquals("…nor widened", neighbour.width, current.width, 1f)
        assertEquals(
            "…nor lifted: V2.1 marks the current page with a ring, not with a transform",
            0f,
            neighbour.center.y - current.center.y,
            1f,
        )
    }

    /** Row 5.10: choosing a sheet navigates — the capability V1 had, unchanged (OD-9). */
    @Test
    fun choosing_a_sheet_goes_to_that_page() {
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(benchThumbTag(5)).performClick()
        composeRule.waitForIdle()
        assertEquals(4, store.uiState.value.currentPageIndex)
    }

    // ---------------------------------------------------------------------------------------------
    // The grid (rows 5.11-5.15)
    // ---------------------------------------------------------------------------------------------

    /**
     * Row 5.11a — the assertion the whole "summoned, never default" argument rests on: with nothing asked
     * for, the grid is not merely invisible, it is *not composed*. A hidden-but-present panel would pass an
     * `assertIsNotDisplayed` and still be the database the Bench review refused.
     */
    @Test
    fun the_grid_does_not_exist_until_it_is_summoned() {
        setScreen(store())
        assertEquals(0, count(BenchPageGridTestTag))
        // And no window either. This started life as a full-screen Dialog and the assertion below caught
        // a mutation the content check could not: a Dialog composed with empty content still raises a
        // window that takes every touch invisibly. The Dialog is gone (the freeze scopes the grid to the
        // canvas — see below), but the assertion stays: it is what proves "not composed" rather than
        // "composed and hidden", and it would catch the Dialog coming back.
        assertEquals(0, composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().size)
        // P5 added a second child — the frozen `.scrim` — and with it a host to hold the two. That host
        // is what the early return in `BenchPageGrid` now exists to suppress: a full-screen node standing
        // over the canvas for the life of the editor would take no touch and paint nothing, and would
        // pass every other assertion in this file.
        assertEquals("the scrim outlives the grid", 0, count(BenchPageGridScrimTestTag))
    }

    @Test
    fun the_grid_button_summons_it_holding_one_cell_per_page() {
        setScreen(store(pageCount = 8))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertIsDisplayed()
        // Not in a window of its own: the frozen `.pgrid` is `position:absolute`, a child of the phone
        // (`v21-bench.html:444`, markup `:568`), and the host still mounts it inside `.canvasArea`.
        assertEquals(0, composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().size)
        (1..8).forEach { assertEquals("cell $it", 1, count(benchPageCellTag(it))) }
        assertEquals("there is no ninth cell", 0, count(benchPageCellTag(9)))
    }

    /** Row 5.14: choosing a page in the grid navigates *and* stands the grid down. */
    @Test
    fun choosing_a_cell_navigates_and_takes_the_grid_away_with_it() {
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(benchPageCellTag(6)).performClick()
        composeRule.waitForIdle()
        assertEquals(5, store.uiState.value.currentPageIndex)
        assertEquals(0, count(BenchPageGridTestTag))
    }

    /**
     * Row 5.15: the head's close dismisses without changing the page — leaving is not choosing.
     *
     * ⚠ The control is the frozen `.dclose` cross now (`v21-bench.html:462`, `openGrid()` at `:780`),
     * where V2 drew the word `Done`. Same row, same job, different object.
     */
    @Test
    fun the_close_stands_the_grid_down_and_leaves_the_page_where_it_was() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridCloseTag).performClick()
        composeRule.waitForIdle()
        assertEquals(3, store.uiState.value.currentPageIndex)
        assertEquals(0, count(BenchPageGridTestTag))
    }

    /**
     * The frozen `.dclose{34×34}` (`v21-bench.html:462`) drawn at 34 and touched at 48 —
     * [D-009](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009)'s *extend the target, keep the
     * paint*, asserted as both halves because either alone is satisfiable by breaking the other.
     *
     * **Only the target half is assertable here, and that is stated rather than hidden.** The 34dp pill
     * is a plain `Box` inside the tagged target: it publishes no semantics of its own, so no semantics
     * instrument can measure it, and tagging it would put a paint detail into the module's public API.
     * The paint half is [BenchC5GoldenTest]'s, which reads pixels.
     */
    @Test
    fun the_grid_close_is_touched_at_forty_eight() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val target = bounds(BenchPageGridCloseTag)
        assertEquals("the touch target's width", px(48.dp), target.width, 1f)
        assertEquals("the touch target's height", px(48.dp), target.height, 1f)
    }

    /**
     * The frozen `.scrim` (`v21-bench.html:376-378`) rises with the panel and dismisses on tap —
     * `$('scrim').onclick = closeOverlays()` (`:845`).
     *
     * It is not decoration: V2's grid was an opaque overlay across the whole canvas, and V2.1's covers
     * about three-quarters of it. Without the scrim the visible remainder of the page would take taps
     * that do nothing, which is worse than a page that is plainly behind something.
     */
    @Test
    fun the_scrim_rises_with_the_grid_and_a_tap_on_it_stands_the_grid_down() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(1, count(BenchPageGridScrimTestTag))

        // ⚠ Tapped near the scrim's TOP edge, not at its centre, and the difference is not pedantry.
        // `performClick()` dispatches at the node's centre — and the scrim fills the screen while the
        // panel covers its bottom 78 %, so the centre of the scrim is *underneath the panel*. The first
        // run of this test tapped there, the topmost node took the event, and a page card answered it:
        // the assertion below failed with `currentPageIndex` = 1, i.e. "leaving" had chosen page one.
        // What a person can actually tap is the strip of scrim left showing above the sheet, so that is
        // what this taps.
        composeRule.onNodeWithTag(BenchPageGridScrimTestTag).performTouchInput {
            click(Offset(centerX, top + 4f))
        }
        composeRule.waitForIdle()
        assertEquals("the scrim did not stand the grid down", 0, count(BenchPageGridTestTag))
        assertEquals("…and left itself behind", 0, count(BenchPageGridScrimTestTag))
        assertEquals("leaving is not choosing", 3, store.uiState.value.currentPageIndex)
    }

    // ---------------------------------------------------------------------------------------------
    // The rows independent review found unasserted (RF-3, RF-4, RF-5)
    // ---------------------------------------------------------------------------------------------

    /**
     * Row 5.2b / [D-009](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-009): *extend the target,
     * keep the paint.*
     *
     * The sheet is drawn at the frozen 26×34 (asserted above) while its **touch** bounds reach the 48dp
     * floor. Both halves in one test on purpose: either alone is satisfiable by breaking the other.
     *
     * **What this does not claim.** Compose reports a 48dp `touchBoundsInRoot` for any small pointer node,
     * but hit arbitration between two nodes whose expanded regions overlap goes to the nearer centre — and
     * at a 26dp sheet on a 7dp pitch they do overlap, so the *exclusive* horizontal slop is about half the
     * gap, not 11dp a side. The reported bound is what the platform publishes and what D-009's ruling is
     * written against; the real-world reachability of adjacent sheets is a device-pass question, and it is
     * named here rather than papered over.
     */
    @Test
    fun a_sheet_is_drawn_at_twenty_nine_and_touched_at_forty_eight() {
        setScreen(store())
        val drawn = bounds(benchThumbTag(2))
        val touch = composeRule.onNodeWithTag(benchThumbTag(2)).fetchSemanticsNode().touchBoundsInRoot
        assertEquals("drawn width", px(29.dp), drawn.width, 1f)
        assertEquals("drawn height", px(38.dp), drawn.height, 1f)
        assertTrue(
            "the touch target is ${touch.width}×${touch.height}px, under the 48dp floor",
            touch.width >= px(48.dp) - 1f && touch.height >= px(48.dp) - 1f,
        )
    }

    /** The frozen `.gridbtn{38×38}` (`v21-bench.html:329`), and its own ≥48dp touch target. */
    @Test
    fun the_grid_button_is_drawn_at_thirty_eight_and_touched_at_forty_eight() {
        setScreen(store())
        val drawn = bounds(BenchGridButtonTestTag)
        val touch =
            composeRule.onNodeWithTag(BenchGridButtonTestTag).fetchSemanticsNode().touchBoundsInRoot
        assertEquals("drawn width", px(38.dp), drawn.width, 1f)
        assertEquals("drawn height", px(38.dp), drawn.height, 1f)
        assertTrue(
            "the grid button's touch target is ${touch.width}×${touch.height}px, under the 48dp floor",
            touch.width >= px(48.dp) - 1f && touch.height >= px(48.dp) - 1f,
        )
    }

    /** Row 5.3: the frozen `.filmstrip{gap:7px}` — measured between two sheets, not read from a constant. */
    @Test
    fun the_sheets_stand_seven_apart() {
        setScreen(store())
        // Page 2 and 3, so neither is the current sheet: the current one is scaled 1.16 and would report
        // the gap minus its own overhang.
        val second = bounds(benchThumbTag(2))
        val third = bounds(benchThumbTag(3))
        assertEquals(px(7.dp), third.left - second.right, 1f)
    }

    /**
     * Row 5.10: the frozen `setPage()` scrolls the chosen sheet to the **centre** of the strip
     * (`scrollIntoView({inline:'center'})`, `v2-bench.html:727`).
     *
     * The only non-trivial state logic in the package, and it had no test until independent review said so.
     * Asserted as *the current sheet's centre is nearer the strip's centre than it was* — which is what the
     * behaviour is for, and which a deleted `LaunchedEffect` cannot satisfy. A literal centre equality would
     * be wrong at the ends of a short strip, where the scroll range runs out first.
     */
    @Test
    fun choosing_a_far_sheet_scrolls_it_toward_the_middle_of_the_strip() {
        val store = store(pageCount = 20)
        setScreen(store)
        val stripCentre = bounds(BenchFilmstripTestTag).center.x
        val before = bounds(benchThumbTag(14)).center.x
        store.dispatch(Intent.GoToPage(13))
        composeRule.waitForIdle()
        val after = bounds(benchThumbTag(14)).center.x
        assertTrue(
            "sheet 14 started ${before - stripCentre}px from the strip's centre and ended " +
                "${after - stripCentre}px away — it was not scrolled toward the middle",
            kotlin.math.abs(after - stripCentre) < kotlin.math.abs(before - stripCentre),
        )
    }

    /** Row 5.12: the frozen `.pgg{grid-template-columns:repeat(3,1fr); gap:var(--gap-md)}` (`:451`). */
    @Test
    fun the_grid_lays_three_cells_to_a_row_twelve_apart() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val one = bounds(benchPageCellTag(1))
        val two = bounds(benchPageCellTag(2))
        val three = bounds(benchPageCellTag(3))
        val four = bounds(benchPageCellTag(4))
        assertEquals("the gap between columns", px(12.dp), two.left - one.right, 1f)
        assertEquals("three to a row: the fourth cell wraps", one.left, four.left, 1f)
        assertTrue("the fourth cell is on the next row", four.top > three.bottom - 1f)
        assertEquals("the gap between rows", px(12.dp), four.top - one.bottom, 1f)
        // Row 5.12a: `aspect-ratio:3/4` is width ÷ height — .75, where V2's `.66` was taller than any
        // paper the app imposes. The literal is quoted rather than `BenchCellAspect`, so an assertion
        // written here cannot move with its own mutation.
        assertEquals("the cell's aspect", 0.75f, one.width / one.height, 0.02f)
    }

    /**
     * Row 5.15 / 5.16: the grid's header counts the **document's** pages.
     *
     * ADR-095 §4 forbids reading `N` from a constant, and names *"hard-code 12"* as the mutation. Until
     * independent review pointed it out, nothing in the suite read this string at all: the forbidden
     * mutation survived every test in the package. Two different page counts, so a constant of either
     * value fails.
     */
    @Test
    fun the_grid_header_counts_the_documents_pages() {
        setScreen(store(pageCount = 5))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your zine · 5 pages").assertIsDisplayed()
    }

    @Test
    fun the_grid_header_counts_a_different_documents_pages() {
        setScreen(store(pageCount = 9))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your zine · 9 pages").assertIsDisplayed()
    }

    /**
     * Row 5.11, **rewritten for V2.1**: the grid is a bottom sheet, not an overlay.
     *
     * V2's `.pgrid` was `position:absolute; inset:0` — it *filled* the canvas in `--desk`, and this test
     * asserted exactly that. V2.1's (`v21-bench.html:444-447`) is the same object `.sheet` is: anchored
     * to the bottom edge, `max-height:78%`, with the page still visible above it. So the reading is not
     * merely retuned, it is inverted at the top edge — a panel that still filled its box would pass V2's
     * version of this test and fail this one, which is the point of keeping it.
     *
     * ⚠ **The box is the canvas, not the phone**, and that is a recorded P5 deviation rather than the
     * freeze: the frozen `.pgrid` is a child of `.phone` (markup `:568`) and covers the navigation row
     * and the bar, while the host still mounts this inside `.canvasArea` where V2's `inset:0` correctly
     * put it. Re-homing the call site is `EditorScreen.kt`'s, which P5 does not own. The assertions
     * below are written against the box the panel is actually given, and they say so.
     */
    @Test
    fun the_grid_rises_from_the_bottom_of_its_box_and_leaves_the_page_visible_above_it() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val grid = bounds(BenchPageGridTestTag)
        val nav = bounds(BenchNavRowTestTag)
        // The scrim fills the box the overlay was given, so it — not the navigation row — is what the
        // panel's own edges are honestly measured against. The row is asserted separately, below.
        val box = bounds(BenchPageGridScrimTestTag)
        // Its floor is its box's floor: the panel is anchored, not floating.
        assertEquals("the panel does not sit on the bottom edge of its box", box.bottom, grid.bottom, 1f)
        // And its ceiling is NOT: `max-height:78%` leaves the top of the box showing, which is the whole
        // difference between a bottom sheet and V2's opaque overlay.
        assertTrue(
            "the panel is ${grid.height}px tall in a ${box.height}px box — the frozen ceiling is 78 %",
            grid.height <= box.height * 0.78f + 1f,
        )
        assertTrue(
            "the panel reaches the top of its box (${grid.top} vs ${box.top}) — it is still an overlay, " +
                "not the frozen sheet",
            grid.top > box.top + 1f,
        )
        // ⚠ **Inverted.** This line used to require the panel to stop above the navigation row, because
        // the host mounted the overlay inside the canvas. The frozen markup does not: `.pgrid` is a
        // direct child of `.phone` (`v21-bench.html:585`; `.canvasArea` closes at `:530`), so it rises
        // from the *screen's* bottom edge and covers the filmstrip and the bar. The host was re-homed to
        // match, and the assertion now says the opposite of what it said this morning — deliberately,
        // because a panel that stopped short of the bottom would be a sheet that failed to open.
        assertTrue(
            "the panel's floor (${grid.bottom}) stops above the navigation row (top ${nav.top}) — it is " +
                "anchored to the canvas again, not to the screen",
            grid.bottom > nav.top,
        )
        // What stays visible is the top of the screen, which is where the artifact is. `max-height:78%`
        // is what guarantees it, and the status strip is the landmark that proves the ceiling held.
        composeRule.onNodeWithTag(BenchStatusStripTestTag).assertIsDisplayed()
        assertTrue(
            "the panel (top ${grid.top}) has climbed over the status strip — the 78 % ceiling is gone",
            grid.top > bounds(BenchStatusStripTestTag).bottom,
        )
    }

    /**
     * Row 5.4d / [OD-23](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-059-ruling): the sheet is a
     * light-theme island, and it restates **five** tokens — not eight, and not the whole scheme.
     *
     * `.pthumb{--paper;--paper-edge;--ink;--ink-soft;--ink-faint}` (`v2-bench.html:282`). `--matcha` and
     * `--strawberry` are deliberately excluded: the spine, the current border and the dot are the row's marks
     * on the sheet, and they have to keep reading against the chrome. Asserted as a *set* — which tokens
     * moved and which did not — because "the thumb looks right in dark" is satisfied by lightening
     * everything, which is the mistake C1 made once and review caught ([D-010](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010)).
     */
    @Test
    fun the_sheet_takes_the_page_s_five_tokens_and_leaves_the_row_s_marks_alone() {
        val room = zinelyV2DarkColors()
        // ⚠ The lit values are **V2.1's**, though the scheme type is still V2's — see [benchThumbIsland].
        // This test read `zinelyV2LightColors()` and therefore held the strip to V2's `#F7F2E7` while the
        // page grid's cards had already moved to V2.1's `#FFF6E8`: it was defending, not catching, one
        // screen drawing the same eight pages on two different papers (the OD-47 defect P5 closes). Each
        // island was internally consistent, so nothing failed; a review found it by opening both.
        val light = zinelyV21LightColors()
        val sheet = benchThumbIsland(room)

        assertEquals("--paper", light.paper, sheet.paper)
        assertEquals("--paper-edge", light.paperEdge, sheet.paperEdge)
        assertEquals("--ink", light.ink, sheet.ink)
        assertEquals("--ink-soft", light.inkSoft, sheet.inkSoft)
        assertEquals("--ink-faint", light.inkFaint, sheet.inkFaint)

        assertEquals("--matcha is the ROW's mark, not the page's ink", room.matcha, sheet.matcha)
        assertEquals("--strawberry likewise", room.strawberry, sheet.strawberry)
        assertEquals("the sheet's shadow is the row's, as C1 left the page's", room.frameShadow, sheet.frameShadow)
        assertEquals("the room around the sheet is untouched", room.chrome, sheet.chrome)
        assertEquals(room.desk, sheet.desk)
    }

    /**
     * The frozen `.navrow{gap:var(--gap-sm); padding:… var(--gap-md) …}` (`v21-bench.html:328`) — 8 and 12,
     * where V2 had 8 and 10.
     *
     * The row's two halves and its ends, in one test. A second independent review found both numbers
     * unasserted — no test, no mutation — which is how a frozen value ships as decoration.
     */
    @Test
    fun the_row_pads_its_ends_by_twelve_and_parts_its_halves_by_eight() {
        setScreen(store())
        val nav = bounds(BenchNavRowTestTag)
        val button = bounds(BenchGridButtonTestTag)
        val strip = bounds(BenchFilmstripTestTag)
        assertEquals("--gap-md on the leading end", px(12.dp), button.left - nav.left, 1f)
        assertEquals("--gap-sm between the button and the strip", px(8.dp), strip.left - button.right, 1f)
    }

    /**
     * The frozen `.filmstrip{padding:var(--gap-hair) 0 var(--gap-xs)}` (`v21-bench.html:333`) — **zero** on
     * the sides, where V2 had 4.
     *
     * Asserting a zero looks like asserting nothing, and it is the opposite: the strip's leading sheet now
     * sits flush against the strip's edge, and a 4dp inset creeping back in — from a stray `padding`, or
     * from someone "tidying" the horizontal term out of the auto-centre arithmetic — is invisible to every
     * other test here.
     *
     * V2's version of this test had to move the current page away first, because `scale(1.16)` made sheet 1
     * overhang its own box by 2.08dp and read 1.92 instead of 4. That confound is gone with the transform,
     * but the `GoToPage` is kept: it costs nothing and it keeps the two readings comparable.
     */
    @Test
    fun the_first_sheet_stands_flush_with_the_strips_edge() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        assertEquals(0f, bounds(benchThumbTag(1)).left - bounds(BenchFilmstripTestTag).left, 1f)
    }

    /**
     * **P3 deleted the transition this test was about, so it now asserts there is no transition.**
     *
     * V2's `.pthumb{transition:transform .2s var(--settle)}` lifted the current sheet over 200ms, and the
     * original test read the tween mid-flight because `BenchThumbMillis = 0` had once made the sheet snap
     * while every settled-state assertion stayed green. V2.1 has no transform to ease: the current page is
     * a flat `berry` ring, applied and removed with the state.
     *
     * So the reading is inverted — hold the clock, change page, and the geometry must be **final on the
     * first frame**. Kept rather than deleted because an animation quietly returning here is precisely the
     * V2 habit this conversion is removing, and it would look like polish rather than a regression.
     */
    @Test
    fun changing_page_moves_no_sheet_on_any_frame() {
        val store = store()
        setScreen(store)
        val resting = bounds(benchThumbTag(3))
        composeRule.mainClock.autoAdvance = false
        store.dispatch(Intent.GoToPage(2))
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        val midFlight = bounds(benchThumbTag(3))
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settled = bounds(benchThumbTag(3))

        assertEquals("the sheet changed size mid-flight", resting.height, midFlight.height, 1f)
        assertEquals("…or on its way to settling", resting.height, settled.height, 1f)
        assertEquals("…or moved", resting.center.y, settled.center.y, 1f)
    }

    /**
     * Row 5.11: the frozen `.pgrid{padding:0 var(--gap-lg) var(--gap-xl)}` (`v21-bench.html:447`) — the
     * inset the cards stand in. Sixteen, as in V2, but now a token rather than a number, and with a
     * **zero** top: the grip is what stands the head off the panel's edge.
     */
    @Test
    fun the_grid_insets_its_cells_by_sixteen() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(
            px(16.dp),
            bounds(benchPageCellTag(1)).left - bounds(BenchPageGridTestTag).left,
            1f,
        )
    }

    /**
     * Row 5.14, the other half: the frozen `.pgcell span` is the page **number** (`:382`, `:733`).
     *
     * The badge half (`COVER`/`BACK`) was asserted; the number was not read anywhere, by any test or any
     * mutation — so a grid of unnumbered cells passed the whole suite.
     */
    @Test
    fun every_cell_wears_its_page_number() {
        setScreen(store(pageCount = 8))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        // The first row, which needs no scrolling, plus the last cell scrolled into view — first and last
        // together, because an off-by-one in the numbering shows at the ends.
        (1..3).forEach { composeRule.onNodeWithText("$it").assertIsDisplayed() }
        composeRule.onNodeWithText("8").performScrollTo().assertIsDisplayed()
    }

    /**
     * Row 5.1, the stacking order: the frozen `.navrow` is opened at `v2-bench.html:481` and `.bar` at
     * `:488`, both children of `.phone` in normal flow — so the sheets sit **above** Undo/Redo/Add/Done,
     * and the row that answers *"which page?"* is the one nearer the page.
     *
     * C5 shipped these two inverted and every test still passed, because each row was only ever measured
     * against itself. Independent review found it by reading the frozen markup against the emitted Column.
     * This is the assertion that was missing: not a size, a *relation*.
     */
    @Test
    fun the_navigation_row_sits_above_the_bar_as_the_freeze_stacks_them() {
        setScreen(store())
        val nav = bounds(BenchNavRowTestTag)
        val bar = bounds(BenchBottomBarTestTag)
        assertTrue(
            "the navigation row (top ${nav.top}) must sit above the bar (top ${bar.top})",
            nav.bottom <= bar.top + 1f,
        )
    }

    /**
     * Back stands the grid down rather than leaving the editor.
     *
     * Not a frozen property — the prototype has no back button — but a platform contract, and one the
     * package lost when independent review removed the `Dialog` that had been supplying it for free. The
     * grid consumes every pointer event beneath it, so without this its only exits are `Done` and a cell.
     */
    @Test
    fun back_stands_the_grid_down_and_leaves_the_editor_where_it_was() {
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals(1, count(BenchPageGridTestTag))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals("Back did not stand the grid down", 0, count(BenchPageGridTestTag))
        assertTrue("Back finished the editor instead of closing the grid", !composeRule.activity.isFinishing)
    }

    /**
     * **P5 deleted the transform this test was about, so it now asserts the V2.1 press instead.**
     *
     * V2's `.pgcell:active{transform:scale(.96)}` shrank the cell over a 100ms tween. V2.1's `.pgc`
     * carries `box-shadow:3px 3px 0` (`v21-bench.html:460`) and no `:active` rule at all, and P5 applies
     * [com.aritr.zinely.ui.theme.ZinelyV21Press.Raised] to it on the precedent the frozen file set for
     * `.doneEdit` (`:285-292`). That press is a **translation**: the card travels 2dp down-right and its
     * shadow shortens; nothing scales, and nothing animates, because a press is a position rather than a
     * transition.
     *
     * So the reading is inverted on the size and made exact on the position. Kept rather than deleted:
     * a scale creeping back in here would look like polish and would be the V2 habit this conversion is
     * removing.
     *
     * The clock must still be driven by hand. The cards live inside a `verticalScroll`, so `clickable`
     * holds its `PressInteraction.Press` back by the tap timeout in case the gesture turns into a drag —
     * with the pointer held and nothing else pending, `waitForIdle` returns before the press is ever
     * emitted, and the first cut of this test read the resting bounds and called them pressed.
     */
    @Test
    fun a_pressed_cell_travels_down_and_right_without_changing_size() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val resting = bounds(benchPageCellTag(2))
        // A first-row cell, and checked to be *whole*: `boundsInRoot` is intersected with the scroll
        // viewport's clip, so a partly-clipped cell reports a height that is not its height and turns
        // every comparison below into nonsense.
        assertEquals(
            "the measured cell is clipped by the scroll viewport, so its bounds are not its size",
            resting.width / 0.75f,
            resting.height,
            1.5f,
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(benchPageCellTag(2)).performTouchInput { down(center) }
        // Past the tap timeout. Frame by frame, because a single large `advanceTimeBy` jumps the clock
        // without giving the composition the frames it needs.
        repeat(60) { composeRule.mainClock.advanceTimeByFrame() }
        val pressed = bounds(benchPageCellTag(2))
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag(benchPageCellTag(2)).performTouchInput { up() }

        // `2dp` is quoted as a literal — an assertion written against `ZinelyV21Press.Raised.travel`
        // would move with its own mutation.
        assertEquals("the card travels 2dp right", px(2.dp), pressed.left - resting.left, 1f)
        assertEquals("…and 2dp down", px(2.dp), pressed.top - resting.top, 1f)
        assertEquals("a card does not shrink under a finger", resting.width, pressed.width, 1f)
        assertEquals("…on either axis", resting.height, pressed.height, 1f)
    }

    /**
     * **Row 5.14's badge half is deleted, so this asserts its absence.**
     *
     * V2's `.pgcell b` drew `COVER` and `BACK` on the first and last cards. V2.1's frozen `openGrid()`
     * writes `${i+1}` and nothing else (`v21-bench.html:794`), and `.pgc` declares no rule for a badge.
     * The cover distinction survives where it is *spoken* — [benchPageLabel], asserted below — and is no
     * longer drawn.
     *
     * Inverted rather than deleted, and inverted on the *word*: a conversion that kept the badge would
     * pass every other assertion in this file, since each of them checks for the presence of its own
     * mark. The second assertion is what stops the inversion being satisfied by a grid that has quietly
     * stopped naming its covers at all.
     */
    @Test
    fun no_card_wears_a_cover_badge_and_the_covers_are_still_named_out_loud() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        assertEquals("a card still draws the COVER badge", 0, textCount("COVER"))
        assertEquals("…or the BACK badge", 0, textCount("BACK"))
        assertEquals("…or either of them in the case V2 shipped by mistake", 0, textCount("Cover"))

        composeRule.onNodeWithTag(benchPageCellTag(1))
            .assertContentDescriptionEquals("Page 1 of 8 (front cover)")
        composeRule.onNodeWithTag(benchPageCellTag(8))
            .assertContentDescriptionEquals("Page 8 of 8 (back)")
    }

    /**
     * Row 5.11: the frozen grid **arrives from below** — `transform:translateY(103%)` settling to `0`
     * over `.3s cubic-bezier(.05,.7,.1,1)` (`v21-bench.html:446`), where V2 wrote 102%.
     *
     * Asserted in flight, with the clock held, because the settled frame is identical whether the slide
     * happened or not: setting the offset to 0 passed every other test in the package and both goldens.
     *
     * Driven at the component, and by a state write rather than a tap. Under a held clock the input
     * dispatcher needs frames of its own to deliver a click, and those frames are the ones the animation
     * is being measured in — the screen-level version of this test read an empty tree for exactly that
     * reason. Here the only thing the clock advances is the tween.
     */
    @Test
    fun the_grid_arrives_from_below_rather_than_appearing_in_place() {
        val open = mutableStateOf(false)
        val pages = (0 until 6).map { Page(index = it, role = PageRole.INTERIOR) }
        composeRule.setContent {
            ZinelyTheme {
                Box(Modifier.size(host.first, host.second)) {
                    BenchPageGrid(
                        visible = open.value,
                        pages = pages,
                        currentPageIndex = 0,
                        pageSizePt = PtSize(100.0, 130.0),
                        defaults = DocumentDefaults(),
                        onSelectPage = {},
                        onDismiss = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread { open.value = true }
        // The node is not in the tree for the first few frames, and exactly how many it takes is a
        // recomposition detail — a fixed frame count made this test flaky. Advance frame by frame and
        // read the FIRST frame the grid exists in: that is the start of the slide, which is the instant
        // the frozen property is about.
        var midFlight: androidx.compose.ui.geometry.Rect? = null
        repeat(40) {
            composeRule.mainClock.advanceTimeByFrame()
            if (midFlight == null && count(BenchPageGridTestTag) == 1) {
                midFlight = bounds(BenchPageGridTestTag)
            }
        }
        assertTrue("the grid never entered the tree at all", midFlight != null)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settled = bounds(BenchPageGridTestTag)
        assertTrue(
            "the grid was already seated at ${midFlight?.top} on its first frame " +
                "(it settles at ${settled.top}) — it did not arrive from below",
            midFlight!!.top > settled.top + px(8.dp),
        )
    }

    /**
     * [OD-47](../../../../../../../docs/design/V2-SPEC-DEFECTS.md)'s disposition, asserted as a **set**:
     * the card restates the six tokens `.pgc` re-declares (`v21-bench.html:456-457`) and leaves the room
     * alone. Six, not five, and not the whole scheme.
     *
     * *"The card looks right in dark"* is satisfied by lightening everything, which is the mistake C1
     * made once and review caught ([D-010](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-010)).
     * So the tokens that must **not** move are asserted beside the ones that must — including `--desk`
     * and `--bench`, because the amendment's own words are *"the panel is CHROME and keeps the room; only
     * the CARDS are lit."*
     *
     * `--ink-line` is the sixth, and the amendment says why: the card's hard shadow falls on the card's
     * own plane, unlike `.page`'s, which falls on the bench and must follow the room.
     *
     * Pure, so no composition is involved and the dark room is the one that can fail.
     */
    @Test
    fun the_card_takes_the_six_lit_tokens_and_leaves_the_room_alone() {
        val room = zinelyV21DarkColors()
        val lit = zinelyV21LightColors()
        val card = benchGridCardIsland(room)

        assertEquals("--paper", lit.paper, card.paper)
        assertEquals("--ink", lit.ink, card.ink)
        assertEquals("--ink-soft", lit.inkSoft, card.inkSoft)
        assertEquals("--ink-line", lit.inkLine, card.inkLine)
        assertEquals("--leaf-tint", lit.leafTint, card.leafTint)
        assertEquals("--leaf-text", lit.leafText, card.leafText)

        assertEquals("--desk is the panel's, and the panel keeps the room", room.desk, card.desk)
        assertEquals("--bench likewise", room.bench, card.bench)
        assertEquals("--paper-edge is not one of the six", room.paperEdge, card.paperEdge)
        assertEquals("--ink-faint is not one of the six", room.inkFaint, card.inkFaint)
        assertEquals("--berry is not one of the six", room.berry, card.berry)
        assertEquals("--hair is not one of the six", room.hair, card.hair)
    }

    /**
     * The lit card is lit in **both** themes — which is the whole content of the OD-47 ruling, and the
     * thing the island above cannot state on its own: a `copy()` that read the *room's* values instead of
     * the light palette's would still move exactly six fields and pass every assertion up there when the
     * room happens to be light.
     */
    @Test
    fun the_card_paints_the_same_six_colours_whichever_room_it_is_in() {
        val fromDark = benchGridCardIsland(zinelyV21DarkColors())
        val fromLight = benchGridCardIsland(zinelyV21LightColors())
        assertEquals(fromLight.paper, fromDark.paper)
        assertEquals(fromLight.ink, fromDark.ink)
        assertEquals(fromLight.inkSoft, fromDark.inkSoft)
        assertEquals(fromLight.inkLine, fromDark.inkLine)
        assertEquals(fromLight.leafTint, fromDark.leafTint)
        assertEquals(fromLight.leafText, fromDark.leafText)
    }
}
