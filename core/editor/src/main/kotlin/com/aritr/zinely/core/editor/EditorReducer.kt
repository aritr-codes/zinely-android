package com.aritr.zinely.core.editor

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
            committing(model.copy(nextToken = model.nextToken + 1, selection = setOf(id)),
                PlaceCommand(model.currentPageIndex, intent.element.copy(id = id, zIndex = nextZ(model))))
        }
        is Intent.CommitText -> {
            val before = currentPage(model).elements.firstOrNull { it.id == intent.id } as? TextElement
                ?: return Reduction(model)
            // Normalise the committed element's id to the target so selection/lookups can't desync.
            committing(model, EditTextCommand(model.currentPageIndex, intent.id, before, intent.after.copy(id = intent.id)))
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
        Intent.CancelTransform -> Reduction(model.copy(interaction = Interaction.Idle))
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
        is AddPageCommand, is DeletePageCommand -> null
    }
}
