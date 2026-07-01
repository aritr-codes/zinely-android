package com.aritr.zinely.export

import com.aritr.zinely.core.imposition.ImpositionLayout
import com.aritr.zinely.core.model.ColorRgba
import com.aritr.zinely.core.model.PtLine
import com.aritr.zinely.core.model.PtRect
import com.aritr.zinely.core.render.DrawCommand
import com.aritr.zinely.core.render.FillRect
import kotlin.math.max
import kotlin.math.min

/**
 * Builds the sheet-space guide overlay (fold + cut lines) for an [ImpositionLayout] as a plain
 * `:core:render` [DrawCommand] tape — the `overlay` argument [SheetComposer][com.aritr.zinely.render.android.SheetComposer]
 * draws after the panels (ADR-039 §1/§6). Pure and Android-free, so it unit-tests headlessly.
 *
 * The single-sheet-8 grid tiles the sheet edge-to-edge, but each panel insets its content by the safe
 * area (~17 pt), so the fold/cut lines — which sit on panel **boundaries** — fall in the content-free
 * inter-panel gutters and never cross a page's artwork. Folds are a faint semi-transparent grey; the one
 * cut is coral (the mockup's cut colour), a touch bolder so "this is where you cut" reads at a glance.
 *
 * The [ADR-012] calibration ruler is intentionally absent: the edge-to-edge tiling leaves no sheet
 * margin to hold it (ADR-039 deferral). When a margin/bleed variant lands, its ruler commands append here.
 */
internal object SheetGuides {

    private const val FOLD_THICKNESS_PT = 0.75
    private const val CUT_THICKNESS_PT = 1.25
    private val FOLD_COLOR = ColorRgba(r = 107, g = 99, b = 88, a = 110)
    private val CUT_COLOR = ColorRgba(r = 231, g = 111, b = 81, a = 205)

    fun overlay(layout: ImpositionLayout): List<DrawCommand> = buildList {
        layout.foldLines.forEach { add(lineRect(it.line, FOLD_THICKNESS_PT, FOLD_COLOR)) }
        layout.cutLines.forEach { add(lineRect(it.line, CUT_THICKNESS_PT, CUT_COLOR)) }
    }

    /**
     * A thin filled rectangle centred on an axis-aligned [line]. For a vertical line the width collapses
     * to [thickness]; for a horizontal one the height does. (The imposition emits only axis-aligned
     * fold/cut lines; a diagonal would render as its bounding box, which is fine as a fallback.)
     */
    private fun lineRect(line: PtLine, thickness: Double, color: ColorRgba): FillRect {
        val half = thickness / 2.0
        val minX = min(line.start.x, line.end.x) - half
        val minY = min(line.start.y, line.end.y) - half
        val width = (max(line.start.x, line.end.x) - min(line.start.x, line.end.x)) + thickness
        val height = (max(line.start.y, line.end.y) - min(line.start.y, line.end.y)) + thickness
        return FillRect(rect = PtRect(minX, minY, width, height), color = color)
    }
}
