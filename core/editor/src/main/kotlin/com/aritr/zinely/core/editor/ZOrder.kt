package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.Page

/** A single-step reorder of one element within its page's z-stack (ADR-029 §3). */
public enum class ReorderOp { BRING_FORWARD, SEND_BACKWARD, TO_FRONT, TO_BACK }

/**
 * Pure z-order helpers (ADR-029 §3, Codex required-fix #4). A persisted document may carry duplicate or
 * sparse `zIndex`, and `SceneRenderer` breaks ties by **list order**. To make reorder well-defined the
 * editor [normalize]s a page on load to **dense unique** `zIndex` `0..n-1` derived from the visual order
 * `(zIndex, listIndex)`; thereafter [reorder] is a simple adjacent-rank swap on a clean total order.
 */
public object ZOrder {

    /** The element ids in back-to-front visual order (`zIndex` asc, list order breaking ties). */
    private fun visualOrder(page: Page): List<Element> =
        page.elements.withIndex()
            .sortedWith(compareBy<IndexedValue<Element>> { it.value.zIndex }.thenBy { it.index })
            .map { it.value }

    /** Rewrite every element's `zIndex` to a dense unique rank from current visual order. Idempotent. */
    public fun normalize(page: Page): Page {
        val rank = visualOrder(page).withIndex().associate { (i, e) -> e.id to i }
        return page.copy(elements = page.elements.map { it.withZIndex(rank.getValue(it.id)) })
    }

    /** Apply a single-step [op] to element [id]; assumes a normalised (dense, unique) page. */
    public fun reorder(page: Page, id: String, op: ReorderOp): Page {
        val ordered = visualOrder(page)                 // back-to-front; index == current rank
        val from = ordered.indexOfFirst { it.id == id }
        if (from < 0) return page
        val last = ordered.lastIndex
        val to = when (op) {
            ReorderOp.BRING_FORWARD -> (from + 1).coerceAtMost(last)
            ReorderOp.SEND_BACKWARD -> (from - 1).coerceAtLeast(0)
            ReorderOp.TO_FRONT -> last
            ReorderOp.TO_BACK -> 0
        }
        if (to == from) return page
        val moved = ordered.toMutableList().apply { add(to, removeAt(from)) }
        val rank = moved.withIndex().associate { (i, e) -> e.id to i }
        return page.copy(elements = page.elements.map { it.withZIndex(rank.getValue(it.id)) })
    }
}
