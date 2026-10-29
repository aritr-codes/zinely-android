package com.aritr.zinely.feature.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.input.ImeAction
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
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
 * The text-edit session UI (ADR-029 §5.6): typing stays feature-ephemeral; the Done chip (focus loss)
 * commits the whole draft as one [Intent.CommitText] against a real [EditorStore]; the token guard makes a
 * second commit a no-op. Robolectric NATIVE.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditTextSessionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(Dispatchers.Unconfined)

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

    // The style row's Done chip commits by clearing focus (EditorScreen `onDone`), so this is that exact path.
    private var focusManager: FocusManager? = null

    private fun pressDone() {
        composeRule.runOnIdle { focusManager!!.clearFocus() }
    }

    private fun setSession(store: EditorStore, onCommitted: () -> Unit = {}) {
        composeRule.setContent {
            focusManager = LocalFocusManager.current
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
                        onCommitted = onCommitted,
                    )
                }
            }
        }
    }

    @Test
    fun typing_then_done_commits_the_whole_draft_as_one_edit() {
        val store = store(initialText = "old")
        val id = store.uiState.value.selection.single()
        setSession(store)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("new copy")
        // Mid-typing the document still holds the original — the draft is feature-ephemeral.
        assertEquals("old", (store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement).text)

        pressDone()
        composeRule.waitForIdle()

        val el = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("new copy", el.text)
        assertTrue("session closed", store.uiState.value.interaction is Interaction.Idle)
        // One edit ⇒ exactly one undo step restores the original.
        store.dispatch(Intent.Undo)
        val restored = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("old", restored.text)
    }

    @Test
    fun enter_inserts_a_newline_and_does_not_commit() {
        val store = store(initialText = "")
        val id = store.uiState.value.selection.single()
        var commits = 0
        setSession(store, onCommitted = { commits += 1 })

        // A soft keyboard's Enter key sends the field's IME action when it is Done (commit) and a newline
        // when it is Default, so the action the field advertises to the platform is what decides it.
        composeRule.onNodeWithTag(EditTextSessionTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ImeAction, ImeAction.Default))
        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextInput("one")
        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextInput("\n")
        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextInput("two")
        composeRule.waitForIdle()

        assertEquals(0, commits)
        assertTrue("still editing", store.uiState.value.interaction is Interaction.EditingText)
        pressDone()
        composeRule.waitForIdle()
        val el = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("one\ntwo", el.text)
    }

    @Test
    fun done_commits_once_even_though_disposal_also_fires() {
        // After Done the session goes Idle and the composable leaves composition; the onDispose commit must
        // be a no-op (committed latch + token guard), so the single edit stays a single undo step.
        val store = store(initialText = "old")
        val id = store.uiState.value.selection.single()
        setSession(store)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("edited")
        pressDone()
        composeRule.waitForIdle()

        store.dispatch(Intent.Undo)
        val restored = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("old", restored.text) // one undo fully restores ⇒ there was exactly one command
    }

    @Test
    fun cancellation_never_reports_a_successful_text_commit() {
        val store = store(initialText = "old")
        val id = store.uiState.value.selection.single()
        var commits = 0
        setSession(store, onCommitted = { commits += 1 })

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("discarded")
        val token = (store.uiState.value.interaction as Interaction.EditingText).token
        store.dispatch(Intent.CancelText(id, token))
        composeRule.waitForIdle()

        assertEquals(0, commits)
        val element = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("old", element.text)
    }
}
