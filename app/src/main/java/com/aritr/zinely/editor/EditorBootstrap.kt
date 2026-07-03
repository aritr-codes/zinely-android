package com.aritr.zinely.editor

import com.aritr.zinely.core.data.repository.DataError
import com.aritr.zinely.core.data.repository.DataResult
import com.aritr.zinely.core.data.repository.DocumentRepository
import com.aritr.zinely.core.imposition.Imposer
import com.aritr.zinely.core.model.Page
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineDocument
import com.aritr.zinely.core.model.ZineFormat

/**
 * Pure (Android-free) bootstrap helpers for the editor host (ADR-030 §2/§5). Kept out of the
 * `@HiltViewModel` so the load flow and the imposition-derived page size are unit-testable with a
 * fake [DocumentRepository] and a real [Imposer] — no Robolectric, no Hilt.
 */

/** A blank SINGLE_SHEET_8 document — the shape DocumentValidator accepts (count must match the
 * format; role is unconstrained). Kept for test fixtures; production creation is the store's
 * `createProject` (ADR-042 §5) since ADR-046 §3 retired the editor's seed-on-miss. */
internal fun blankDocument(): ZineDocument = ZineDocument(
    format = ZineFormat.SINGLE_SHEET_8,
    paperSize = PaperSize.LETTER,
    pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
)

/**
 * Load the document for [projectId]. Every failure — **including [DataError.NotFound]** — propagates
 * unchanged for the host to surface as a boot error. The ADR-030 §4 seed-on-miss was retired by
 * ADR-046 §3: with Home as the start destination, the editor is only ever entered with an id that
 * came from a shelf card or a fresh `createProject`, so a missing document is a real error (and
 * silently re-creating a blank would fake-resurrect a deleted project).
 */
internal suspend fun bootstrapDocument(
    repository: DocumentRepository,
    projectId: String,
): DataResult<ZineDocument> = repository.load(projectId)

/**
 * The edited page/panel size in points, derived from imposition (ADR-030 §5 — never hardcoded).
 * Every SINGLE_SHEET_8 panel is uniform, so panel 0's local bounds is the single source of truth for
 * the editor canvas (per-page sizing is a future concern). [panelLocalBounds] is `(0,0,w,h)` by the
 * imposition contract, so width/height are the panel extent.
 */
internal fun editedPageSize(document: ZineDocument, imposer: Imposer): PtSize {
    val layout = imposer.layout(document.format, document.paperSize)
    val panel = layout.panels.first().panelLocalBounds
    return PtSize(width = panel.width, height = panel.height)
}
