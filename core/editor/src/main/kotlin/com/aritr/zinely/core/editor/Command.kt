package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument

/**
 * An undoable edit (ADR-029 §4, [ADR-005]). Each command carries a **field-level memento** — only the
 * touched fields, never a whole-document snapshot or deep clone (the R9.3 perf trap) — so
 * `invertOn(applyTo(doc)) == doc`. Pure: no Android, no I/O.
 */
public sealed interface Command {
    public fun applyTo(doc: ZineDocument): ZineDocument
    public fun invertOn(doc: ZineDocument): ZineDocument
}

/** One gesture / a11y tap / group transform: set each id's [Transform]; invert restores [before]. */
public data class TransformCommand(
    val pageIndex: Int,
    val before: Map<String, Transform>,
    val after: Map<String, Transform>,
) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument = setTransforms(doc, after)
    override fun invertOn(doc: ZineDocument): ZineDocument = setTransforms(doc, before)
    private fun setTransforms(doc: ZineDocument, map: Map<String, Transform>): ZineDocument =
        doc.mapPage(pageIndex) { page ->
            page.copy(elements = page.elements.map { e -> map[e.id]?.let(e::withTransform) ?: e })
        }
}

/** Integer-zIndex reorder; memento captures every changed rank (Codex required-fix #4). */
public data class ReorderCommand(
    val pageIndex: Int,
    val beforeZ: Map<String, Int>,
    val afterZ: Map<String, Int>,
) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument = setZ(doc, afterZ)
    override fun invertOn(doc: ZineDocument): ZineDocument = setZ(doc, beforeZ)
    private fun setZ(doc: ZineDocument, map: Map<String, Int>): ZineDocument =
        doc.mapPage(pageIndex) { page ->
            page.copy(elements = page.elements.map { e -> map[e.id]?.let(e::withZIndex) ?: e })
        }
}

/** Append a placed element (generalises to Duplicate — Codex obs); invert removes it by id. */
public data class PlaceCommand(val pageIndex: Int, val element: Element) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument =
        doc.mapPage(pageIndex) { it.copy(elements = it.elements + element) }
    override fun invertOn(doc: ZineDocument): ZineDocument =
        doc.mapPage(pageIndex) { it.copy(elements = it.elements.filterNot { e -> e.id == element.id }) }
}

/** Remove elements; the memento keeps each `(listIndex, element)` so invert re-inserts in place. */
public data class DeleteCommand(val pageIndex: Int, val removed: List<Pair<Int, Element>>) : Command {
    private val ids = removed.map { it.second.id }.toSet()
    override fun applyTo(doc: ZineDocument): ZineDocument =
        doc.mapPage(pageIndex) { it.copy(elements = it.elements.filterNot { e -> e.id in ids }) }
    override fun invertOn(doc: ZineDocument): ZineDocument =
        doc.mapPage(pageIndex) { page ->
            val list = page.elements.toMutableList()
            for ((index, element) in removed.sortedBy { it.first }) list.add(index.coerceAtMost(list.size), element)
            page.copy(elements = list)
        }
}

/** Replace a text element's content/style; field memento = the before/after element. */
public data class EditTextCommand(
    val pageIndex: Int,
    val id: String,
    val before: TextElement,
    val after: TextElement,
) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument = replace(doc, after)
    override fun invertOn(doc: ZineDocument): ZineDocument = replace(doc, before)
    private fun replace(doc: ZineDocument, value: TextElement): ZineDocument =
        doc.mapPage(pageIndex) { page ->
            page.copy(elements = page.elements.map { if (it.id == id) value else it })
        }
}

/** Insert a page; page `index` fields are renumbered to list position. Invert removes it. */
public data class AddPageCommand(val page: Page, val atIndex: Int) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument =
        doc.copy(pages = renumber(doc.pages.toMutableList().apply { add(atIndex.coerceIn(0, size), page) }))
    override fun invertOn(doc: ZineDocument): ZineDocument =
        doc.copy(pages = renumber(doc.pages.toMutableList().apply { removeAt(atIndex.coerceIn(0, lastIndex)) }))
}

/** Remove a page (keeps the full [page] + [priorSelection] for restore — Codex required-fix #8). */
public data class DeletePageCommand(
    val page: Page,
    val atIndex: Int,
    val priorSelection: Set<String>,
) : Command {
    override fun applyTo(doc: ZineDocument): ZineDocument =
        doc.copy(pages = renumber(doc.pages.toMutableList().apply { removeAt(atIndex.coerceIn(0, lastIndex)) }))
    override fun invertOn(doc: ZineDocument): ZineDocument =
        doc.copy(pages = renumber(doc.pages.toMutableList().apply { add(atIndex.coerceIn(0, size), page) }))
}
