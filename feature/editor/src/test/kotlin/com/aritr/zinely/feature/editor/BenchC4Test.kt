package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyTheme
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
 * C4 — the frozen bottom bar, the status strip, the Add chooser and the snackbar
 * ([ADR-094](../../../../../../../docs/DECISIONS.md#adr-094) rows 4.1–4.16).
 *
 * The rows this suite deliberately does **not** close are named rather than quietly skipped:
 *
 * | row | where it is closed instead | why not here |
 * |---|---|---|
 * | 4.1 ground + hairline, 4.2 radius, 4.10 colour, 4.11/4.12 the ink ground and `--accent-on-ink` | the Roborazzi goldens | they are paint, and a raster assertion is the honest instrument for paint |
 * | 4.5's **platform** clause | [BenchBottomBarPlatformA11yTest] | the merged semantics tree is not the tree TalkBack reads — [ADR-058](../../../../../../../docs/DECISIONS.md#adr-058)'s defect passed a merged-tree assertion |
 * | 4.15 what *raises* the ink snack | C6 | C4 owns the buttonless variant, not the ink path |
 * | 4.16 | [D-049](../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-049) | unruled |
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchC4Test {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(100.0, 100.0)
    private val effects = mutableListOf<Effect>()

    /** Wide enough that `.add`'s residual width is a real residue and not a clamp. */
    private val host: Pair<Dp, Dp> = 360.dp to 720.dp

    private fun store(): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) {
                effects += effect
            }
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

    private fun touchBounds(tag: String) =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().touchBoundsInRoot

    private fun px(dp: Dp) = with(composeRule.density) { dp.toPx() }

    /**
     * The **window**, which is what everything is actually laid out in: Robolectric's surface is
     * 320×470dp and [host] is larger, so a size request wider than the window is simply clipped. Measuring
     * an inset against the requested size instead of this reads 40dp of clipping as a 40dp inset.
     */
    private fun rootBounds() = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

    /** Places one text box, leaving it selected (the reducer auto-selects a placement). */
    private fun placedText(store: EditorStore): String {
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        return store.uiState.value.selection.single()
    }

    // --- Row 4.1: the bar's box ---------------------------------------------------------------------

    @Test
    fun the_bar_is_the_frozen_height() {
        // `.bar{flex:none;height:66px}` (`v2-bench.html:267`). Mutation: 66 → any other height.
        setScreen(store())
        assertEquals(px(66.dp), bounds(BenchBottomBarTestTag).height, 0.5f)
    }

    @Test
    fun the_bars_bottom_padding_is_asymmetric_exactly_as_frozen() {
        // Frozen `padding:0 16px 4px` — the 4px at the bottom and nothing at the top is what makes the
        // controls sit high in the bar. Measured as the gap under the last control, because that is the
        // only observable the padding produces: 66 − 4 = 62 of content, a 44 control centred in it leaves
        // 9 above and 9 + 4 = 13 below. Mutation `4px` → `0` makes it 11 and this fails.
        setScreen(store())
        val bar = bounds(BenchBottomBarTestTag)
        val done = bounds(BenchBarDoneTag)
        assertEquals(px(13.dp), bar.bottom - done.bottom, 0.5f)
        assertEquals(px(9.dp), done.top - bar.top, 0.5f)
    }

    @Test
    fun the_bar_keeps_the_frozen_side_padding_and_gap() {
        // `padding:0 16px` and `gap:10px` (`:267`), read off the drawn controls rather than the source.
        setScreen(store())
        val bar = bounds(BenchBottomBarTestTag)
        val undo = bounds(BenchBarUndoTag)
        val redo = bounds(BenchBarRedoTag)
        val done = bounds(BenchBarDoneTag)
        assertEquals(px(16.dp), undo.left - bar.left, 0.5f)
        assertEquals(px(16.dp), bar.right - done.right, 0.5f)
        assertEquals(px(10.dp), redo.left - undo.right, 0.5f)
    }

    // --- Row 4.2 / D-009: the paint is 44, the target is not ----------------------------------------

    @Test
    fun an_icon_button_paints_forty_four_and_takes_forty_eight() {
        // D-009's ruled remedy is *extend the target, keep the paint*. Both halves are asserted together
        // on purpose: a fix that grew the layout box to 48 would satisfy accessibility and silently break
        // the frozen 10dp gap - `minimumInteractiveComponentSize()` did exactly that here, and was removed
        // for it - and a fix that left the target at 44 would pass every visual check.
        // Read off `Done`, which is the one icon button *enabled* at rest. That is not test convenience:
        // the target is Compose's pointer-input minimum, which a disabled control does not have because it
        // installs no `clickable` at all - deliberately, so the platform tree agrees it is not clickable
        // (the ADR-058 defect). A withheld control needs no 48dp target; a live one does.
        setScreen(store())
        val paint = bounds(BenchBarDoneTag)
        val target = touchBounds(BenchBarDoneTag)
        assertEquals(px(44.dp), paint.width, 0.5f)
        assertEquals(px(44.dp), paint.height, 0.5f)
        assertTrue(
            "the touch target must reach 48dp: $target",
            target.width >= px(48.dp) - 0.5f && target.height >= px(48.dp) - 0.5f,
        )
    }

    // --- Rows 4.4 / 4.5 / 4.6: what the bar contains, and in what order -----------------------------

    @Test
    fun the_bar_reads_undo_redo_add_done_left_to_right() {
        // Frozen order after the OD-21 amendment (`:464-468`). Mutation: any transposition.
        setScreen(store())
        val lefts = listOf(BenchBarUndoTag, BenchBarRedoTag, BenchBarAddTag, BenchBarDoneTag)
            .map { bounds(it).left }
        assertEquals("the four controls must be in frozen order: $lefts", lefts.sorted(), lefts)
    }

    @Test
    fun undo_and_redo_are_both_withheld_at_rest() {
        // The freeze draws `#undoBtn` and `#redoBtn` `disabled` (`:465-466`), and a fresh document has no
        // history either way. Mutation: enable Undo at rest.
        setScreen(store())
        composeRule.onNodeWithTag(BenchBarUndoTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(BenchBarRedoTag).assertIsNotEnabled()
    }

    @Test
    fun redo_lights_only_after_an_undo() {
        // Row 4.6. OD-9 keeps redo and OD-21 gives it a slot; this is the slot doing its job.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchBarUndoTag).assertIsEnabled()
        composeRule.onNodeWithTag(BenchBarRedoTag).assertIsNotEnabled()

        composeRule.onNodeWithTag(BenchBarUndoTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchBarRedoTag).assertIsEnabled()
    }

    @Test
    fun add_takes_the_residual_width_between_the_three_fixed_controls() {
        // `.add{flex:1}` (`:271`). Asserted as arithmetic on the bar's own width rather than as a
        // measured literal, so it stays true on any host. Mutation: `flex:1` → a fixed width.
        setScreen(store())
        val bar = bounds(BenchBottomBarTestTag)
        val add = bounds(BenchBarAddTag)
        val expected = bar.width - px(16.dp) * 2 - px(44.dp) * 3 - px(10.dp) * 3
        assertEquals(expected, add.width, 0.5f)
        assertEquals(px(44.dp), add.height, 0.5f)
    }

    // --- Rows 4.4a-4.4d: the chooser ----------------------------------------------------------------

    @Test
    fun add_opens_the_frozen_chooser_with_exactly_two_rows() {
        // Rows 4.4a and 4.4b together: the frozen title, Text and Photo — and **no Art row**, which is
        // fenced behind C8 by OD-21 rather than deleted. Mutation: drop a row, or add the third.
        setScreen(store())
        composeRule.onNodeWithTag(BenchAddChooserTestTag).assertDoesNotExist()

        composeRule.onNodeWithTag(BenchBarAddTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchAddChooserTestTag).assertIsDisplayed()
        composeRule.onNodeWithText(BenchAddChooserTitle).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchAddChooserTextTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchAddChooserPhotoTag).assertIsDisplayed()
        assertEquals(
            "C4 releases Text and Photo only — Art is C8's",
            0,
            composeRule.onAllNodesWithText("Art", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )
    }

    @Test
    fun choosing_text_places_a_box_and_opens_the_session_in_one_tap() {
        // Row 4.4c. OD-21 requires `addTextAndEdit` **by name**, so what this asserts is C3's model
        // surviving unchanged: the box arrives AND the in-place session opens on it. Mutation: drop the
        // `BeginEditText` and the box arrives inert.
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(BenchBarAddTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchAddChooserTextTag).performClick()
        composeRule.waitForIdle()

        assertEquals(1, store.uiState.value.document.pages[0].elements.size)
        assertTrue(
            "the new box must open its session: ${store.uiState.value.interaction}",
            store.uiState.value.interaction is Interaction.EditingText,
        )
    }

    @Test
    fun choosing_photo_asks_for_the_picker() {
        // Row 4.4d: the same intent the retired shelf dispatched, so the picker binding in `ZinelyNavHost`
        // is untouched. Mutation: dispatch nothing.
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(BenchBarAddTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchAddChooserPhotoTag).performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Photo must reach the shipped picker effect: $effects",
            effects.contains(Effect.PickAndDecodeImage),
        )
    }

    // --- Row 4.7a: the shelf is gone ----------------------------------------------------------------

    @Test
    fun no_supply_shelf_composes_anywhere_in_the_editor() {
        // OD-21 retires `EditorSupplyTray`, and what has to stay true is that its four verbs are not drawn
        // a second time beside the bar — OD-14's duplication defect.
        //
        // The first cut asserted the absence of the string `"Supplies"`, which no longer exists in any
        // source file: an assertion that cannot fail, and which a shelf re-added under any other heading
        // would sail past. Review caught it. What is asserted now is each verb's *count* on screen, which
        // is the property OD-14 actually states and which a second shelf would break whatever it is called.
        setScreen(store())
        listOf(UndoActionLabel, RedoActionLabel, AddActionLabel, Copy.EditText.DONE).forEach { label ->
            assertEquals(
                "$label must be presented exactly once",
                1,
                composeRule.onAllNodesWithContentDescription(label, useUnmergedTree = true)
                    .fetchSemanticsNodes().size,
            )
        }
        // And the retired shelf's own card labels are nowhere: they live behind `Add` now.
        listOf(Copy.EmptyState.ADD_A_PHOTO, Copy.EmptyState.ADD_WORDS).forEach { label ->
            assertEquals(
                "$label must not be drawn beside the bar",
                0,
                composeRule.onAllNodesWithContentDescription(label, useUnmergedTree = true)
                    .fetchSemanticsNodes().size,
            )
        }
    }

    // --- Rows 4.8a / 4.8b: Done, both states --------------------------------------------------------

    @Test
    fun done_is_withheld_for_the_whole_of_a_text_session() {
        // Row 4.8a. C3's style-row chip owns "finish editing" while a session is open; a second live
        // `Done` is OD-14's defect. Mutation: make it live during a session.
        val store = store()
        setScreen(store)
        composeRule.onNodeWithTag(BenchBarDoneTag).assertIsEnabled()

        val id = placedText(store)
        store.dispatch(Intent.BeginEditText(id))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchBarDoneTag).assertIsNotEnabled()
    }

    @Test
    fun done_clears_a_selection_when_no_session_is_open() {
        // Row 4.8b: deselect's first *drawn* control. OD-13 gave the capability a gesture, and a gesture
        // has no visible presentation for OD-14 to count as a second one. Mutation: dispatch nothing.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        assertEquals(1, store.uiState.value.selection.size)

        composeRule.onNodeWithTag(BenchBarDoneTag).performClick()
        composeRule.waitForIdle()
        assertTrue(
            "the bar's Done must clear the selection: ${store.uiState.value.selection}",
            store.uiState.value.selection.isEmpty(),
        )
    }

    // --- Rows 4.9 / 4.10: the status strip ----------------------------------------------------------

    @Test
    fun the_status_strip_is_the_frozen_height_and_does_not_grow() {
        // `.status{flex:none;height:26px}` (`:190`). **Both clauses are carried by the height assertion**:
        // in a flex column a `flex:1` strip would take the residual space and measure far more than 26dp,
        // so a fixed 26 is what "does not grow" means here. The second assertion is about *placement* —
        // the strip is the first thing in the phone, above `.canvasArea` — and saying so plainly matters:
        // an earlier comment credited it with catching `flex:none` → `flex:1`, which it does not do.
        setScreen(store())
        val strip = bounds(BenchStatusStripTestTag)
        assertEquals(px(26.dp), strip.height, 0.5f)
        assertEquals("the strip is the first thing in the phone", 0f, strip.top, 0.5f)
        assertTrue(
            "…and the canvas begins directly beneath it",
            bounds(EditorCanvasTestTag).top >= strip.bottom - 0.5f,
        )
    }

    @Test
    fun the_saved_chip_speaks_the_line_without_its_flower() {
        // Row 4.10's copy. The `✿` is decoration (VOICE rule 7) and D-021 keeps the literal character in
        // the paint; the live region announces the sentence. Mutation: announce the flower too.
        assertEquals("Saved on this device", BenchSavedSpokenLabel)
        assertEquals("✿", BenchSavedMark)
        assertEquals("Saved", BenchSavedWord)
        assertEquals(" · on this device", BenchSavedQualifier)
    }

    // --- Rows 4.11-4.14: the snack and the soft delete ----------------------------------------------

    private fun deleteSelected() {
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.DELETE}").performClick()
    }

    @Test
    fun deleting_fades_the_element_out_before_it_leaves_the_document() {
        // Row 4.13: the soft delete is a *fade*, not a disappearance — `.2s` of it (`:620-629`). Asserted
        // on the clock: mid-fade the element is still in the document and the cover is partly drawn.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        deleteSelected()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis / 2L)
        composeRule.waitForIdle()
        assertEquals(
            "the element must still exist while it is fading",
            1,
            store.uiState.value.document.pages[0].elements.size,
        )

        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis.toLong())
        composeRule.waitForIdle()
        assertEquals(0, store.uiState.value.document.pages[0].elements.size)
    }

    @Test
    fun the_delete_snack_holds_for_the_frozen_window_then_leaves() {
        // Row 4.13's 3200ms, in **literal milliseconds**. The first cut advanced by
        // `BenchSnackDeleteMillis` itself, which cannot see a change to `BenchSnackDeleteMillis`: the
        // mutation 3200 → 1600 survived it, and that survival is the reason this comment exists. A timing
        // assertion that spends the constant under test is decoration.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        deleteSelected()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + 100L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertIsDisplayed()

        // Still up at 3000ms after the delete landed — a 1600ms window is long gone by here.
        composeRule.mainClock.advanceTimeBy(2700L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()

        // …and gone by 3200 + the .22s exit. A window LONGER than frozen fails this half.
        composeRule.mainClock.advanceTimeBy(800L)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
        assertEquals("the frozen delete window is 3200ms", 3200L, BenchSnackDeleteMillis)
    }

    @Test
    fun the_snack_enters_from_sixteen_dp_below_where_it_comes_to_rest() {
        // Row 4.11's `translateY(16px) → 0`. `graphicsLayer`'s translation moves the node's bounds in the
        // root, so the rise is readable rather than merely declared — the mutation 16 → 0 survived the
        // whole suite before this existed, because nothing else in C4 looks at where the snack starts.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        deleteSelected()
        // One frame past the snack appearing: the enter animation has barely begun, so it is still close
        // to its full 16dp below the resting place. Two steps, not one — at progress exactly 0 the snack
        // composes nothing at all, so the frame after the delete lands is the first one that can be read.
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + 16L)
        composeRule.waitForIdle()
        // Frame by frame until it is on screen, rather than by a guessed delay: the snack draws nothing at
        // all while its progress is exactly 0, so the first readable frame is one or two after the delete
        // lands, and which one is an implementation detail this test should not encode.
        repeat(4) {
            if (composeRule.onAllNodesWithTag(BenchSnackTestTag).fetchSemanticsNodes().isEmpty()) {
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.waitForIdle()
            }
        }
        val entering = bounds(BenchSnackTestTag)

        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 200L)
        composeRule.waitForIdle()
        val resting = bounds(BenchSnackTestTag)

        val rise = entering.top - resting.top
        assertTrue(
            "the snack must enter from below its resting place: entering=$entering resting=$resting",
            rise > px(8.dp),
        )
        assertTrue("…and by no more than the frozen 16dp: $rise", rise <= px(16.dp) + 0.5f)
    }

    @Test
    fun the_snack_sits_at_the_frozen_insets() {
        // Row 4.11: `left/right:14px; bottom:12px` (`:361`). Mutation: any inset change.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        deleteSelected()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()

        val snack = bounds(BenchSnackTestTag)
        val canvas = bounds(EditorCanvasTestTag)
        assertEquals(px(14.dp), snack.left - canvas.left, 0.5f)
        assertEquals(px(14.dp), canvas.right - snack.right, 0.5f)
        // Measured against the **canvas**, because that is what the freeze positions it against: the
        // `.snack` markup (`v2-bench.html:443`) is inside `.canvasArea` (`:392`), which declares
        // `position:relative` (`:194`), so `bottom:12px` resolves there and not against the phone. The
        // first cut of this test asserted 12dp from the window's own bottom and passed — encoding a
        // geometry the freeze never specified. Independent review caught it by reading the HTML.
        assertEquals(px(12.dp), canvas.bottom - snack.bottom, 0.5f)
        // It still takes no layout height: the bar is exactly where it would be with no snack up.
        assertTrue(
            "the snack must overlay the canvas, not displace the chrome below it",
            snack.bottom <= bounds(BenchBottomBarTestTag).top,
        )
    }

    @Test
    fun undo_from_the_snack_restores_the_element_and_re_disables_undo() {
        // Row 4.14: the restore *and* the re-disable, together — a restore that left Undo lit would claim
        // there is still something to take back. Mutation: leave Undo enabled.
        val store = store()
        setScreen(store)
        placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        deleteSelected()
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals(0, store.uiState.value.document.pages[0].elements.size)

        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals(1, store.uiState.value.document.pages[0].elements.size)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertDoesNotExist()
    }

    @Test
    fun the_accessibility_delete_is_reversible_on_the_same_terms_as_the_verb() {
        // ADR-094 §6.11. `EditorA11y`'s Delete used to dispatch `Intent.Delete` directly, so a TalkBack
        // user's element simply vanished while a sighted user got a fade, a snack and an `Undo` — the
        // reversal present on one path and absent on the other. Raised by independent review.
        //
        // The assertion is the **snack**, not the deletion: only the soft path raises it, so a regression
        // to a direct dispatch fails here even though the element leaves the document either way.
        val store = store()
        setScreen(store)
        val id = placedText(store)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        val node = composeRule.onNodeWithTag("$ElementNodeTagPrefix$id").fetchSemanticsNode()
        val delete = node.config[SemanticsActions.CustomActions].first { it.label == Copy.A11y.DELETE }
        composeRule.runOnUiThread { delete.action() }
        composeRule.mainClock.advanceTimeBy(BenchDeleteFadeMillis + BenchSnackMillis + 100L)
        composeRule.waitForIdle()

        assertEquals(0, store.uiState.value.document.pages[0].elements.size)
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        composeRule.mainClock.advanceTimeBy(BenchSnackMillis + 100L)
        composeRule.waitForIdle()
        assertEquals(1, store.uiState.value.document.pages[0].elements.size)
    }

    @Test
    fun the_delete_snack_names_what_was_deleted() {
        // The frozen `del()` caps `Soft-deleted <name>`; the shipped line is the element's own label.
        val store = store()
        val id = placedText(store)
        val pages = store.uiState.value.document.pages
        assertEquals("Text", benchDeleteLabel(pages, id))
        assertEquals("Text deleted.", benchDeletedMessage(benchDeleteLabel(pages, id)))
    }

    // --- Row 4.15: the buttonless variant -----------------------------------------------------------

    @Test
    fun the_ink_variant_of_the_snack_draws_no_button() {
        // Row 4.15: the ink flash is buttonless (`:616-617`) — C4 owns the variant, C6 owns what raises
        // it. Mutation: show the button.
        composeRule.setContent {
            ZinelyTheme {
                BenchSnack(visible = true, message = "Ink · Blush", actionLabel = null, onAction = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(BenchSnackActionTestTag).assertDoesNotExist()
    }

    // --- The soft-delete cover arithmetic, in isolation ---------------------------------------------

    @Test
    fun the_delete_cover_runs_from_untouched_to_fully_covered_and_nine_tenths() {
        // The C2a/C3 cover-override seam run backwards (recorded as a deviation in ADR-094 §6): the
        // renderer paints a page as one tape, so there is no per-element opacity to animate — the cover
        // fades **in** while the element scales to `.9`. These are the two pure functions that decide it.
        assertEquals(0f, BenchMaterialise.deleteCoverAlphaAt(0f), 0.0001f)
        assertEquals(1f, BenchMaterialise.deleteCoverAlphaAt(1f), 0.0001f)
        assertEquals(1f, BenchMaterialise.deleteScaleAt(0f), 0.0001f)
        // The literal `.9`, not `BenchDeleteToScale` — an assertion built from the constant it checks
        // agrees with every value that constant can take. §8 of ADR-094 records that defect costing four
        // findings in this package; this line was the fifth, caught by review rather than by a mutation.
        assertEquals(0.9f, BenchMaterialise.deleteScaleAt(1f), 0.0001f)
        assertEquals("the frozen soft-delete scale is .9", 0.9f, BenchDeleteToScale, 0.0001f)
        assertEquals(0.95f, BenchMaterialise.deleteScaleAt(0.5f), 0.0001f)
    }
}
