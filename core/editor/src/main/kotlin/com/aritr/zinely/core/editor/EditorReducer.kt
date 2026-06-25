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
        is Intent.CommitAddImage ->
            committing(model.copy(selection = setOf(intent.element.id)),
                PlaceCommand(model.currentPageIndex, intent.element.copy(zIndex = nextZ(model))))
        is Intent.CommitText -> {
            val before = currentPage(model).elements.firstOrNull { it.id == intent.id } as? TextElement
                ?: return Reduction(model)
            committing(model, EditTextCommand(model.currentPageIndex, intent.id, before, intent.after))
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
                committing(model.copy(interaction = Interaction.Idle),
                    TransformCommand(tx.pageIndex, tx.before, intent.after))
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

        // — pages —
        is Intent.GoToPage ->
            Reduction(model.copy(currentPageIndex = intent.index.coerceIn(0, model.document.pages.lastIndex)))
        Intent.AddPage -> {
            val at = model.document.pages.size
            committing(model.copy(currentPageIndex = at),
                AddPageCommand(Page(index = at, role = PageRole.INTERIOR), at))
        }
        is Intent.DeletePage -> {
            if (model.document.pages.size <= 1) Reduction(model) else {
                val at = intent.index.coerceIn(0, model.document.pages.lastIndex)
                committing(
                    model.copy(currentPageIndex = (model.currentPageIndex).coerceAtMost(model.document.pages.size - 2)),
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
        val next = model.copy(
            document = doc,
            history = history,
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
