package com.aritr.zinely.feature.library

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineCoverRecipe
import com.aritr.zinely.core.model.ZineCoverStamp
import com.aritr.zinely.core.model.ZineCoverSurface
import com.aritr.zinely.feature.editor.HomePaperChooserTestTag
import com.aritr.zinely.feature.editor.HomeRenameConfirmTestTag
import com.aritr.zinely.feature.editor.HomeRenameFieldTestTag
import com.aritr.zinely.feature.editor.HomeShelfEvent
import com.aritr.zinely.feature.editor.homeDeletedMessage
import com.aritr.zinely.feature.editor.homePaperChoiceTestTag
import com.aritr.zinely.ui.golden.rasterizeToBitmap
import com.aritr.zinely.ui.theme.ZinelyTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * The V2 Library **screen** — B5's integration package ([ADR-086](docs/DECISIONS.md#adr-086)).
 *
 * What this file asserts, and what it deliberately does not: every property here belongs to the *screen* —
 * the ground, the four states and their exclusivity, where the dock stands, which zine an action lands on,
 * and where each action goes. The insides of B1–B4's components are proven at the component level, and
 * re-asserting them here would be duplication ([ADR-085](docs/DECISIONS.md#adr-085) decision 5).
 *
 * **The exclusivity halves are the point.** For each state this asserts the state's own surface is present
 * *and* that the other states' surfaces are absent. Phase A's record is that the last eight consecutive
 * packages each shipped an assertion blind to the defect class it claimed to gate, and this screen's whole
 * defect class has exactly that shape: a screen rendering both the shelf and the invitation looks perfect
 * in a screenshot of either one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h960dp", sdk = [28])
class ZineLibraryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val shared = mutableListOf<String>()
    private val started = mutableListOf<PaperSize>()
    private val renamed = mutableListOf<Pair<String, String>>()
    private val duplicated = mutableListOf<String>()
    private val deleted = mutableListOf<String>()
    private val undone = mutableListOf<String>()
    private val committed = mutableListOf<String>()
    private var retries = 0

    private val events = Channel<HomeShelfEvent>(Channel.BUFFERED)

    private var deskColor: Color = Color.Unspecified
    private var leafColor: Color = Color.Unspecified

    // ---------------------------------------------------------------------------------------------
    // Row 1 — the ground
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the screen's ground is the desk, in both themes`() {
        // `.phone{background:var(--desk)}`. B1's cover and B2's shelf each paint no ground of their own
        // and record that they owe it to whatever places them; this is that place, so the claim is a
        // *pixel* rather than a modifier. Probed inside the shelf's own 30px top and 22px side padding —
        // bare ground, clear of the heading and of every cover's cast shadow.
        for (dark in listOf(false, true)) {
            content(zines(2), dark = dark)
            assertTrue(
                "the ground above the shelf head is not --desk (dark=$dark)",
                decorRaster().matches(GROUND_PROBE, GROUND_PROBE, deskColor),
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rows 2, 3, 16a — the dock
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the dock is bottom-anchored over the shelf, not stacked after it`() {
        content(zines(6))
        val root = bounds(ZineLibraryTestTag)
        val dock = bounds(ZineDockTestTag)
        val shelf = bounds(ZineLibraryShelfTestTag)

        // `.dock{position:absolute;left:0;right:0;bottom:0}` — both halves together. Touching the bottom
        // edge alone would also hold for a dock placed in a `Column` after the shelf; the shelf running
        // *under* it is what separates "floating over" from "placed after", and the two are identical at
        // rest. The shelf's own `padding-bottom:152px` is what keeps a cover from hiding beneath it.
        assertEquals("the dock does not reach the bottom edge", root.bottom, dock.bottom, HALF_PIXEL)
        assertTrue(
            "the shelf stops above the dock — it is stacked after it, not overlaid by it",
            shelf.bottom > dock.top + 1f,
        )
    }

    @Test
    fun `the dock stands in all four states`() {
        // `.dock` sits outside `.empty` and outside `.fail`, and **no** `body.is-*` rule targets it —
        // read off the frozen file rather than inferred. The ruling gives the reason: *"the dock is part
        // of the workspace rather than the loaded content."* So a user whose shelf failed to open can
        // still start a zine, and the empty state's only exit is never hidden by a slow read.
        for (state in fourStates()) {
            render(state)
            composeRule.onNodeWithTag(ZineDockTestTag).assertIsDisplayed()
            composeRule.onNodeWithTag(ZineStartTestTag).assertIsDisplayed()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rows 4, 5 — empty replaces the shelf
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the empty state fills the screen`() {
        render(LibraryShelfState.Empty)
        // `.empty{position:absolute;inset:0}` — the invitation IS the screen, not a card in the middle
        // of one. A shrink-wrapping column centres identically and is a different design.
        assertEquals(bounds(ZineLibraryTestTag), bounds(ZineShelfEmptyTestTag))
    }

    @Test
    fun `an empty store shows the invitation and no shelf, and zines show the inverse`() {
        render(LibraryShelfState.Empty)
        composeRule.onNodeWithTag(ZineShelfEmptyTestTag).assertIsDisplayed()
        // The half that matters: `body.is-empty .shelf{display:none}`. A screen that renders both passes
        // every "the invitation appears" assertion anyone would think to write.
        composeRule.onNodeWithText(SHELF_HEADING).assertDoesNotExist()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(0)).assertDoesNotExist()

        // and the failure state belongs to neither: an error surface that survives into a loaded or an
        // empty shelf is the same class of defect as the shelf surviving into the invitation, and until
        // now only Loading asserted it away.
        composeRule.onNodeWithTag(ZineShelfFailTestTag).assertDoesNotExist()

        content(zines(2))
        composeRule.onNodeWithText(SHELF_HEADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ZineShelfEmptyTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineShelfFailTestTag).assertDoesNotExist()
    }

    // ---------------------------------------------------------------------------------------------
    // Rows 6, 7 — covers only; the metadata is disclosed in the sheet
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `a cover carries the title, and the shelf discloses no metadata`() {
        content(zines(3))
        composeRule
            .onNodeWithTag(zineShelfCoverTestTag(0), useUnmergedTree = true)
            .assert(hasAnyDescendant(hasText(titleOf(0))))
        // *"Covers only — no metadata line … Format & date are disclosed there, not stamped on every
        // card"* (`:142-144`). Every zine carries the line and none of them may draw it here: a subtitle
        // that leaks onto the shelf is a one-line mistake that looks like a feature.
        //
        // **Unmerged**, deliberately. B1's cover clears its own semantics, so a subtitle drawn inside it
        // would be invisible to a merged-tree search — the negative would pass while the line was on
        // screen, which is the blind-assertion failure this programme keeps finding. The same finder
        // proves the title above, so it demonstrably reaches text inside a cover.
        for (i in 0 until 3) {
            composeRule.onNodeWithText(subtitleOf(i), useUnmergedTree = true).assertDoesNotExist()
        }
    }

    @Test
    fun `the sheet discloses the format and date of the zine that was pressed`() {
        openSheetFor(2)
        // The element and its position meet in one matcher (ADR-082's rule): it is the sheet's *subtitle
        // node* that must carry the *third* zine's line. "The sheet shows a subtitle" passes on a sheet
        // hard-wired to the first zine, which is the defect this row exists to gate.
        composeRule.onNodeWithTag(ZineActionSubtitleTestTag).assertTextEquals(subtitleOf(2))
        composeRule.onNodeWithTag(ZineActionTitleTestTag).assertTextEquals(titleOf(2))
    }

    // ---------------------------------------------------------------------------------------------
    // Row 12 — the repository's order, unmodified
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `the shelf stands the zines in the order it was given`() {
        val given = zines(4)
        content(given)
        // Title **and** position in one matcher, so `.reversed()` and an added `sortedBy` both fail here.
        // "All four titles appear" would pass on either. The frozen file states no sort, V1's sort
        // control was dropped by ruling, and the repository's newest-first contract is a data-layer fact
        // — re-deriving an order on this screen would be inventing design out of silence (D-020).
        given.forEachIndexed { index, zine ->
            composeRule
                .onNodeWithTag(zineShelfCoverTestTag(index), useUnmergedTree = true)
                .assert(hasAnyDescendant(hasText(zine.title)))
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Row 15 — loading
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `loading stands placeholders in the head's kept space, and never the invitation`() {
        render(LibraryShelfState.Loading)

        composeRule.onAllNodesWithTag(ZineShelfPlaceholderTestTag).assertCountEquals(PLACEHOLDERS)

        // **The re-freeze inverted this claim, so the assertion is rewritten rather than re-baselined.**
        // V2 kept the heading up while loading; V2.1's `state()` writes
        // `.shelf-head{visibility:hidden}` (`v21-library.html:440`) — *hidden*, which keeps the space
        // and drops the text. It has to: the head carries a **count**, and a count of zero while the
        // read is still running tells a user with twelve zines that they have none.
        //
        // `visibility:hidden` is also out of the accessibility tree, so this is `assertDoesNotExist`
        // and not `assertIsNotDisplayed` — an alpha-zero heading that still spoke would be the same
        // defect said aloud.
        composeRule.onNodeWithText(SHELF_HEADING).assertDoesNotExist()

        // The other half of *hidden*, and the reason it is not `if (loading) {}`: the space stays, so
        // the grid does not restructure when the data lands. Measured as the placeholders standing
        // where the covers will stand — a removed head moves the whole grid up by its height, which is
        // a jump every screenshot ratifies as "the content arrived".
        val placeholderTop = composeRule.onAllNodesWithTag(ZineShelfPlaceholderTestTag)[0]
            .fetchSemanticsNode().boundsInRoot.top
        // **This is the assertion.** `body.is-loading .empty{display:none}` exists because a slow read
        // that rendered *"Make your first little zine"* would tell a user with twelve zines that they
        // have none. "Placeholders appear" alone passes on precisely that defect.
        composeRule.onNodeWithTag(ZineShelfEmptyTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineShelfFailTestTag).assertDoesNotExist()
        composeRule.onNodeWithTag(zineShelfCoverTestTag(0)).assertDoesNotExist()

        content(zines(2))
        assertEquals(
            "the covers must land where the placeholders stood — the hidden head keeps its space",
            bounds(zineShelfCoverTestTag(0)).top,
            placeholderTop,
            HALF_PIXEL,
        )
        composeRule.onNodeWithText(SHELF_HEADING).assertIsDisplayed()
    }

    @Test
    fun `a loaded shelf stands no placeholders`() {
        content(zines(2))
        composeRule.onAllNodesWithTag(ZineShelfPlaceholderTestTag).assertCountEquals(0)
    }

    // ---------------------------------------------------------------------------------------------
    // Row 16 — error
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `an unreadable shelf shows the failure alone, and the reassurance comes first`() {
        render(LibraryShelfState.Error)

        composeRule.onNodeWithTag(ZineShelfFailTestTag).assertIsDisplayed()
        // `body.is-error{.shelf:none;.empty:none}` — the shelf is *gone*, not merely empty, and the
        // invitation must never stand in for a failed read.
        composeRule.onNodeWithText(SHELF_HEADING).assertDoesNotExist()
        composeRule.onNodeWithTag(ZineShelfEmptyTestTag).assertDoesNotExist()

        // *Reassurance precedes explanation* — the ruling's words, and the ORDER is the finding rather
        // than the wording. A paragraph saying the same two things the other way round is a different
        // screen: on a blank shelf, "something went wrong" first reads as loss.
        val paragraph = failParagraph()
        val reassurance = paragraph.indexOf(ZineShelfFailReassurance)
        val explanation = paragraph.indexOf(ZineShelfFailExplanation)
        assertTrue("the reassurance is missing from the failure copy", reassurance >= 0)
        assertTrue("the explanation is missing from the failure copy", explanation >= 0)
        assertTrue("the explanation is placed before the reassurance", reassurance < explanation)
    }

    @Test
    fun `retry re-asks the store`() {
        render(LibraryShelfState.Error)
        composeRule.onNodeWithTag(ZineRetryTestTag).performClick()
        assertEquals("retry did not re-ask the store", 1, retries)
    }

    @Test
    fun `no pixel of the retry control is leaf`() {
        // `.retry{background:var(--paper);border:1.5px solid var(--ink)}` — the paper + hairline grammar,
        // because `.start` is the screen's one primary and **stands in this state too**; two leaf buttons
        // would make the recovery compete with the invitation. "It looks quiet" is not checkable, and the
        // mutation — style `.retry` as `.start` — is a one-line change that a screenshot ratifies, so the
        // claim is made against the pixels.
        //
        // Both themes: `--leaf` is a different colour in each (#4E7A3C / #8FAE6B), so a light-only probe
        // would miss a dark-mode-only regression entirely — and dark is where the two inks are closest.
        for (dark in listOf(false, true)) {
            render(LibraryShelfState.Error, dark = dark)
            val retry = bounds(ZineRetryTestTag)
            val raster = decorRaster()
            var leafPixels = 0
            for (y in retry.top.toInt() until retry.bottom.toInt()) {
                for (x in retry.left.toInt() until retry.right.toInt()) {
                    if (raster.matches(x, y, leafColor)) leafPixels++
                }
            }
            assertEquals(
                "the retry control is painted in the primary's ink (dark=$dark)",
                0,
                leafPixels,
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rows 17-20 — every action hands over to an existing flow
    // ---------------------------------------------------------------------------------------------

    @Test
    fun `tapping a cover opens that zine`() {
        content(zines(3))
        composeRule.onNodeWithTag(zineShelfCoverTestTag(1)).performClick()
        assertEquals(listOf(idOf(1)), opened)
    }

    @Test
    fun `each sheet row hands its own zine to its own flow`() {
        // One test over four rows because the defect they share is one defect, and it is an *identity*
        // defect: a sheet wired to the first zine, or to the right zine by the wrong route, reads
        // correctly in every screenshot ever taken of it. Every assertion names the zine that was
        // pressed, and none of them would survive a hard-wired index.
        val pressed = 2

        openSheetFor(pressed)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Open)).performClick()
        assertEquals("Open reached the wrong zine", listOf(idOf(pressed)), opened)

        openSheetFor(pressed)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.ShareExport)).performClick()
        assertEquals("Share & export reached the wrong zine", listOf(idOf(pressed)), shared)

        openSheetFor(pressed)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Duplicate)).performClick()
        assertEquals("Duplicate reached the wrong zine", listOf(idOf(pressed)), duplicated)

        openSheetFor(pressed)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Delete)).performClick()
        assertEquals("Delete reached the wrong zine", listOf(idOf(pressed)), deleted)
    }

    @Test
    fun `Rename raises the existing rename input and saves against the pressed zine`() {
        val pressed = 1
        openSheetFor(pressed)
        composeRule.onNodeWithTag(zineActionTestTag(ZineAction.Rename)).performClick()

        // The *existing* input: the same field and the same Save button V1's shelf reveals, reached by
        // the same two test tags. D-025 is "reuse existing behaviour", and a re-implemented field would
        // be a new product concept wearing the old one's name.
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextClearance()
        composeRule.onNodeWithTag(HomeRenameFieldTestTag).performTextInput(NEW_TITLE)
        composeRule.onNodeWithTag(HomeRenameConfirmTestTag).performClick()

        assertEquals(listOf(idOf(pressed) to NEW_TITLE), renamed)
    }

    @Test
    fun `a delete raises the undo affordance, and undo reaches the existing flow`() {
        // The ruling reuses the **flow**, and ADR-046 §4's undo *is* that flow — a delete with no undo is
        // a new concept, not a reused one. The prompt is the event the store's own delete path emits, so
        // this asserts the wiring of that event rather than a re-implementation of it.
        content(zines(2))
        events.trySend(HomeShelfEvent.DeletePrompt(idOf(0), titleOf(0)))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(homeDeletedMessage(titleOf(0))).assertIsDisplayed()
        composeRule.onNodeWithText(Copy.Shelf.UNDO).performClick()

        assertEquals("undo did not reach the existing flow", listOf(idOf(0)), undone)
        assertTrue("undo committed the delete it was undoing", committed.isEmpty())
    }

    @Test
    fun `Make a zine raises the paper chooser, and the choice starts the zine`() {
        // ADR-047's chooser, reused: choosing the paper *is* the create action, so a fixed `PaperSize`
        // handed straight to `startZine` would skip a step the existing flow owns.
        render(LibraryShelfState.Empty)
        composeRule.onNodeWithTag(ZineStartTestTag).performClick()
        composeRule.onNodeWithTag(HomePaperChooserTestTag).assertIsDisplayed()

        composeRule.onNodeWithTag(homePaperChoiceTestTag(PaperSize.LETTER)).performClick()
        assertEquals(listOf(PaperSize.LETTER), started)
    }

    // ---------------------------------------------------------------------------------------------
    // Harness
    // ---------------------------------------------------------------------------------------------

    private var shelfState by mutableStateOf<LibraryShelfState>(LibraryShelfState.Loading)
    private var darkTheme by mutableStateOf(false)
    private var composed = false

    private fun fourStates(): List<LibraryShelfState> = listOf(
        LibraryShelfState.Content(zines(3)),
        LibraryShelfState.Empty,
        LibraryShelfState.Loading,
        LibraryShelfState.Error,
    )

    private fun content(zines: List<LibraryZine>, dark: Boolean = false) =
        render(LibraryShelfState.Content(zines), dark)

    /**
     * One composition per test, driven by state — `setContent` may be called only once per rule, and
     * several of these tests deliberately walk the screen through more than one state.
     */
    private fun render(state: LibraryShelfState, dark: Boolean = false) {
        shelfState = state
        darkTheme = dark
        if (!composed) {
            composed = true
            composeRule.setContent { Host() }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Host() {
        ZinelyTheme(darkTheme = darkTheme) {
            deskColor = ZinelyTheme.v21Colors.desk
            leafColor = ZinelyTheme.v21Colors.leaf
            ZineLibraryScreen(
                state = shelfState,
                events = events.receiveAsFlow(),
                onOpenZine = { opened += it },
                onShareExport = { shared += it },
                onStartZine = { started += it },
                onRenameZine = { id, title -> renamed += id to title },
                onDuplicateZine = { duplicated += it },
                onDeleteZine = { deleted += it },
                onDeleteUndo = { undone += it },
                onDeleteCommit = { committed += it },
                onRetry = { retries++ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    private fun openSheetFor(index: Int) {
        content(zines(3))
        composeRule.onNodeWithTag(zineShelfMoreTestTag(index)).performClick()
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** The failure paragraph as it is actually drawn, so the order assertion reads real text. */
    private fun failParagraph(): String = composeRule
        .onNode(hasText(ZineShelfFailReassurance, substring = true))
        .fetchSemanticsNode()
        .config[SemanticsProperties.Text]
        .joinToString(separator = "") { it.text }

    private fun decorRaster(): Bitmap {
        assertEquals(
            "these pixel offsets assume dp == px; density was ${composeRule.density.density}",
            1.0f,
            composeRule.density.density,
            0.0001f,
        )
        return composeRule.activity.window.decorView.rasterizeToBitmap()
    }

    /** One 8-bit step of tolerance: an exact value still rounds through the bitmap's own channels. */
    private fun Bitmap.matches(x: Int, y: Int, expected: Color): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val actual = Color(getPixel(x, y))
        val tolerance = 1.5f / 255f
        return abs(actual.red - expected.red) <= tolerance &&
            abs(actual.green - expected.green) <= tolerance &&
            abs(actual.blue - expected.blue) <= tolerance
    }

    private companion object {
        const val SHELF_HEADING = "Your shelf"
        const val NEW_TITLE = "Coffee log"
        const val PLACEHOLDERS = 4
        const val HALF_PIXEL = 0.5f

        /** Inside `.shelf{padding:30px 22px}` on both axes: bare ground, above and left of every cell. */
        const val GROUND_PROBE = 8

        fun idOf(index: Int) = "p$index"
        fun titleOf(index: Int) = "Zine ${index + 1}"
        fun subtitleOf(index: Int) = "A4 · Edited ${index + 1} days ago"

        fun zines(count: Int): List<LibraryZine> = List(count) { i ->
            LibraryZine(
                id = idOf(i),
                title = titleOf(i),
                subtitle = subtitleOf(i),
                cover = ZineCoverRecipe(
                    surface = ZineCoverSurface.entries[i % ZineCoverSurface.entries.size],
                    stamp = ZineCoverStamp.entries[i % ZineCoverStamp.entries.size],
                ),
            )
        }
    }
}
