package com.aritr.zinely.core.model

/** Standard paper sizes, defined by their **portrait** dimensions in points (1/72"). */
public enum class PaperSize(public val portrait: PtSize) {
    /** US Letter: 8.5 × 11 in. */
    LETTER(PtSize(width = 612.0, height = 792.0)),

    /** A4: 210 × 297 mm ≈ 595.276 × 841.890 pt. */
    A4(PtSize(width = 595.276, height = 841.890));

    /** The landscape orientation of this paper (width/height swapped). */
    public fun landscape(): PtSize = PtSize(width = portrait.height, height = portrait.width)
}

/** A zine layout format. The grid is the flat-sheet panel arrangement. */
public enum class ZineFormat(
    public val pageCount: Int,
    public val rows: Int,
    public val cols: Int,
) {
    /** Classic single-sheet mini-zine: 8 pages on a 2×4 landscape grid, one cut, folded. */
    SINGLE_SHEET_8(pageCount = 8, rows = 2, cols = 4),
}

/** Panel rotation on the flat sheet. Only upright and half-turn are used by single-sheet zines. */
public enum class Rotation(public val degrees: Int) {
    NONE(0),
    HALF(180),
}

/** The role a booklet page plays. */
public enum class PageRole {
    FRONT_COVER,
    BACK_COVER,
    INTERIOR,
}

/** A cell in the flat-sheet grid: `row` 0 = top, `col` 0 = left. */
public data class GridCell(val row: Int, val col: Int)
