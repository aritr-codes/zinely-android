package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform

/**
 * Editor intents (ADR-029 §2.2). Each folds purely into a new [EditorModel] via [EditorReducer.reduce].
 * `UpdateTransform` is deliberately absent — live drag frames are preview-only via `graphicsLayer{}` and
 * never reach the reducer (§5.1).
 */
public sealed interface Intent {
    // — selection —
    public data class Select(val id: String?) : Intent
    public data class SelectAt(val pagePoint: PtPoint) : Intent
    public data object ClearSelection : Intent

    // — placement / content —
    public data class PlaceText(val transform: Transform, val text: String) : Intent
    public data object RequestAddImage : Intent
    public data class CommitAddImage(val element: com.aritr.zinely.core.model.ImageElement) : Intent

    // — text-edit session (§5.6, D5): begin/commit/cancel, like a drag — one session = one EditTextCommand.
    /** Open a text-edit session on a [TextElement] by id (the a11y "Edit text" action). */
    public data class BeginEditText(val id: String) : Intent

    /** Open a text-edit session on the topmost [TextElement] at a page point (the double-tap seam). */
    public data class BeginEditTextAt(val pagePoint: PtPoint) : Intent

    /**
     * The double-tap seam that **retargets by the topmost element's type** (ADR-053 §4): text → inline
     * edit, image → [Interaction.Reframing]; empty space → no-op. Supersedes [BeginEditTextAt] as the
     * single double-tap entry so the two verbs can never steal each other's gesture — the feature layer
     * dispatches this one intent and the reducer routes.
     */
    public data class DoubleTapAt(val pagePoint: PtPoint) : Intent

    // — image reframe session (ADR-053): begin/commit/cancel like the text-edit + transform sessions —
    // one session bakes to exactly one [EditImageCommand]; the working pan/zoom preview lives in the
    // feature layer (§5.1) until [CommitReframe]. Framing persists as the element's crop/fit — no new model.
    /** Open a Reframe session on an [com.aritr.zinely.core.model.ImageElement] by id (the a11y "Reframe" action). */
    public data class BeginReframe(val id: String) : Intent

    /** Commit the session's framing (only `crop`/`fit` are taken; the crop is clamped valid). [token] rejects
     *  a late commit after nav/cancel/new session, like [CommitText]/[CommitTransform]. */
    public data class CommitReframe(
        val id: String,
        val after: com.aritr.zinely.core.model.ImageElement,
        val token: Long,
    ) : Intent

    /** Discard an open Reframe session's preview. [token]-gated so a stale cancel can't idle a newer session. */
    public data class CancelReframe(val token: Long) : Intent

    /** Replace a photo's bytes **in place**, preserving its framing (crop/fit/transform/zIndex) — ADR-053 §6.
     *  One undoable command; no-op if [assetId] is unchanged. */
    public data class ReplaceImage(val id: String, val assetId: String) : Intent

    /** Reset a photo's framing to the placement default (`Fit.FILL`, full crop) — ADR-053 §6. One undoable
     *  command; no-op if already at the default. */
    public data class ResetFraming(val id: String) : Intent

    /** Commit the session's draft. [token] rejects a late commit after nav/cancel/new session (D5). */
    public data class CommitText(val id: String, val after: TextElement, val token: Long) : Intent

    /** Discard the session's draft (back/dismiss); a still-empty box is removed (§5.6). [token] rejects a
     *  stale cancel after a newer session opened (D5). */
    public data class CancelText(val id: String, val token: Long) : Intent

    // — transform (gesture + a11y twins share the commit path, §6) —
    public data class BeginTransform(val ids: Set<String>) : Intent
    public data class CommitTransform(val after: Map<String, Transform>, val token: Long) : Intent

    /** Discard an open transform session's preview. [token]-gated (like [CommitTransform]) so a stale cancel
     *  from a superseded session can't idle a newer one (Codex review, host increment). */
    public data class CancelTransform(val token: Long) : Intent
    public data class Nudge(val deltaPt: PtPoint) : Intent
    public data class ScaleBy(val factor: Double) : Intent
    public data class RotateBy(val degrees: Double) : Intent

    // — structure —
    public data class Reorder(val id: String, val op: ReorderOp) : Intent
    public data class Delete(val ids: Set<String>) : Intent

    // — view (display-only; never autosaved, never in history — §5) —
    /**
     * Set the preview viewport ([ViewState]) — device px-per-point and page pan — after the host measures
     * the canvas. Display-only: it mutates neither the document, the selection, nor the interaction, emits
     * no [Effect.Autosave], and never enters undo history. The host keeps it the single source so the
     * gesture commit ([CommitTransform] via `LiveSnap`) and the preview render share one scale.
     */
    public data class SetViewport(val screenPxPerPt: Float, val pageOffset: PtPoint) : Intent

    // — pages —
    public data class GoToPage(val index: Int) : Intent
    public data object AddPage : Intent
    public data class DeletePage(val index: Int) : Intent

    // — history —
    public data object Undo : Intent
    public data object Redo : Intent
}

/** Side effects a reduction can request; performed by the store's runner, never in the reducer (§2.2). */
public sealed interface Effect {
    /** Persist the document — emitted ONLY by document-mutating intents; runner debounces (required-fix #5). */
    public data class Autosave(val document: com.aritr.zinely.core.model.ZineDocument) : Effect

    /** Launch the pick→decode→AssetStore pipeline; success dispatches [Intent.CommitAddImage] (rec #6). */
    public data object PickAndDecodeImage : Effect

    /** An accessibility live-region announcement (e.g. selection / off-page undo). */
    public data class Announce(val text: String) : Effect
}

/** The result of one reduction: the next model plus any side effects to run. */
public data class Reduction(val model: EditorModel, val effects: List<Effect> = emptyList())
