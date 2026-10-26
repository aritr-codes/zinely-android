package com.aritr.zinely.feature.editor

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import com.aritr.zinely.ui.a11y.platformNode
import com.aritr.zinely.ui.theme.ZinelyMakerInkId
import com.aritr.zinely.ui.theme.ZinelyTheme
import com.aritr.zinely.ui.theme.zinelyContentInks
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Focus in the Ink tray (A40 rev. 2 `keepFocus` and its fallbacks, INK task 9), and the store-failure line,
 * at the screen. A control that survives a re-render keeps focus; one that is gone hands it on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class BenchInkTrayFocusTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeLibrary(initial: InkLibrary) {
        var library by mutableStateOf(initial)
        var fail = false
        var failed = 0

        suspend fun edit(transform: (InkLibrary) -> InkLibrary): InkLibraryEdit {
            if (fail) { failed++; throw IOException("disk full") }
            val before = library
            library = transform(before)
            return InkLibraryEdit(before, library)
        }
    }

    private val inks = zinelyContentInks()
    private val brick = inks[ZinelyMakerInkId.Brick].value.inkHex()
    private val forest = inks[ZinelyMakerInkId.Forest].value.inkHex()
    private val grey = InkHex("505955")
    private val red = InkHex("C0392B")

    private lateinit var inputMode: InputModeManager

    private fun screen(fake: FakeLibrary): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val store = EditorStore(
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
            inputMode = LocalInputModeManager.current
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
        // Input focus is a keyboard's (and a switch's): in touch mode a clickable takes none, which is the
        // platform's rule, not the tray's. TalkBack's own focus is the device pass's to check.
        composeRule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        store.dispatch(Intent.PlaceText(Transform(20.0, 60.0, 60.0, 18.0), "hi"))
        val id = store.uiState.value.selection.single()
        store.dispatch(Intent.StyleText(id, color = brick.toColorRgba()))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("$BenchContextBarTestTag-${Copy.BenchVerbs.INK}").performClick()
        composeRule.waitForIdle()
        return store
    }

    /** Scroll on a running clock, then act; a click handler's write needs the apply notifications sent. */
    private fun SemanticsNodeInteraction.act(action: SemanticsNodeInteraction.() -> Unit) {
        runCatching { performScrollTo() }
        composeRule.waitForIdle()
        action()
        composeRule.runOnUiThread { Snapshot.sendApplyNotifications() }
        composeRule.waitForIdle()
    }

    private fun SemanticsNodeInteraction.focusThenClick() = act {
        performSemanticsAction(SemanticsActions.RequestFocus)
        performClick()
    }

    private fun menu(label: String) = composeRule.onNodeWithTag("$BenchInkMenuItemTestTag-$label").act { performClick() }

    private fun undo() = composeRule.onNodeWithTag(BenchSnackActionTestTag).act { performClick() }

    private fun snackSays(line: String) =
        composeRule.onAllNodesWithContentDescription(line).fetchSemanticsNodes().size == 1

    private fun twoPalettes(openId: Long = 2) = InkLibrary(
        myInks = listOf(MyInk(grey, 2), MyInk(red, 1)),
        palettes = listOf(InkPalette(2, "Sea", listOf(forest, brick)), InkPalette(1, "Warm", listOf(brick, forest))),
        clock = 2,
        nextPaletteId = 3,
        openPaletteId = openId,
    )

    // — a control that survives keeps focus —

    @Test
    fun `given a pot, a strip or a toggle has focus when it is tapped then it keeps focus`() {
        screen(FakeLibrary(twoPalettes()))

        composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[1].focusThenClick()
        composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[1].assertIsFocused()

        composeRule.onAllNodesWithTag(BenchInkStripTestTag)[1].focusThenClick()
        composeRule.onAllNodesWithTag(BenchInkStripTestTag)[1].assertIsFocused()

        composeRule.onNodeWithTag(BenchInkNewPaletteTestTag).act { performClick() }
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).focusThenClick()
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).assertIsFocused()
    }

    // — fallbacks —

    @Test
    fun `given Delete with a palette left then focus goes to the open palette's More, and with none to New palette`() {
        val fake = FakeLibrary(twoPalettes())
        screen(fake)
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DELETE)
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).assertIsFocused()

        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DELETE)
        assertTrue(fake.library.palettes.isEmpty())
        composeRule.onNodeWithTag(BenchInkNewPaletteTestTag).assertIsFocused()
    }

    @Test
    fun `given Duplicate or a starter's Save a copy then focus goes to the open palette's More`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DUPLICATE)
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).assertIsFocused()

        composeRule.onNodeWithTag(BenchInkStartersTestTag).act { performClick() }
        composeRule.onAllNodesWithTag(BenchInkSaveCopyTestTag)[0].act { performClick() }
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).assertIsFocused()
    }

    @Test
    fun `given Save or Cancel of an edit then focus goes to the open palette's More`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.EDIT)
        composeRule.onNodeWithTag(BenchInkCancelTestTag).act { performClick() }
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).assertIsFocused()

        composeRule.onNodeWithTag(BenchInkNewPaletteTestTag).act { performClick() }
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).act { performClick() }
        composeRule.onNodeWithTag(BenchInkSavePaletteTestTag).act { performClick() }
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).assertIsFocused()
    }

    @Test
    fun `given Remove from My inks then focus goes to the first kept pot, and to Mix when none is left`() {
        val fake = FakeLibrary(twoPalettes())
        screen(fake)
        val remove = { i: Int ->
            composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[i].act {
                val action = fetchSemanticsNode().config[SemanticsActions.CustomActions]
                    .single { it.label == Copy.BenchInk.REMOVE_FROM_MY_INKS }
                composeRule.runOnUiThread { action.action() }
            }
        }
        remove(0)
        composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[0].assertIsFocused()
        remove(0)
        assertTrue(fake.library.myInks.isEmpty())
        composeRule.onNodeWithTag(BenchInkMixTestTag).assertIsFocused()
    }

    @Test
    fun `given a strip pot's remove then focus goes to that ink's toggle`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.EDIT)
        composeRule.onNodeWithContentDescription(Copy.BenchInk.removeFromPalette(Copy.BenchInk.FOREST)).act { performClick() }
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).assertIsFocused()
    }

    @Test
    fun `given Undo of a Delete then focus goes to Current ink, and Undo of a Cancel to the count line`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DELETE)
        undo()
        composeRule.onNodeWithTag(BenchInkCurrentTestTag).assertIsFocused()

        composeRule.onNodeWithTag(BenchInkNewPaletteTestTag).act { performClick() }
        composeRule.onNodeWithContentDescription(Copy.BenchInk.FOREST).act { performClick() }
        composeRule.onNodeWithTag(BenchInkCancelTestTag).act { performClick() }
        undo()
        composeRule.onNodeWithTag(BenchInkCountTestTag).assertIsFocused()
    }

    @Test
    fun `given an ink tap's Undo then focus goes to Current ink`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[1].act { performClick() }
        undo()
        composeRule.onNodeWithTag(BenchInkCurrentTestTag).assertIsFocused()
    }

    // — never silent —

    @Test
    fun `given the store fails on Delete, Duplicate, Remove or a strip then nothing changes and the snack says so, without Undo`() {
        val start = twoPalettes()
        val fake = FakeLibrary(start)
        screen(fake)
        fake.fail = true
        // Each step must reach the store and fail there (`failed` counts it), so a later step cannot pass on an
        // earlier step's snack alone.
        val step = { n: Int, act: () -> Unit ->
            act()
            assertEquals("step $n reached the store", n, fake.failed)
            assertTrue("step $n is said", snackSays(Copy.BenchInk.SAVE_FAILED))
        }

        step(1) { composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }; menu(Copy.BenchInk.DELETE) }
        step(2) { composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }; menu(Copy.BenchInk.DUPLICATE) }
        step(3) {
            composeRule.onAllNodesWithTag(BenchInkKeptTestTag)[0].act {
                val action = fetchSemanticsNode().config[SemanticsActions.CustomActions]
                    .single { it.label == Copy.BenchInk.REMOVE_FROM_MY_INKS }
                composeRule.runOnUiThread { action.action() }
            }
        }
        step(4) { composeRule.onAllNodesWithTag(BenchInkStripTestTag)[1].act { performClick() } }

        assertEquals(start, fake.library)
        assertEquals(0, composeRule.onAllNodesWithTag(BenchSnackActionTestTag, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `given the store fails on an Undo then the snack says it could not save`() {
        val fake = FakeLibrary(twoPalettes())
        screen(fake)
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DELETE)
        fake.fail = true
        undo()
        assertTrue(snackSays(Copy.BenchInk.SAVE_FAILED))
    }

    @Test
    fun `given a tray snack then its line is a polite live region on the platform`() {
        screen(FakeLibrary(twoPalettes()))
        composeRule.onNodeWithTag(BenchInkPaletteMoreTestTag).act { performClick() }
        menu(Copy.BenchInk.DELETE)
        val voice = composeRule.onNodeWithTag(BenchSnackVoiceTestTag, useUnmergedTree = true).platformNode(composeRule.activity)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, voice.liveRegion)
        assertEquals(Copy.BenchInk.deleted("Sea"), voice.contentDescription)
    }
}
