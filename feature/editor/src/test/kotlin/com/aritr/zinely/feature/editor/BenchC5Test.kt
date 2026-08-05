package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyV2DarkColors
import com.aritr.zinely.ui.theme.zinelyV2LightColors
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

    /** Row 5.5 / D-009: the frozen 26×34 sheet, kept in terms by the ruling that changed its interior. */
    @Test
    fun a_sheet_keeps_the_frozen_twenty_six_by_thirty_four() {
        setScreen(store())
        val thumb = bounds(benchThumbTag(2))
        assertEquals(px(26.dp), thumb.width, 1f)
        assertEquals(px(34.dp), thumb.height, 1f)
    }

    /**
     * Row 5.6: the current sheet is lifted and enlarged, so the row says which page you are on —
     * `transform:scale(1.16) translateY(-2px)` (`v2-bench.html:288`).
     *
     * The **magnitudes** are asserted, not merely the directions: *taller and higher* is satisfied by
     * `scale(1.01) translateY(-.1px)`, which is a difference nobody can see and which would leave the row
     * silent about which page you are on. Both numbers are quoted as frozen literals.
     */
    @Test
    fun the_current_sheet_is_the_one_that_stands_taller_than_its_neighbours() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(2))
        composeRule.waitForIdle()
        val current = bounds(benchThumbTag(3))
        val neighbour = bounds(benchThumbTag(4))
        assertEquals("scale(1.16) of the frozen 34", px(34.dp) * 1.16f, current.height, 1.5f)
        assertEquals("scale(1.16) of the frozen 26", px(26.dp) * 1.16f, current.width, 1.5f)
        // The lift is measured centre to centre: the scale grows the sheet about its own centre, so
        // comparing top edges would fold the extra height into the lift and report 2dp + 2.7dp.
        assertEquals(
            "translateY(-2px) from its neighbour's centre",
            px(2.dp),
            neighbour.center.y - current.center.y,
            1.5f,
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
    }

    @Test
    fun the_grid_button_summons_it_holding_one_cell_per_page() {
        setScreen(store(pageCount = 8))
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridTestTag).assertIsDisplayed()
        // Not in a window of its own: the frozen `.pgrid` is `position:absolute` inside `.canvasArea`.
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

    /** Row 5.15: `Done` dismisses without changing the page — leaving is not choosing. */
    @Test
    fun done_stands_the_grid_down_and_leaves_the_page_where_it_was() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchPageGridDoneTag).performClick()
        composeRule.waitForIdle()
        assertEquals(3, store.uiState.value.currentPageIndex)
        assertEquals(0, count(BenchPageGridTestTag))
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
    fun a_sheet_is_drawn_at_twenty_six_and_touched_at_forty_eight() {
        setScreen(store())
        val drawn = bounds(benchThumbTag(2))
        val touch = composeRule.onNodeWithTag(benchThumbTag(2)).fetchSemanticsNode().touchBoundsInRoot
        assertEquals("drawn width", px(26.dp), drawn.width, 1f)
        assertEquals("drawn height", px(34.dp), drawn.height, 1f)
        assertTrue(
            "the touch target is ${touch.width}×${touch.height}px, under the 48dp floor",
            touch.width >= px(48.dp) - 1f && touch.height >= px(48.dp) - 1f,
        )
    }

    /** Row 5.2: the frozen `.gridbtn{34×34}`, and its own ≥48dp touch target. */
    @Test
    fun the_grid_button_is_drawn_at_thirty_four_and_touched_at_forty_eight() {
        setScreen(store())
        val drawn = bounds(BenchGridButtonTestTag)
        val touch =
            composeRule.onNodeWithTag(BenchGridButtonTestTag).fetchSemanticsNode().touchBoundsInRoot
        assertEquals("drawn width", px(34.dp), drawn.width, 1f)
        assertEquals("drawn height", px(34.dp), drawn.height, 1f)
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

    /** Row 5.12: the frozen `.pgg{grid-template-columns:repeat(3,1fr); gap:12px}`. */
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
        // Row 5.12a: `aspect-ratio:.66` is width ÷ height.
        assertEquals("the cell's aspect", 0.66f, one.width / one.height, 0.02f)
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
     * Row 5.11: the frozen grid is `position:absolute; inset:0` on markup **inside `.canvasArea`**
     * (`v2-bench.html:374`, `:470`) — so it covers the canvas and leaves the navigation row and the bar
     * standing.
     *
     * The first cut of C5 made it a full-screen Dialog. This is the assertion that would have caught it:
     * the grid's own bounds stop above the navigation row, and the row is still on screen and still usable
     * while the grid is open.
     */
    @Test
    fun the_grid_covers_the_canvas_and_leaves_the_navigation_row_standing() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val grid = bounds(BenchPageGridTestTag)
        val nav = bounds(BenchNavRowTestTag)
        assertTrue(
            "the grid's bottom edge (${grid.bottom}) reaches into the navigation row (top ${nav.top})",
            grid.bottom <= nav.top + 1f,
        )
        // And the other edge: `inset:0` inside `.canvasArea` bounds the TOP as well, so a grid that ate
        // the status strip above the canvas would pass the assertion above on its own.
        val status = bounds(BenchStatusStripTestTag)
        assertTrue(
            "the grid's top edge (${grid.top}) reaches into the status strip (bottom ${status.bottom})",
            grid.top >= status.bottom - 1f,
        )
        composeRule.onNodeWithTag(BenchStatusStripTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchNavRowTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchBottomBarTestTag).assertIsDisplayed()
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
        val light = zinelyV2LightColors()
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
     * Row 5.1b: the frozen `.navrow{gap:8px; padding:0 10px}` (`v2-bench.html:275`).
     *
     * The row's two halves and its ends, in one test. A second independent review found both numbers
     * unasserted — no test, no mutation — which is how a frozen value ships as decoration.
     */
    @Test
    fun the_row_pads_its_ends_by_ten_and_parts_its_halves_by_eight() {
        setScreen(store())
        val nav = bounds(BenchNavRowTestTag)
        val button = bounds(BenchGridButtonTestTag)
        val strip = bounds(BenchFilmstripTestTag)
        assertEquals("padding:0 10px on the leading end", px(10.dp), button.left - nav.left, 1f)
        assertEquals("gap:8px between the button and the strip", px(8.dp), strip.left - button.right, 1f)
    }

    /**
     * Row 5.3: the frozen `.filmstrip{padding:9px 4px}` (`:279`).
     *
     * Measured with the current sheet somewhere else, for a reason worth recording: `scale(1.16)` grows
     * the current sheet about its own centre, so when sheet 1 is the current one it overhangs its own
     * layout box by `26 × .16 ÷ 2 = 2.08dp` and reads an inset of 1.92 instead of 4. That is the lift
     * working, not the padding failing — and it is exactly the reading that would have been mistaken for a
     * defect. The document does not scroll here (eight sheets on a 33dp pitch fit the host), so the
     * measurement is the content inset and not a scroll offset.
     */
    @Test
    fun the_first_sheet_stands_four_from_the_strips_edge() {
        val store = store()
        setScreen(store)
        store.dispatch(Intent.GoToPage(3))
        composeRule.waitForIdle()
        assertEquals(px(4.dp), bounds(benchThumbTag(1)).left - bounds(BenchFilmstripTestTag).left, 1f)
    }

    /**
     * Row 5.4a: the frozen `.pthumb{transition:transform .2s var(--settle)}` (`:283`).
     *
     * The lift **takes time**. Nothing asserted this: with `BenchThumbMillis = 0` the sheet snapped to its
     * full 1.16 in one frame and every other test in this suite still passed, because they all measure the
     * settled state. Read mid-tween with the clock held — strictly between resting and lifted is the only
     * reading a duration of 0 cannot produce, and the frozen 200 is quoted rather than the constant.
     */
    @Test
    fun the_lift_is_a_transition_rather_than_a_jump() {
        val store = store()
        setScreen(store)
        val resting = bounds(benchThumbTag(3)).height
        composeRule.mainClock.autoAdvance = false
        store.dispatch(Intent.GoToPage(2))
        // ~40ms into the frozen 200: far enough that a frame has run, early enough that a finished tween
        // is unmistakable. Frame by frame, for the reason the pressed-cell test records.
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        val midFlight = bounds(benchThumbTag(3)).height
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val lifted = bounds(benchThumbTag(3)).height

        assertEquals("the sheet did not end up lifted", px(34.dp) * 1.16f, lifted, 1.5f)
        assertTrue(
            "the lift arrived in one frame ($midFlight px at ~40ms of the frozen 200), so there is no " +
                "transition: resting $resting, lifted $lifted",
            midFlight > resting + 0.5f && midFlight < lifted - 0.5f,
        )
    }

    /** Row 5.11: the frozen `.pgrid{padding:16px}` (`:374`) — the inset the cells stand in. */
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
     * Row 5.13a: the frozen `.pgcell:active{transform:scale(.96)}` over `transition:transform .1s`
     * (`v2-bench.html:380-381`).
     *
     * Held pointer, held clock: press, let the 100ms tween finish, and measure. `.96` is quoted as a
     * literal — an assertion written against `BenchCellPressedScale` would move with its own mutation.
     *
     * The clock must be driven by hand. The cells live inside a `verticalScroll`, so `clickable` holds its
     * `PressInteraction.Press` back by the tap timeout in case the gesture turns into a drag — with the
     * pointer held and nothing else pending, `waitForIdle` returns before the press is ever emitted, and
     * the first cut of this test read the resting size and called it pressed.
     */
    @Test
    fun a_pressed_cell_shrinks_under_the_finger() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        val resting = bounds(benchPageCellTag(2))
        // A first-row cell, and checked to be *whole*: `boundsInRoot` is intersected with the scroll
        // viewport's clip, so a partly-clipped cell reports a height that is not its height and turns
        // every ratio below into nonsense. The first version of this test measured a second-row cell and
        // read a "scale" of .975 that was really the fold.
        assertEquals(
            "the measured cell is clipped by the scroll viewport, so its bounds are not its size",
            resting.width / 0.66f,
            resting.height,
            1.5f,
        )

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag(benchPageCellTag(2)).performTouchInput { down(center) }
        // Past the tap timeout and then past the 100ms tween — frame by frame, because a single large
        // `advanceTimeBy` jumps the clock without giving the animation the frames it needs to run, and
        // reads back a value mid-tween.
        repeat(60) { composeRule.mainClock.advanceTimeByFrame() }
        val pressed = bounds(benchPageCellTag(2))
        composeRule.mainClock.autoAdvance = true
        composeRule.onNodeWithTag(benchPageCellTag(2)).performTouchInput { up() }

        assertEquals("scale(.96) of the resting width", resting.width * 0.96f, pressed.width, 1.5f)
        assertEquals("scale(.96) of the resting height", resting.height * 0.96f, pressed.height, 1.5f)
    }

    /**
     * Row 5.14: the frozen `.pgcell b{text-transform:uppercase; letter-spacing:.1em}` (`:383`).
     *
     * The case was missing until the second review pass — the build painted `Cover`, the freeze paints
     * `COVER` — and the goldens had been recorded from the wrong output, so the raster certified it. This
     * reads the text, which no golden can do.
     */
    @Test
    fun the_two_covers_wear_their_names_in_capitals() {
        setScreen(store())
        composeRule.onNodeWithTag(BenchGridButtonTestTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("COVER").assertIsDisplayed()
        // The back cover is the eighth cell, which at this host size sits in the grid's third row —
        // below the fold of the canvas the grid is scoped to. It is reachable (the grid scrolls), and
        // whether a first-time user *knows* to scroll for it is a Pass 2 question, recorded as such.
        composeRule.onNodeWithText("BACK").performScrollTo().assertIsDisplayed()
    }

    /**
     * Row 5.11: the frozen grid **arrives from below** — `transform:translateY(102%)` settling to `0` over
     * `.3s var(--settle)` (`v2-bench.html:374-375`).
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
}
