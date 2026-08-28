package com.aritr.zinely.feature.editor

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
// NB: compose-ui's LocalLifecycleOwner is deprecated in favour of lifecycle-runtime-compose's, but moving
// homes means bumping the graph-wide lifecycle version (catalog is on 2.6.1 ktx) — deferred to a dep-bump.
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aritr.zinely.core.copy.Copy
import com.aritr.zinely.core.editor.Intent
import com.aritr.zinely.core.editor.Interaction
import com.aritr.zinely.core.model.TextCoverage
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.analyzeTextCoverage
import com.aritr.zinely.ui.theme.ZinelyTheme
import androidx.compose.ui.text.TextStyle as ComposeTextStyle

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
 * **Unsupported-character coverage ([ADR-070](../DECISIONS.md#adr-070)).** The draft is the earliest
 * place an unprintable character can be *seen*, so coverage is analysed **here** — on the seed (catching
 * text opened from an import or a prior session) and on every keystroke — and reported out via
 * [onCoverageChanged] for the host to surface as the [EditorCoverageNotice]. This stays out of the
 * reducer for the same reason the draft does: it is per-keystroke feature state, not a document mutation.
 * The analysis ([analyzeTextCoverage]) is pure and allocation-light, so running it per key is cheap and
 * touches no font, canvas, or device. On dispose the report resets to [TextCoverage.Covered] so the
 * notice clears with the session; the character itself is **never stripped** from the draft.
 *
 * @param session the open edit session (its `id`/`token` scope the commit).
 * @param element the document [TextElement] being edited (the `before`; seeds the draft + carries style).
 * @param commitText forwards [Intent.CommitText] into the store and reports whether it was accepted.
 * @param modifier sizing/placement applied by the host (C3: the element's own device rect on the page).
 * @param textStyle how the draft is drawn. Hoisted for C3 ([BenchEditingSurface]): editing happens **in
 *   place** on the page now, so the draft must be drawn in the element's own size, ink, alignment and
 *   weight rather than in a fixed sheet style, or the text visibly changes appearance the moment it is
 *   tapped. The default is the pre-C3 sheet style and is what the non-in-place callers (tests) still get.
 * @param cursorColor the caret colour — frozen `.caret{background:var(--matcha)}` (`v2-bench.html:204`)
 *   for the V2 in-place surface. Defaults to the V1 `coralStrong` this file shipped with. [BenchEditingSurface]
 *   passes `Transparent` and draws the frozen caret itself; see [onTextLayout].
 * @param onTextLayout the field's measured layout, and [onDraftChanged] the live draft. Both exist for the
 *   frozen caret: it is 1.5px × 1.05em, `--matcha`, blinking `1.05s steps(1)` and **still** under reduced
 *   motion (ADR-093 row 3.8), and none of those four are settable on the platform cursor, which exposes
 *   only a brush. Drawing it needs the cursor's rect, which needs the layout and the selection — so the
 *   two escape here rather than the caret moving inside this file, which owns the *session*, not the skin.
 * @param onCoverageChanged reports the draft's current [TextCoverage] (ADR-070): on the seed, on every
 *   keystroke, and [TextCoverage.Covered] on dispose. The host raises the [EditorCoverageNotice] from it.
 * @param onCommitted observes an explicit [Intent.CommitText] boundary (focus loss, IME Done or lifecycle
 *   pause). Hosts use this only for ephemeral follow-up work that must happen after the draft is dispatched,
 *   such as a queued page transition; it is never invoked by the disposal fallback after cancellation.
 * @param commitText dispatches a text commit and reports whether the reducer accepted it. A stale commit
 *   must not trigger host follow-up work.
 */
@Composable
public fun EditTextSession(
    session: Interaction.EditingText,
    element: TextElement,
    commitText: (Intent.CommitText) -> Boolean,
    modifier: Modifier = Modifier,
    onCoverageChanged: (TextCoverage) -> Unit = {},
    textStyle: ComposeTextStyle = LocalTextStyle.current
        .merge(MaterialTheme.typography.bodyLarge)
        .copy(fontFamily = ZinelyTheme.typography.shell),
    cursorColor: Color = ZinelyTheme.colors.coralStrong,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onDraftChanged: (TextFieldValue) -> Unit = {},
    onCommitted: () -> Unit = {},
) {
    var draft by remember(session.token) { mutableStateOf(TextFieldValue(element.text)) }
    var committed by remember(session.token) { mutableStateOf(false) }
    var hadFocus by remember(session.token) { mutableStateOf(false) }
    val focusRequester = remember(session.token) { FocusRequester() }

    // rememberUpdatedState so the lifecycle/dispose effects always read the LATEST draft, not the value
    // captured when the effect first ran (the draft mutates every keystroke).
    val latestDraft by rememberUpdatedState(draft)
    // Same reason for the coverage callback: the dispose-clear below must call whatever the host passed
    // most recently, not the lambda captured when the effect first ran.
    val latestOnCoverage by rememberUpdatedState(onCoverageChanged)

    // ADR-070: analyse the draft's script coverage on the seed (this runs on first composition, catching
    // pre-existing/imported unprintable text) and on every keystroke (`draft.text` re-keys it). Pure and
    // allocation-light, so per-key is cheap; the host raises the EditorCoverageNotice from the result.
    LaunchedEffect(session.token, draft.text) {
        latestOnCoverage(analyzeTextCoverage(draft.text))
    }

    fun commit(notifyHost: Boolean = true) {
        if (committed) return
        committed = true
        val accepted = commitText(Intent.CommitText(session.id, element.copy(text = latestDraft.text), session.token))
        // A disposal commit is deliberately silent: after CancelText its token is stale and the reducer
        // correctly ignores it, so treating it as a successful commit could advance a queued page change.
        // Focus loss, IME Done and lifecycle pause are the explicit commit boundaries that may notify a host.
        if (notifyHost && accepted) onCommitted()
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
            commit(notifyHost = false)
            // Clear the coverage notice with the session (ADR-070): the draft is gone, so there is no
            // longer any unprintable-in-progress text to warn about.
            latestOnCoverage(TextCoverage.Covered)
        }
    }

    DisposableEffect(session.token) {
        focusRequester.requestFocus()
        onDispose { }
    }

    BasicTextField(
        value = draft,
        onValueChange = { draft = it; onDraftChanged(it) },
        onTextLayout = onTextLayout,
        modifier = modifier
            .testTag(EditTextSessionTestTag)
            // The empty edit box renders as only a caret (frozen bench.html spec), so it carries no text to
            // name it — TalkBack would focus a bare "edit box". A non-visual name fixes that (WCAG 4.1.2)
            // without touching the design; typed content still reads via the field's editableText value.
            .semantics { contentDescription = Copy.EditText.ZINE_TEXT }
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) hadFocus = true else if (hadFocus) commit()
            },
        // Both hoisted for C3 — the in-place surface draws the draft in the ELEMENT's own style, not in a
        // fixed sheet style, and tints the caret the frozen `--matcha`. The defaults above preserve the
        // pre-C3 appearance (bundled Inter at bodyLarge, V1's `coral-strong` caret) for every other caller.
        textStyle = textStyle,
        cursorBrush = SolidColor(cursorColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
    )
}
