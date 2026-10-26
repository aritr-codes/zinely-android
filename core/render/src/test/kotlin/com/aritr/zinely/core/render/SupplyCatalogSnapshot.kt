package com.aritr.zinely.core.render

import com.aritr.zinely.core.model.PtPoint

/** Exact text form of an outline (Double.toString round-trips), one line per id. Test-only. */
internal object SupplyCatalogSnapshot {
    private fun PtPoint.s() = "$x,$y"

    fun serialize(outlines: Map<String, SupplyOutline>): String = outlines.entries.joinToString("\n") { (id, o) ->
        id + "|" + o.subpaths.joinToString(";") { sp ->
            "M" + sp.start.s() + sp.segments.joinToString("") {
                when (it) {
                    is Segment.LineTo -> " L" + it.to.s()
                    is Segment.CubicTo -> " C" + it.c1.s() + " " + it.c2.s() + " " + it.to.s()
                }
            }
        }
    } + "\n"
}
