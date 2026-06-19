package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.Rotation
import com.aritr.zinely.core.model.ZineFormat

/**
 * Where one booklet page lands on the physical sheet.
 *
 * [contentToSheet] is the **consumer contract**: content is authored upright in [panelLocalBounds]
 * (origin top-left), and a renderer maps it to sheet space purely by applying this transform —
 * it must never re-derive rotation from [rotation] + [bounds] (see docs/spikes/imposition-engine.md §1.6).
 * [rotation], [cell], and [bounds] remain for proof/debug/validation.
 */
public data class PanelPlacement(
    /** Stable zero-based physical panel id, row-major across the grid (row 0 left→right, then row 1). */
    val panelIndex: Int,
    val bookletPage: Int,
    val role: PageRole,
    val cell: GridCell,
    val bounds: PtRect,
    val rotation: Rotation,
    val panelLocalBounds: PtRect,
    val safeLocalBounds: PtRect,
    val clipLocalBounds: PtRect,
    val contentToSheet: AffineTransform2D,
)

/** A fold line on the flat sheet, identified so a [CutLine] can reference the fold it lies on. */
public data class FoldLine(
    val id: String,
    val line: PtLine,
    val axis: FoldAxis,
    val type: FoldType = FoldType.UNSPECIFIED,
)

/** The single cut, modeled as lying **on** the fold named [onFoldId] (topology, not a free line). */
public data class CutLine(
    val line: PtLine,
    val onFoldId: String,
)

/** The complete imposition for one sheet: panel placements plus fold/cut guides, all in points. */
public data class ImpositionLayout(
    val format: ZineFormat,
    val paper: PaperSize,
    val sheet: PtSize,
    val conventionName: String,
    val panels: List<PanelPlacement>,
    val foldLines: List<FoldLine>,
    val cutLines: List<CutLine>,
    val safeAreaInsetPt: Double,
)
