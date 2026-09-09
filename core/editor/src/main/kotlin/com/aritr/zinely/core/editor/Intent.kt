package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.TextAlign
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform

/** One element-local reflection axis (ADR-113). */
public enum class FlipAxis { HORIZONTAL, VERTICAL }

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
    /** Place a new blank text box and open its edit session atomically. The reducer owns the minted id,
     *  so this cannot accidentally begin editing a previously selected element between two dispatches. */
    public data class PlaceTextAndEdit(val transform: Transform) : Intent
    public data object RequestAddImage : Intent
    /**
     * Ask the existing image pipeline to replace the current-page photo at [id]. This request is deliberately
     * not a document edit: a picker cancel or failure must leave the document, history and selection intact.
     */
    public data class RequestReplaceImage(val id: String) : Intent
    public data class CommitAddImage(val element: com.aritr.zinely.core.model.ImageElement) : Intent

    /**
     * Place one supply from the cabinet on the current page as a
     * [com.aritr.zinely.core.model.DecorElement] (SUPPLIES-SPEC §5, ADR-105 step S7).
     *
     * Shaped exactly like [PlaceText] and for the same reason: one user act ⇒ one [PlaceCommand] ⇒ one
     * undo step, the id minted reducer-side from `nextToken`, and the new element auto-selected (which is
     * also what the freeze's own `openArt()` tile handler does — `selectByKind('decor')`).
     *
     * **Geometry arrives in the intent, it is not computed here.** SUPPLIES-SPEC §5.2's per-family default
     * scale needs the family, and the family lives in `Copy.Supplies.BY_FAMILY` (`:core:copy`) which this
     * Android-free module deliberately does not depend on. So the caller supplies the [transform], as
     * `PlaceText`'s caller already does via `centeredTextBox` — see `benchSupplyPlacement` in
     * `:feature:editor`, which is where the craft constants live and are tested.
     *
     * An unauthored or unknown [supplyId] is **not** rejected here: catalogue membership is checked at the
     * render boundary (SUPPLIES-SPEC §2.2), and the picker never offers one. `mirrored` is deliberately
     * absent — §5.1 lands a supply flat, at 0°, unmirrored; mirroring is a maker verb that arrives later.
     */
    public data class PlaceSupply(
        val supplyId: String,
        val ink: ColorRgba,
        val transform: Transform,
    ) : Intent

    /**
     * Recolour a placed supply — SUPPLIES-SPEC §8's *Change ink*, the second of decor's two type-specific
     * verbs and the first to land.
     *
     * **Why this is not [StyleText] with a decor arm.** `StyleText` resolves its target with
     * `as? TextElement` and carries four other style fields a supply has no concept of (size, align, bold,
     * italic). Widening it would mean four fields that are meaningless for half its callers; a separate
     * intent keeps *"one verb, one intent"* and lets the reducer's decor arm stay a total function.
     *
     * **One tap = one undo step**, matching the ink behaviour text already ships: an immediate commit, not
     * a session. A supply has no blank-equivalent, so there is no counterpart to `styleText`'s
     * refuse-to-style-a-blank-box guard — every ink change on a real supply is a real change.
     *
     * An [id] naming a text box, an image, a missing element, or an element on another page is a **no-op**,
     * exactly as the other type-specific verbs are for the types they do not own.
     */
    public data class InkSupply(val id: String, val ink: ColorRgba) : Intent

    /**
     * Swap the supply at [id] for a different one — SUPPLIES-SPEC §8's *Replace supply*, decor's second
     * type-specific verb.
     *
     * **What survives the swap, and why each is a decision rather than an oversight.** The element keeps
     * its **id** (so selection, z-order and the undo stack all still name it), its **ink** (the maker chose
     * that colour for this spot, not for that outline) and its **mirror** flag. What changes is the
     * [supplyId] and the [transform].
     *
     * **Geometry arrives in the intent for the same reason [PlaceSupply]'s does**: the incoming supply's
     * §5.2 family size needs `Copy.Supplies.BY_FAMILY` from `:core:copy`, which this Android-free module
     * deliberately does not depend on. The caller is `benchSupplyReplacement` in `:feature:editor`, where
     * the craft constants live and are tested — and which owns the owner's 2026-08-17 ruling that a
     * replacement takes the **new family's scale** while keeping the old centre and rotation.
     *
     * Replacing a supply with **itself** is not rejected here; it reduces to a no-op through the same
     * `after == el` short-circuit [InkSupply] uses, so it pushes no empty undo entry.
     *
     * An [id] naming a text box, an image or nothing at all is a no-op, like every other typed verb.
     */
    public data class ReplaceSupply(
        val id: String,
        val supplyId: String,
        val transform: Transform,
    ) : Intent

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

    /** Turn the photocopier filter (X3b, ADR-106) on or off for one photo. One undoable command, and
     *  its own inverse; no-op if [id] names nothing or names a text element. */
    public data class ToggleCopier(val id: String) : Intent

    /**
     * Toggle one reflection axis on a Photo or Art element. Text, missing ids and off-page ids are
     * no-ops. Each accepted tap is one typed edit command, one autosave and one undo step (ADR-113).
     */
    public data class ToggleFlip(val id: String, val axis: FlipAxis) : Intent

    /**
     * Turn one placed photo into the current page's half of a fixed mini-zine spread and add the other
     * half to its booklet partner (ADR-109). [photoAspect] is read from the authoritative import master
     * by the Android feature boundary; [pageSizePt] is the same imposed panel size the editor renders.
     * The pure reducer owns the fixed 2|3, 4|5, 6|7, 8|1 table, crop geometry, id allocation and one-step
     * undo. Invalid dimensions, a missing partner page, or a non-image [id] are no-ops.
     */
    public data class MakeImageSpread(
        val id: String,
        val photoAspect: Double,
        val pageSizePt: PtSize,
    ) : Intent

    /** Commit the session's draft. [token] rejects a late commit after nav/cancel/new session (D5). */
    public data class CommitText(val id: String, val after: TextElement, val token: Long) : Intent

    /** Discard the session's draft (back/dismiss); a still-empty box is removed (§5.6). [token] rejects a
     *  stale cancel after a newer session opened (D5). */
    public data class CancelText(val id: String, val token: Long) : Intent

    /**
     * Immediate-commit style change to a [TextElement] by id (FR-3, ADR-055) — like [Nudge]/[Reorder],
     * not a session: each committed change is one undoable [EditTextCommand]. Every field is a nullable
     * patch; `null` keeps the element's current value, so an untouched field (and `fontFamily`, which has
     * no patch here) is always preserved. No-op if [id] is absent / not text, or the style is unchanged.
     */
    public data class StyleText(
        val id: String,
        val sizePt: Double? = null,
        val color: ColorRgba? = null,
        val align: TextAlign? = null,
        val bold: Boolean? = null,
        val italic: Boolean? = null,
    ) : Intent

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
    /**
     * Make one independent copy of the current page's element at [id]. The reducer mints the copy's id,
     * raises it above the page's current stack, offsets it within [pageSizePt], and selects the copy. The
     * complete act is one [PlaceCommand], so Undo removes only the copy. Missing ids, blank text boxes and
     * invalid page sizes are no-ops.
     */
    public data class DuplicateElement(val id: String, val pageSizePt: PtSize) : Intent
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

    /**
     * Launch the pick→decode→AssetStore pipeline. A null [replacingId] adds a photo; a non-null target
     * replaces that existing photo in place after a successful decode.
     */
    public data class PickAndDecodeImage(val replacingId: String? = null) : Effect

    /** An accessibility live-region announcement (e.g. selection / off-page undo). */
    public data class Announce(val text: String) : Effect
}

/** The result of one reduction: the next model plus any side effects to run. */
public data class Reduction(val model: EditorModel, val effects: List<Effect> = emptyList())
