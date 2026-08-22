package com.aritr.zinely.feature.editor

import com.aritr.zinely.core.editor.Effect
import com.aritr.zinely.core.editor.EditorModel
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The empty-state "Add words" wiring ([addTextAndEdit]) must take the beginner straight to typing — it
 * places an EMPTY text box (no committed placeholder sentence) and opens its edit session immediately,
 * rather than leaving a fake sentence behind the hidden double-tap affordance (Codex UX finding). Pure
 * store test (no Compose) — one reducer-owned intent mints the new id, places the box and opens that exact
 * session, even when an older text element was selected before the action.
 */
class EditorAddTextTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

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
            scope, Dispatchers.Unconfined, runner,
        )
    }

    @Test
    fun addWords_places_empty_text_and_opens_its_edit_session() {
        val store = store()

        addTextAndEdit(PtSize(612.0, 792.0), store::dispatch)

        val state = store.uiState.value
        val text = state.document.pages[0].elements.filterIsInstance<TextElement>().single()
        assertEquals("", text.text) // empty — not a "Your words here" placeholder
        val interaction = state.interaction
        assertTrue(
            "expected an open EditingText session on the new element",
            interaction is Interaction.EditingText && interaction.id == text.id,
        )
    }

    @Test
    fun addWords_never_reopens_the_previously_selected_text() {
        val store = store()
        store.dispatch(Intent.PlaceText(Transform(10.0, 10.0, 80.0, 30.0), "old"))
        val oldId = store.uiState.value.selection.single()

        addTextAndEdit(PtSize(612.0, 792.0), store::dispatch)

        val state = store.uiState.value
        val interaction = state.interaction as Interaction.EditingText
        val newText = state.document.pages[0].elements.filterIsInstance<TextElement>().single { it.id != oldId }
        assertEquals(newText.id, interaction.id)
        assertEquals(setOf(newText.id), state.selection)
        assertEquals("old", state.document.pages[0].elements.filterIsInstance<TextElement>().single { it.id == oldId }.text)
    }
}
