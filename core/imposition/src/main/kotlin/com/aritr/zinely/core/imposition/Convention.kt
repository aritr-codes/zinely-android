package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.GridCell
import com.aritr.zinely.core.model.PageRole
import com.aritr.zinely.core.model.Rotation

/** The axis a fold runs along on the flat sheet. */
public enum class FoldAxis { HORIZONTAL, VERTICAL }

/** Mountain/valley fold sense. Advisory for proof rendering; refined only after physical testing. */
public enum class FoldType { VALLEY, MOUNTAIN, UNSPECIFIED }

/**
 * One **named** imposition convention: a full booklet-page → (cell, rotation, role) table.
 * Each convention has its own golden table and proof snapshot — there is no implicit
 * "flip a flag" alternate (see docs/DECISIONS.md ADR-007, Codex review).
 */
public data class ConventionSpec(
    val name: String,
    val cellOf: Map<Int, GridCell>,
    val rotationOf: Map<Int, Rotation>,
    val roleOf: Map<Int, PageRole>,
)

/** Canonical conventions for the single-sheet 8-page mini-zine. */
public object SingleSheet8 {
    /**
     * The verified canonical convention from docs/RESEARCH.md R1.2
     * (NASA/Chandra diagram, corroborated by Cambridge + university guides):
     * top row printed upside-down (`5 4 3 2`), bottom row upright (`6 7 8 1`),
     * front cover at grid (1,3), back cover at (1,2).
     */
    public val TOP_ROW_ROTATED: ConventionSpec = ConventionSpec(
        name = "TOP_ROW_ROTATED",
        cellOf = mapOf(
            1 to GridCell(1, 3),
            2 to GridCell(0, 3),
            3 to GridCell(0, 2),
            4 to GridCell(0, 1),
            5 to GridCell(0, 0),
            6 to GridCell(1, 0),
            7 to GridCell(1, 1),
            8 to GridCell(1, 2),
        ),
        rotationOf = mapOf(
            1 to Rotation.NONE,
            2 to Rotation.HALF,
            3 to Rotation.HALF,
            4 to Rotation.HALF,
            5 to Rotation.HALF,
            6 to Rotation.NONE,
            7 to Rotation.NONE,
            8 to Rotation.NONE,
        ),
        roleOf = mapOf(
            1 to PageRole.FRONT_COVER,
            2 to PageRole.INTERIOR,
            3 to PageRole.INTERIOR,
            4 to PageRole.INTERIOR,
            5 to PageRole.INTERIOR,
            6 to PageRole.INTERIOR,
            7 to PageRole.INTERIOR,
            8 to PageRole.BACK_COVER,
        ),
    )
}
