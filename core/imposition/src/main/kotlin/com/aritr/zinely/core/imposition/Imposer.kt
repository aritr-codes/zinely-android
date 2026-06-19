package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.PaperSize
import com.aritr.zinely.core.model.ZineFormat

/**
 * Maps the logical pages of a zine format onto a physical sheet.
 *
 * Implementations are **pure and deterministic**: identical inputs yield identical output.
 * They throw [IllegalArgumentException] only on programmer error (an unsupported format/paper).
 */
public interface Imposer {
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
        safeAreaInsetPt: Double = 17.0,
    ): ImpositionLayout
}
