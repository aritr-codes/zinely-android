package com.aritr.zinely.core.imposition

/** Whether an issue is about metric geometry or about the fold/cut/page graph. */
public enum class IssueCategory { GEOMETRY, TOPOLOGY }

/** Severity of a validation issue. ERROR = the layout is unsafe to print/render. */
public enum class Severity { ERROR, WARNING }

/** One problem found in an [ImpositionLayout]. [panelIndex] is set when an issue is panel-specific. */
public data class ValidationIssue(
    val code: String,
    val category: IssueCategory,
    val severity: Severity,
    val message: String,
    val panelIndex: Int? = null,
)

/** Stable machine-readable codes for [ValidationIssue.code]. */
public object ValidationCodes {
    public const val PANEL_COUNT: String = "PANEL_COUNT"
    public const val PAGE_NOT_BIJECTIVE: String = "PAGE_NOT_BIJECTIVE"
    public const val CELL_NOT_BIJECTIVE: String = "CELL_NOT_BIJECTIVE"
    public const val CELL_OUT_OF_GRID: String = "CELL_OUT_OF_GRID"
    public const val PANEL_INDEX_INVALID: String = "PANEL_INDEX_INVALID"
    public const val PANEL_OUT_OF_BOUNDS: String = "PANEL_OUT_OF_BOUNDS"
    public const val PANEL_OVERLAP: String = "PANEL_OVERLAP"
    public const val PANELS_DONT_TILE: String = "PANELS_DONT_TILE"
    public const val PANEL_LOCAL_MISMATCH: String = "PANEL_LOCAL_MISMATCH"
    public const val CLIP_NOT_IN_PANEL: String = "CLIP_NOT_IN_PANEL"
    public const val TRANSFORM_BOUNDS_MISMATCH: String = "TRANSFORM_BOUNDS_MISMATCH"
    public const val SAFE_NOT_IN_PANEL: String = "SAFE_NOT_IN_PANEL"
    public const val COVER_COUNT: String = "COVER_COUNT"
    public const val FOLD_ID_DUPLICATE: String = "FOLD_ID_DUPLICATE"
    public const val FOLD_DEGENERATE: String = "FOLD_DEGENERATE"
    public const val FOLD_OUT_OF_BOUNDS: String = "FOLD_OUT_OF_BOUNDS"
    public const val FOLD_AXIS_MISMATCH: String = "FOLD_AXIS_MISMATCH"
    public const val CUT_FOLD_MISSING: String = "CUT_FOLD_MISSING"
    public const val CUT_NOT_ON_FOLD: String = "CUT_NOT_ON_FOLD"
    public const val CUT_DEGENERATE: String = "CUT_DEGENERATE"
}
