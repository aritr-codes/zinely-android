package com.aritr.zinely.core.editor

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
}

internal fun Element.withZIndex(z: Int): Element = when (this) {
    is TextElement -> copy(zIndex = z)
    is ImageElement -> copy(zIndex = z)
}

/** Replace page `pageIndex` by mapping it; out-of-range index returns the document unchanged. */
internal fun ZineDocument.mapPage(pageIndex: Int, f: (Page) -> Page): ZineDocument {
    if (pageIndex !in pages.indices) return this
    return copy(pages = pages.mapIndexed { i, p -> if (i == pageIndex) f(p) else p })
}

/** Re-number every page's `index` to its list position (kept consistent after page insert/remove). */
internal fun renumber(pages: List<Page>): List<Page> = pages.mapIndexed { i, p -> p.copy(index = i) }
