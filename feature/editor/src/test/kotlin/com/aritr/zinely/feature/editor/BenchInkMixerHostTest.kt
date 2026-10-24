package com.aritr.zinely.feature.editor

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import com.aritr.zinely.core.model.TextElement
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** Hosting the mixer (INK task 8b, A40 rules 5 and rev. 2 rule 4, prototype `openMixer` / `mixUse`). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class BenchInkMixerHostTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
    private val brick get() = inks[ZinelyMakerInkId.Brick].value.inkHex()
    private val plum = InkHex("7A3B69")
    private val slate = InkHex("505955")

    private lateinit var store: EditorStore
    private lateinit var id: String

    private fun screen(fake: FakeLibrary, wears: InkHex = brick) {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        store = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, runner,
        )
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
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        id = store.uiState.value.selection.single()
        store.dispatch(Intent.StyleText(id, color = wears.toColorRgba()))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
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

    /** T7's lesson: a click handler's write needs the snapshot's apply notifications on a held clock. */
    private fun advance() {
        composeRule.runOnUiThread { Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
    }

    private fun press(node: SemanticsNodeInteraction) {
        composeRule.mainClock.autoAdvance = false
        node.performClick()
        advance()
    }

    private fun mix() = tap(composeRule.onNodeWithTag(BenchInkMixTestTag))

    private fun type(hex: InkHex) {
        val field = composeRule.onNodeWithTag(InkMixerHexFieldTestTag)
        field.performTextClearance()
        field.performTextInput(hex.value)
        advance()
    }

    private fun use() = press(composeRule.onNodeWithTag(InkMixerUseTestTag))

    private fun undo() = press(composeRule.onNodeWithTag(BenchSnackActionTestTag))

    /** Back as the platform sends it: to the dialog window when the mixer is up, else to the activity. */
    private fun back() {
        composeRule.runOnUiThread {
            val dialog = ShadowDialog.getLatestDialog()
            if (dialog != null && dialog.isShowing) {
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
            } else {
                composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
        }
        // The dialog's dismissal lands a frame later than a click's write; let the clock run it out.
        advance()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
    }

    private fun mixerOpen() = composeRule.onAllNodesWithTag(InkMixerHexFieldTestTag).fetchSemanticsNodes().isNotEmpty()
    private fun trayOpen() = composeRule.onAllNodesWithTag(BenchInkMixTestTag).fetchSemanticsNodes().isNotEmpty()
    private fun colour() = (store.uiState.value.document.pages[0].elements.first { it.id == id } as TextElement).style.color
    private fun undoButtons() = composeRule.onAllNodesWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNodes().size
    private fun snackSays(line: String) = composeRule.onAllNodesWithContentDescription(line).fetchSemanticsNodes().size == 1
    private fun words(hex: InkHex) = inkColourWords(colourName(hex))
    private fun fieldText() = composeRule.onNodeWithTag(InkMixerHexFieldTestTag).fetchSemanticsNode()
        .config[SemanticsProperties.EditableText].text

    private fun stripPots() = composeRule.onAllNodesWithTag(BenchInkDraftPotTestTag).fetchSemanticsNodes().size

    @Test
    fun `given the tray when Mix then the mixer opens on the piece's ink, and Use inks the piece, keeps the ink, and Undo restores both`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        mix()
        assertTrue(mixerOpen())
        assertEquals(brick.value, fieldText())
        composeRule.onNodeWithTag(InkMixerUseTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.InkMix.USE_THIS_INK, it) }

        type(slate)
        use()
        assertEquals("one write: use, no touch after it", 1, fake.edits)
        assertFalse("Use closes the mixer", mixerOpen())
        assertEquals(slate.toColorRgba(), colour())
        assertEquals(listOf(slate), fake.library.myInks.map { it.hex })
        assertTrue(snackSays(Copy.BenchInk.applied(inkName(slate, inks, BenchVerbKind.TEXT))))

        undo()
        assertEquals("the page's ink is back", brick.toColorRgba(), colour())
        assertEquals("My inks is back", InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given the piece already wears 7A3B69, not kept, when Mix then Use, then Undo takes it out of My inks and leaves the page alone`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake, wears = plum)
        val docBefore = store.uiState.value.document
        mix()
        assertEquals(plum.value, fieldText())
        use()
        assertSame("no page change: the piece already wears it", docBefore, store.uiState.value.document)
        assertEquals(listOf(plum), fake.library.myInks.map { it.hex })
        assertEquals("Undo is offered for the library", 1, undoButtons())

        undo()
        assertEquals(InkLibrary.EMPTY, fake.library)
        assertSame("no Intent.Undo: the document is the same object", docBefore, store.uiState.value.document)
        assertEquals(plum.toColorRgba(), colour())
    }

    @Test
    fun `given a kept hex when Mix then Use, then it is bumped, not added twice`() {
        val fake = FakeLibrary(InkLibrary(myInks = listOf(MyInk(slate, 1), MyInk(plum, 2)), clock = 2))
        screen(fake)
        mix()
        type(slate)
        use()
        assertEquals(listOf(slate, plum), fake.library.myInks.map { it.hex })
        assertEquals(3L, fake.library.kept(slate)?.used)
    }

    @Test
    fun `given My inks is full when Use a new hex, then the oldest goes, and Undo brings it back in its place`() {
        val list = listOf("C0392B", "2E6F95", "E8A87C", "6B4E71", "1F7A5C", "D4B000", "8C1C13", "3D5A80", "505955")
        val full = InkLibrary(myInks = list.mapIndexed { i, h -> MyInk(InkHex(h), (list.size - i).toLong()) }, clock = 9)
        val fake = FakeLibrary(full)
        screen(fake)
        mix()
        type(plum)
        composeRule.onNodeWithTag(InkMixerFullNoteTestTag).fetchSemanticsNode() // it says what will go
        use()
        assertFalse("the least recently used is gone", fake.library.myInks.any { it.hex == InkHex("505955") })
        assertTrue(fake.library.myInks.any { it.hex == plum })

        undo()
        assertEquals("same inks, same order", full, fake.library)
    }

    @Test
    fun `given a draft when Mix then Use, then the ink joins the palette and My inks, the piece is not re-inked, and Undo restores both`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        assertEquals("the draft holds the piece's ink", 1, stripPots())
        mix()
        composeRule.onNodeWithTag(InkMixerUseTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.InkMix.ADD_TO_PALETTE, it) }
        type(slate)
        use()
        assertEquals(2, stripPots())
        assertEquals(listOf(slate), fake.library.myInks.map { it.hex })
        assertEquals("the piece keeps its ink", brick.toColorRgba(), colour())
        assertTrue(snackSays(Copy.BenchInk.mixedAdded(words(slate))))

        undo()
        assertEquals(InkLibrary.EMPTY, fake.library)
        assertEquals("the draft is back as it was", 1, stripPots())
    }

    @Test
    fun `given a draft holding the piece's ink when Mix then Use it, then it is already in the palette`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        mix()
        use()
        assertEquals(1, stripPots())
        assertTrue(snackSays(Copy.BenchInk.mixedAlreadyIn(words(brick))))
        assertEquals(listOf(brick), fake.library.myInks.map { it.hex })
    }

    @Test
    fun `given a palette of six being edited when Mix then Use, then the ink is only kept in My inks, and Undo restores it`() {
        val six = listOf("C0392B", "2E6F95", "E8A87C", "6B4E71", "1F7A5C", "D4B000").map { InkHex(it) }
        val start = InkLibrary(palettes = listOf(InkPalette(1, "Six", six)), nextPaletteId = 2)
        val fake = FakeLibrary(start)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag))
        composeRule.onNodeWithTag("$BenchInkMenuItemTestTag-${Copy.BenchInk.EDIT}").performClick()
        advance()
        assertEquals(6, stripPots())
        mix()
        composeRule.onNodeWithTag(InkMixerUseTestTag).fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text.let { assertEquals(Copy.InkMix.SAVE_TO_MY_INKS, it) }
        use()
        assertEquals(6, stripPots())
        assertTrue(snackSays(Copy.BenchInk.mixedKept(words(brick))))
        assertEquals(listOf(brick), fake.library.myInks.map { it.hex })
        assertEquals(brick.toColorRgba(), colour())

        undo()
        assertEquals(start, fake.library)
        assertEquals(6, stripPots())
    }

    @Test
    fun `given the store fails when Mix then Use in edit mode, then nothing changes and no snack claims it`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        tap(composeRule.onNodeWithTag(BenchInkNewPaletteTestTag))
        mix()
        type(slate)
        fake.fail = true
        use()
        assertEquals(1, stripPots())
        assertEquals(0, composeRule.onAllNodesWithContentDescription(Copy.BenchInk.mixedAdded(words(slate))).fetchSemanticsNodes().size)
    }

    @Test
    fun `given the mixer is open when Back, then the mixer closes first and the tray only on a second Back`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        mix()
        assertTrue(mixerOpen())
        back()
        assertFalse("Back closed the mixer", mixerOpen())
        assertTrue("the tray is still open", trayOpen())
        back()
        assertFalse("a second Back closes the tray", trayOpen())
        assertEquals("Cancel-by-Back changes nothing", InkLibrary.EMPTY, fake.library)
    }

    @Test
    fun `given the mixer is open when Cancel, then it closes and nothing changes`() {
        val fake = FakeLibrary(InkLibrary.EMPTY)
        screen(fake)
        mix()
        type(slate)
        press(composeRule.onNodeWithTag(InkMixerCancelTestTag))
        assertFalse(mixerOpen())
        assertTrue(trayOpen())
        assertEquals(brick.toColorRgba(), colour())
        assertEquals(InkLibrary.EMPTY, fake.library)
    }
}
