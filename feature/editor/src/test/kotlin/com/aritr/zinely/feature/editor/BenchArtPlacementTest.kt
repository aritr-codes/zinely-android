package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.core.render.SupplyCatalog
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
 * The whole path a maker walks: **Add → Art → a tile → a supply on the page** (SUPPLIES-SPEC §5,
 * ADR-105 step S7).
 *
 * [BenchArtSheetTest] already proves the sheet's own contract in isolation (which tiles are pickable, what
 * they are called). What is asserted here is the seam that did not exist until this package — the sheet had
 * **no production call site**, so every one of those assertions was about a composable nothing reached.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BenchArtPlacementTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val pageSizePt = PtSize(153.0, 198.0)
    private val effects = mutableListOf<Effect>()

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
                EditorScreen(store = store, pageSizePt = pageSizePt, modifier = Modifier.size(360.dp, 720.dp))
            }
        }
        composeRule.waitForIdle()
    }

    private fun openArt() {
        composeRule.onNodeWithTag(BenchBarAddTag).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchAddChooserArtTag).performClick()
        composeRule.waitForIdle()
    }

    private fun decor(store: EditorStore) =
        store.uiState.value.document.pages[0].elements.filterIsInstance<DecorElement>()

    @Test
    fun `Given the Add chooser, When Art is chosen, Then the cabinet opens and the chooser stands down`() {
        setScreen(store())
        openArt()

        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertIsDisplayed()
        // One sheet at a time: the freeze swapped `#sheet`'s innerHTML; two Compose Dialogs must not stack.
        composeRule.onNodeWithTag(BenchAddChooserTestTag).assertDoesNotExist()
    }

    @Test
    fun `Given the cabinet, When an authored tile is tapped, Then the supply is on the page and the sheet is gone`() {
        val store = store()
        setScreen(store)
        openArt()

        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()

        val placed = decor(store).single()
        assertEquals("shape.circle", placed.supplyId)
        // The fixture is an authored supply on purpose: an unauthored one draws nothing, and a placement
        // test whose subject paints no pixels proves less than it looks like it does.
        assertTrue("the fixture must be a supply that can actually be drawn", SupplyCatalog.outlineOf(placed.supplyId) != null)
        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertDoesNotExist()
    }

    @Test
    fun `Given a placed supply, When the screen settles, Then it is selected and offers decor's verbs`() {
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.rect")).performClick()
        composeRule.waitForIdle()

        // The freeze's `selectByKind('decor')`, delivered by the reducer's own auto-select. The context bar
        // is the visible consequence, and it is the one that would fail silently: `benchVerbKindOf` is
        // exhaustive, but nothing before this package ever reached its DECOR arm from the UI.
        assertEquals(setOf(decor(store).single().id), store.uiState.value.selection)
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertIsDisplayed()
    }

    @Test
    fun `Given the cabinet, When an unauthored tile is tapped, Then nothing is placed`() {
        val store = store()
        setScreen(store)
        openArt()

        // `tape.torn` has no authored outline, so its tile carries no click at all. `performClick` on a
        // node with no click action is a no-op — the assertion is that the document did not move.
        composeRule.onNodeWithTag(benchArtTileTestTag("tape.torn")).performClick()
        composeRule.waitForIdle()

        assertTrue("an unauthored supply must not reach the page", decor(store).isEmpty())
        // …and the cabinet stays open. This half is a **regression test for a defect this package found**:
        // an inert tile had no pointer-input node, so the touch fell through to ZSheet's scrim sibling and
        // dismissed the sheet. Twelve of sixteen tiles closed the cabinet when tapped. Mutation: delete the
        // `pointerInput` from `BenchArtTile`'s inert branch and this line goes red.
        composeRule.onNodeWithTag(BenchArtSheetTestTag).assertIsDisplayed()
    }

    @Test
    fun `Given a placement, When it lands, Then it is centred at its family's default size`() {
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.triangle")).performClick()
        composeRule.waitForIdle()

        // The screen must use the same geometry the pure helper computes — not a second, drifting copy.
        assertEquals(benchSupplyPlacement("shape.triangle", pageSizePt), decor(store).single().transform)
    }

    @Test
    fun `Given a placement, When it lands, Then it is inked from the maker palette and not left blank`() {
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.rule")).performClick()
        composeRule.waitForIdle()

        val ink = decor(store).single().ink
        assertEquals("a placed supply must be opaque", 255, ink.a)
        assertTrue("…and must not land as invisible white", ink.r < 200 || ink.g < 200 || ink.b < 200)
    }

    @Test
    fun `Given a placed supply, When it is deleted, Then the snack calls it Art and not Photo`() {
        // Independent review's Required Fix 1. `benchDeleteLabel` was `if (text) … else PHOTO` — a two-way
        // test over a three-way sealed hierarchy — so a supply was deleted as *"Photo deleted."*, including
        // out loud through the TalkBack Delete action. Harmless only while decor was unreachable, which is
        // the state this package ends. Mutation: restore the `else`.
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()

        val id = decor(store).single().id
        assertEquals(BenchAddArtTitle, benchDeleteLabel(store.uiState.value.document.pages, id))
        assertEquals(Copy.Snack.deleted(BenchAddArtTitle), benchDeletedMessage(BenchAddArtTitle))
    }

    @Test
    fun `Given a placement, When Undo is pressed on the bar, Then the supply leaves the page`() {
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()
        assertEquals(1, decor(store).size)

        composeRule.onNodeWithTag(BenchBarUndoTag).performClick()
        composeRule.waitForIdle()

        assertTrue("one placement is one undo step", decor(store).isEmpty())
    }

    @Test
    fun `Given a placement, When it lands, Then the frozen toast says so and offers Undo`() {
        // The freeze's `toast('Placed on the page', true)` (`v21-bench.html:862`) — the message AND the
        // `undoable=true` half, which is the affordance that makes an unwanted supply a one-tap mistake.
        // Mutation: delete the snack block in `placeSupply`, or pass `snackAction = null`.
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BenchSnackTestTag).assertIsDisplayed()
        // By content description, not by text: the message node is `clearAndSetSemantics` with a polite
        // live region, so the string a screen reader hears is the one asserted here — which is the copy
        // that matters most on a surface that disappears on a timer.
        composeRule.onNodeWithContentDescription(Copy.Snack.PLACED).assertIsDisplayed()

        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        composeRule.waitForIdle()
        assertTrue("the snack's Undo must reverse the placement", decor(store).isEmpty())
    }

    @Test
    fun `Given a selected element, When the cabinet opens, Then the context bar stands down`() {
        // `showSheet` removes `.ctx` (BenchState.Adding). The Art sheet is a *second* sheet, so the Bench
        // state term has to name it too. Mutation: drop `|| artSheetOpen` from `benchStateOf`'s argument in
        // EditorScreen and the bar survives behind the open cabinet.
        val store = store()
        setScreen(store)
        openArt()
        composeRule.onNodeWithTag(benchArtTileTestTag("shape.circle")).performClick()
        composeRule.waitForIdle()
        // The placement auto-selected, so the context bar is up — the precondition, asserted so this test
        // cannot pass by the bar never having been there.
        composeRule.onNodeWithTag(BenchContextBarTestTag).assertIsDisplayed()

        openArt()

        composeRule.onNodeWithTag(BenchContextBarTestTag).assertDoesNotExist()
    }

    @Test
    fun `Given the sixteen, When the cabinet opens on the real screen, Then every tile is there`() {
        // The chooser's promise — "tape, stamps and cut paper live here" — is true of the *cabinet*, not of
        // every tile in it, so the cabinet must hold all sixteen however few are authored.
        //
        // ⚠ This test also used to re-assert a hard-coded set of four authored ids, under a comment saying
        // it was pinned against the catalogue so it would "follow instead of failing". It did neither: it
        // duplicated `SupplyCatalogTest`'s membership assertion and then failed the day the catalogue grew.
        // The pickable-iff-authored correspondence now lives once, in `BenchArtSheetTest`, exercised
        // through the sheet rather than read out of the catalogue. What is unique *here* is the wiring
        // through `EditorScreen`, which is all this keeps.
        setScreen(store())
        openArt()
        for (id in Copy.Supplies.NAMES.keys) {
            composeRule.onNodeWithTag(benchArtTileTestTag(id)).assertExists()
        }
        assertEquals("the freeze specifies sixteen", 16, Copy.Supplies.NAMES.size)
    }
}
