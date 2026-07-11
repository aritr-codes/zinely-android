package com.aritr.zinely.feature.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
// NB: compose-ui's LocalLifecycleOwner is deprecated in favour of lifecycle-runtime-compose's, but moving
// homes means bumping the graph-wide lifecycle version (catalog is on 2.6.1 ktx) — deferred to a dep-bump.
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.ui.theme.ZinelyTheme

/** Test tag on the edit-session text field. */
public const val EditTextSessionTestTag: String = "edit-text-session"

/**
 * The race-safe text-edit session UI (ADR-029 §5.6, D5). The draft text is **feature-ephemeral** — held in
 * a local [TextFieldValue], never written to the document until commit — mirroring how live gesture deltas
 * stay out of the reducer. The whole session is one [Intent.CommitText] (one undo step); intermediate
 * keystrokes never reach the store.
 *
 * **Commit triggers** (whichever first, each fires exactly once via the [committed] latch):
 *  - keyboard **Done** action,
 *  - **focus loss** (the user tapped away),
 *  - **`ON_PAUSE`** — the durability force-commit (ADR-009): backgrounding the editor flushes the pending
 *    draft into the document **synchronously** so the autosave that the `CommitText` reduction emits runs
 *    before the process can be killed. The reduction's [session] token rejects any later duplicate.
 *  - leaving composition (session replaced/closed) → commit the pending draft so a tap-away is never lost;
 *    the token guard no-ops if the store already moved on.
 *
 * An empty draft is resolved by the reducer (a still-blank freshly-placed box is removed, coalescing its
 * placement; an existing box cleared to empty is deleted) — this UI never special-cases empty. Stateless
 * otherwise: [session] + [element] are hoisted; keying the drafts on [Interaction.EditingText.token]
 * resets them when a new session opens.
 *
 * @param session the open edit session (its `id`/`token` scope the commit).
 * @param element the document [TextElement] being edited (the `before`; seeds the draft + carries style).
 * @param dispatch forwards an [Intent] into the store.
 * @param modifier sizing/placement applied by the host (e.g. an IME-padded sheet).
 */
@Composable
public fun EditTextSession(
    session: Interaction.EditingText,
    element: TextElement,
    dispatch: (Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(session.token) { mutableStateOf(TextFieldValue(element.text)) }
    var committed by remember(session.token) { mutableStateOf(false) }
    var hadFocus by remember(session.token) { mutableStateOf(false) }
    val focusRequester = remember(session.token) { FocusRequester() }

    // rememberUpdatedState so the lifecycle/dispose effects always read the LATEST draft, not the value
    // captured when the effect first ran (the draft mutates every keystroke).
    val latestDraft by rememberUpdatedState(draft)

    fun commit() {
        if (committed) return
        committed = true
        dispatch(Intent.CommitText(session.id, element.copy(text = latestDraft.text), session.token))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, session.token) {
        val observer = LifecycleEventObserver { _, event ->
            // ON_PAUSE is the latest point a foreground commit is guaranteed to run before the process may
            // be killed; commit synchronously so the autosave the reduction emits captures this draft.
            if (event == Lifecycle.Event.ON_PAUSE) commit()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Leaving composition without an explicit commit (e.g. the session was navigated away) ⇒ commit
            // the draft so a tap-away is never lost; the token guard no-ops if the store already moved on.
            commit()
        }
    }

    DisposableEffect(session.token) {
        focusRequester.requestFocus()
        onDispose { }
    }

    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .testTag(EditTextSessionTestTag)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) hadFocus = true else if (hadFocus) commit()
            },
        // Draft renders in the frozen shell voice (Inter) — the SAME bundled face the export/preview path
        // renders the committed TextElement with — so the draft and the baked artifact read alike (bench.html
        // sets the editing surface in a bundled voice, never a system fallback).
        textStyle = LocalTextStyle.current
            .merge(MaterialTheme.typography.bodyLarge)
            .copy(fontFamily = ZinelyTheme.typography.shell),
        // Frozen caret colour: bench.html `.block.text .surface[contenteditable]{ caret-color:var(--coral-strong) }`.
        cursorBrush = SolidColor(ZinelyTheme.colors.coralStrong),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
}
