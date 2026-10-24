package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.editor.ink.colourName
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Palette edit mode, the ⋯ menu, Save a copy and the kept pot's Remove (INK task 7, A40 rev. 2 rule 4,
 * prototype `startEdit` / `renderEdit` / `peCancel` / `peSave` / `wireTray`), at the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class BenchInkPaletteEditTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeLibrary(initial: InkLibrary) {
        var library by mutableStateOf(initial)
        var fail = false

        suspend fun edit(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit {
            if (fail) throw IOException("disk full")
            val before = library
            library = transform(before)
            return InkLibraryEdit(before, library)
        }
    }

    private val inks = zinelyContentInks()
    private fun hexOf(id: ZinelyMakerInkId) = inks[id].value.inkHex()
    private val brick get() = hexOf(ZinelyMakerInkId.Brick)
    private val forest get() = hexOf(ZinelyMakerInkId.Forest)
    private val plum = InkHex("7A3B69")
    private val teal = InkHex("1F7A5C")

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
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
    }

    private fun screen(fake: FakeLibrary): EditorStore = screen({ fake.library }, fake::edit)

    private fun screen(
        shown: () -> InkLibrary,
        edit: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit,
    ): EditorStore {
        val store = store()
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 130.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                    inkLibrary = shown(),
                    onEditInkLibrary = edit,
                )
            }
        }
        composeRule.waitForIdle()
        // A text box inked Brick, so the draft opens holding a built-in the toggles can show.
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.StyleText(id, color = brick.toColorRgba()))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        return store
    }

    /** T6's lesson: scroll on a running clock, hold the clock only from the tap. */
    private fun tap(node: SemanticsNodeInteraction) {
        composeRule.mainClock.autoAdvance = true
        node.performScrollTo()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        node.performClick()
        advance()
    }

    /**
     * A click handler's state write is not applied until something sends the snapshot's apply notifications;
     * on a held clock nothing does, so the screen would never recompose. Send them, then step the clock.
     */
    private fun advance() {
        composeRule.runOnUiThread { Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
    }

    /** The edit bar is pinned under the scroll, so its buttons are pressed where they stand. */
    private fun press(node: SemanticsNodeInteraction) {
        composeRule.mainClock.autoAdvance = false
        node.performClick()
        advance()
    }

    /** A menu row lives in a popup, outside the tray's scroll: tapped without scrolling. */
    private fun menu(label: String) {
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithTag("$BenchInkMenuItemTestTag-$label").performClick()
        advance()
    }

    private fun undo() {
        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        advance()
    }

    private fun undoButtons() =
        composeRule.onAllNodesWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun snackSays(line: String) =
        composeRule.onAllNodesWithContentDescription(line).fetchSemanticsNodes().size == 1

    private fun editing() =
        composeRule.onAllNodesWithTag(BenchInkPaletteNameTestTag).fetchSemanticsNodes().isNotEmpty()

    private fun nameText(): String =
        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text

    private fun stripPots() =
        composeRule.onAllNodesWithTag(BenchInkDraftPotTestTag).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.ContentDescription].single() }

    private fun toggles() = composeRule.onAllNodesWithTag(BenchInkToggleTestTag).fetchSemanticsNodes()

    private fun builtInToggle(name: String) =
        composeRule.onNodeWithContentDescription(name).also { it.assertExistsInTag(BenchInkToggleTestTag) }

    private fun SemanticsNodeInteraction.assertExistsInTag(tag: String) {
        assertEquals(tag, fetchSemanticsNode().config.getOrNull(SemanticsProperties.TestTag))
    }

    private fun nine() = InkLibrary(
        palettes = (9L downTo 1L).map { InkPalette(it, "P$it", listOf(brick, forest)) },
        nextPaletteId = 10,
    )

    @Test
    fun `given nine palettes when New, Duplicate or Save a copy then My palettes is full, with no Undo and no change`() {
        val start = nine()
        val fake = FakeLibrary(start)
        screen(fake)

        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        assertTrue(snackSays(Copy.BenchInk.PALETTES_FULL))
        assertEquals(0, undoButtons())
        assertFalse("no draft opens", editing())

        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))
        menu(Copy.BenchInk.DUPLICATE)
        assertTrue(snackSays(Copy.BenchInk.PALETTES_FULL))
        assertEquals(0, undoButtons())

        tap(composeRule.onNodeWithTag(BenchInkStartersTestTag))
        tap(composeRule.onAllNodesWithTag(BenchInkSaveCopyTestTag)[0])
        assertTrue(snackSays(Copy.BenchInk.PALETTES_FULL))
        assertEquals(0, undoButtons())
        assertEquals(start, fake.library)
    }

    @Test
    fun `given a New draft then it is named uniquely, holds the current ink, and the name field is not focused`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)

        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        assertEquals(Copy.BenchInk.DEFAULT_PALETTE_NAME, nameText())
        assertEquals(listOf(Copy.BenchInk.removeFromPalette(Copy.BenchInk.BRICK)), stripPots())
        builtInToggle(Copy.BenchInk.BRICK).assertToggle(ToggleableState.On)
        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).assertIsNotFocused()
        composeRule.onNodeWithTag(BenchInkTitleTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.BenchInk.NEW_PALETTE, it) }
        assertTrue("Done hides while editing", composeRule.onAllNodesWithTag(BenchInkDoneTestTag).fetchSemanticsNodes().isEmpty())

        press(composeRule.onNodeWithTag(BenchInkCancelTestTag)) // no change: silent
        fake.library = InkLibrary(palettes = listOf(InkPalette(1, "My palette", listOf(brick, forest))), nextPaletteId = 2)
        composeRule.waitForIdle()
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        assertEquals("My palette 2", nameText())
    }

    private fun SemanticsNodeInteraction.assertToggle(state: ToggleableState) {
        assertEquals(state, fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState))
    }

    @Test
    fun `given six picked when a seventh is tapped then a palette holds six, and the badges and toggles match the picks`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        val more = listOf(Copy.BenchInk.MATCHA, Copy.BenchInk.FOREST, Copy.BenchInk.STRAWBERRY, Copy.BenchInk.SUNFLOWER, Copy.BenchInk.OCHRE)
        for (name in more) tap(builtInToggle(name))
        assertEquals(6, stripPots().size)

        tap(builtInToggle(Copy.BenchInk.AQUA))

        assertTrue(snackSays(Copy.BenchInk.PALETTE_HOLDS_SIX))
        assertEquals(0, undoButtons())
        assertEquals(6, stripPots().size)
        val on = toggles().filter { it.config.getOrNull(SemanticsProperties.ToggleableState) == ToggleableState.On }
        assertEquals(6, on.size)
        assertTrue(
            "every toggle reads checked or unchecked",
            toggles().all { it.config.getOrNull(SemanticsProperties.ToggleableState) != null },
        )
        val badges = composeRule.onAllNodesWithTag(BenchInkPickedBadgeTestTag, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals("one ✓ per picked ink", 6, badges.size)
    }

    @Test
    fun `given an edit removes an ink nothing else offers then Was in this palette shows it and tapping puts it back`() {
        val fake = FakeLibrary(InkLibrary(palettes = listOf(InkPalette(1, "Night", listOf(plum, brick))), nextPaletteId = 2))
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))
        menu(Copy.BenchInk.EDIT)
        val plumName = inkColourWords(colourName(plum))

        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.removeFromPalette(plumName)))

        assertEquals(1, stripPots().size)
        val lost = composeRule.onNodeWithTag(BenchInkLostTestTag)
        lost.assertToggle(ToggleableState.Off)
        assertEquals(
            Copy.BenchInk.pickInkLabel(plumName, plum.spoken()),
            lost.fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single(),
        )
        tap(lost)
        assertEquals(2, stripPots().size)
        assertTrue(composeRule.onAllNodesWithTag(BenchInkLostTestTag).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `given Save then the snack offers Undo, and Undo restores the palettes and reopens the edit as it was`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))
        val before = fake.library

        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        assertEquals(listOf(listOf(brick, forest)), fake.library.palettes.map { it.inks })
        assertFalse(editing())
        assertTrue(snackSays(Copy.BenchInk.newPalette(Copy.BenchInk.DEFAULT_PALETTE_NAME)))
        assertEquals(1, undoButtons())

        undo()
        assertEquals(before, fake.library)
        assertTrue("the edit reopens", editing())
        assertEquals(Copy.BenchInk.DEFAULT_PALETTE_NAME, nameText())
        assertEquals(
            listOf(Copy.BenchInk.removeFromPalette(Copy.BenchInk.BRICK), Copy.BenchInk.removeFromPalette(Copy.BenchInk.FOREST)),
            stripPots(),
        )
    }

    @Test
    fun `given Save with one ink then pick at least two, no Undo, the edit stays open`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))

        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        assertTrue(snackSays(Copy.BenchInk.PICK_AT_LEAST_TWO))
        assertEquals(0, undoButtons())
        assertTrue(editing())
        assertEquals(InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given Cancel after a rename then Palette not saved with Undo reopens it, and Cancel with no change says nothing`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).performTextReplacement("Night bus")
        composeRule.waitForIdle()

        press(composeRule.onNodeWithTag(BenchInkCancelTestTag))
        assertFalse(editing())
        assertTrue(snackSays(Copy.BenchInk.PALETTE_NOT_SAVED))
        assertEquals(1, undoButtons())
        undo()
        assertTrue(editing())
        assertEquals("Night bus", nameText())

        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).performTextReplacement(Copy.BenchInk.DEFAULT_PALETTE_NAME)
        composeRule.mainClock.advanceTimeBy(5_000L) // the old snack goes
        composeRule.waitForIdle()
        press(composeRule.onNodeWithTag(BenchInkCancelTestTag))
        assertFalse(editing())
        assertFalse(snackSays(Copy.BenchInk.PALETTE_NOT_SAVED))
        assertEquals(InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given Back during an edit then it is Cancel, and the tray stays open`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()

        assertFalse(editing())
        assertTrue(snackSays(Copy.BenchInk.PALETTE_NOT_SAVED))
        assertEquals("the same Undo as Cancel", 1, undoButtons())
        composeRule.onNodeWithTag(BenchInkPopoverTestTag).assertExistsInTag(BenchInkPopoverTestTag)
        composeRule.onNodeWithTag(BenchInkDoneTestTag).assertExistsInTag(BenchInkDoneTestTag)
    }

    @Test
    fun `given Delete then the snack names it, and Undo puts it back in its place`() {
        val start = InkLibrary(
            palettes = listOf(InkPalette(2, "Sea", listOf(forest, brick)), InkPalette(1, "Warm", listOf(brick, forest))),
            nextPaletteId = 3,
            openPaletteId = 2,
        )
        val fake = FakeLibrary(start)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))

        menu(Copy.BenchInk.DELETE)

        assertEquals(listOf("Warm"), fake.library.palettes.map { it.name })
        assertTrue(snackSays(Copy.BenchInk.deleted("Sea")))
        assertEquals(1, undoButtons())
        undo()
        assertEquals(start, fake.library)
    }

    @Test
    fun `given Duplicate then the copy is new and open, and Undo takes it away`() {
        val start = InkLibrary(palettes = listOf(InkPalette(1, "Sea", listOf(forest, brick))), nextPaletteId = 2, openPaletteId = 1)
        val fake = FakeLibrary(start)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))

        menu(Copy.BenchInk.DUPLICATE)

        assertEquals(listOf("Sea copy", "Sea"), fake.library.palettes.map { it.name })
        assertTrue(snackSays(Copy.BenchInk.duplicated("Sea")))
        undo()
        assertEquals(start, fake.library)
    }

    @Test
    fun `given a starter's Save a copy then it lands in My palettes with Undo`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkStartersTestTag))

        tap(composeRule.onAllNodesWithTag(BenchInkSaveCopyTestTag)[0])

        assertEquals(listOf(Copy.BenchInk.PRESET_TWO_COLOUR), fake.library.palettes.map { it.name })
        assertTrue(snackSays(Copy.BenchInk.savedCopy(Copy.BenchInk.PRESET_TWO_COLOUR)))
        assertEquals(1, undoButtons())
        undo()
        assertEquals(InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given a kept ink long-pressed when Remove then it leaves My inks, palettes keep it, and Undo restores it`() {
        val start = InkLibrary(
            myInks = listOf(MyInk(teal, 1)),
            palettes = listOf(InkPalette(1, "Sea", listOf(teal, brick))),
            clock = 1,
            nextPaletteId = 2,
        )
        val fake = FakeLibrary(start)
        screen(fake)
        val kept = composeRule.onNodeWithTag(BenchInkKeptTestTag)
        composeRule.mainClock.autoAdvance = true
        kept.performScrollTo()
        composeRule.waitForIdle()
        kept.performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        menu(Copy.BenchInk.REMOVE_FROM_MY_INKS)

        assertTrue(fake.library.myInks.isEmpty())
        assertEquals(listOf(teal, brick), fake.library.palettes.single().inks)
        assertTrue(snackSays(Copy.BenchInk.removedInk(inkColourWords(colourName(teal)))))
        undo()
        assertEquals(start, fake.library)
    }

    @Test
    fun `given a kept ink's Remove accessibility action then it removes directly, without a menu`() {
        val fake = FakeLibrary(InkLibrary(myInks = listOf(MyInk(teal, 1)), clock = 1))
        screen(fake)
        val node = composeRule.onNodeWithTag(BenchInkKeptTestTag)
        val action = node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == Copy.BenchInk.REMOVE_FROM_MY_INKS }

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread { action.action() }
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()

        assertTrue(fake.library.myInks.isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("$BenchInkMenuItemTestTag-${Copy.BenchInk.REMOVE_FROM_MY_INKS}").fetchSemanticsNodes().isEmpty())
        assertEquals(1, undoButtons())
    }

    @Test
    fun `given the store fails on Save then the edit stays open and nothing is offered`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))
        fake.fail = true

        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        assertTrue("the maker's draft is not thrown away", editing())
        assertEquals(0, undoButtons())
    }

    @Test
    fun `given a New draft then focus opens on the count line, and Save with no name focuses the name`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        composeRule.onNodeWithTag(BenchInkCountTestTag).assertIsFocused()

        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).performTextReplacement("")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BenchInkCountTestTag).performSemanticsAction(SemanticsActions.RequestFocus)
        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).assertIsFocused()
        assertEquals(0, undoButtons())
        assertEquals(InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given an existing palette edited when Save then it is saved in place, and Undo restores it`() {
        val start = InkLibrary(palettes = listOf(InkPalette(1, "Sea", listOf(forest, brick))), nextPaletteId = 2, openPaletteId = 1)
        val fake = FakeLibrary(start)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))
        menu(Copy.BenchInk.EDIT)
        composeRule.onNodeWithTag(BenchInkTitleTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.BenchInk.EDIT_PALETTE, it) }
        composeRule.onNodeWithTag(BenchInkPaletteNameTestTag).performTextReplacement("Seaside")
        composeRule.waitForIdle()

        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        assertEquals(listOf(InkPalette(1, "Seaside", listOf(forest, brick))), fake.library.palettes)
        assertTrue(snackSays(Copy.BenchInk.saved("Seaside")))
        undo()
        assertEquals(start, fake.library)
        assertEquals("Seaside", nameText())
    }

    @Test
    fun `given the ninth palette lands while a New draft is open when Save then My palettes is full and the draft stays`() {
        val fake = FakeLibrary(InkLibrary(palettes = nine().palettes.drop(1), nextPaletteId = 10))
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))
        fake.library = nine()
        composeRule.waitForIdle()

        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))

        assertTrue(snackSays(Copy.BenchInk.PALETTES_FULL))
        assertEquals(0, undoButtons())
        assertTrue(editing())
        assertEquals(nine(), fake.library)
    }

    @Test
    fun `given Save is tapped twice before the write lands then the palette is added once`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))
        val save = composeRule.onNodeWithTag(BenchInkSavePaletteTestTag)

        save.performClick()
        save.performClick()
        advance()

        assertEquals(1, fake.library.palettes.size)
    }

    @Test
    fun `given the Undo of a Save is standing when the tray closes then the Undo is withdrawn and the palette stays`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        tap(builtInToggle(Copy.BenchInk.FOREST))
        press(composeRule.onNodeWithTag(BenchInkSavePaletteTestTag))
        assertEquals(1, undoButtons())

        press(composeRule.onNodeWithTag(BenchInkDoneTestTag))

        assertEquals("no Undo that would take the palette away with no draft to return to", 0, undoButtons())
        assertEquals(1, fake.library.palettes.size)
    }

    @Test
    fun `given the screen is a write behind the store when Undo then before is not written over a newer library`() {
        // The real ViewModel's StateFlow can emit after the write returns; the screen's copy lags here forever.
        val start = InkLibrary(palettes = listOf(InkPalette(1, "Sea", listOf(forest, brick))), nextPaletteId = 2, openPaletteId = 1)
        var stored = start
        val edit: suspend ((InkLibrary) -> InkLibrary) -> InkLibraryEdit = { t ->
            val before = stored
            stored = t(before)
            InkLibraryEdit(before, stored)
        }
        screen({ start }, edit)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))
        menu(Copy.BenchInk.DELETE)
        assertEquals(1, undoButtons())
        val newer = InkLibrary(palettes = listOf(InkPalette(7, "Elsewhere", listOf(brick, forest))), nextPaletteId = 8)
        stored = newer

        undo()

        assertEquals("the newer library is kept", newer, stored)
    }
}
