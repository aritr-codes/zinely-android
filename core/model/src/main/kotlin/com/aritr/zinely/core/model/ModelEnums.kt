package com.aritr.zinely.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard paper sizes, defined by their **portrait** dimensions in points (1/72").
 *
 * Enum entries carry explicit `@SerialName` wire values so a Kotlin rename never silently changes
 * the persisted document (S2 spike §3; [docs/DECISIONS.md ADR-018] convention/id stability).
 */
@Serializable
public enum class PaperSize(public val portrait: PtSize) {
    /** US Letter: 8.5 × 11 in. */
    @SerialName("letter")
    LETTER(PtSize(width = 612.0, height = 792.0)),

    /** A4: 210 × 297 mm ≈ 595.276 × 841.890 pt. */
    @SerialName("a4")
    A4(PtSize(width = 595.276, height = 841.890));

    /** The landscape orientation of this paper (width/height swapped). */
    public fun landscape(): PtSize = PtSize(width = portrait.height, height = portrait.width)
}

/** A zine layout format. The grid is the flat-sheet panel arrangement. */
@Serializable
public enum class ZineFormat(
    public val pageCount: Int,
    public val rows: Int,
    public val cols: Int,
) {
    /** Classic single-sheet mini-zine: 8 pages on a 2×4 landscape grid, one cut, folded. */
    @SerialName("single_sheet_8")
    SINGLE_SHEET_8(pageCount = 8, rows = 2, cols = 4),
}

/** Panel rotation on the flat sheet. Only upright and half-turn are used by single-sheet zines. */
public enum class Rotation(public val degrees: Int) {
    NONE(0),
    HALF(180),
}

/** The role a booklet page plays. */
@Serializable
public enum class PageRole {
    @SerialName("front_cover")
    FRONT_COVER,

    @SerialName("back_cover")
    BACK_COVER,

    @SerialName("interior")
    INTERIOR,
}

/** How an image fills its element box: preserve aspect inside (FIT) or cover the box (FILL). */
@Serializable
public enum class Fit {
    @SerialName("fit")
    FIT,

    @SerialName("fill")
    FILL,
}

/** Horizontal text alignment within a text element. */
@Serializable
public enum class TextAlign {
    @SerialName("start")
    START,

    @SerialName("center")
    CENTER,

    @SerialName("end")
    END,
}

/** A cell in the flat-sheet grid: `row` 0 = top, `col` 0 = left. */
public data class GridCell(val row: Int, val col: Int)
