package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument

/**
 * The reducer's immutable unit (ADR-029 §2.1). History rides inside the model (valid MVI) but the store
 * exposes a derived, history-free `EditorUiState` to Compose (Codex rec #1) — see [toUiState].
 */
public data class EditorModel(
    val document: ZineDocument,
    val currentPageIndex: Int = 0,
    val selection: Set<String> = emptySet(),
    val view: ViewState = ViewState(),
    val interaction: Interaction = Interaction.Idle,
    val history: History = History(),
    /** Monotonic session-token source — keeps the reducer pure (no clock); see [Interaction] tokens. */
    val nextToken: Long = 1,
)

/** Hoisted preview view-state handed to `PagePreview`: device px per point (density×zoom) and pan. */
public data class ViewState(val screenPxPerPt: Float = 1f, val pageOffset: PtPoint = PtPoint(0.0, 0.0))

/** Undo/redo command stacks (field mementos, §4). */
public data class History(
    val undo: List<Command> = emptyList(),
    val redo: List<Command> = emptyList(),
)

/** The active interaction marker. Per-frame gesture deltas are NOT here — they stay ephemeral (§5). */
public sealed interface Interaction {
    public data object Idle : Interaction

    /**
     * A transform session open on [ids] of page [pageIndex]. The reducer owns the pre-gesture [before]
     * snapshot (single source of truth — Codex required-fix #1); [token] rejects a stale commit after
     * nav/cancel (Codex rec #2).
     */
    public data class Transforming(
        val pageIndex: Int,
        val ids: Set<String>,
        val before: Map<String, Transform>,
        val token: Long,
    ) : Interaction

    /** A text-edit session; draft text lives in the feature layer until [Intent.CommitText] (§5.6). */
    public data class EditingText(val id: String, val token: Long) : Interaction

    /**
     * An image Reframe session (ADR-053) open on [id] of page [pageIndex]. The reducer owns the pre-session
     * [before] snapshot (single source of truth, mirroring [Transforming]); the live pan/zoom preview stays
     * ephemeral in the feature layer and bakes on [Intent.CommitReframe]. [token] rejects a stale commit.
     */
    public data class Reframing(
        val pageIndex: Int,
        val id: String,
        val before: com.aritr.zinely.core.model.ImageElement,
        val token: Long,
    ) : Interaction
}

/** History-free projection exposed to Compose (Codex rec #1). */
public data class EditorUiState(
    val document: ZineDocument,
    val currentPageIndex: Int,
    val selection: Set<String>,
    val view: ViewState,
    val interaction: Interaction,
    val canUndo: Boolean,
    val canRedo: Boolean,
)

/** Project the reducer model into the history-free UI state. */
public fun EditorModel.toUiState(): EditorUiState = EditorUiState(
    document = document,
    currentPageIndex = currentPageIndex,
    selection = selection,
    view = view,
    interaction = interaction,
    canUndo = history.undo.isNotEmpty(),
    canRedo = history.redo.isNotEmpty(),
)
