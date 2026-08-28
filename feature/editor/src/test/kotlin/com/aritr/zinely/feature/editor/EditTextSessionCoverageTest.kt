package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.Script
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.core.model.TextElement
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
 * ADR-070 detection seam: [EditTextSession] analyses the draft's script coverage and reports it out
 * ([EditTextSession]'s `onCoverageChanged`) so the host can raise the [EditorCoverageNotice]. This proves
 * the four properties the design requires: coverage is reported for **pre-existing** text on the seed,
 * updated as the user **types**, reset to [TextCoverage.Covered] on **dispose**, and — the one that
 * matters most — the unsupported character is **never stripped**: committing keeps it verbatim, so no
 * work is silently lost. Robolectric NATIVE.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditTextSessionCoverageTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    // U+0985 BENGALI LETTER A — an unprintable (out-of-bundled-set) character, built from its code point
    // so the test is independent of source-file encoding.
    private val bengaliA = String(Character.toChars(0x0985))

    private fun store(initialText: String): EditorStore {
        val runner = object : EditorEffectRunner {
            override fun run(effect: Effect, dispatch: (Intent) -> Unit) = Unit
        }
        val s = EditorStore(
            EditorModel(
                document = ZineDocument(
                    format = ZineFormat.SINGLE_SHEET_8,
                    paperSize = PaperSize.LETTER,
                    pages = listOf(Page(index = 0, role = PageRole.INTERIOR)),
                ),
            ),
            scope, Dispatchers.Unconfined, runner,
        )
        s.dispatch(Intent.PlaceText(Transform(40.0, 40.0, 40.0, 20.0), initialText))
        val id = s.uiState.value.selection.single()
        s.dispatch(Intent.BeginEditText(id))
        return s
    }

    private fun setSession(store: EditorStore, onCoverage: (TextCoverage) -> Unit) {
        composeRule.setContent {
            ZinelyTheme {
                val state by store.uiState.collectAsState()
                val session = state.interaction
                if (session is Interaction.EditingText) {
                    val element = state.document.pages[0].elements
                        .first { it.id == session.id } as TextElement
                    EditTextSession(
                        session = session,
                        element = element,
                        commitText = { intent ->
                            val before = store.uiState.value
                            store.dispatch(intent)
                            store.uiState.value != before
                        },
                        onCoverageChanged = onCoverage,
                    )
                }
            }
        }
    }

    @Test
    fun seed_reports_coverage_for_preexisting_unsupported_text() {
        var latest: TextCoverage = TextCoverage.Covered
        setSession(store(initialText = bengaliA)) { latest = it }
        composeRule.waitForIdle()

        assertTrue("seed should flag the pre-existing Bengali character", !latest.isFullyCovered)
        assertEquals(listOf(Script.BENGALI), latest.unsupportedScripts)
    }

    @Test
    fun typing_updates_then_clears_coverage() {
        var latest: TextCoverage = TextCoverage.Covered
        setSession(store(initialText = "hello")) { latest = it }
        composeRule.waitForIdle()
        assertTrue("plain Latin seed is fully covered", latest.isFullyCovered)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("hello $bengaliA")
        composeRule.waitForIdle()
        assertTrue("typing Bengali should flag it", !latest.isFullyCovered)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("hello")
        composeRule.waitForIdle()
        assertTrue("removing it should clear the notice", latest.isFullyCovered)
    }

    @Test
    fun committing_retains_the_unsupported_character() {
        // The one non-negotiable of Direction A: the character is warned about, never stripped. After
        // commit the document must still contain it, so it prints the day Bengali is supported.
        val store = store(initialText = "old")
        val id = store.uiState.value.selection.single()
        setSession(store) { }

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("cafe $bengaliA")
        composeRule.onNodeWithTag(EditTextSessionTestTag).performImeAction()
        composeRule.waitForIdle()

        val el = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("cafe $bengaliA", el.text)
    }

    @Test
    fun dispose_resets_coverage_to_covered() {
        // Committing (Done) closes the session and disposes the field; its onDispose must report Covered
        // so the notice never outlives the draft that raised it.
        var latest: TextCoverage = TextCoverage.Covered
        setSession(store(initialText = bengaliA)) { latest = it }
        composeRule.waitForIdle()
        assertTrue("precondition: seed flagged", !latest.isFullyCovered)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performImeAction()
        composeRule.waitForIdle()

        assertTrue("dispose should reset coverage to Covered", latest.isFullyCovered)
    }
}
