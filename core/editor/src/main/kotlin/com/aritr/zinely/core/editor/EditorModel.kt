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
    /**
     * Monotonic session-token source — keeps the reducer pure (no clock); see [Interaction] tokens.
     * Also the element-id source (`el-<token>`), which is why it **must** be seeded from [document]
     * rather than from a constant — see [firstFreeToken].
     */
    val nextToken: Long = firstFreeToken(document),
)

/**
 * The first `el-<n>` token no element of [document] has already claimed.
 *
 * **This is a persistence-boundary invariant, and it used to be broken.** The reducer mints element ids
 * as `el-${nextToken}` and documents that "it can never collide with an existing element" — true within
 * a session, because the counter only ever advances. But `nextToken` is reducer state and `ZineDocument`
 * is what gets saved, so reopening a project rebuilt the model with a *constant* `nextToken = 1` while
 * the document still held `el-1`. The next element placed after any reopen therefore duplicated an
 * existing id, and because a reopen is the normal way to use the app, this fired for essentially every
 * returning user on their first edit.
 *
 * The damage was not cosmetic. `DefaultDocumentValidator` rejects `element.id.duplicate`, and it runs on
 * **load**, not on save — so autosave happily wrote the duplicate and the project became permanently
 * unopenable ("Couldn't open this project."), with the zine recoverable only from `document.json.bak`
 * on disk and not through any UI. Two further symptoms shared the same cause: selection is keyed by id,
 * so both elements reported *selected* to TalkBack at once, and `PlaceCommand.invertOn` would have
 * deleted **both** matches on undo.
 *
 * Seeding here fixes it at the point the invariant is actually established — the model is the only thing
 * that knows both the document and the counter — and needs no schema change or migration, so a document
 * saved before the fix stops *acquiring* duplicates the next time it is opened. It does not repair a
 * document that already contains them: that damage is on disk, the load-side validator still rejects it,
 * and `isLoadable` treats the payload as decodable so the `.bak` beside it is never offered either. Such
 * a file stays unopenable (see ARCHITECTURE §7.0). Ids that are not of the `el-<n>` shape (a
 * document authored elsewhere) are ignored rather than parsed: they cannot collide with a generated id,
 * because a generated one always has that shape.
 *
 * Note that [EditorModel.copy] does **not** re-run this default, which is what keeps the counter
 * monotonic across an undo — a token stays spent even after the element that spent it is removed.
 */
public fun firstFreeToken(document: ZineDocument): Long =
    document.pages
        .asSequence()
        .flatMap { it.elements.asSequence() }
        .mapNotNull { GENERATED_ELEMENT_ID.matchEntire(it.id)?.groupValues?.get(1)?.toLongOrNull() }
        .maxOrNull()
        ?.plus(1)
        ?: 1L

/** The shape [EditorReducer] mints element ids in — `el-` followed by a [EditorModel.nextToken] value. */
private val GENERATED_ELEMENT_ID = Regex("""el-(\d+)""")

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
