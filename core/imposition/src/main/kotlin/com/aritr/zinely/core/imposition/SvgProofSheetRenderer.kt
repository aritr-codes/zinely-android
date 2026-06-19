package com.aritr.zinely.core.imposition

import com.aritr.zinely.core.model.AffineTransform2D
import java.math.BigDecimal
import kotlin.math.round

/** A rendered proof sheet: a standalone SVG document plus the sheet dimensions in points. */
public data class ProofSheet(
    val svg: String,
    val widthPt: Double,
    val heightPt: Double,
)

/**
 * Renders an [ImpositionLayout] to a human-checkable SVG proof sheet — the L2 verification artifact
 * from the spike. Each panel is drawn in its own local coordinate space and placed with an SVG
 * `matrix(...)` built directly from [PanelPlacement.contentToSheet], so the proof itself exercises
 * the consumer contract: if the transform is wrong, the page number and orientation marker land
 * wrong on the proof.
 *
 * Pure and deterministic; number formatting is locale-independent (always a `.` separator).
 */
public class SvgProofSheetRenderer {

    public fun render(layout: ImpositionLayout): ProofSheet {
        val w = layout.sheet.width
        val h = layout.sheet.height
        val sb = StringBuilder()

        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
            .append("width=\"").append(fmt(w)).append("pt\" height=\"").append(fmt(h)).append("pt\" ")
            .append("viewBox=\"0 0 ").append(fmt(w)).append(' ').append(fmt(h)).append("\">\n")

        sb.append("  <title>Zinely proof — ").append(esc(layout.conventionName))
            .append(" — ").append(layout.format.name).append("</title>\n")

        // Sheet background and border.
        sb.append("  <rect x=\"0\" y=\"0\" width=\"").append(fmt(w)).append("\" height=\"").append(fmt(h))
            .append("\" fill=\"white\" stroke=\"#333\" stroke-width=\"1\"/>\n")

        // Panels, in booklet-page order for a stable, readable document.
        for (p in layout.panels.sortedBy { it.bookletPage }) {
            val pw = p.panelLocalBounds.width
            val ph = p.panelLocalBounds.height
            val cx = pw / 2.0
            sb.append("  <g transform=\"").append(matrix(p.contentToSheet)).append("\">\n")
            sb.append("    <rect class=\"panel\" x=\"0\" y=\"0\" width=\"").append(fmt(pw)).append("\" height=\"").append(fmt(ph))
                .append("\" fill=\"none\" stroke=\"#999\" stroke-width=\"0.75\"/>\n")
            sb.append("    <rect class=\"safe\" x=\"").append(fmt(p.safeLocalBounds.x)).append("\" y=\"").append(fmt(p.safeLocalBounds.y))
                .append("\" width=\"").append(fmt(p.safeLocalBounds.width)).append("\" height=\"").append(fmt(p.safeLocalBounds.height))
                .append("\" fill=\"none\" stroke=\"#3a7\" stroke-width=\"0.5\" stroke-dasharray=\"4 3\"/>\n")
            // Orientation marker: a triangle near the panel's local top, so a 180-degree panel points down.
            sb.append("    <polygon class=\"up\" points=\"")
                .append(fmt(cx)).append(',').append(fmt(ph * 0.10)).append(' ')
                .append(fmt(cx - 7)).append(',').append(fmt(ph * 0.19)).append(' ')
                .append(fmt(cx + 7)).append(',').append(fmt(ph * 0.19))
                .append("\" fill=\"#bbb\"/>\n")
            sb.append("    <text x=\"").append(fmt(cx)).append("\" y=\"").append(fmt(ph / 2.0))
                .append("\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"28\" fill=\"#222\">")
                .append(p.bookletPage).append("</text>\n")
            sb.append("    <text x=\"").append(fmt(cx)).append("\" y=\"").append(fmt(ph / 2.0 + 18))
                .append("\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"9\" fill=\"#777\">")
                .append(esc(p.role.name)).append("</text>\n")
            sb.append("  </g>\n")
        }

        // Fold guides (dashed).
        for (fold in layout.foldLines) {
            sb.append("  <line class=\"fold\" x1=\"").append(fmt(fold.line.start.x)).append("\" y1=\"").append(fmt(fold.line.start.y))
                .append("\" x2=\"").append(fmt(fold.line.end.x)).append("\" y2=\"").append(fmt(fold.line.end.y))
                .append("\" stroke=\"#39f\" stroke-width=\"0.75\" stroke-dasharray=\"6 4\"/>\n")
        }
        // Cut guide (solid red, the single slit).
        for (cut in layout.cutLines) {
            sb.append("  <line class=\"cut\" x1=\"").append(fmt(cut.line.start.x)).append("\" y1=\"").append(fmt(cut.line.start.y))
                .append("\" x2=\"").append(fmt(cut.line.end.x)).append("\" y2=\"").append(fmt(cut.line.end.y))
                .append("\" stroke=\"#e33\" stroke-width=\"1.5\"/>\n")
        }

        sb.append("</svg>\n")
        return ProofSheet(svg = sb.toString(), widthPt = w, heightPt = h)
    }

    private fun matrix(t: AffineTransform2D): String =
        "matrix(${fmt(t.a)} ${fmt(t.b)} ${fmt(t.c)} ${fmt(t.d)} ${fmt(t.e)} ${fmt(t.f)})"

    /**
     * Locale-independent number formatting: round to 3 decimals and emit a plain decimal with no
     * trailing zeros (e.g. 198.0 -> "198", 595.276 -> "595.276"). BigDecimal.toPlainString()
     * guarantees no scientific notation, making the SVG a stable golden-test contract.
     */
    private fun fmt(v: Double): String {
        val r = round(v * 1000.0) / 1000.0
        if (r == 0.0) return "0" // also collapses -0.0
        return BigDecimal.valueOf(r).stripTrailingZeros().toPlainString()
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
