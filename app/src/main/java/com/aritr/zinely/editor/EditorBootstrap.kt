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
 * Pure (Android-free) bootstrap helpers for the editor host (ADR-030 §2/§4). Kept out of the
 * `@HiltViewModel` so the seed-on-miss flow and the imposition-derived page size are unit-testable
 * with a fake [DocumentRepository] and a real [Imposer] — no Robolectric, no Hilt.
 */

/** The blank document seeded when [projectId] does not yet exist (MVP: one fixed `"default"` project). */
internal fun blankDocument(): ZineDocument = ZineDocument(
    format = ZineFormat.SINGLE_SHEET_8,
    paperSize = PaperSize.LETTER,
    // pageCount pages, all INTERIOR — the shape DocumentValidator accepts (count must match the format;
    // role is unconstrained for SINGLE_SHEET_8). Matches the data-android validDoc() fixture.
    pages = (0 until ZineFormat.SINGLE_SHEET_8.pageCount).map { Page(index = it, role = PageRole.INTERIOR) },
)

/**
 * Load the document for [projectId], seeding a blank one on first run (ADR-030 §4). The single
 * [DataError.NotFound] case is the expected new-project path: build [blankDocument], persist it, and
 * return it (so the very first autosave has a real on-disk baseline). Every other failure
 * (Corrupt / Invalid / Io / SchemaTooNew) propagates unchanged — those are real errors the host
 * surfaces as a bootstrap failure, never silently overwritten with a blank (which would destroy a
 * recoverable document).
 */
internal suspend fun bootstrapDocument(
    repository: DocumentRepository,
    projectId: String,
): DataResult<ZineDocument> =
    when (val loaded = repository.load(projectId)) {
        is DataResult.Success -> loaded
        is DataResult.Failure -> when (loaded.error) {
            is DataError.NotFound -> {
                val seed = blankDocument()
                when (val saved = repository.save(projectId, seed)) {
                    is DataResult.Success -> DataResult.Success(seed)
                    is DataResult.Failure -> saved
                }
            }
            else -> loaded
        }
    }

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
