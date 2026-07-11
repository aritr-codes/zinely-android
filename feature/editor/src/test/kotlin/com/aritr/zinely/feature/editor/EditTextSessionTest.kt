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
 * The text-edit session UI (ADR-029 §5.6): typing stays feature-ephemeral; the keyboard Done action
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

    private fun setSession(store: EditorStore) {
        composeRule.setContent {
            ZinelyTheme {
                val state by store.uiState.collectAsState()
                val session = state.interaction
                if (session is Interaction.EditingText) {
                    val element = state.document.pages[0].elements
                        .first { it.id == session.id } as TextElement
                    EditTextSession(session = session, element = element, dispatch = store::dispatch)
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

        composeRule.onNodeWithTag(EditTextSessionTestTag).performImeAction()
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
    fun done_commits_once_even_though_disposal_also_fires() {
        // After Done the session goes Idle and the composable leaves composition; the onDispose commit must
        // be a no-op (committed latch + token guard), so the single edit stays a single undo step.
        val store = store(initialText = "old")
        val id = store.uiState.value.selection.single()
        setSession(store)

        composeRule.onNodeWithTag(EditTextSessionTestTag).performTextReplacement("edited")
        composeRule.onNodeWithTag(EditTextSessionTestTag).performImeAction()
        composeRule.waitForIdle()

        store.dispatch(Intent.Undo)
        val restored = store.uiState.value.document.pages[0].elements.single { it.id == id } as TextElement
        assertEquals("old", restored.text) // one undo fully restores ⇒ there was exactly one command
    }
}
