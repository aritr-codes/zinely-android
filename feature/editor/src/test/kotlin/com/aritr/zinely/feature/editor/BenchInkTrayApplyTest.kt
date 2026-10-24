package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.editor.ink.InkHex
import com.aritr.zinely.core.editor.ink.InkLibrary
import com.aritr.zinely.core.editor.ink.InkLibraryEdit
import com.aritr.zinely.core.editor.ink.InkPalette
import com.aritr.zinely.core.editor.ink.MyInk
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyPaperTintId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Ink tray's one entry point (`applyInk`, ADR-138 decision 5; INK task 6) at the screen: what a pot tap
 * changes, when Undo is offered, what Undo restores, and when a standing Undo is withdrawn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class BenchInkTrayApplyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** An in-memory library seam with the store's contract: a transform in, before/after out; can fail. */
    private class FakeLibrary(initial: InkLibrary) {
        var library by mutableStateOf(initial)
        var fail = false
        var edits = 0

        suspend fun edit(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit {
            edits++
            if (fail) throw IOException("disk full")
            val before = library
            library = transform(before)
            return InkLibraryEdit(before, library)
        }
    }

    private val inks = zinelyContentInks()
    private fun hexOf(id: ZinelyMakerInkId) = inks[id].value.inkHex()
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

    private fun screen(store: EditorStore, fake: FakeLibrary) {
        composeRule.setContent {
            ZinelyTheme {
                EditorScreen(
                    store = store,
                    pageSizePt = PtSize(100.0, 130.0),
                    modifier = Modifier.size(360.dp, 720.dp),
                    inkLibrary = fake.library,
                    onEditInkLibrary = fake::edit,
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Places a text box (selected by the reducer) and opens the tray on it. */
    private fun openInk(store: EditorStore, words: String = "hi"): String {
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), words))
        val id = store.uiState.value.selection.single()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        return id
    }

    /**
     * Scrolls [node] into view on a running clock, then holds the clock and taps it.
     *
     * ⚠ Order matters. `performScrollTo` is an *animated* semantics scroll; on a held clock it never ends,
     * and the next scroll on the same tray cancels it — the cancelled `animate` then spins registering frame
     * awaiters (4.2M `BroadcastFrameClock$FrameAwaiter` in the heap) until the worker runs out of memory.
     * The clock is held only from the tap: the 4000ms snack would otherwise open and close inside one
     * `waitForIdle`.
     */
    private fun tap(node: SemanticsNodeInteraction) {
        composeRule.mainClock.autoAdvance = true
        node.performScrollTo()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        node.performClick()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(300L)
        composeRule.waitForIdle()
    }

    private fun colourOf(store: EditorStore, id: String) =
        (store.uiState.value.document.pages[0].elements.first { it.id == id } as TextElement).style.color

    private fun undoButtons() =
        composeRule.onAllNodesWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun snackSays(line: String) =
        composeRule.onAllNodesWithContentDescription(line).fetchSemanticsNodes().size == 1

    @Test
    fun `given a kept ink unlike the piece's when its pot is tapped then it is applied, bumped, and Undo restores both`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary(myInks = listOf(MyInk(teal, 1)), clock = 1))
        screen(store, fake)
        val id = openInk(store)
        val colourBefore = colourOf(store, id)
        val libraryBefore = fake.library

        tap(composeRule.onNodeWithTag(BenchInkKeptTestTag))
        settle()

        assertEquals(teal.toColorRgba(), colourOf(store, id))
        assertEquals("the kept ink is bumped to most recently used", 2L, fake.library.kept(teal)?.used)
        assertTrue(snackSays(Copy.BenchInk.applied(inkName(teal, inks, BenchVerbKind.TEXT))))
        assertEquals("Undo is offered", 1, undoButtons())

        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        settle()
        assertEquals("one document step undone", colourBefore, colourOf(store, id))
        assertEquals("the library is restored to its before-value", libraryBefore, fake.library)
    }

    @Test
    fun `given the piece's own ink, not kept, when its pot is tapped then nothing changes and no Undo is offered`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(store, fake)
        val id = openInk(store)
        // A new box is `ColorRgba.BLACK`, which no pot carries; give it a built-in ink so its own pot exists.
        store.dispatch(Intent.StyleText(id, color = hexOf(ZinelyMakerInkId.Brick).toColorRgba()))
        composeRule.waitForIdle()
        val docBefore = store.uiState.value.document
        val current = composeRule.onAllNodesWithTag(BenchInkSwatchTestTag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .single { it.config.getOrNull(SemanticsProperties.Selected) == true }
        val name = current.config[SemanticsProperties.ContentDescription].single()

        tap(composeRule.onNodeWithContentDescription(name))
        settle()

        assertSame("no history entry: the document is the same object", docBefore, store.uiState.value.document)
        assertEquals(InkLibrary.EMPTY, fake.library)
        assertTrue(snackSays(Copy.BenchInk.applied(name)))
        assertEquals("no Undo for no change", 0, undoButtons())
    }

    /**
     * The reviewer's wrong-step case. The reducer refuses a style on a blank box (`EditorReducer.styleText`),
     * so the tap changes nothing; a *predicted* change would offer Undo, and that Undo would pop the box's
     * own placement.
     *
     * The `Ink` verb is disabled on a blank box (`benchContextVerbs`, "Type something first"), so the tray
     * cannot be *opened* on one. It can be left open on one: words typed into a fresh box, the tray opened,
     * then the bar's Undo takes the words back — the box is blank again, still selected, the tray still up,
     * and the history's last step is the box's own `PlaceCommand`.
     */
    @Test
    fun `given a fresh blank text box when another ink is tapped then no Undo is offered and the box stays`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(store, fake)
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), ""))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.BeginEditText(id))
        val session = store.uiState.value.interaction as Interaction.EditingText
        val blank = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        store.dispatch(Intent.CommitText(id, blank.copy(text = "hi"), session.token))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        store.dispatch(Intent.Undo)
        composeRule.waitForIdle()
        assertEquals("precondition: the box is blank again", blank, store.uiState.value.document.pages[0].elements.single { it.id == id })
        assertEquals("precondition: the box is still selected", setOf(id), store.uiState.value.selection)

        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.BRICK))
        settle()

        assertEquals("no Undo over an unchanged document", 0, undoButtons())
        assertTrue("the box is still on the page", store.uiState.value.document.pages[0].elements.any { it.id == id })
    }

    @Test
    fun `given an ink snack's Undo is standing when another document change lands then the Undo is withdrawn`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(store, fake)
        val id = openInk(store)
        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.BRICK))
        settle()
        assertEquals(1, undoButtons())

        store.dispatch(Intent.StyleText(id, sizePt = 30.0))
        settle()

        assertEquals("a 4s Undo must never pop an unrelated step", 0, undoButtons())
        composeRule.onNodeWithTag(BenchSnackTestTag).assertExists()
    }

    @Test
    fun `given three palettes when a strip is tapped then that palette opens and stays open`() {
        val store = store()
        val pals = listOf(
            InkPalette(3, "Night", listOf(hexOf(ZinelyMakerInkId.Ink), hexOf(ZinelyMakerInkId.Strawberry))),
            InkPalette(2, "Sea", listOf(hexOf(ZinelyMakerInkId.Forest), hexOf(ZinelyMakerInkId.Aqua))),
            InkPalette(1, "Sun", listOf(hexOf(ZinelyMakerInkId.Brick), hexOf(ZinelyMakerInkId.Sunflower))),
        )
        val fake = FakeLibrary(InkLibrary(palettes = pals, nextPaletteId = 4, openPaletteId = 3))
        screen(store, fake)
        openInk(store)

        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.paletteGroup("Sea", 2)))
        settle()

        assertEquals(2L, fake.library.openPaletteId)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.palettePot(Copy.BenchInk.AQUA, "Sea")).assertExists()
    }

    @Test
    fun `given a starter pot Sunflower when tapped then Sunflower is applied, not the starter's first colour`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(store, fake)
        val id = openInk(store)
        tap(composeRule.onNodeWithTag(BenchInkStartersTestTag))
        settle()

        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.palettePot(Copy.BenchInk.SUNFLOWER, Copy.BenchInk.PRESET_WARM)))
        settle()

        assertEquals(hexOf(ZinelyMakerInkId.Sunflower).toColorRgba(), colourOf(store, id))
    }

    /** Controller ruling 2026-10-22: a pale ink on text says so in the same snack, and is still applied. */
    @Test
    fun `given a text piece when Cream is tapped from a starter palette then the snack reads the pale form`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(store, fake)
        val id = openInk(store)
        tap(composeRule.onNodeWithTag(BenchInkStartersTestTag))
        settle()

        tap(composeRule.onNodeWithContentDescription(Copy.BenchInk.palettePot(Copy.BenchInk.CREAM, Copy.BenchInk.PRESET_WARM)))
        settle()

        assertEquals(inks[ZinelyPaperTintId.Cream].value.inkHex().toColorRgba(), colourOf(store, id))
        assertTrue(snackSays(Copy.BenchInk.appliedPale(Copy.BenchInk.CREAM)))
    }

    /** Ruling: a store `IOException` is "the library did not change" — the tray never crashes on storage. */
    @Test
    fun `given the library store fails when an ink is tapped then the ink still lands and Undo covers the document only`() {
        val store = store()
        val fake = FakeLibrary(InkLibrary(myInks = listOf(MyInk(teal, 1)), clock = 1)).apply { fail = true }
        screen(store, fake)
        val id = openInk(store)
        val colourBefore = colourOf(store, id)

        tap(composeRule.onNodeWithTag(BenchInkKeptTestTag))
        settle()

        assertEquals("the ink is applied", teal.toColorRgba(), colourOf(store, id))
        assertEquals("the write was attempted once", 1, fake.edits)
        assertEquals("Undo for the document change", 1, undoButtons())
        composeRule.onNodeWithTag(BenchSnackActionTestTag).performClick()
        settle()
        assertEquals(colourBefore, colourOf(store, id))
        assertEquals("no library restore was attempted for a write that never landed", 1, fake.edits)
    }
}
