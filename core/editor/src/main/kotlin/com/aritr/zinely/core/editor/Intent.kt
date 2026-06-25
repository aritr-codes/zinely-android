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
    public data class CommitText(val id: String, val after: TextElement) : Intent

    // — transform (gesture + a11y twins share the commit path, §6) —
    public data class BeginTransform(val ids: Set<String>) : Intent
    public data class CommitTransform(val after: Map<String, Transform>, val token: Long) : Intent
    public data object CancelTransform : Intent
    public data class Nudge(val deltaPt: PtPoint) : Intent
    public data class ScaleBy(val factor: Double) : Intent
    public data class RotateBy(val degrees: Double) : Intent

    // — structure —
    public data class Reorder(val id: String, val op: ReorderOp) : Intent
    public data class Delete(val ids: Set<String>) : Intent

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
