package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Crop
import com.aritr.zinely.core.model.Fit
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform

/**
 * The pure MVI reducer (ADR-029 §2). `reduce(model, intent)` is total, synchronous, and side-effect-free:
 * I/O is **returned** as [Effect]s (autosave, image decode, a11y announce), never performed. Determinism is
 * preserved without a clock/RNG — element ids and session tokens both draw from [EditorModel.nextToken].
 */
public object EditorReducer {

    public fun reduce(model: EditorModel, intent: Intent): Reduction = when (intent) {
        // — selection (no document mutation ⇒ no autosave) —
        is Intent.Select -> Reduction(model.copy(selection = setOfNotNull(intent.id)))
        is Intent.SelectAt -> {
            val hit = HitTest.topmostAt(currentPage(model), intent.pagePoint)
            Reduction(model.copy(selection = setOfNotNull(hit)))
        }
        Intent.ClearSelection -> Reduction(model.copy(selection = emptySet()))

        // — placement / content —
        is Intent.PlaceText -> {
            val id = "el-${model.nextToken}"
            val el = TextElement(id = id, transform = intent.transform, zIndex = nextZ(model), text = intent.text)
            committing(model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, el))
        }
        Intent.RequestAddImage -> Reduction(model, listOf(Effect.PickAndDecodeImage))
        is Intent.CommitAddImage -> {
            // Mint the id reducer-side (single source of id allocation) so it can never collide with an
            // existing element — a duplicate id would make PlaceCommand.invertOn delete BOTH matches.
            val id = "el-${model.nextToken}"
            // Placement-default policy (ADR-053 §2/§3): a newly placed photo covers its frame — Fit.FILL,
            // full crop — applied HERE at placement time, not as a render-time default flip. The model's
            // fallback stays Fit.FIT (Document.kt) so every already-saved zine renders byte-identically.
            val placed = intent.element.copy(id = id, zIndex = nextZ(model), fit = Fit.FILL)
            committing(model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, placed))
        }
        is Intent.BeginEditText -> openTextSession(model, intent.id)
        is Intent.BeginEditTextAt -> openTextSession(model, HitTest.topmostAt(currentPage(model), intent.pagePoint))
        is Intent.CommitText -> {
            val tx = model.interaction as? Interaction.EditingText
            // Stale / mismatched commit (nav, cancel, or a newer session replaced ours) ⇒ no-op (D5).
            if (tx == null || tx.token != intent.token || tx.id != intent.id) Reduction(model)
            else endTextSession(model, intent.id, intent.after)
        }
        is Intent.CancelText -> {
            val tx = model.interaction as? Interaction.EditingText
            if (tx == null || tx.token != intent.token || tx.id != intent.id) Reduction(model)
            else endTextSession(model, intent.id, after = null)
        }

        // — double-tap seam: retarget by topmost element type (ADR-053 §4) —
        is Intent.DoubleTapAt -> when (val hit = HitTest.topmostAt(currentPage(model), intent.pagePoint)
            ?.let { id -> currentPage(model).elements.firstOrNull { it.id == id } }) {
            is TextElement -> openTextSession(model, hit.id)
            is ImageElement -> openReframeSession(model, hit.id)
            else -> Reduction(model) // empty space / unknown ⇒ no-op
        }

        // — image reframe: begin/commit/cancel session + replace/reset one-shots (ADR-053) —
        is Intent.BeginReframe -> openReframeSession(model, intent.id)
        is Intent.CommitReframe -> {
            val rx = model.interaction as? Interaction.Reframing
            // Stale / mismatched commit (nav, cancel, or a newer session replaced ours) ⇒ no-op.
            if (rx == null || rx.token != intent.token || rx.id != intent.id || rx.pageIndex != model.currentPageIndex) {
                Reduction(model)
            } else if (currentPage(model).elements.none { it.id == rx.id }) {
                // The element was deleted mid-session (Delete doesn't close the interaction) ⇒ just close,
                // never push a command that matches no id (mirrors endTextSession's vanished-element bail).
                Reduction(model.copy(interaction = Interaction.Idle))
            } else {
                // Take ONLY crop/fit from the draft (clamped valid); keep before's assetId/geometry/zIndex so a
                // malformed draft can neither swap the photo nor move the element (mirrors EditTextCommand).
                val committed = rx.before.copy(crop = FramingMath.clampCrop(intent.after.crop), fit = intent.after.fit)
                val idle = model.copy(interaction = Interaction.Idle)
                if (committed == rx.before) Reduction(idle) // no change ⇒ close, no command/autosave
                else committing(idle, EditImageCommand(rx.pageIndex, rx.id, rx.before, committed))
            }
        }
        is Intent.CancelReframe -> {
            val rx = model.interaction as? Interaction.Reframing
            if (rx == null || rx.token != intent.token) Reduction(model)
            else Reduction(model.copy(interaction = Interaction.Idle))
        }
        is Intent.ReplaceImage -> {
            val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? ImageElement
            // Preserve framing (crop/fit/transform/zIndex) — only the bytes change (ADR-053 §6).
            if (el == null || el.assetId == intent.assetId) Reduction(model)
            else committing(model, EditImageCommand(model.currentPageIndex, el.id, el, el.copy(assetId = intent.assetId)))
        }
        is Intent.ResetFraming -> {
            val el = currentPage(model).elements.firstOrNull { it.id == intent.id } as? ImageElement
            val reset = el?.copy(crop = Crop.FULL, fit = Fit.FILL)
            if (el == null || reset == el) Reduction(model) // absent or already at default ⇒ no-op
            else committing(model, EditImageCommand(model.currentPageIndex, el.id, el, reset!!))
        }

        // — transform: begin/commit/cancel + a11y twins —
        is Intent.BeginTransform -> {
            val before = transformsOf(model, intent.ids)
            if (before.isEmpty()) Reduction(model) else Reduction(
                model.copy(
                    selection = intent.ids,
                    nextToken = model.nextToken + 1,
                    interaction = Interaction.Transforming(model.currentPageIndex, intent.ids, before, model.nextToken),
                ),
            )
        }
        is Intent.CommitTransform -> {
            val tx = model.interaction as? Interaction.Transforming
            if (tx == null || tx.token != intent.token || tx.pageIndex != model.currentPageIndex) {
                Reduction(model) // stale / mismatched commit ⇒ no-op (required-fix #1)
            } else {
                // Keep only ids the session actually snapshotted, so the command stays fully invertible
                // (a foreign id in `after` would have no `before` entry to restore).
                val after = intent.after.filterKeys { it in tx.before }
                committing(model.copy(interaction = Interaction.Idle),
                    TransformCommand(tx.pageIndex, tx.before, after))
            }
        }
        is Intent.CancelTransform -> {
            val tx = model.interaction as? Interaction.Transforming
            // Stale / mismatched cancel (a newer session replaced ours) ⇒ no-op, so it can't wipe a live one.
            if (tx == null || tx.token != intent.token) Reduction(model)
            else Reduction(model.copy(interaction = Interaction.Idle))
        }

        // Display-only viewport update: no autosave, no history, and selection/interaction untouched so a
        // resize/rotation mid-session can't disturb an open gesture. Idempotent — equal view ⇒ no-op.
        is Intent.SetViewport -> {
            val next = ViewState(intent.screenPxPerPt, intent.pageOffset)
            if (next == model.view) Reduction(model) else Reduction(model.copy(view = next))
        }
        is Intent.Nudge -> bakeSelection(model) { it.copy(xPt = it.xPt + intent.deltaPt.x, yPt = it.yPt + intent.deltaPt.y) }
        is Intent.ScaleBy -> bakeSelection(model) { TransformMath.bakeCentreAnchored(it, PtPoint(0.0, 0.0), intent.factor, 0.0) }
        is Intent.RotateBy -> bakeSelection(model) { it.copy(rotationDegrees = it.rotationDegrees + intent.degrees) }

        // — structure —
        is Intent.Reorder -> {
            val page = currentPage(model)
            val beforeZ = page.elements.associate { it.id to it.zIndex }
            val afterZ = ZOrder.reorder(page, intent.id, intent.op).elements.associate { it.id to it.zIndex }
            if (beforeZ == afterZ) Reduction(model)
            else committing(model, ReorderCommand(model.currentPageIndex, beforeZ, afterZ))
        }
        is Intent.Delete -> {
            val page = currentPage(model)
            val removed = page.elements.withIndex().filter { it.value.id in intent.ids }.map { it.index to it.value }
            if (removed.isEmpty()) Reduction(model)
            else committing(model.copy(selection = model.selection - intent.ids),
                DeleteCommand(model.currentPageIndex, removed))
        }

        // — pages — selection/interaction are per-page, so a page switch clears them and ends any
        // open transform session (else a stale same-index/same-token commit could hit the wrong page).
        is Intent.GoToPage ->
            Reduction(leavePage(model, intent.index.coerceIn(0, model.document.pages.lastIndex)))
        Intent.AddPage -> {
            val at = model.document.pages.size
            committing(leavePage(model, at), AddPageCommand(Page(index = at, role = PageRole.INTERIOR), at))
        }
        is Intent.DeletePage -> {
            if (model.document.pages.size <= 1) Reduction(model) else {
                val at = intent.index.coerceIn(0, model.document.pages.lastIndex)
                // If the deleted page is at/ before current, current shifts down one to follow its page.
                val shifted = if (at <= model.currentPageIndex) model.currentPageIndex - 1 else model.currentPageIndex
                val newCurrent = shifted.coerceIn(0, model.document.pages.size - 2)
                committing(
                    leavePage(model, newCurrent),
                    DeletePageCommand(model.document.pages[at], at, model.selection),
                )
            }
        }

        // — history —
        Intent.Undo -> stepHistory(model, redo = false)
        Intent.Redo -> stepHistory(model, redo = true)
    }

    // — helpers —

    private fun currentPage(model: EditorModel): Page = model.document.pages[model.currentPageIndex]

    /** Switch to [pageIndex], dropping the (per-page) selection and ending any open transform session. */
    private fun leavePage(model: EditorModel, pageIndex: Int): EditorModel =
        model.copy(currentPageIndex = pageIndex, selection = emptySet(), interaction = Interaction.Idle)

    private fun nextZ(model: EditorModel): Int = (currentPage(model).elements.maxOfOrNull { it.zIndex } ?: -1) + 1

    private fun transformsOf(model: EditorModel, ids: Set<String>): Map<String, Transform> =
        currentPage(model).elements.filter { it.id in ids }.associate { it.id to it.transform }

    /** Apply [cmd], push to undo, clear redo, and request an autosave (the only place autosave is emitted). */
    private fun committing(model: EditorModel, cmd: Command): Reduction {
        val doc = cmd.applyTo(model.document)
        val next = model.copy(
            document = doc,
            history = History(undo = model.history.undo + cmd, redo = emptyList()),
        )
        return Reduction(next, listOf(Effect.Autosave(doc)))
    }

    /** Open a text-edit session on [id] iff it names a [TextElement] on the current page; else a no-op. */
    private fun openTextSession(model: EditorModel, id: String?): Reduction {
        val el = id?.let { currentPage(model).elements.firstOrNull { e -> e.id == it } } as? TextElement
        return if (el == null) Reduction(model) else Reduction(
            model.copy(
                selection = setOf(el.id),
                nextToken = model.nextToken + 1,
                interaction = Interaction.EditingText(el.id, model.nextToken),
            ),
        )
    }

    /** Open a Reframe session on [id] iff it names an [ImageElement] on the current page; else a no-op. */
    private fun openReframeSession(model: EditorModel, id: String?): Reduction {
        val el = id?.let { currentPage(model).elements.firstOrNull { e -> e.id == it } } as? ImageElement
        return if (el == null) Reduction(model) else Reduction(
            model.copy(
                selection = setOf(el.id),
                nextToken = model.nextToken + 1,
                interaction = Interaction.Reframing(model.currentPageIndex, el.id, el, model.nextToken),
            ),
        )
    }

    /**
     * End a text-edit session (§5.6). [after] == null means **discard** (cancel); a non-null [after] is the
     * committed draft. Either way the session closes to [Interaction.Idle]. No empty `TextElement` ever
     * leaks (matches the `text.empty` warning):
     *  - **Blank result** ⇒ the box is removed. If the box was a still-blank **freshly-placed** one (its
     *    placement is the last undo step), the placement is **coalesced away** — undone and popped — so
     *    "add text, type nothing, dismiss" leaves no undo cruft. Otherwise an existing box is removed via a
     *    [DeleteCommand] (one undo restores it).
     *  - **Non-blank** ⇒ only the `text`/`style` are taken from [after]; geometry/zIndex are kept from
     *    `before` (a malformed commit can't move the element). Equal to `before` ⇒ no command/autosave.
     */
    private fun endTextSession(model: EditorModel, id: String, after: TextElement?): Reduction {
        val idle = model.copy(interaction = Interaction.Idle)
        val before = currentPage(model).elements.firstOrNull { it.id == id } as? TextElement
            ?: return Reduction(idle) // element vanished mid-session ⇒ just close
        // Cancel (after == null) keeps `before`'s text; commit takes the draft's. The box is "blank" only if
        // its RESULTING text is blank — so cancelling a box that already has text is never a delete.
        val resultText = after?.text ?: before.text
        if (resultText.isBlank()) {
            val lastPlace = model.history.undo.lastOrNull() as? PlaceCommand
            val freshBlankPlace = lastPlace != null && lastPlace.element.id == id &&
                (lastPlace.element as? TextElement)?.text?.isBlank() == true
            return if (freshBlankPlace) {
                // The box was just placed empty and never gained content ⇒ undo the placement entirely.
                val doc = lastPlace!!.invertOn(model.document)
                Reduction(
                    idle.copy(
                        document = doc,
                        selection = idle.selection - id,
                        history = model.history.copy(undo = model.history.undo.dropLast(1)),
                    ),
                    listOf(Effect.Autosave(doc)),
                )
            } else {
                val removed = currentPage(model).elements.withIndex()
                    .filter { it.value.id == id }.map { it.index to it.value }
                committing(idle.copy(selection = idle.selection - id), DeleteCommand(model.currentPageIndex, removed))
            }
        }
        // Non-blank: change only text/style; keep before's geometry/zIndex. Cancel (after == null) keeps
        // `before` verbatim ⇒ no-op. No command/autosave if nothing actually changed.
        val committed = if (after == null) before else before.copy(text = after.text, style = after.style)
        return if (committed == before) Reduction(idle)
        else committing(idle, EditTextCommand(model.currentPageIndex, id, before, committed))
    }

    /** a11y single-pointer twin: commit a per-selected-element transform via one [TransformCommand]. */
    private fun bakeSelection(model: EditorModel, f: (Transform) -> Transform): Reduction {
        val before = transformsOf(model, model.selection)
        if (before.isEmpty()) return Reduction(model)
        val after = before.mapValues { f(it.value) }
        return committing(model, TransformCommand(model.currentPageIndex, before, after))
    }

    private fun stepHistory(model: EditorModel, redo: Boolean): Reduction {
        val stack = if (redo) model.history.redo else model.history.undo
        val cmd = stack.lastOrNull() ?: return Reduction(model)
        val doc = if (redo) cmd.applyTo(model.document) else cmd.invertOn(model.document)
        val history = if (redo) {
            History(undo = model.history.undo + cmd, redo = model.history.redo.dropLast(1))
        } else {
            History(undo = model.history.undo.dropLast(1), redo = model.history.redo + cmd)
        }
        // Document-global undo/redo: if the command touched another page, navigate there + announce (Codex obs).
        val target = cmd.touchedPageIndex()?.coerceIn(0, doc.pages.lastIndex)
        val nav = target != null && target != model.currentPageIndex
        // Undoing a page delete restores the selection that page carried (Codex required-fix #8).
        val selection = if (!redo && cmd is DeletePageCommand) cmd.priorSelection else model.selection
        val next = model.copy(
            document = doc,
            history = history,
            selection = selection,
            currentPageIndex = if (nav) target!! else model.currentPageIndex.coerceIn(0, doc.pages.lastIndex),
        )
        val effects = buildList {
            add(Effect.Autosave(doc))
            if (nav) add(Effect.Announce("Changed page ${target!! + 1}"))
        }
        return Reduction(next, effects)
    }

    /** The page a command edits, for document-global undo navigation; `null` = structural/whole-doc. */
    private fun Command.touchedPageIndex(): Int? = when (this) {
        is TransformCommand -> pageIndex
        is ReorderCommand -> pageIndex
        is DeleteCommand -> pageIndex
        is PlaceCommand -> pageIndex
        is EditTextCommand -> pageIndex
        is EditImageCommand -> pageIndex
        is AddPageCommand, is DeletePageCommand -> null
    }
}
