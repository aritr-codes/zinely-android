package com.aritr.zinely.core.editor

import com.aritr.zinely.core.model.DecorElement
import com.aritr.zinely.core.model.Element
import com.aritr.zinely.core.model.ImageElement
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.TextElement
import com.aritr.zinely.core.model.Transform
import com.aritr.zinely.core.model.ZineDocument

/**
 * Internal pure mutation helpers over the [ZineDocument] tree (ADR-029 §3). All produce new immutable
 * copies; the concrete [Element] subtype is preserved via the exhaustive `when` (a new kind forces a
 * compile error here, by design).
 */

internal fun Element.withTransform(t: Transform): Element = when (this) {
    is TextElement -> copy(transform = t)
    is ImageElement -> copy(transform = t)
    // A supply is moved, resized and rotated exactly like the other two — everything geometric it has
    // lives in `Transform` (SUPPLIES-SPEC §2). These two arms are what make every reducer verb that
    // routes through here (Nudge, ScaleBy, RotateBy, Transform commit, Reorder) work on decor for free.
    is DecorElement -> copy(transform = t)
}

internal fun Element.withZIndex(z: Int): Element = when (this) {
    is TextElement -> copy(zIndex = z)
    is ImageElement -> copy(zIndex = z)
    is DecorElement -> copy(zIndex = z)
}

/**
 * Copy every authored property while replacing only the three identities of a new placement: durable id,
 * geometry and stack rank. Exhaustive on purpose — a future element kind must declare what duplication
 * preserves instead of silently losing one of its fields.
 */
internal fun Element.duplicateAs(id: String, transform: Transform, zIndex: Int): Element = when (this) {
    is TextElement -> copy(id = id, transform = transform, zIndex = zIndex)
    is ImageElement -> copy(id = id, transform = transform, zIndex = zIndex)
    is DecorElement -> copy(id = id, transform = transform, zIndex = zIndex)
}

/** Replace page `pageIndex` by mapping it; out-of-range index returns the document unchanged. */
internal fun ZineDocument.mapPage(pageIndex: Int, f: (Page) -> Page): ZineDocument {
    if (pageIndex !in pages.indices) return this
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) f(p) else p })
}

/** Re-number every page's `index` to its list position (kept consistent after page insert/remove). */
internal fun renumber(pages: List<Page>): List<Page> = pages.mapIndexed { i, p -> p.copy(index = i) }
