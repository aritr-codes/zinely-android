package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.PtSize
import com.aritr.zinely.core.model.ZineFormat

/**
 * Maps the logical pages of a zine format onto a physical sheet.
 *
 * Implementations are **pure and deterministic**: identical inputs yield identical output.
 * They throw [IllegalArgumentException] only on programmer error (an unsupported format/paper).
 */
public interface Imposer {

    public companion object {
        /**
         * The default keep-clear margin held inside every panel, in points — ≈ 6 mm
         * ([ADR-012](../../../../../../../../docs/DECISIONS.md#adr-012)).
         *
         * Published as a constant, rather than left as a literal on [layout]'s parameter, because the
         * Bench draws this boundary: `.keepclear` is the whole of the product's felt print-correctness
         * story (BP-4), and its inset is this number scaled to the page box
         * ([D-033](../../../../../../../../docs/design/V2-SPEC-DEFECTS.md#d-033)). A cue derived from a
         * *copy* of 17.0 would keep drawing the old boundary the day the real one moved — which is
         * exactly the class of defect D-033 was raised for.
         */
        public const val DEFAULT_SAFE_AREA_INSET_PT: Double = 17.0
    }

    /** The formats this imposer can lay out. */
    public val supportedFormats: Set<ZineFormat>

    /** The single, named convention this imposer applies. */
    public val convention: ConventionSpec

    /**
     * Builds the imposition for [format] on [paper], keeping content/guides inside a
     * [safeAreaInsetPt]-point safe area (default ≈ 6 mm).
     *
     * @throws IllegalArgumentException if [format] is not in [supportedFormats].
     */
    public fun layout(
        format: ZineFormat,
        paper: PaperSize,
        safeAreaInsetPt: Double = DEFAULT_SAFE_AREA_INSET_PT,
    ): ImpositionLayout = layout(format, paper.portrait, safeAreaInsetPt)

    /**
     * Builds the imposition for [format] on any sheet whose **portrait** size is [sheetPortrait].
     *
     * The engine is open over geometry and the product is closed over names
     * ([ADR-133](../../../../../../../../docs/DECISIONS.md#adr-133)): [PaperSize] is the set a person
     * can choose and a file can store, and this overload is how the engine is proved for any sheet
     * before a size joins that set.
     *
     * @throws IllegalArgumentException if [format] is not in [supportedFormats], or the sheet has a zero
     *   side.
     */
    public fun layout(
        format: ZineFormat,
        sheetPortrait: PtSize,
        safeAreaInsetPt: Double = DEFAULT_SAFE_AREA_INSET_PT,
    ): ImpositionLayout
}
