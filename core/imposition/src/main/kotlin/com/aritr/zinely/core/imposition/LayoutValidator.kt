package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.Rotation
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Checks an [ImpositionLayout] against the engine's structural contract: panel/page/cell
 * bijections, panels that tile the sheet without overlap, the consumer-facing contentToSheet
 * transform agreeing with each panel's bounds + rotation, safe-area margins, and the fold/cut
 * topology.
 *
 * Pure and deterministic; issues are returned in a canonical order (by code, then panel). An empty
 * list means the layout is sound. This is the L1 check from the spike — no physical printer needed.
 * The transform check is **independent** of the engine (it maps panel-local corners through the
 * stored transform), so it catches a producer that shares a faulty transform assumption.
 */
public class LayoutValidator {

    private val eps = 1e-6

    public fun validate(layout: ImpositionLayout): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val sheet = layout.sheet
        val rows = layout.format.rows
        val cols = layout.format.cols
        val pageCount = layout.format.pageCount
        val sheetRect = PtRect(0.0, 0.0, sheet.width, sheet.height)

        // --- Counts & bijections ---------------------------------------------------------
        if (layout.panels.size != pageCount) {
            issues += err(
                ValidationCodes.PANEL_COUNT, IssueCategory.GEOMETRY,
                "expected $pageCount panels, found ${layout.panels.size}",
            )
        }
        val pages = layout.panels.map { it.bookletPage }
        if (pages.toSet() != (1..pageCount).toSet() || pages.size != pageCount) {
            issues += err(ValidationCodes.PAGE_NOT_BIJECTIVE, IssueCategory.TOPOLOGY, "booklet pages are not exactly 1..$pageCount: $pages")
        }
        val cells = layout.panels.map { it.cell }
        if (cells.toSet().size != cells.size) {
            issues += err(ValidationCodes.CELL_NOT_BIJECTIVE, IssueCategory.TOPOLOGY, "duplicate grid cells: $cells")
        }
        val indices = layout.panels.map { it.panelIndex }
        val indexNotUnique = indices.toSet().size != indices.size

        // --- Roles -----------------------------------------------------------------------
        val fronts = layout.panels.count { it.role == PageRole.FRONT_COVER }
        val backs = layout.panels.count { it.role == PageRole.BACK_COVER }
        if (fronts != 1 || backs != 1) {
            issues += err(
                ValidationCodes.COVER_COUNT, IssueCategory.TOPOLOGY,
                "expected exactly one front and one back cover, found front=$fronts back=$backs",
            )
        }

        // --- Per-panel geometry ----------------------------------------------------------
        for (p in layout.panels) {
            if (p.cell.row !in 0 until rows || p.cell.col !in 0 until cols) {
                issues += err(ValidationCodes.CELL_OUT_OF_GRID, IssueCategory.GEOMETRY, "cell ${p.cell} outside ${rows}x$cols grid", p.panelIndex)
            }
            val expectedIndex = p.cell.row * cols + p.cell.col
            if (p.panelIndex != expectedIndex || p.panelIndex !in 0 until rows * cols || indexNotUnique) {
                issues += err(ValidationCodes.PANEL_INDEX_INVALID, IssueCategory.GEOMETRY, "panelIndex ${p.panelIndex} != row-major ${expectedIndex} for cell ${p.cell}", p.panelIndex)
            }
            if (!within(p.bounds, sheetRect)) {
                issues += err(ValidationCodes.PANEL_OUT_OF_BOUNDS, IssueCategory.GEOMETRY, "panel bounds ${p.bounds} outside sheet ${sheet.width}x${sheet.height}", p.panelIndex)
            }
            // panel-local must be the origin-anchored panel size; clip within panel; safe within clip.
            if (!approx(p.panelLocalBounds.x, 0.0) || !approx(p.panelLocalBounds.y, 0.0) ||
                !approx(p.panelLocalBounds.width, p.bounds.width) || !approx(p.panelLocalBounds.height, p.bounds.height)
            ) {
                issues += err(ValidationCodes.PANEL_LOCAL_MISMATCH, IssueCategory.GEOMETRY, "panelLocalBounds ${p.panelLocalBounds} != (0,0,${p.bounds.width},${p.bounds.height})", p.panelIndex)
            }
            // This engine renders the full panel with no bleed, so clip must equal the panel exactly.
            // (Relax to safe ⊆ clip ⊆ panel when a bleed/clipped-content feature is introduced.)
            if (!rectEqual(p.clipLocalBounds, p.panelLocalBounds)) {
                issues += err(ValidationCodes.CLIP_NOT_IN_PANEL, IssueCategory.GEOMETRY, "clip ${p.clipLocalBounds} must equal panel ${p.panelLocalBounds}", p.panelIndex)
            }
            if (!safeMarginsOk(p, layout.safeAreaInsetPt)) {
                issues += err(ValidationCodes.SAFE_NOT_IN_PANEL, IssueCategory.GEOMETRY, "safe area ${p.safeLocalBounds} does not keep a ${layout.safeAreaInsetPt}pt margin inside ${p.panelLocalBounds}", p.panelIndex)
            }
            if (!transformConsistent(p)) {
                issues += err(ValidationCodes.TRANSFORM_BOUNDS_MISMATCH, IssueCategory.GEOMETRY, "contentToSheet does not map the panel onto bounds ${p.bounds} with rotation ${p.rotation}", p.panelIndex)
            }
        }

        // --- Overlap & tiling ------------------------------------------------------------
        val panels = layout.panels
        outer@ for (i in panels.indices) {
            for (j in i + 1 until panels.size) {
                if (overlaps(panels[i].bounds, panels[j].bounds)) {
                    issues += err(ValidationCodes.PANEL_OVERLAP, IssueCategory.GEOMETRY, "panels ${panels[i].panelIndex} and ${panels[j].panelIndex} overlap")
                    break@outer
                }
            }
        }
        val coverage = panels.sumOf { it.bounds.area }
        if (abs(coverage - sheetRect.area) > sheetRect.area * 1e-6 + eps) {
            issues += err(ValidationCodes.PANELS_DONT_TILE, IssueCategory.GEOMETRY, "panel area $coverage does not cover sheet area ${sheetRect.area}")
        }

        // --- Fold / cut topology ---------------------------------------------------------
        val foldIds = layout.foldLines.map { it.id }
        if (foldIds.toSet().size != foldIds.size) {
            issues += err(ValidationCodes.FOLD_ID_DUPLICATE, IssueCategory.TOPOLOGY, "duplicate fold ids: $foldIds")
        }
        for (fold in layout.foldLines) {
            if (fold.line.length <= eps) {
                issues += err(ValidationCodes.FOLD_DEGENERATE, IssueCategory.GEOMETRY, "fold '${fold.id}' is zero-length")
            }
            if (!inSheet(fold.line.start, sheetRect) || !inSheet(fold.line.end, sheetRect)) {
                issues += err(ValidationCodes.FOLD_OUT_OF_BOUNDS, IssueCategory.GEOMETRY, "fold '${fold.id}' ${fold.line} leaves the sheet")
            }
            val axisOk = when (fold.axis) {
                FoldAxis.HORIZONTAL -> approx(fold.line.start.y, fold.line.end.y)
                FoldAxis.VERTICAL -> approx(fold.line.start.x, fold.line.end.x)
            }
            if (!axisOk) {
                issues += err(ValidationCodes.FOLD_AXIS_MISMATCH, IssueCategory.TOPOLOGY, "fold '${fold.id}' axis ${fold.axis} disagrees with ${fold.line}")
            }
        }
        val foldById = layout.foldLines.associateBy { it.id }
        for (cut in layout.cutLines) {
            if (cut.line.length <= eps) {
                issues += err(ValidationCodes.CUT_DEGENERATE, IssueCategory.GEOMETRY, "cut ${cut.line} is zero-length")
            }
            val fold = foldById[cut.onFoldId]
            if (fold == null) {
                issues += err(ValidationCodes.CUT_FOLD_MISSING, IssueCategory.TOPOLOGY, "cut references unknown fold '${cut.onFoldId}'")
            } else if (!onSegment(cut.line.start, fold.line) || !onSegment(cut.line.end, fold.line)) {
                issues += err(ValidationCodes.CUT_NOT_ON_FOLD, IssueCategory.TOPOLOGY, "cut ${cut.line} does not lie on fold '${cut.onFoldId}' ${fold.line}")
            }
        }

        return issues.sortedWith(compareBy({ it.code }, { it.panelIndex ?: -1 }))
    }

    private fun err(code: String, category: IssueCategory, message: String, panelIndex: Int? = null) =
        ValidationIssue(code, category, Severity.ERROR, message, panelIndex)

    private fun approx(a: Double, b: Double) = abs(a - b) <= eps

    private fun rectEqual(a: PtRect, b: PtRect): Boolean =
        approx(a.x, b.x) && approx(a.y, b.y) && approx(a.width, b.width) && approx(a.height, b.height)

    private fun within(inner: PtRect, outer: PtRect): Boolean =
        inner.x >= outer.x - eps && inner.y >= outer.y - eps && inner.right <= outer.right + eps && inner.bottom <= outer.bottom + eps

    private fun inSheet(p: PtPoint, sheet: PtRect): Boolean =
        p.x >= sheet.x - eps && p.x <= sheet.right + eps && p.y >= sheet.y - eps && p.y <= sheet.bottom + eps

    /** True when the rectangles share interior area (touching edges do not count). */
    private fun overlaps(a: PtRect, b: PtRect): Boolean =
        a.x < b.right - eps && b.x < a.right - eps && a.y < b.bottom - eps && b.y < a.bottom - eps

    /** Safe rect is inside the panel AND keeps at least [inset] on every side. */
    private fun safeMarginsOk(p: PanelPlacement, inset: Double): Boolean {
        val s = p.safeLocalBounds
        val pl = p.panelLocalBounds
        return within(s, pl) &&
            s.x - pl.x >= inset - eps &&
            s.y - pl.y >= inset - eps &&
            pl.right - s.right >= inset - eps &&
            pl.bottom - s.bottom >= inset - eps
    }

    /**
     * Independent transform check: map the panel-local rectangle's corners through [contentToSheet]
     * and assert (a) their bounding box equals [PanelPlacement.bounds] and (b) the orientation matches
     * [PanelPlacement.rotation]. Does not reuse the engine's transform formula.
     */
    private fun transformConsistent(p: PanelPlacement): Boolean {
        val pl = p.panelLocalBounds
        val tl = p.contentToSheet.map(PtPoint(pl.x, pl.y))
        val tr = p.contentToSheet.map(PtPoint(pl.right, pl.y))
        val bl = p.contentToSheet.map(PtPoint(pl.x, pl.bottom))
        val br = p.contentToSheet.map(PtPoint(pl.right, pl.bottom))
        val minX = minOf(tl.x, tr.x, bl.x, br.x)
        val maxX = maxOf(tl.x, tr.x, bl.x, br.x)
        val minY = minOf(tl.y, tr.y, bl.y, br.y)
        val maxY = maxOf(tl.y, tr.y, bl.y, br.y)
        val bboxOk = approx(minX, p.bounds.x) && approx(minY, p.bounds.y) && approx(maxX, p.bounds.right) && approx(maxY, p.bounds.bottom)
        val orientationOk = when (p.rotation) {
            Rotation.NONE -> samePoint(tl, p.bounds.x, p.bounds.y) && samePoint(br, p.bounds.right, p.bounds.bottom)
            Rotation.HALF -> samePoint(tl, p.bounds.right, p.bounds.bottom) && samePoint(br, p.bounds.x, p.bounds.y)
        }
        return bboxOk && orientationOk
    }

    private fun samePoint(p: PtPoint, x: Double, y: Double) = approx(p.x, x) && approx(p.y, y)

    /** True when point [p] lies on segment [seg] within eps. */
    private fun onSegment(p: PtPoint, seg: PtLine): Boolean {
        val dx = seg.end.x - seg.start.x
        val dy = seg.end.y - seg.start.y
        val len2 = dx * dx + dy * dy
        if (len2 <= eps * eps) return abs(p.x - seg.start.x) <= eps && abs(p.y - seg.start.y) <= eps
        val cross = dx * (p.y - seg.start.y) - dy * (p.x - seg.start.x)
        if (abs(cross) / sqrt(len2) > eps) return false
        val t = ((p.x - seg.start.x) * dx + (p.y - seg.start.y) * dy) / len2
        return t >= -eps && t <= 1.0 + eps
    }
}
