package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtPoint
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.core.model.ZineFormat
import kotlin.math.min

/**
 * Imposer for the classic single-sheet 8-page mini-zine, using the canonical
 * [SingleSheet8.TOP_ROW_ROTATED] convention (docs/RESEARCH.md R1.2).
 *
 * Pure and deterministic: it derives all geometry from the paper size and the convention table,
 * and emits each panel's [PanelPlacement.contentToSheet] transform so a renderer never re-derives
 * rotation. The sheet is laid landscape; content is authored upright in each panel's local space.
 */
public class SingleSheet8Imposer : Imposer {

    override val supportedFormats: Set<ZineFormat> = setOf(ZineFormat.SINGLE_SHEET_8)

    override val convention: ConventionSpec = SingleSheet8.TOP_ROW_ROTATED

    override fun layout(
        format: ZineFormat,
        paper: PaperSize,
        safeAreaInsetPt: Double,
    ): ImpositionLayout {
        require(format in supportedFormats) { "Unsupported format: $format" }

        val sheet = paper.landscape()
        val w = sheet.width
        val h = sheet.height
        val cols = format.cols
        val rows = format.rows
        val pw = w / cols
        val ph = h / rows

        require(safeAreaInsetPt >= 0.0) { "safeAreaInsetPt must be >= 0, was $safeAreaInsetPt" }
        require(2.0 * safeAreaInsetPt < min(pw, ph)) {
            "safeAreaInsetPt $safeAreaInsetPt too large for panel ${pw}x$ph"
        }

        val panelLocal = PtRect(0.0, 0.0, pw, ph)
        val safeLocal = PtRect(safeAreaInsetPt, safeAreaInsetPt, pw - 2 * safeAreaInsetPt, ph - 2 * safeAreaInsetPt)

        val panels = (1..format.pageCount).map { page ->
            val cell = convention.cellOf.getValue(page)
            val rotation = convention.rotationOf.getValue(page)
            val role = convention.roleOf.getValue(page)
            val bounds = PtRect(cell.col * pw, cell.row * ph, pw, ph)

            // Content authored upright in panelLocal; map to the sheet cell. For a half-turn,
            // rotate 180 about the panel center first (exact), then translate to the cell origin.
            val toCell = AffineTransform2D.translate(bounds.x, bounds.y)
            val contentToSheet = when (rotation) {
                Rotation.NONE -> toCell
                Rotation.HALF -> AffineTransform2D.halfTurnAbout(pw / 2.0, ph / 2.0).then(toCell)
            }

            PanelPlacement(
                panelIndex = cell.row * cols + cell.col,
                bookletPage = page,
                role = role,
                cell = cell,
                bounds = bounds,
                rotation = rotation,
                panelLocalBounds = panelLocal,
                safeLocalBounds = safeLocal,
                clipLocalBounds = panelLocal,
                contentToSheet = contentToSheet,
            )
        }

        val foldLines = listOf(
            FoldLine("H-center", PtLine(PtPoint(0.0, h / 2), PtPoint(w, h / 2)), FoldAxis.HORIZONTAL),
            FoldLine("V-quarter-1", PtLine(PtPoint(w / 4, 0.0), PtPoint(w / 4, h)), FoldAxis.VERTICAL),
            FoldLine("V-center", PtLine(PtPoint(w / 2, 0.0), PtPoint(w / 2, h)), FoldAxis.VERTICAL),
            FoldLine("V-quarter-3", PtLine(PtPoint(3 * w / 4, 0.0), PtPoint(3 * w / 4, h)), FoldAxis.VERTICAL),
        )

        val cutLines = listOf(
            CutLine(PtLine(PtPoint(w / 4, h / 2), PtPoint(3 * w / 4, h / 2)), onFoldId = "H-center"),
        )

        return ImpositionLayout(
            format = format,
            paper = paper,
            sheet = sheet,
            conventionName = convention.name,
            panels = panels,
            foldLines = foldLines,
            cutLines = cutLines,
            safeAreaInsetPt = safeAreaInsetPt,
        )
    }
}
